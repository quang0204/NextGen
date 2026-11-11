package com.wedbackendservice.nextgen.entity;

import com.wedbackendservice.nextgen.enums.StorageProvider;
import com.wedbackendservice.nextgen.enums.StorageType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "product_images",
        indexes = @Index(name = "idx_product_images_object_key", columnList = "objectKey")
)
@EntityListeners(AuditingEntityListener.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long imageId;

    @ManyToOne(fetch = FetchType.LAZY)
    Product product;

    @Column(length = 500)
    String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    StorageProvider storageProvider;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    StorageType storageType;

    @Column(length = 100)
    String bucket;

    @Column(length = 500)
    String objectKey;

    @Column(length = 255)
    String originalFileName;

    @Column(length = 100)
    String contentType;

    Long fileSize;

    @Column(length = 128)
    String eTag;

    Integer displayOrder;

    @Builder.Default
    Boolean isPrimary = false;

    @CreatedDate
    LocalDateTime createdAt;

    public Integer getSortOrder() {
        return displayOrder != null ? displayOrder : 0;
    }

    public void setSortOrder(Integer sortOrder) {
        this.displayOrder = sortOrder;
    }
}
