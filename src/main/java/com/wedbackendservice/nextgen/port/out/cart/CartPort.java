package com.wedbackendservice.nextgen.port.out.cart;

import java.util.Collection;

public interface CartPort {
  void clearCartItems(Long userId, Collection<Long> variantIds);
}
