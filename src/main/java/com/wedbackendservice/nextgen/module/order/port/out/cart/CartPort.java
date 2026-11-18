package com.wedbackendservice.nextgen.module.order.port.out.cart;

import java.util.Collection;

public interface CartPort {
  void clearCartItems(Long userId, Collection<Long> variantIds);
}
