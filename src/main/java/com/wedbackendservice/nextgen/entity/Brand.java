package com.wedbackendservice.nextgen.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
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

  @CreatedDate LocalDateTime createdAt;

  @LastModifiedDate LocalDateTime updatedAt;
}
