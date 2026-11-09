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
@Table(name = "product_specs")
@EntityListeners(AuditingEntityListener.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductSpec {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long specId;

    @ManyToOne(fetch = FetchType.LAZY)
    Product product;

    @Column(name = "group_name")
    String groupName;

    String specName;
    String specValue;

    @Column(name = "sort_order")
    Integer sortOrder;

    @CreatedDate
    LocalDateTime createdAt;
}
