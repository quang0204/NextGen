package com.wedbackendservice.nextgen.repository;

import com.wedbackendservice.nextgen.entity.Wishlist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    Optional<Wishlist> findByUser_UserIdAndProduct_ProductId(Long userId, Long productId);

    @EntityGraph(attributePaths = {"product"})
    Page<Wishlist> findByUser_UserId(Long userId, Pageable pageable);

    long countByUser_UserId(Long userId);
}
