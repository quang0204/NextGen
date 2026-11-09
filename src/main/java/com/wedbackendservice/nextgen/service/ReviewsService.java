package com.wedbackendservice.nextgen.service;

import com.wedbackendservice.nextgen.dto.client.request.review.ReviewCreateRequest;
import com.wedbackendservice.nextgen.dto.portal.request.review.ReviewReplyRequest;
import com.wedbackendservice.nextgen.dto.common.PageInfoResponse;
import com.wedbackendservice.nextgen.dto.portal.response.review.AdminReviewListResponse;
import com.wedbackendservice.nextgen.dto.client.response.review.ProductReviewListResponse;
import com.wedbackendservice.nextgen.dto.client.response.review.ReviewResponse;
import com.wedbackendservice.nextgen.entity.Order;
import com.wedbackendservice.nextgen.entity.OrderItem;
import com.wedbackendservice.nextgen.entity.Review;
import com.wedbackendservice.nextgen.entity.User;
import com.wedbackendservice.nextgen.exception.AppException;
import com.wedbackendservice.nextgen.exception.ErrorCode;
import com.wedbackendservice.nextgen.repository.OrderItemRepository;
import com.wedbackendservice.nextgen.repository.ReviewRepository;
import com.wedbackendservice.nextgen.security.SecurityHelper;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewsService {

    private static final String VISIBLE = "VISIBLE";

    private final ReviewRepository reviewRepository;
    private final OrderItemRepository orderItemRepository;
    private final SecurityHelper securityHelper;

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

        return ProductReviewListResponse.builder()
                .items(result.getContent().stream().map(this::toResponse).toList())
                .totalReviews(result.getTotalElements())
                .averageRating(average == null ? 0d : Math.round(average * 10.0) / 10.0)
                .pageInfo(PageInfoResponse.of(safePage, safeSize, result))
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

        Review review = Review.builder()
                .orderItem(item)
                .rating(request.getRating())
                .comment(request.getComment())
                .isVerifiedPurchase(true)
                .status(VISIBLE)
                .build();
        return toResponse(reviewRepository.save(review));
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

        return AdminReviewListResponse.builder()
                .items(result.getContent().stream().map(this::toResponse).toList())
                .pageInfo(PageInfoResponse.of(safePage, safeSize, result))
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
        return toResponse(reviewRepository.save(review));
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

    private ReviewResponse toResponse(Review review) {
        OrderItem item = review.getOrderItem();
        Order order = item != null ? item.getOrder() : null;
        User customer = order != null ? order.getUser() : null;

        return ReviewResponse.builder()
                .reviewId(review.getReviewId())
                .orderItemId(item != null ? item.getOrderItemId() : null)
                .productId(item != null && item.getVariant() != null && item.getVariant().getProduct() != null
                        ? item.getVariant().getProduct().getProductId() : null)
                .productName(item != null ? item.getProductNameSnapshot() : null)
                .variantName(item != null && item.getVariant() != null ? item.getVariant().getVariantName() : null)
                .customerName(customer != null ? customer.getFullName() : null)
                .rating(review.getRating())
                .comment(review.getComment())
                .isVerifiedPurchase(review.getIsVerifiedPurchase())
                .status(review.getStatus())
                .reply(review.getReply())
                .repliedAt(review.getRepliedAt())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}
