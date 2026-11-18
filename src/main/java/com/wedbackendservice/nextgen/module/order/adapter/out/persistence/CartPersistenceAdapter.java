package com.wedbackendservice.nextgen.module.order.adapter.out.persistence;

import com.wedbackendservice.nextgen.module.cart.repository.CartItemRepository;
import com.wedbackendservice.nextgen.module.cart.repository.CartRepository;
import com.wedbackendservice.nextgen.module.order.port.out.cart.CartPort;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CartPersistenceAdapter implements CartPort {

  private final CartRepository cartRepository;
  private final CartItemRepository cartItemRepository;

  @Override
  public void clearCartItems(Long userId, Collection<Long> variantIds) {
    if (userId != null && variantIds != null && !variantIds.isEmpty()) {
      try {
        cartRepository
            .findByUserId(userId)
            .ifPresent(
                cart -> {
                  for (Long vId : variantIds) {
                    cartItemRepository
                        .findByCartIdAndVariantId(cart.getCartId(), vId)
                        .ifPresent(cartItemRepository::delete);
                  }
                });
      } catch (Exception ex) {
        log.warn("[CartAdapter] Xóa giỏ hàng thất bại cho user {}: {}", userId, ex.getMessage());
      }
    }
  }
}
