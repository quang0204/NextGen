package com.wedbackendservice.nextgen.module.user.repository;

import com.wedbackendservice.nextgen.module.user.entity.User;
import com.wedbackendservice.nextgen.module.user.enums.Role;
import com.wedbackendservice.nextgen.module.user.enums.UserStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
  @Query(
      """
        SELECT u FROM User u
        WHERE LOWER(u.email) = LOWER(:identity)
           OR u.phone = :identity
        """)
  Optional<User> findByIdentity(@Param("identity") String identity);

  @Query(
      """
        SELECT COUNT(u) FROM User u
        WHERE LOWER(u.email) = LOWER(:email)
           OR u.phone = :phone
        """)
  long countByEmailOrPhone(@Param("email") String email, @Param("phone") String phone);

  @Query(
      """
        SELECT COUNT(u) FROM User u
        WHERE (LOWER(u.email) = LOWER(:email) OR u.phone = :phone)
          AND u.userId <> :userId
        """)
  long countByEmailOrPhoneExcludingUser(
      @Param("email") String email, @Param("phone") String phone, @Param("userId") Long userId);

  @Query("SELECT COUNT(u) FROM User u WHERE LOWER(u.email) = LOWER(:email) AND u.userId <> :userId")
  long countByEmailExcludingUser(@Param("email") String email, @Param("userId") Long userId);

  @Query("SELECT COUNT(u) FROM User u WHERE u.phone = :phone AND u.userId <> :userId")
  long countByPhoneExcludingUser(@Param("phone") String phone, @Param("userId") Long userId);

  long countByRole(Role role);

  long countByStatus(UserStatus status);

  long countByRoleAndStatus(Role role, UserStatus status);

  long countByRole(Role role);

  long countByStatus(UserStatus status);

  @Query(
      """
        SELECT u FROM User u
        WHERE (:role IS NULL OR u.role = :role)
          AND (:status IS NULL OR u.status = :status)
          AND (:kw IS NULL OR LOWER(u.fullName) LIKE :kw OR LOWER(u.email) LIKE :kw OR u.phone LIKE :kw)
        """)
  Page<User> searchUsers(
      @Param("role") Role role,
      @Param("status") UserStatus status,
      @Param("kw") String kw,
      Pageable pageable);
}
