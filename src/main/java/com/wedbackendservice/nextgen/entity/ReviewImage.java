package com.wedbackendservice.nextgen.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Ảnh của một đánh giá (API 12.1 / 12.2 / 12.3), lưu trên MinIO / S3 bucket "reviews".
 * Mỗi đánh giá tối đa 5 ảnh.
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "review_images")
@EntityListeners(AuditingEntityListener.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReviewImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long imageId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_review_images_review"))
    Review review;

    String imageUrl;
    String bucket;
    String objectKey;
    String contentType;

    Long fileSize;

    Integer displayOrder;

    @CreatedDate
    LocalDateTime createdAt;
}
