package com.wedbackendservice.nextgen.service.shared;

import com.wedbackendservice.nextgen.dto.portal.request.brand.BrandRequest;
import com.wedbackendservice.nextgen.dto.portal.request.category.CategoryRequest;
import com.wedbackendservice.nextgen.dto.client.response.brand.BrandResponse;
import com.wedbackendservice.nextgen.dto.client.response.category.CategoryResponse;
import com.wedbackendservice.nextgen.entity.Brand;
import com.wedbackendservice.nextgen.entity.Category;
import com.wedbackendservice.nextgen.entity.User;
import com.wedbackendservice.nextgen.enums.Role;
import com.wedbackendservice.nextgen.exception.AppException;
import com.wedbackendservice.nextgen.exception.ErrorCode;
import com.wedbackendservice.nextgen.mapper.CategoryMapper;
import com.wedbackendservice.nextgen.repository.AuditLogRepository;
import com.wedbackendservice.nextgen.repository.BrandRepository;
import com.wedbackendservice.nextgen.repository.CategoryRepository;
import com.wedbackendservice.nextgen.repository.ProductRepository;
import com.wedbackendservice.nextgen.security.SecurityHelper;
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
    private final CategoryMapper  categoryMapper;

    // =========================================================================
    // CATEGORIES
    // =========================================================================

    /**
     * API 3.1: Lấy cây danh mục phân cấp cha/con (Public, bảo vệ includeHidden)
     */
    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategoriesTree(boolean includeHidden) {
        boolean canSeeHidden = includeHidden && isStaffOrAdmin();

        List<Category> allCategories = canSeeHidden
                ? categoryRepository.findAll()
                : categoryRepository.findAllActive();

        Map<Long, Integer> productCountMap = getCategoryProductCounts();

        Map<Long, List<CategoryResponse>> childrenMap = new HashMap<>();
        List<CategoryResponse> rootResponses = new ArrayList<>();

        for (Category cat : allCategories) {
            int count = productCountMap.getOrDefault(cat.getCategoryId(), 0);
            CategoryResponse resp = categoryMapper.tocategoryResponse(cat);
            resp.setProductCount(count);
            resp.setChildren(new ArrayList<>());

            if (cat.getParent() == null) {
                rootResponses.add(resp);
            } else {
                Long parentId = cat.getParent().getCategoryId();
                childrenMap.computeIfAbsent(parentId, k -> new ArrayList<>()).add(resp);
            }
        }

        rootResponses.sort(Comparator.comparing(CategoryResponse::getCategoryId));

        for (CategoryResponse root : rootResponses) {
            attachChildrenAndSumCounts(root, childrenMap);
        }

        return rootResponses;
    }

    private int attachChildrenAndSumCounts(CategoryResponse parent, Map<Long, List<CategoryResponse>> childrenMap) {
        List<CategoryResponse> children = childrenMap.getOrDefault(parent.getCategoryId(), new ArrayList<>());
        children.sort(Comparator.comparing(CategoryResponse::getCategoryId));
        parent.setChildren(children);

        int totalCount = parent.getProductCount() != null ? parent.getProductCount() : 0;
        for (CategoryResponse child : children) {
            totalCount += attachChildrenAndSumCounts(child, childrenMap);
        }
        parent.setProductCount(totalCount);
        return totalCount;
    }

    /**
     * API 3.2: Chi tiết danh mục sản phẩm (Public, chặn ẩn với khách vãng lai)
     */
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        Category cat = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        if (!Boolean.TRUE.equals(cat.getIsActive()) && !isStaffOrAdmin()) {
            throw new AppException(ErrorCode.CATEGORY_NOT_FOUND);
        }

        int productCount = (int) productRepository.countByCategory_CategoryId(id);
        CategoryResponse resp = categoryMapper.tocategoryResponse(cat);
        resp.setProductCount(productCount);
        return resp;
    }

    /**
     * API 3.3: Tạo mới danh mục sản phẩm (STAFF / ADMIN)
     */
    @Transactional
    public CategoryResponse createCategory( CategoryRequest request) {
        User operator = securityHelper.getCurrentUser();

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

        if (categoryRepository.existsBySlugIgnoreCase(slug)) {
            throw new AppException(ErrorCode.CATEGORY_SLUG_ALREADY_EXISTS);
        }

        Boolean isVisible = request.getIsVisible() != null ? request.getIsVisible() : true;

        Category category = Category.builder()
                .categoryName(categoryName)
                .parent(parent)
                .slug(slug)
                .description(request.getDescription())
                .iconUrl(request.getIconUrl())
                .isActive(isVisible)
                .build();

        Category saved = categoryRepository.save(category);

        auditLogRepository.log(operator, "CREATE_CATEGORY", "Category", saved.getCategoryId(), null, saved.getCategoryName());
        CategoryResponse resp = categoryMapper.tocategoryResponse(saved);
        resp.setProductCount(0);
        return resp;
    }

    /**
     * API 3.4: Cập nhật thông tin danh mục (STAFF / ADMIN)
     */
    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        User operator = securityHelper.authenticateStaffOrAdmin();

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        if (request.getName() == null || request.getName().trim().isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Tên danh mục không được để trống");
        }
        String categoryName = request.getName().trim();

        Category parent = null;
        if (request.getParentId() != null) {
            if (request.getParentId().equals(id)) {
                throw new AppException(ErrorCode.CATEGORY_CYCLE_DETECTED);
            }
            parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST, "Danh mục cha không tồn tại."));

            // Chặn gán cha là chính nó hoặc con cháu của nó (phát hiện vòng lặp cây)
            Category current = parent;
            while (current != null) {
                if (current.getCategoryId().equals(id)) {
                    throw new AppException(ErrorCode.CATEGORY_CYCLE_DETECTED);
                }
                current = current.getParent();
            }

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

        if (categoryRepository.existsBySlugIgnoreCaseAndCategoryIdNot(slug, id)) {
            throw new AppException(ErrorCode.CATEGORY_SLUG_ALREADY_EXISTS);
        }

        category.setParent(parent);
        category.setCategoryName(categoryName);
        category.setSlug(slug);
        category.setDescription(request.getDescription());
        category.setIconUrl(request.getIconUrl());
        if (request.getIsVisible() != null) {
            category.setIsActive(request.getIsVisible());
        }
        category.setUpdatedAt(LocalDateTime.now());

        Category updated = categoryRepository.save(category);

        auditLogRepository.log(operator, "UPDATE_CATEGORY", "Category", id, oldValue, updated.getCategoryName());

        int productCount = (int) productRepository.countByCategory_CategoryId(id);
        CategoryResponse resp = categoryMapper.tocategoryResponse(updated);
        resp.setProductCount(productCount);
        return resp;
    }

    /**
     * API 3.5: Xóa danh mục (ADMIN)
     */
    @Transactional
    public void deleteCategory(Long id) {
        User operator = securityHelper.authenticateAdmin();

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

        auditLogRepository.log(operator, "DELETE_CATEGORY", "Category", id, category.getCategoryName(), null);
    }

    // =========================================================================
    // BRANDS
    // =========================================================================

    /**
     * API 3.6: Danh sách thương hiệu đối tác (Public, hỗ trợ includeHidden cho staff)
     */
    @Transactional(readOnly = true)
    public List<BrandResponse> getBrands(boolean includeHidden) {
        boolean canSeeHidden = includeHidden && isStaffOrAdmin();

        List<Brand> brands = canSeeHidden
                ? brandRepository.findAll()
                : brandRepository.findAllActive();

        Map<Long, Integer> productCountMap = getBrandProductCounts();

        return brands.stream()
                .map(b -> toBrandResponse(b, productCountMap.getOrDefault(b.getBrandId(), 0)))
                .toList();
    }
    /**
     * API 3.7: Chi tiết thương hiệu và logo (Public, chặn ẩn với khách vãng lai)
     */
    @Transactional(readOnly = true)
    public BrandResponse getBrandById(Long id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BRAND_NOT_FOUND));

        if (!Boolean.TRUE.equals(brand.getIsActive()) && !isStaffOrAdmin()) {
            throw new AppException(ErrorCode.BRAND_NOT_FOUND);
        }

        int totalProducts = (int) productRepository.countByBrand_BrandId(id);
        return toBrandResponse(brand, totalProducts);
    }
    /**
     * API 3.8: Thêm mới thương hiệu đối tác (STAFF / ADMIN)
     */
    @Transactional
    public BrandResponse createBrand(BrandRequest request) {
        User operator = securityHelper.authenticateStaffOrAdmin();

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

        if (brandRepository.existsBySlugIgnoreCase(slug)) {
            throw new AppException(ErrorCode.BRAND_SLUG_ALREADY_EXISTS);
        }

        Boolean isVisible = request.getIsVisible() != null ? request.getIsVisible() : true;

        Brand brand = Brand.builder()
                .brandName(brandName)
                .slug(slug)
                .logoUrl(request.getLogoUrl())
                .description(request.getDescription())
                .isActive(isVisible)
                .build();

        Brand saved = brandRepository.save(brand);

        auditLogRepository.log(operator, "CREATE_BRAND", "Brand", saved.getBrandId(), null, saved.getBrandName());

        return toBrandResponse(saved, 0);
    }

    /**
     * API 3.9: Cập nhật thương hiệu và logo (STAFF / ADMIN)
     */
    @Transactional
    public BrandResponse updateBrand(Long id, BrandRequest request) {
        User operator = securityHelper.authenticateStaffOrAdmin();

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

        if (brandRepository.existsBySlugIgnoreCaseAndBrandIdNot(slug, id)) {
            throw new AppException(ErrorCode.BRAND_SLUG_ALREADY_EXISTS);
        }

        brand.setBrandName(brandName);
        brand.setSlug(slug);
        brand.setLogoUrl(request.getLogoUrl());
        brand.setDescription(request.getDescription());
        if (request.getIsVisible() != null) {
            brand.setIsActive(request.getIsVisible());
        }
        brand.setUpdatedAt(LocalDateTime.now());

        Brand updated = brandRepository.save(brand);

        auditLogRepository.log(operator, "UPDATE_BRAND", "Brand", id, oldValue, updated.getBrandName());

        int totalProducts = (int) productRepository.countByBrand_BrandId(id);
        return toBrandResponse(updated, totalProducts);
    }

    /**
     * API 3.10: Xóa thương hiệu (ADMIN)
     */
    @Transactional
    public void deleteBrand(Long id) {
        User operator = securityHelper.authenticateAdmin();

        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BRAND_NOT_FOUND));

        long productsCount = productRepository.countByBrand_BrandId(id);
        if (productsCount > 0) {
            log.warn("Không thể xóa thương hiệu {}: có {} sản phẩm liên kết", id, productsCount);
            throw new AppException(ErrorCode.BRAND_HAS_PRODUCTS);
        }

        brandRepository.delete(brand);

        auditLogRepository.log(operator, "DELETE_BRAND", "Brand", id, brand.getBrandName(), null);
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================


    private BrandResponse toBrandResponse(Brand brand, int totalProducts) {
        return BrandResponse.builder()
                .brandId(brand.getBrandId())
                .name(brand.getBrandName())
                .slug(brand.getSlug())
                .logoUrl(brand.getLogoUrl())
                .description(brand.getDescription())
                .isVisible(brand.getIsActive())
                .totalProducts(totalProducts)
                .build();
    }

    private boolean isStaffOrAdmin() {
        try {
            User user = securityHelper.getCurrentUser();
            return user.getRole() == Role.STAFF || user.getRole() == Role.ADMIN;
        } catch (Exception e) {
            return false;
        }
    }

    private Map<Long, Integer> getCategoryProductCounts() {
        Map<Long, Integer> countMap = new HashMap<>();
        List<Object[]> rows = productRepository.countProductsGroupedByCategory();
        if (rows != null) {
            for (Object[] row : rows) {
                if (row.length >= 2 && row[0] != null && row[1] != null) {
                    Long catId = ((Number) row[0]).longValue();
                    Integer count = ((Number) row[1]).intValue();
                    countMap.put(catId, count);
                }
            }
        }
        return countMap;
    }

    private Map<Long, Integer> getBrandProductCounts() {
        Map<Long, Integer> countMap = new HashMap<>();
        List<Object[]> rows = productRepository.countProductsGroupedByBrand();
        if (rows != null) {
            for (Object[] row : rows) {
                if (row.length >= 2 && row[0] != null && row[1] != null) {
                    Long brandId = ((Number) row[0]).longValue();
                    Integer count = ((Number) row[1]).intValue();
                    countMap.put(brandId, count);
                }
            }
        }
        return countMap;
    }

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    public static String toSlug(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        String nowhitespace = WHITESPACE.matcher(input.trim()).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = Pattern.compile("\\p{InCombiningDiacriticalMarks}+").matcher(normalized).replaceAll("");
        slug = slug.replace("đ", "d").replace("Đ", "d");
        slug = NONLATIN.matcher(slug).replaceAll("");
        slug = slug.replaceAll("-{2,}", "-");
        return slug.toLowerCase(Locale.ENGLISH).replaceAll("^-|-$", "");
    }
}
