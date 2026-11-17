package com.wedbackendservice.nextgen.repository;

import com.wedbackendservice.nextgen.entity.Cart;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
  @Query("SELECT c FROM Cart c WHERE c.user.userId = :userId")
  Optional<Cart> findByUserId(@Param("userId") Long userId);
}
