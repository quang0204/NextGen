package com.wedbackendservice.nextgen.entity;

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
@Table(name = "product_images")
@EntityListeners(AuditingEntityListener.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long imageId;

    @ManyToOne(fetch = FetchType.LAZY)
    Product product;

    @Column(length = 1000)
    String imageUrl;
    Integer displayOrder;

    @Builder.Default
    Boolean isPrimary = false;

    @Column(length = 100)
    String bucket;

    @Column(length = 500)
    String objectKey;

    String originalFileName;

    @Column(length = 100)
    String contentType;

    Long fileSize;

    @Column(length = 100)
    String etag;

    @CreatedDate
    LocalDateTime createdAt;

    public Integer getSortOrder() {
        return displayOrder != null ? displayOrder : 0;
    }

    public void setSortOrder(Integer sortOrder) {
        this.displayOrder = sortOrder;
    }
}
