package com.wedbackendservice.nextgen.repository;

import com.wedbackendservice.nextgen.entity.User;
import com.wedbackendservice.nextgen.enums.Role;
import com.wedbackendservice.nextgen.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    @Query("""
        SELECT u FROM User u
        WHERE LOWER(u.email) = LOWER(:identity)
           OR u.phone = :identity
        """)
    Optional<User> findByIdentity(@Param("identity") String identity);

    @Query("""
        SELECT COUNT(u) FROM User u
        WHERE LOWER(u.email) = LOWER(:email)
           OR u.phone = :phone
        """)
    long countByEmailOrPhone(@Param("email") String email, @Param("phone") String phone);

    @Query("""
        SELECT COUNT(u) FROM User u
        WHERE (LOWER(u.email) = LOWER(:email) OR u.phone = :phone)
          AND u.userId <> :userId
        """)
    long countByEmailOrPhoneExcludingUser(@Param("email") String email, @Param("phone") String phone, @Param("userId") Long userId);

    @Query("SELECT COUNT(u) FROM User u WHERE LOWER(u.email) = LOWER(:email) AND u.userId <> :userId")
    long countByEmailExcludingUser(@Param("email") String email, @Param("userId") Long userId);

    @Query("SELECT COUNT(u) FROM User u WHERE u.phone = :phone AND u.userId <> :userId")
    long countByPhoneExcludingUser(@Param("phone") String phone, @Param("userId") Long userId);

    long countByRoleAndStatus(Role role, UserStatus status);
}
