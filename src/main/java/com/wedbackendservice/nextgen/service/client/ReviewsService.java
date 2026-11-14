package com.wedbackendservice.nextgen.service.client;

import com.wedbackendservice.nextgen.dto.client.request.review.ReviewCreateRequest;
import com.wedbackendservice.nextgen.dto.portal.request.review.ReviewReplyRequest;
import com.wedbackendservice.nextgen.dto.common.PageResponse;
import com.wedbackendservice.nextgen.dto.portal.response.review.AdminReviewListResponse;
import com.wedbackendservice.nextgen.dto.client.response.review.ProductReviewListResponse;
import com.wedbackendservice.nextgen.dto.client.response.review.ReviewResponse;
import com.wedbackendservice.nextgen.dto.session.UploadSessionData;
import com.wedbackendservice.nextgen.entity.Order;
import com.wedbackendservice.nextgen.entity.OrderItem;
import com.wedbackendservice.nextgen.entity.Review;
import com.wedbackendservice.nextgen.entity.ReviewImage;
import com.wedbackendservice.nextgen.entity.User;
import com.wedbackendservice.nextgen.enums.StorageType;
import com.wedbackendservice.nextgen.exception.AppException;
import com.wedbackendservice.nextgen.exception.ErrorCode;
import com.wedbackendservice.nextgen.mapper.ReviewMapper;
import com.wedbackendservice.nextgen.repository.OrderItemRepository;
import com.wedbackendservice.nextgen.repository.ReviewImageRepository;
import com.wedbackendservice.nextgen.repository.ReviewRepository;
import com.wedbackendservice.nextgen.security.SecurityHelper;
import com.wedbackendservice.nextgen.service.shared.MinioStorageService;
import com.wedbackendservice.nextgen.service.shared.StorageFileService;
import com.wedbackendservice.nextgen.service.shared.TokenRedisService;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewsService {

    private static final String VISIBLE = "VISIBLE";

    private final ReviewRepository reviewRepository;
    private final OrderItemRepository orderItemRepository;
    private final SecurityHelper securityHelper;
    private final ReviewImageRepository reviewImageRepository;
    private final TokenRedisService tokenRedisService;
    private final MinioStorageService minioStorageService;
    private final RedisTemplate<String, String> redisTemplate;
    private final ReviewMapper reviewMapper;

    /** Thời gian tối đa giữ chỗ ảnh trong lúc lưu đánh giá (bình thường được nhả ngay khi lưu xong). */
    private static final Duration CLAIM_TTL = Duration.ofMinutes(5);

    /**
     * API 12.1: xem đánh giá công khai của sản phẩm.
     */
    @Transactional(readOnly = true)
    public ProductReviewListResponse getProductReviews(Long productId, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Review> result = reviewRepository
                .findByOrderItem_Variant_Product_ProductIdAndStatus(productId, VISIBLE, pageable);
        Double average = reviewRepository.averageRatingByProductId(productId);

        Map<Long, List<String>> images = loadImageUrls(result.getContent());
        return ProductReviewListResponse.builder()
                .items(result.getContent().stream().map(r -> reviewMapper.toResponse(r, images.getOrDefault(r.getReviewId(), List.of()))).toList())
                .totalReviews(result.getTotalElements())
                .averageRating(average == null ? 0d : Math.round(average * 10.0) / 10.0)
                .pageInfo(PageResponse.of(safePage, safeSize, result))
                .build();
    }

    /**
     * API 12.2: gửi đánh giá cho sản phẩm đã mua.
     */
    @Transactional
    public ReviewResponse createReview(ReviewCreateRequest request) {
        User user = securityHelper.getCurrentUser();
        OrderItem item = orderItemRepository.findById(request.getOrderItemId())
                .orElseThrow(() -> new AppException(ErrorCode.ERR_ORDER_NOT_FOUND));
        Order order = item.getOrder();

        if (order == null || order.getUser() == null
                || !user.getUserId().equals(order.getUser().getUserId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        if (!"COMPLETED".equalsIgnoreCase(order.getStatus())
                && !"DELIVERED".equalsIgnoreCase(order.getStatus())) {
            throw new AppException(ErrorCode.ERR_REVIEW_NOT_ALLOWED);
        }
        if (reviewRepository.existsByOrderItem_OrderItemId(item.getOrderItemId())) {
            throw new AppException(ErrorCode.ERR_REVIEW_ALREADY_EXISTS);
        }

        List<String> uploadIds = normalizeUploadIds(request.getUploadIds());
        // Giữ chỗ các ảnh trong lúc lưu: 1 ảnh không thể gắn vào 2 đánh giá gửi cùng lúc
        claimUploads(uploadIds);
        boolean releaseOnCompletion = releaseClaimsAfterCompletion(uploadIds);
        try {
            return saveReview(item, request, uploadIds, user);
        } finally {
            if (!releaseOnCompletion) {
                releaseClaims(uploadIds);
            }
        }
    }

    private ReviewResponse saveReview(OrderItem item, ReviewCreateRequest request, List<String> uploadIds, User user) {
        // Kiểm tra ảnh trước khi lưu: chỉ cần 1 ảnh chưa lên được S3 là cả đánh giá không thành công
        List<UploadSessionData> uploads = resolveReviewUploads(uploadIds, user);

        Review saved = reviewRepository.save(Review.builder()
                .orderItem(item)
                .rating(request.getRating())
                .comment(request.getComment())
                .isVerifiedPurchase(true)
                .status(VISIBLE)
                .build());

        List<ReviewImage> images = new ArrayList<>(uploads.size());
        int displayOrder = 1;
        for (UploadSessionData upload : uploads) {
            images.add(ReviewImage.builder()
                    .review(saved)
                    .imageUrl(minioStorageService.getFileUrl(upload.getBucket(), upload.getObjectKey()))
                    .bucket(upload.getBucket())
                    .objectKey(upload.getObjectKey())
                    .contentType(upload.getContentType())
                    .fileSize(upload.getActualSize() != null ? upload.getActualSize() : upload.getExpectedSize())
                    .displayOrder(displayOrder++)
                    .build());
        }
        if (!images.isEmpty()) {
            reviewImageRepository.saveAll(images);
            deleteUploadSessionsAfterCommit(uploads);
        }

        return reviewMapper.toResponse(saved, images.stream().map(this::toViewUrl).toList());
    }

    /**
     * API 12.3: quản lý đánh giá (STAFF / ADMIN).
     */
    @Transactional(readOnly = true)
    public AdminReviewListResponse getAdminReviews(int page, int size, String status, Integer rating, String search) {
        securityHelper.authenticateStaffOrAdmin();

        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Review> result = reviewRepository.findAll(
                buildSpecification(status, rating, search), pageable);

        Map<Long, List<String>> images = loadImageUrls(result.getContent());
        return AdminReviewListResponse.builder()
                .items(result.getContent().stream().map(r -> reviewMapper.toResponse(r, images.getOrDefault(r.getReviewId(), List.of()))).toList())
                .pageInfo(PageResponse.of(safePage, safeSize, result))
                .build();
    }

    /**
     * API 12.3 phụ: cửa hàng phản hồi đánh giá.
     */
    @Transactional
    public ReviewResponse replyReview(Long reviewId, ReviewReplyRequest request) {
        User admin = securityHelper.authenticateStaffOrAdmin();
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new AppException(ErrorCode.ERR_REVIEW_NOT_FOUND));

        review.setReply(request.getReply());
        review.setRepliedAt(LocalDateTime.now());
        log.info("User {} replied review {}", admin.getUserId(), reviewId);
        Review saved = reviewRepository.save(review);
        return reviewMapper.toResponse(saved, loadImageUrls(List.of(saved)).getOrDefault(saved.getReviewId(), List.of()));
    }

    /**
     * API 12.3 phụ: Ẩn / Hiện đánh giá (VISIBLE <-> HIDDEN).
     */
    @Transactional
    public ReviewResponse updateReviewStatus(Long reviewId, String requestedStatus) {
        User admin = securityHelper.authenticateStaffOrAdmin();
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new AppException(ErrorCode.ERR_REVIEW_NOT_FOUND));

        String nextStatus;
        if (requestedStatus != null && !requestedStatus.isBlank()) {
            nextStatus = "HIDDEN".equalsIgnoreCase(requestedStatus.trim()) ? "HIDDEN" : VISIBLE;
        } else {
            nextStatus = "HIDDEN".equalsIgnoreCase(review.getStatus()) ? VISIBLE : "HIDDEN";
        }
        review.setStatus(nextStatus);
        log.info("User {} updated review {} status to {}", admin.getUserId(), reviewId, nextStatus);
        Review saved = reviewRepository.save(review);
        return reviewMapper.toResponse(saved, loadImageUrls(List.of(saved)).getOrDefault(saved.getReviewId(), List.of()));
    }

    private Specification<Review> buildSpecification(String status, Integer rating, String search) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("status")), status.trim().toUpperCase()));
            }
            if (rating != null) {
                predicates.add(cb.equal(root.get("rating"), rating));
            }
            if (search != null && !search.isBlank()) {
                query.distinct(true);
                Join<Review, OrderItem> itemJoin = root.join("orderItem", JoinType.LEFT);
                Join<OrderItem, Order> orderJoin = itemJoin.join("order", JoinType.LEFT);
                Join<Order, User> userJoin = orderJoin.join("user", JoinType.LEFT);
                String like = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(itemJoin.get("productNameSnapshot")), like),
                        cb.like(cb.lower(userJoin.get("fullName")), like),
                        cb.like(cb.lower(root.get("comment")), like)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Kiểm tra các uploadId ảnh đánh giá: tối đa 5 ảnh, phiên còn hạn, đúng người tải, đúng loại REVIEWS,
     * đã confirm và file đang có thật trên MinIO / S3.
     */
    private List<UploadSessionData> resolveReviewUploads(List<String> ids, User user) {
        List<UploadSessionData> sessions = new ArrayList<>(ids.size());
        for (String uploadId : ids) {
            UploadSessionData session = tokenRedisService.getUploadSession(uploadId);
            if (session == null) {
                throw new AppException(ErrorCode.STORAGE_SESSION_NOT_FOUND,
                        "Phiên tải ảnh '" + uploadId + "' không tồn tại hoặc đã hết hạn.");
            }
            if (!Objects.equals(session.getOwnerId(), user.getUserId())) {
                throw new AppException(ErrorCode.UNAUTHORIZED, "Ảnh này không phải do bạn tải lên.");
            }
            if (!StorageType.REVIEWS.name().equalsIgnoreCase(session.getStorageType())) {
                throw new AppException(ErrorCode.STORAGE_SESSION_INVALID, "Ảnh không phải loại ảnh đánh giá (REVIEWS).");
            }
            if (!UploadSessionData.STATUS_COMPLETED.equalsIgnoreCase(session.getStatus())) {
                throw new AppException(ErrorCode.STORAGE_SESSION_INVALID,
                        "Ảnh chưa được xác nhận tải lên (gọi /api/v1/storage/confirm trước).");
            }
            // Ảnh phải thực sự có trên MinIO / S3; không kết nối được thì statObject báo lỗi và đánh giá cũng không lưu
            Optional<MinioStorageService.ObjectMetadata> stat =
                    minioStorageService.statObject(session.getBucket(), session.getObjectKey());
            if (stat.isEmpty()) {
                throw new AppException(ErrorCode.STORAGE_OBJECT_NOT_UPLOADED);
            }
            sessions.add(session);
        }
        return sessions;
    }

    /** Bỏ uploadId rỗng / trùng; quá 5 ảnh thì báo lỗi. */
    private List<String> normalizeUploadIds(List<String> uploadIds) {
        if (uploadIds == null || uploadIds.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        for (String id : uploadIds) {
            if (id != null && !id.isBlank()) {
                ids.add(id.trim());
            }
        }
        if (ids.size() > StorageFileService.MAX_REVIEW_IMAGES) {
            throw new AppException(ErrorCode.STORAGE_MAX_FILES_EXCEEDED,
                    "Mỗi đánh giá chỉ được gửi tối đa " + StorageFileService.MAX_REVIEW_IMAGES + " ảnh.");
        }
        return List.copyOf(ids);
    }

    /**
     * Đánh dấu trong Redis rằng các ảnh đang được gửi kèm đánh giá này (SET NX).
     * Ảnh nào đã bị một yêu cầu khác giữ thì báo lỗi, và nhả các ảnh đã giữ.
     */
    private void claimUploads(List<String> uploadIds) {
        List<String> claimed = new ArrayList<>(uploadIds.size());
        for (String uploadId : uploadIds) {
            if (!tokenRedisService.setIfAbsent(StorageFileService.REVIEW_CLAIM_PREFIX + uploadId, "1", CLAIM_TTL)) {
                releaseClaims(claimed);
                throw new AppException(ErrorCode.STORAGE_SESSION_INVALID,
                        "Ảnh '" + uploadId + "' đang được gửi kèm một đánh giá khác.");
            }
            claimed.add(uploadId);
        }
    }

    /** Nhả giữ chỗ khi transaction kết thúc (lưu xong hoặc lỗi). Trả false nếu không có transaction. */
    private boolean releaseClaimsAfterCompletion(List<String> uploadIds) {
        if (uploadIds.isEmpty() || !TransactionSynchronizationManager.isSynchronizationActive()) {
            return false;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                releaseClaims(uploadIds);
            }
        });
        return true;
    }

    private void releaseClaims(List<String> uploadIds) {
        if (uploadIds.isEmpty()) {
            return;
        }
        try {
            redisTemplate.delete(uploadIds.stream().map(id -> StorageFileService.REVIEW_CLAIM_PREFIX + id).toList());
        } catch (Exception e) {
            log.warn("Không thể nhả giữ chỗ ảnh đánh giá {}: {}", uploadIds, e.getMessage());
        }
    }

    /** Xóa phiên upload trong Redis sau khi đánh giá đã lưu, để uploadId không dùng lại được. */
    private void deleteUploadSessionsAfterCommit(List<UploadSessionData> uploads) {
        Runnable cleanup = () -> uploads.forEach(u -> {
            try {
                tokenRedisService.deleteUploadSession(u.getUploadId());
                tokenRedisService.removePendingUpload(u.getUploadId());
            } catch (Exception e) {
                log.warn("Không thể xóa phiên tải ảnh {}: {}", u.getUploadId(), e.getMessage());
            }
        });
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    cleanup.run();
                }
            });
        } else {
            cleanup.run();
        }
    }

    /** Ảnh của nhiều đánh giá, lấy 1 lần: reviewId -> danh sách link xem được. */
    private Map<Long, List<String>> loadImageUrls(List<Review> reviews) {
        List<Long> ids = reviews.stream().map(Review::getReviewId).filter(Objects::nonNull).toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return reviewImageRepository.findByReview_ReviewIdInOrderByDisplayOrderAsc(ids).stream()
                .filter(img -> img.getReview() != null)
                .collect(Collectors.groupingBy(img -> img.getReview().getReviewId(),
                        Collectors.mapping(this::toViewUrl, Collectors.toList())));
    }

    /** Link lưu DB -> link xem được (bucket riêng tư thì cấp presigned GET). */
    private String toViewUrl(ReviewImage image) {
        if (image.getBucket() != null && image.getObjectKey() != null) {
            try {
                return minioStorageService.getAccessUrl(image.getBucket(), image.getObjectKey());
            } catch (Exception e) {
                log.warn("Không tạo được link xem ảnh đánh giá {}: {}", image.getImageId(), e.getMessage());
            }
        }
        return image.getImageUrl();
    }
}
