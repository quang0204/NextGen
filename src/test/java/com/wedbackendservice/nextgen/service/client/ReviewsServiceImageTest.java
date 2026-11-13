package com.wedbackendservice.nextgen.service.client;

import com.wedbackendservice.nextgen.dto.client.request.review.ReviewCreateRequest;
import com.wedbackendservice.nextgen.dto.client.response.review.ProductReviewListResponse;
import com.wedbackendservice.nextgen.dto.client.response.review.ReviewResponse;
import com.wedbackendservice.nextgen.dto.portal.response.review.AdminReviewListResponse;
import com.wedbackendservice.nextgen.dto.session.UploadSessionData;
import com.wedbackendservice.nextgen.entity.Order;
import com.wedbackendservice.nextgen.entity.OrderItem;
import com.wedbackendservice.nextgen.entity.Review;
import com.wedbackendservice.nextgen.entity.ReviewImage;
import com.wedbackendservice.nextgen.entity.User;
import com.wedbackendservice.nextgen.enums.Role;
import com.wedbackendservice.nextgen.exception.AppException;
import com.wedbackendservice.nextgen.exception.ErrorCode;
import com.wedbackendservice.nextgen.repository.OrderItemRepository;
import com.wedbackendservice.nextgen.repository.ReviewImageRepository;
import com.wedbackendservice.nextgen.repository.ReviewRepository;
import com.wedbackendservice.nextgen.security.SecurityHelper;
import com.wedbackendservice.nextgen.service.shared.MinioStorageService;
import com.wedbackendservice.nextgen.service.shared.TokenRedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.Collection;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** API 12.1 / 12.2 / 12.3 với ảnh lưu ở bảng review_images. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReviewsServiceImageTest {

    private static final String BASE = "http://localhost:9000/reviews/";

    @Mock private ReviewRepository reviewRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private SecurityHelper securityHelper;
    @Mock private ReviewImageRepository reviewImageRepository;
    @Mock private TokenRedisService tokenRedisService;
    @Mock private MinioStorageService minioStorageService;
    @Mock private RedisTemplate<String, String> redisTemplate;

    @InjectMocks
    private ReviewsService reviewsService;

    private OrderItem orderItem;

    @BeforeEach
    void setUp() {
        User customer = User.builder().userId(7L).fullName("Trần Thị Hoa").role(Role.CUSTOMER).build();
        Order order = Order.builder().orderId(3L).user(customer).status("COMPLETED").build();
        orderItem = OrderItem.builder().orderItemId(11L).order(order).build();

        when(securityHelper.getCurrentUser()).thenReturn(customer);
        when(orderItemRepository.findById(11L)).thenReturn(Optional.of(orderItem));
        when(reviewRepository.existsByOrderItem_OrderItemId(11L)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> {
            Review r = inv.getArgument(0);
            r.setReviewId(99L);
            return r;
        });
        when(minioStorageService.statObject(eq("reviews"), anyString()))
                .thenReturn(Optional.of(new MinioStorageService.ObjectMetadata(2048L, "etag", "image/jpeg")));
        when(minioStorageService.getFileUrl(eq("reviews"), anyString())).thenAnswer(inv -> BASE + inv.getArgument(1));
        when(minioStorageService.getAccessUrl(eq("reviews"), anyString())).thenAnswer(inv -> BASE + inv.getArgument(1));
        when(tokenRedisService.setIfAbsent(startsWith("storage:review-claim:"), anyString(), any(Duration.class))).thenReturn(true);
    }

    private UploadSessionData session(String id, Long owner, String type, String status) {
        return UploadSessionData.builder().uploadId(id).ownerId(owner).storageType(type).status(status)
                .bucket("reviews").objectKey("users/7/" + id + "-anh.jpg").contentType("image/jpeg").actualSize(2048L)
                .build();
    }

    private ReviewCreateRequest request(List<String> uploadIds) {
        return ReviewCreateRequest.builder().orderItemId(11L).rating(5).comment("Máy dùng rất tốt").uploadIds(uploadIds).build();
    }

    @Test
    @SuppressWarnings("unchecked")
    void createReview_savesOneRowPerImageInReviewImages() {
        when(tokenRedisService.getUploadSession("u1")).thenReturn(session("u1", 7L, "REVIEWS", "COMPLETED"));
        when(tokenRedisService.getUploadSession("u2")).thenReturn(session("u2", 7L, "REVIEWS", "COMPLETED"));

        ReviewResponse response = reviewsService.createReview(request(List.of("u1", "u2", "u1")));

        ArgumentCaptor<List<ReviewImage>> captor = ArgumentCaptor.forClass(List.class);
        verify(reviewImageRepository).saveAll(captor.capture());
        List<ReviewImage> saved = captor.getValue();
        assertThat(saved).hasSize(2);
        assertThat(saved.get(0).getImageUrl()).isEqualTo(BASE + "users/7/u1-anh.jpg");
        assertThat(saved.get(0).getDisplayOrder()).isEqualTo(1);
        assertThat(saved.get(1).getDisplayOrder()).isEqualTo(2);
        assertThat(saved.get(0).getReview().getReviewId()).isEqualTo(99L);
        assertThat(response.getImageUrls()).containsExactly(BASE + "users/7/u1-anh.jpg", BASE + "users/7/u2-anh.jpg");
        verify(tokenRedisService).deleteUploadSession("u1");
        verify(tokenRedisService).deleteUploadSession("u2");
        verify(tokenRedisService).removePendingUpload("u1");
        verify(redisTemplate).delete(List.of("storage:review-claim:u1", "storage:review-claim:u2"));
    }

    @Test
    void createReview_withoutImages_savesNoImageRows() {
        ReviewResponse response = reviewsService.createReview(request(null));

        verify(reviewImageRepository, never()).saveAll(any());
        assertThat(response.getImageUrls()).isEmpty();
    }

    @Test
    void createReview_moreThanFiveImages_isRejected() {
        assertThatThrownBy(() -> reviewsService.createReview(request(List.of("a", "b", "c", "d", "e", "f"))))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STORAGE_MAX_FILES_EXCEEDED);
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void createReview_imageNotOnS3_isNotSaved() {
        when(tokenRedisService.getUploadSession("u1")).thenReturn(session("u1", 7L, "REVIEWS", "COMPLETED"));
        when(minioStorageService.statObject("reviews", "users/7/u1-anh.jpg")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewsService.createReview(request(List.of("u1"))))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STORAGE_OBJECT_NOT_UPLOADED);
        verify(reviewRepository, never()).save(any());
        verify(reviewImageRepository, never()).saveAll(any());
        // lỗi thì nhả giữ chỗ để khách gửi lại được
        verify(redisTemplate).delete(List.of("storage:review-claim:u1"));
    }

    @Test
    void createReview_imageAlreadyUsedByAnotherPendingReview_isRejected() {
        when(tokenRedisService.getUploadSession(anyString()))
                .thenAnswer(inv -> session(inv.getArgument(0), 7L, "REVIEWS", "COMPLETED"));
        when(tokenRedisService.setIfAbsent(eq("storage:review-claim:u2"), anyString(), any(Duration.class))).thenReturn(false);

        assertThatThrownBy(() -> reviewsService.createReview(request(List.of("u1", "u2"))))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STORAGE_SESSION_INVALID);
        verify(reviewRepository, never()).save(any());
        // chỉ nhả ảnh u1 mà yêu cầu này đã giữ, không đụng vào u2 của yêu cầu kia
        verify(redisTemplate).delete(List.of("storage:review-claim:u1"));
        verify(redisTemplate, never()).delete(argThat((Collection<String> keys) -> keys.contains("storage:review-claim:u2")));
    }

    @Test
    void createReview_imageNotConfirmed_isNotSaved() {
        when(tokenRedisService.getUploadSession("u1")).thenReturn(session("u1", 7L, "REVIEWS", "PENDING"));

        assertThatThrownBy(() -> reviewsService.createReview(request(List.of("u1"))))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STORAGE_SESSION_INVALID);
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void createReview_imageOfAnotherUser_isRejected() {
        when(tokenRedisService.getUploadSession("u1")).thenReturn(session("u1", 8L, "REVIEWS", "COMPLETED"));

        assertThatThrownBy(() -> reviewsService.createReview(request(List.of("u1"))))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED);
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void createReview_productImageSession_isRejected() {
        when(tokenRedisService.getUploadSession("u1")).thenReturn(session("u1", 7L, "PRODUCTS", "COMPLETED"));

        assertThatThrownBy(() -> reviewsService.createReview(request(List.of("u1"))))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STORAGE_SESSION_INVALID);
    }

    @Test
    @SuppressWarnings("unchecked")
    void productAndAdminLists_returnImagesFromReviewImagesTable() {
        Review review = Review.builder().reviewId(99L).orderItem(orderItem).rating(5).comment("Rất tốt").status("VISIBLE").build();
        ReviewImage image = ReviewImage.builder().imageId(1L).review(review).bucket("reviews")
                .objectKey("users/7/u1-anh.jpg").imageUrl(BASE + "users/7/u1-anh.jpg").displayOrder(1).build();
        when(reviewImageRepository.findByReview_ReviewIdInOrderByDisplayOrderAsc(List.of(99L))).thenReturn(List.of(image));
        when(reviewRepository.findByOrderItem_Variant_Product_ProductIdAndStatus(eq(1L), eq("VISIBLE"), any(Pageable.class)))
                .thenAnswer(inv -> new PageImpl<>(List.of(review), inv.getArgument(2), 1));
        when(reviewRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenAnswer(inv -> new PageImpl<>(List.of(review), inv.getArgument(1), 1));
        when(reviewRepository.averageRatingByProductId(1L)).thenReturn(5.0);

        ProductReviewListResponse publicList = reviewsService.getProductReviews(1L, 1, 10);
        assertThat(publicList.getItems().get(0).getImageUrls()).containsExactly(BASE + "users/7/u1-anh.jpg");

        AdminReviewListResponse adminList = reviewsService.getAdminReviews(1, 10, null, null, null);
        assertThat(adminList.getItems().get(0).getImageUrls()).containsExactly(BASE + "users/7/u1-anh.jpg");
    }
}
