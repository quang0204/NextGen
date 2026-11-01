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
@Table(name = "wishlists")
@EntityListeners(AuditingEntityListener.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Wishlist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long wishlistId;

    @ManyToOne(fetch = FetchType.LAZY)
    User user;

    @ManyToOne(fetch = FetchType.LAZY)
    Product product;

    @CreatedDate
    LocalDateTime createdAt;
}
