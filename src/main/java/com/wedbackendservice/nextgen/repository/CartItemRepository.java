package com.wedbackendservice.nextgen.repository;

import com.wedbackendservice.nextgen.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    @Query("SELECT ci FROM CartItem ci WHERE ci.cart.cartId = :cartId ORDER BY ci.createdAt DESC")
    List<CartItem> findByCartId(@Param("cartId") Long cartId);

    @Query("SELECT ci FROM CartItem ci WHERE ci.cart.cartId = :cartId AND ci.variant.variantId = :variantId")
    Optional<CartItem> findByCartIdAndVariantId(@Param("cartId") Long cartId, @Param("variantId") Long variantId);

    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.cart.cartId = :cartId")
    void deleteAllByCartId(@Param("cartId") Long cartId);
}
