package com.wedbackendservice.nextgen.module.user.entity;

import com.wedbackendservice.nextgen.module.user.enums.Role;
import com.wedbackendservice.nextgen.module.user.enums.UserStatus;
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
@Table(
    name = "users",
    uniqueConstraints = {
      @UniqueConstraint(name = "uk_users_email", columnNames = "email"),
      @UniqueConstraint(name = "uk_users_phone", columnNames = "phone")
    })
@EntityListeners(AuditingEntityListener.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long userId;

  @Enumerated(EnumType.STRING)
  Role role;

  String fullName;

  @Column(unique = true, nullable = false)
  String email;

  @Column(unique = true)
  String phone;

  String passwordHash;

  @Enumerated(EnumType.STRING)
  UserStatus status;

  String lockReason;

  @Builder.Default
  @Column(name = "token_version", columnDefinition = "BIGINT NOT NULL DEFAULT 0")
  Long tokenVersion = 0L;

  @CreatedDate LocalDateTime createdAt;
  @LastModifiedDate LocalDateTime updatedAt;

  public long getEffectiveTokenVersion() {
    return tokenVersion != null ? tokenVersion : 0L;
  }

  public long bumpTokenVersion() {
    long next = getEffectiveTokenVersion() + 1L;
    this.tokenVersion = next;
    return next;
  }
}
