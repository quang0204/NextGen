package com.wedbackendservice.nextgen.module.product.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "brands")
@EntityListeners(AuditingEntityListener.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Brand {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long brandId;

  String brandName;

  @Column(name = "logo_url", columnDefinition = "TEXT")
  String logoUrl;

  String slug;
  String description;

  @Builder.Default Boolean isActive = true;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "category_brands",
      joinColumns = @JoinColumn(name = "brand_id"),
      inverseJoinColumns = @JoinColumn(name = "category_id"))
  @Builder.Default
  Set<Category> categories = new HashSet<>();

  @CreatedDate LocalDateTime createdAt;

  @LastModifiedDate LocalDateTime updatedAt;
}
