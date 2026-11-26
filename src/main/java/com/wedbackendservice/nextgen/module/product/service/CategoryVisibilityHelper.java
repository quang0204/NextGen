package com.wedbackendservice.nextgen.module.product.service;

import com.wedbackendservice.nextgen.module.product.entity.Category;
import com.wedbackendservice.nextgen.module.product.entity.Product;
import com.wedbackendservice.nextgen.module.product.entity.ProductVariant;
import java.util.*;

/** Shared ancestor-based visibility evaluation for multi-level category trees. */
public final class CategoryVisibilityHelper {

  private static final int MAX_DEPTH = 10;

  private CategoryVisibilityHelper() {}

  public static boolean isCategoryTreeVisible(Category category) {
    if (category == null) {
      // Null category kept visible so uncategorized fixtures remain compatible.
      return true;
    }
    Set<Long> visited = new HashSet<>();
    Category current = category;
    int depth = 0;
    while (current != null && depth < MAX_DEPTH) {
      if (current.getCategoryId() != null && !visited.add(current.getCategoryId())) {
        return false;
      }
      if (!Boolean.TRUE.equals(current.getIsActive())) {
        return false;
      }
      current = current.getParent();
      depth++;
    }
    return true;
  }

  public static boolean isProductEffectivelyVisible(Product product) {
    return product != null
        && "ACTIVE".equalsIgnoreCase(product.getStatus())
        && isCategoryTreeVisible(product.getCategory());
  }

  public static boolean isVariantEffectivelyVisible(ProductVariant variant) {
    if (variant == null) {
      return false;
    }
    if (variant.getStatus() != null && !"ACTIVE".equalsIgnoreCase(variant.getStatus())) {
      return false;
    }
    Product product = variant.getProduct();
    if (product == null) {
      return true;
    }
    if (product.getStatus() != null && !"ACTIVE".equalsIgnoreCase(product.getStatus())) {
      return false;
    }
    return isCategoryTreeVisible(product.getCategory());
  }

  public static Set<Long> resolveVisibleCategoryIds(Collection<Category> allCategories) {
    if (allCategories == null || allCategories.isEmpty()) {
      return Collections.emptySet();
    }
    Map<Long, Category> byId = indexById(allCategories);
    Set<Long> visible = new HashSet<>();
    for (Category cat : allCategories) {
      if (cat.getCategoryId() != null && isCategoryTreeVisibleInMap(cat.getCategoryId(), byId)) {
        visible.add(cat.getCategoryId());
      }
    }
    return visible;
  }

  public static Set<Long> resolveSubtreeCategoryIds(
      Collection<Category> allCategories, Long targetCategoryId, boolean onlyVisible) {
    if (targetCategoryId == null) {
      return Collections.emptySet();
    }
    if (allCategories == null || allCategories.isEmpty()) {
      return onlyVisible ? Collections.emptySet() : Set.of(targetCategoryId);
    }
    Map<Long, Category> byId = indexById(allCategories);
    if (!byId.containsKey(targetCategoryId)) {
      return onlyVisible ? Collections.emptySet() : Set.of(targetCategoryId);
    }
    if (onlyVisible && !isCategoryTreeVisibleInMap(targetCategoryId, byId)) {
      return Collections.emptySet();
    }

    Map<Long, List<Category>> childrenByParent = new HashMap<>();
    for (Category cat : allCategories) {
      if (cat.getParent() != null && cat.getParent().getCategoryId() != null) {
        childrenByParent
            .computeIfAbsent(cat.getParent().getCategoryId(), k -> new ArrayList<>())
            .add(cat);
      }
    }

    Set<Long> result = new LinkedHashSet<>();
    Deque<Long> queue = new ArrayDeque<>();
    result.add(targetCategoryId);
    queue.add(targetCategoryId);

    while (!queue.isEmpty()) {
      Long currentId = queue.poll();
      for (Category child : childrenByParent.getOrDefault(currentId, Collections.emptyList())) {
        Long childId = child.getCategoryId();
        if (childId == null || result.contains(childId)) {
          continue;
        }
        if (onlyVisible && !Boolean.TRUE.equals(child.getIsActive())) {
          continue;
        }
        result.add(childId);
        queue.add(childId);
      }
    }
    return result;
  }

  private static Map<Long, Category> indexById(Collection<Category> categories) {
    Map<Long, Category> map = new HashMap<>();
    for (Category cat : categories) {
      if (cat != null && cat.getCategoryId() != null) {
        map.put(cat.getCategoryId(), cat);
      }
    }
    return map;
  }

  private static boolean isCategoryTreeVisibleInMap(Long categoryId, Map<Long, Category> byId) {
    Set<Long> visited = new HashSet<>();
    Long currentId = categoryId;
    int depth = 0;
    while (currentId != null && depth < MAX_DEPTH) {
      if (!visited.add(currentId)) {
        return false;
      }
      Category current = byId.get(currentId);
      if (current == null || !Boolean.TRUE.equals(current.getIsActive())) {
        return false;
      }
      currentId = current.getParent() != null ? current.getParent().getCategoryId() : null;
      depth++;
    }
    return true;
  }
}
