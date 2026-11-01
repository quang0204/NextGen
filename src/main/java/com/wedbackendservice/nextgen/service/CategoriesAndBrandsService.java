package com.wedbackendservice.nextgen.service;

import com.wedbackendservice.nextgen.security.SecurityHelper;

import com.wedbackendservice.nextgen.dto.response.brand.BrandResponse;
import com.wedbackendservice.nextgen.dto.response.category.CategoryResponse;
import com.wedbackendservice.nextgen.dto.request.brand.BrandRequest;
import com.wedbackendservice.nextgen.dto.request.category.CategoryRequest;
import com.wedbackendservice.nextgen.entity.AuditLog;
import com.wedbackendservice.nextgen.entity.Brand;
import com.wedbackendservice.nextgen.entity.Category;
import com.wedbackendservice.nextgen.entity.User;
import com.wedbackendservice.nextgen.exception.AppException;
import com.wedbackendservice.nextgen.exception.ErrorCode;
import com.wedbackendservice.nextgen.repository.AuditLogRepository;
import com.wedbackendservice.nextgen.repository.BrandRepository;
import com.wedbackendservice.nextgen.repository.CategoryRepository;
import com.wedbackendservice.nextgen.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoriesAndBrandsService {

    private final SecurityHelper securityHelper;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;
    private final AuditLogRepository auditLogRepository;

    // =========================================================================
    // CATEGORIES
    // =========================================================================

    /**
     * API 3.1: Lấy cây danh mục phân cấp cha/con (Public)
     */
    public List<CategoryResponse> getCategoriesTree(boolean includeHidden) {
        List<Category> allCategories = includeHidden
                ? categoryRepository.findAll()
                : categoryRepository.findAllActive();

        Map<Long, Integer> productCountMap = getCategoryProductCounts();

        Map<Long, List<CategoryResponse>> childrenMap = new HashMap<>();
        List<CategoryResponse> rootResponses = new ArrayList<>();

        for (Category cat : allCategories) {
            int count = productCountMap.getOrDefault(cat.getCategoryId(), 0);
            CategoryResponse resp = CategoryResponse.builder()
                    .categoryId(cat.getCategoryId())
                    .parentId(cat.getParent() != null ? cat.getParent().getCategoryId() : null)
                    .name(cat.getCategoryName())
                    .description(cat.getDescription())
                    .isVisible(cat.getIsActive())
                    .productCount(count)
                    .children(new ArrayList<>())
                    .build();

            if (cat.getParent() == null) {
                rootResponses.add(resp);
            } else {
                Long parentId = cat.getParent().getCategoryId();
                childrenMap.computeIfAbsent(parentId, k -> new ArrayList<>()).add(resp);
            }
        }

        rootResponses.sort(Comparator.comparing(CategoryResponse::getCategoryId));

        for (CategoryResponse root : rootResponses) {
            attachChildren(root, childrenMap);
        }

        return rootResponses;
    }

    private void attachChildren(CategoryResponse parent, Map<Long, List<CategoryResponse>> childrenMap) {
        List<CategoryResponse> children = childrenMap.getOrDefault(parent.getCategoryId(), new ArrayList<>());
        children.sort(Comparator.comparing(CategoryResponse::getCategoryId));
        parent.setChildren(children);
        for (CategoryResponse child : children) {
            attachChildren(child, childrenMap);
        }
    }

    /**
     * API 3.2: Chi tiết danh mục sản phẩm (Public)
     */
    public CategoryResponse getCategoryById(Long id) {
        Category cat = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        int productCount = (int) productRepository.countByCategory_CategoryId(id);

        return CategoryResponse.builder()
                .categoryId(cat.getCategoryId())
                .parentId(cat.getParent() != null ? cat.getParent().getCategoryId() : null)
                .parentName(cat.getParent() != null ? cat.getParent().getCategoryName() : null)
                .name(cat.getCategoryName())
                .description(cat.getDescription())
                .isVisible(cat.getIsActive())
                .productCount(productCount)
                .build();
    }

    /**
     * API 3.3: Tạo mới danh mục sản phẩm (STAFF / ADMIN)
     */
    @Transactional
    public CategoryResponse createCategory(String authHeader, CategoryRequest request) {
        User operator = securityHelper.authenticateStaffOrAdmin(authHeader);

        if (request.getName() == null || request.getName().trim().isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Tên danh mục không được để trống");
        }
        String categoryName = request.getName().trim();

        Category parent = null;
        if (request.getParentId() != null) {
            parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST, "Danh mục cha không tồn tại."));
            if (categoryRepository.existsByCategoryNameIgnoreCaseAndParent_CategoryId(categoryName, request.getParentId())) {
                throw new AppException(ErrorCode.CATEGORY_ALREADY_EXISTS);
            }
        } else {
            if (categoryRepository.existsByCategoryNameIgnoreCaseAndParentIsNull(categoryName)) {
                throw new AppException(ErrorCode.CATEGORY_ALREADY_EXISTS);
            }
        }

        String slug = (request.getSlug() != null && !request.getSlug().isBlank())
                ? toSlug(request.getSlug())
                : toSlug(categoryName);

        Boolean isVisible = request.getIsVisible() != null ? request.getIsVisible() : true;

        Category category = Category.builder()
                .categoryName(categoryName)
                .parent(parent)
                .slug(slug)
                .description(request.getDescription())
                .iconUrl(request.getIconUrl())
                .isActive(isVisible)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Category saved = categoryRepository.save(category);

        auditLogRepository.save(AuditLog.builder()
                .user(operator)
                .action("CREATE_CATEGORY")
                .entityName("Category")
                .entityId(saved.getCategoryId())
                .newValue(saved.getCategoryName())
                .createdAt(LocalDateTime.now())
                .build());

        return CategoryResponse.builder()
                .categoryId(saved.getCategoryId())
                .parentId(saved.getParent() != null ? saved.getParent().getCategoryId() : null)
                .name(saved.getCategoryName())
                .description(saved.getDescription())
                .isVisible(saved.getIsActive())
                .build();
    }

    /**
     * API 3.4: Cập nhật thông tin danh mục (STAFF / ADMIN)
     */
    @Transactional
    public CategoryResponse updateCategory(String authHeader, Long id, CategoryRequest request) {
        User operator = securityHelper.authenticateStaffOrAdmin(authHeader);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        if (request.getName() == null || request.getName().trim().isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Tên danh mục không được để trống");
        }
        String categoryName = request.getName().trim();

        Category parent = null;
        if (request.getParentId() != null) {
            if (request.getParentId().equals(id)) {
                throw new AppException(ErrorCode.INVALID_REQUEST, "Danh mục cha không thể là chính nó.");
            }
            parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST, "Danh mục cha không tồn tại."));
            if (categoryRepository.existsByCategoryNameIgnoreCaseAndParent_CategoryIdAndCategoryIdNot(categoryName, request.getParentId(), id)) {
                throw new AppException(ErrorCode.CATEGORY_ALREADY_EXISTS);
            }
        } else {
            if (categoryRepository.existsByCategoryNameIgnoreCaseAndParentIsNullAndCategoryIdNot(categoryName, id)) {
                throw new AppException(ErrorCode.CATEGORY_ALREADY_EXISTS);
            }
        }

        String oldValue = category.getCategoryName();
        String slug = (request.getSlug() != null && !request.getSlug().isBlank())
                ? toSlug(request.getSlug())
                : toSlug(categoryName);

        category.setParent(parent);
        category.setCategoryName(categoryName);
        category.setSlug(slug);
        if (request.getDescription() != null) {
            category.setDescription(request.getDescription());
        }
        if (request.getIconUrl() != null) {
            category.setIconUrl(request.getIconUrl());
        }
        if (request.getIsVisible() != null) {
            category.setIsActive(request.getIsVisible());
        }
        category.setUpdatedAt(LocalDateTime.now());

        Category updated = categoryRepository.save(category);

        auditLogRepository.save(AuditLog.builder()
                .user(operator)
                .action("UPDATE_CATEGORY")
                .entityName("Category")
                .entityId(id)
                .oldValue(oldValue)
                .newValue(updated.getCategoryName())
                .createdAt(LocalDateTime.now())
                .build());

        return CategoryResponse.builder()
                .categoryId(updated.getCategoryId())
                .parentId(updated.getParent() != null ? updated.getParent().getCategoryId() : null)
                .name(updated.getCategoryName())
                .description(updated.getDescription())
                .isVisible(updated.getIsActive())
                .build();
    }

    /**
     * API 3.5: Xóa danh mục (ADMIN)
     */
    @Transactional
    public void deleteCategory(String authHeader, Long id) {
        User operator = securityHelper.authenticateAdmin(authHeader);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        long childCategoriesCount = categoryRepository.countByParent_CategoryId(id);
        if (childCategoriesCount > 0) {
            log.warn("Không thể xóa danh mục {}: có {} danh mục con", id, childCategoriesCount);
            throw new AppException(ErrorCode.CATEGORY_HAS_CHILDREN);
        }

        long childProductsCount = productRepository.countByCategory_CategoryId(id);
        if (childProductsCount > 0) {
            log.warn("Không thể xóa danh mục {}: có {} sản phẩm liên kết", id, childProductsCount);
            throw new AppException(ErrorCode.CATEGORY_HAS_PRODUCTS);
        }

        categoryRepository.delete(category);

        auditLogRepository.save(AuditLog.builder()
                .user(operator)
                .action("DELETE_CATEGORY")
                .entityName("Category")
                .entityId(id)
                .oldValue(category.getCategoryName())
                .createdAt(LocalDateTime.now())
                .build());
    }

    // =========================================================================
    // BRANDS
    // =========================================================================

    /**
     * API 3.6: Danh sách thương hiệu đối tác (Public)
     */
    public List<BrandResponse> getBrands() {
        List<Brand> activeBrands = brandRepository.findAllActive();
        Map<Long, Integer> productCountMap = getBrandProductCounts();

        return activeBrands.stream()
                .map(b -> BrandResponse.builder()
                        .brandId(b.getBrandId())
                        .name(b.getBrandName())
                        .logoUrl(b.getLogoUrl())
                        .description(b.getDescription())
                        .isVisible(b.getIsActive())
                        .totalProducts(productCountMap.getOrDefault(b.getBrandId(), 0))
                        .build())
                .toList();
    }

    /**
     * API 3.7: Chi tiết thương hiệu và logo (Public)
     */
    public BrandResponse getBrandById(Long id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BRAND_NOT_FOUND));

        int totalProducts = (int) productRepository.countByBrand_BrandId(id);

        return BrandResponse.builder()
                .brandId(brand.getBrandId())
                .name(brand.getBrandName())
                .logoUrl(brand.getLogoUrl())
                .description(brand.getDescription())
                .isVisible(brand.getIsActive())
                .totalProducts(totalProducts)
                .build();
    }

    /**
     * API 3.8: Thêm mới thương hiệu đối tác (STAFF / ADMIN)
     */
    @Transactional
    public BrandResponse createBrand(String authHeader, BrandRequest request) {
        User operator = securityHelper.authenticateStaffOrAdmin(authHeader);

        if (request.getName() == null || request.getName().trim().isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Tên thương hiệu không được để trống");
        }
        String brandName = request.getName().trim();

        if (brandRepository.existsByBrandNameIgnoreCase(brandName)) {
            throw new AppException(ErrorCode.BRAND_ALREADY_EXISTS);
        }

        String slug = (request.getSlug() != null && !request.getSlug().isBlank())
                ? toSlug(request.getSlug())
                : toSlug(brandName);

        Boolean isVisible = request.getIsVisible() != null ? request.getIsVisible() : true;

        Brand brand = Brand.builder()
                .brandName(brandName)
                .slug(slug)
                .logoUrl(request.getLogoUrl())
                .description(request.getDescription())
                .isActive(isVisible)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Brand saved = brandRepository.save(brand);

        auditLogRepository.save(AuditLog.builder()
                .user(operator)
                .action("CREATE_BRAND")
                .entityName("Brand")
                .entityId(saved.getBrandId())
                .newValue(saved.getBrandName())
                .createdAt(LocalDateTime.now())
                .build());

        return BrandResponse.builder()
                .brandId(saved.getBrandId())
                .name(saved.getBrandName())
                .logoUrl(saved.getLogoUrl())
                .description(saved.getDescription())
                .isVisible(saved.getIsActive())
                .build();
    }

    /**
     * API 3.9: Cập nhật thương hiệu và logo (STAFF / ADMIN)
     */
    @Transactional
    public BrandResponse updateBrand(String authHeader, Long id, BrandRequest request) {
        User operator = securityHelper.authenticateStaffOrAdmin(authHeader);

        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BRAND_NOT_FOUND));

        if (request.getName() == null || request.getName().trim().isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Tên thương hiệu không được để trống");
        }
        String brandName = request.getName().trim();

        if (brandRepository.existsByBrandNameIgnoreCaseAndBrandIdNot(brandName, id)) {
            throw new AppException(ErrorCode.BRAND_ALREADY_EXISTS);
        }

        String oldValue = brand.getBrandName();
        String slug = (request.getSlug() != null && !request.getSlug().isBlank())
                ? toSlug(request.getSlug())
                : toSlug(brandName);

        brand.setBrandName(brandName);
        brand.setSlug(slug);
        if (request.getLogoUrl() != null) {
            brand.setLogoUrl(request.getLogoUrl());
        }
        if (request.getDescription() != null) {
            brand.setDescription(request.getDescription());
        }
        if (request.getIsVisible() != null) {
            brand.setIsActive(request.getIsVisible());
        }
        brand.setUpdatedAt(LocalDateTime.now());

        Brand updated = brandRepository.save(brand);

        auditLogRepository.save(AuditLog.builder()
                .user(operator)
                .action("UPDATE_BRAND")
                .entityName("Brand")
                .entityId(id)
                .oldValue(oldValue)
                .newValue(updated.getBrandName())
                .createdAt(LocalDateTime.now())
                .build());

        return BrandResponse.builder()
                .brandId(updated.getBrandId())
                .name(updated.getBrandName())
                .logoUrl(updated.getLogoUrl())
                .description(updated.getDescription())
                .isVisible(updated.getIsActive())
                .build();
    }

    /**
     * API 3.10: Xóa thương hiệu đối tác (ADMIN)
     */
    @Transactional
    public void deleteBrand(String authHeader, Long id) {
        User operator = securityHelper.authenticateAdmin(authHeader);

        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BRAND_NOT_FOUND));

        long productsCount = productRepository.countByBrand_BrandId(id);
        if (productsCount > 0) {
            log.warn("Không thể xóa thương hiệu {}: có {} sản phẩm liên kết", id, productsCount);
            throw new AppException(ErrorCode.BRAND_HAS_PRODUCTS);
        }

        brandRepository.delete(brand);

        auditLogRepository.save(AuditLog.builder()
                .user(operator)
                .action("DELETE_BRAND")
                .entityName("Brand")
                .entityId(id)
                .oldValue(brand.getBrandName())
                .createdAt(LocalDateTime.now())
                .build());
    }

    // =========================================================================
    // HELPERS & MAPPERS
    // =========================================================================

    private Map<Long, Integer> getCategoryProductCounts() {
        Map<Long, Integer> map = new HashMap<>();
        List<Object[]> rows = productRepository.countProductsGroupedByCategory();
        if (rows != null) {
            for (Object[] row : rows) {
                if (row[0] != null && row[1] != null) {
                    map.put((Long) row[0], ((Number) row[1]).intValue());
                }
            }
        }
        return map;
    }

    private Map<Long, Integer> getBrandProductCounts() {
        Map<Long, Integer> map = new HashMap<>();
        List<Object[]> rows = productRepository.countProductsGroupedByBrand();
        if (rows != null) {
            for (Object[] row : rows) {
                if (row[0] != null && row[1] != null) {
                    map.put((Long) row[0], ((Number) row[1]).intValue());
                }
            }
        }
        return map;
    }

    public static String toSlug(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        String normalized = Normalizer.normalize(input.trim().toLowerCase(), Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String noAccents = pattern.matcher(normalized).replaceAll("");
        noAccents = noAccents.replace('đ', 'd').replace('Đ', 'd');
        return noAccents.replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");
    }
}
