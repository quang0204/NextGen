package com.wedbackendservice.nextgen.service.shared;

import com.wedbackendservice.nextgen.dto.client.response.product.*;
import com.wedbackendservice.nextgen.dto.portal.request.product.*;
import com.wedbackendservice.nextgen.dto.portal.response.product.*;
import com.wedbackendservice.nextgen.entity.*;
import com.wedbackendservice.nextgen.exception.AppException;
import com.wedbackendservice.nextgen.exception.ErrorCode;
import com.wedbackendservice.nextgen.repository.*;
import com.wedbackendservice.nextgen.security.SecurityHelper;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductsService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductSpecRepository productSpecRepository;
    private final InventoryRepository inventoryRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final PromotionRepository promotionRepository;
    private final PromotionVariantRepository promotionVariantRepository;
    private final AuditLogRepository auditLogRepository;
    private final SecurityHelper securityHelper;

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    // =========================================================================
    // 4.1 GET /api/v1/products - Danh sách sản phẩm cho khách hàng
    // =========================================================================
    @Transactional(readOnly = true)
    public ProductListResponse getProducts(int page, int limit, String search,
                                          Long brandId, Long categoryId,
                                          BigDecimal minPrice, BigDecimal maxPrice,
                                          Integer ram, Integer rom, String sortBy) {
        int pageNumber = Math.max(1, page);
        int pageSize = Math.min(50, Math.max(1, limit));

        Specification<Product> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), "ACTIVE"));

            if (search != null && !search.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("productName")), "%" + search.trim().toLowerCase() + "%"));
            }
            if (brandId != null) {
                predicates.add(cb.equal(root.get("brand").get("brandId"), brandId));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("categoryId"), categoryId));
            }

            if (minPrice != null || maxPrice != null || ram != null || rom != null) {
                Subquery<Long> subquery = query.subquery(Long.class);
                var variantRoot = subquery.from(ProductVariant.class);
                List<Predicate> variantPredicates = new ArrayList<>();
                variantPredicates.add(cb.equal(variantRoot.get("product").get("productId"), root.get("productId")));
                variantPredicates.add(cb.equal(variantRoot.get("status"), "ACTIVE"));

                if (minPrice != null) {
                    variantPredicates.add(cb.greaterThanOrEqualTo(variantRoot.get("price"), minPrice));
                }
                if (maxPrice != null) {
                    variantPredicates.add(cb.lessThanOrEqualTo(variantRoot.get("price"), maxPrice));
                }
                if (ram != null) {
                    variantPredicates.add(cb.equal(variantRoot.get("ramGb"), ram));
                }
                if (rom != null) {
                    variantPredicates.add(cb.equal(variantRoot.get("romGb"), rom));
                }

                subquery.select(variantRoot.get("product").get("productId"))
                        .where(variantPredicates.toArray(new Predicate[0]));
                predicates.add(cb.exists(subquery));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        if ("priceAsc".equalsIgnoreCase(sortBy)) {
            sort = Sort.by(Sort.Direction.ASC, "basePrice");
        } else if ("priceDesc".equalsIgnoreCase(sortBy)) {
            sort = Sort.by(Sort.Direction.DESC, "basePrice");
        } else if ("newest".equalsIgnoreCase(sortBy)) {
            sort = Sort.by(Sort.Direction.DESC, "createdAt");
        }

        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize, sort);
        Page<Product> pagedProducts = productRepository.findAll(spec, pageable);
        List<Product> products = pagedProducts.getContent();

        if (products.isEmpty()) {
            return ProductListResponse.builder()
                    .items(Collections.emptyList())
                    .pagination(ProductListResponse.PaginationInfo.builder()
                            .currentPage(pageNumber)
                            .pageSize(pageSize)
                            .totalItems(pagedProducts.getTotalElements())
                            .totalPages(pagedProducts.getTotalPages())
                            .build())
                    .build();
        }

        List<Long> productIds = products.stream().map(Product::getProductId).toList();

        // Batch load active variants
        List<ProductVariant> variants = productVariantRepository.findByProduct_ProductIdInAndStatus(productIds, "ACTIVE");
        Map<Long, List<ProductVariant>> variantsByProduct = variants.stream()
                .collect(Collectors.groupingBy(v -> v.getProduct().getProductId()));

        // Batch load primary images
        List<ProductImage> images = productImageRepository.findByProduct_ProductIdIn(productIds);
        Map<Long, List<ProductImage>> imagesByProduct = images.stream()
                .collect(Collectors.groupingBy(img -> img.getProduct().getProductId()));

        // Batch load inventories for variants
        List<Long> variantIds = variants.stream().map(ProductVariant::getVariantId).toList();
        List<Inventory> inventories = variantIds.isEmpty() ? Collections.emptyList() : inventoryRepository.findByVariant_VariantIdIn(variantIds);
        Map<Long, Inventory> inventoryByVariant = inventories.stream()
                .collect(Collectors.toMap(inv -> inv.getVariant().getVariantId(), inv -> inv, (a, b) -> a));

        List<ProductSummaryResponse> items = new ArrayList<>();
        for (Product product : products) {
            List<ProductVariant> pVariants = variantsByProduct.getOrDefault(product.getProductId(), Collections.emptyList());

            BigDecimal minP = pVariants.stream()
                    .map(ProductVariant::getPrice)
                    .filter(Objects::nonNull)
                    .min(BigDecimal::compareTo)
                    .orElse(product.getBasePrice());

            BigDecimal maxP = pVariants.stream()
                    .map(ProductVariant::getPrice)
                    .filter(Objects::nonNull)
                    .max(BigDecimal::compareTo)
                    .orElse(product.getBasePrice());

            List<String> availableColors = pVariants.stream()
                    .map(ProductVariant::getColor)
                    .filter(c -> c != null && !c.isBlank())
                    .distinct()
                    .toList();

            boolean isAvailable = pVariants.stream().anyMatch(v -> {
                Inventory inv = inventoryByVariant.get(v.getVariantId());
                return inv != null && inv.getAvailableQty() > 0;
            });

            // Primary image
            List<ProductImage> pImages = imagesByProduct.getOrDefault(product.getProductId(), Collections.emptyList());
            String primaryImage = pImages.stream()
                    .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                    .map(ProductImage::getImageUrl)
                    .findFirst()
                    .orElseGet(() -> pImages.stream()
                            .min(Comparator.comparingInt(img -> img.getDisplayOrder() != null ? img.getDisplayOrder() : 9999))
                            .map(ProductImage::getImageUrl)
                            .orElse(product.getThumbnailUrl()));

            items.add(ProductSummaryResponse.builder()
                    .productId(product.getProductId())
                    .name(product.getProductName())
                    .brand(product.getBrand() != null ? ProductSummaryResponse.BrandSummary.builder()
                            .brandId(product.getBrand().getBrandId())
                            .name(product.getBrand().getBrandName())
                            .build() : null)
                    .category(product.getCategory() != null ? ProductSummaryResponse.CategorySummary.builder()
                            .categoryId(product.getCategory().getCategoryId())
                            .name(product.getCategory().getCategoryName())
                            .build() : null)
                    .primaryImage(primaryImage)
                    .minPrice(minP)
                    .maxPrice(maxP)
                    .totalVariants(pVariants.size())
                    .availableColors(availableColors)
                    .isAvailable(isAvailable)
                    .build());
        }

        return ProductListResponse.builder()
                .items(items)
                .pagination(ProductListResponse.PaginationInfo.builder()
                        .currentPage(pageNumber)
                        .pageSize(pageSize)
                        .totalItems(pagedProducts.getTotalElements())
                        .totalPages(pagedProducts.getTotalPages())
                        .build())
                .build();
    }

    // =========================================================================
    // 4.2 GET /api/v1/products/featured - Sản phẩm nổi bật
    // =========================================================================
    @Transactional(readOnly = true)
    public List<ProductFeaturedResponse> getFeaturedProducts(String type, int limit) {
        int maxItems = limit > 0 ? limit : 8;
        String featureType = (type != null && !type.isBlank()) ? type.toUpperCase() : "FLASH_SALE";

        if ("FLASH_SALE".equals(featureType)) {
            LocalDateTime now = LocalDateTime.now();
            List<Promotion> activePromotions = promotionRepository.findActivePromotions(now);
            if (!activePromotions.isEmpty()) {
                List<Long> promoIds = activePromotions.stream().map(Promotion::getPromotionId).toList();
                List<PromotionVariant> promoVariants = new ArrayList<>();
                for (Long pId : promoIds) {
                    promoVariants.addAll(promotionVariantRepository.findByPromotion_PromotionId(pId));
                }

                // Filter active product & active variant
                Map<Long, List<PromotionVariant>> promoVariantsByProduct = new HashMap<>();
                for (PromotionVariant pv : promoVariants) {
                    ProductVariant v = pv.getVariant();
                    if (v != null && "ACTIVE".equalsIgnoreCase(v.getStatus())
                            && v.getProduct() != null && "ACTIVE".equalsIgnoreCase(v.getProduct().getStatus())) {
                        promoVariantsByProduct.computeIfAbsent(v.getProduct().getProductId(), k -> new ArrayList<>()).add(pv);
                    }
                }

                List<ProductFeaturedResponse> featured = new ArrayList<>();
                for (Map.Entry<Long, List<PromotionVariant>> entry : promoVariantsByProduct.entrySet()) {
                    if (featured.size() >= maxItems) break;

                    List<PromotionVariant> list = entry.getValue();
                    // pick variant with highest discount or lowest promotional price
                    PromotionVariant bestPv = list.stream()
                            .min(Comparator.comparing(PromotionVariant::getPromotionalPrice))
                            .orElse(list.get(0));

                    ProductVariant variant = bestPv.getVariant();
                    Product product = variant.getProduct();
                    Promotion promo = bestPv.getPromotion();

                    BigDecimal originalPrice = variant.getCompareAtPrice() != null && variant.getCompareAtPrice().compareTo(BigDecimal.ZERO) > 0
                            ? variant.getCompareAtPrice()
                            : variant.getPrice();
                    BigDecimal salePrice = bestPv.getPromotionalPrice();

                    int discountPercent = 0;
                    if (originalPrice != null && originalPrice.compareTo(BigDecimal.ZERO) > 0 && salePrice != null) {
                        discountPercent = originalPrice.subtract(salePrice)
                                .multiply(BigDecimal.valueOf(100))
                                .divide(originalPrice, 0, RoundingMode.HALF_UP)
                                .intValue();
                    }

                    Inventory inv = inventoryRepository.findByVariant_VariantId(variant.getVariantId()).orElse(null);
                    boolean inStock = inv != null && inv.getAvailableQty() > 0;

                    String image = variant.getImageUrl() != null && !variant.getImageUrl().isBlank()
                            ? variant.getImageUrl()
                            : product.getThumbnailUrl();

                    featured.add(ProductFeaturedResponse.builder()
                            .productId(product.getProductId())
                            .name(product.getProductName())
                            .primaryImage(image)
                            .originalPrice(originalPrice)
                            .salePrice(salePrice)
                            .discountPercent(Math.max(0, discountPercent))
                            .promotion(promo != null ? ProductFeaturedResponse.PromotionSummary.builder()
                                    .promotionId(promo.getPromotionId())
                                    .name(promo.getPromotionName())
                                    .endAt(promo.getEndDate() != null ? promo.getEndDate().toString() : null)
                                    .build() : null)
                            .inStock(inStock)
                            .build());
                }

                if (!featured.isEmpty()) {
                    return featured;
                }
            }
        }

        // BEST_SELLER or fallback when no active flash sales
        List<Object[]> bestSellingIds = productRepository.findBestSellingProductIds(PageRequest.of(0, maxItems));
        List<Long> targetProductIds = new ArrayList<>();
        for (Object[] row : bestSellingIds) {
            targetProductIds.add((Long) row[0]);
        }

        if (targetProductIds.size() < maxItems) {
            List<Product> latest = productRepository.findByStatusOrderByCreatedAtDesc("ACTIVE", PageRequest.of(0, maxItems));
            for (Product p : latest) {
                if (!targetProductIds.contains(p.getProductId())) {
                    targetProductIds.add(p.getProductId());
                }
                if (targetProductIds.size() >= maxItems) break;
            }
        }

        List<Product> products = productRepository.findAllById(targetProductIds).stream()
                .filter(p -> "ACTIVE".equalsIgnoreCase(p.getStatus()))
                .toList();

        List<ProductFeaturedResponse> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (Product product : products) {
            List<ProductVariant> variants = productVariantRepository.findByProduct_ProductIdAndStatus(product.getProductId(), "ACTIVE");
            if (variants.isEmpty()) continue;

            ProductVariant firstVariant = variants.get(0);
            BigDecimal originalPrice = firstVariant.getPrice() != null ? firstVariant.getPrice() : product.getBasePrice();
            BigDecimal salePrice = originalPrice;
            Integer discountPercent = 0;
            ProductFeaturedResponse.PromotionSummary promoSummary = null;

            List<PromotionVariant> activePromos = promotionVariantRepository.findActivePromotionsForVariant(firstVariant.getVariantId(), now);
            if (!activePromos.isEmpty()) {
                PromotionVariant best = activePromos.get(0);
                salePrice = best.getPromotionalPrice();
                if (originalPrice != null && originalPrice.compareTo(BigDecimal.ZERO) > 0) {
                    discountPercent = originalPrice.subtract(salePrice)
                            .multiply(BigDecimal.valueOf(100))
                            .divide(originalPrice, 0, RoundingMode.HALF_UP)
                            .intValue();
                }
                Promotion p = best.getPromotion();
                if (p != null) {
                    promoSummary = ProductFeaturedResponse.PromotionSummary.builder()
                            .promotionId(p.getPromotionId())
                            .name(p.getPromotionName())
                            .endAt(p.getEndDate() != null ? p.getEndDate().toString() : null)
                            .build();
                }
            }

            Inventory inv = inventoryRepository.findByVariant_VariantId(firstVariant.getVariantId()).orElse(null);
            boolean inStock = inv != null && inv.getAvailableQty() > 0;

            result.add(ProductFeaturedResponse.builder()
                    .productId(product.getProductId())
                    .name(product.getProductName())
                    .primaryImage(product.getThumbnailUrl())
                    .originalPrice(originalPrice)
                    .salePrice(salePrice)
                    .discountPercent(Math.max(0, discountPercent))
                    .promotion(promoSummary)
                    .inStock(inStock)
                    .build());
        }

        return result;
    }

    // =========================================================================
    // 4.3 GET /api/v1/products/{id} - Chi tiết sản phẩm
    // =========================================================================
    @Transactional(readOnly = true)
    public ProductDetailResponse getProductDetail(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND, "Không tìm thấy sản phẩm có ID " + id + "!"));

        if (!"ACTIVE".equalsIgnoreCase(product.getStatus())) {
            throw new AppException(ErrorCode.PRODUCT_NOT_FOUND, "Không tìm thấy sản phẩm có ID " + id + "!");
        }

        // Images
        List<ProductImage> images = productImageRepository.findByProduct_ProductIdOrderByDisplayOrderAsc(id);
        List<ProductDetailResponse.ProductImageResponse> imageResponses = images.stream()
                .map(img -> ProductDetailResponse.ProductImageResponse.builder()
                        .imageId(img.getImageId())
                        .imageUrl(img.getImageUrl())
                        .isPrimary(Boolean.TRUE.equals(img.getIsPrimary()))
                        .sortOrder(img.getSortOrder())
                        .build())
                .toList();

        // Specs grouped
        List<ProductSpec> specs = productSpecRepository.findByProduct_ProductIdOrderBySortOrderAsc(id);
        Map<String, List<ProductSpec>> grouped = new LinkedHashMap<>();
        for (ProductSpec spec : specs) {
            String group = (spec.getGroupName() != null && !spec.getGroupName().isBlank())
                    ? spec.getGroupName().trim()
                    : "Thông số chung";
            grouped.computeIfAbsent(group, k -> new ArrayList<>()).add(spec);
        }

        List<ProductDetailResponse.SpecGroupResponse> specGroupResponses = new ArrayList<>();
        for (Map.Entry<String, List<ProductSpec>> entry : grouped.entrySet()) {
            List<ProductDetailResponse.SpecItemResponse> items = entry.getValue().stream()
                    .map(s -> ProductDetailResponse.SpecItemResponse.builder()
                            .specName(s.getSpecName())
                            .specValue(s.getSpecValue())
                            .build())
                    .toList();
            specGroupResponses.add(ProductDetailResponse.SpecGroupResponse.builder()
                    .groupName(entry.getKey())
                    .items(items)
                    .build());
        }

        // Variants
        List<ProductVariant> variants = productVariantRepository.findByProduct_ProductId(id);
        List<Long> variantIds = variants.stream().map(ProductVariant::getVariantId).toList();
        List<Inventory> inventories = variantIds.isEmpty() ? Collections.emptyList() : inventoryRepository.findByVariant_VariantIdIn(variantIds);
        Map<Long, Inventory> invMap = inventories.stream()
                .collect(Collectors.toMap(inv -> inv.getVariant().getVariantId(), inv -> inv, (a, b) -> a));

        List<ProductDetailResponse.VariantDetailResponse> variantResponses = variants.stream()
                .map(v -> {
                    Inventory inv = invMap.get(v.getVariantId());
                    int availableQty = inv != null ? inv.getAvailableQty() : 0;
                    boolean isActive = "ACTIVE".equalsIgnoreCase(v.getStatus());
                    return ProductDetailResponse.VariantDetailResponse.builder()
                            .variantId(v.getVariantId())
                            .sku(v.getSku())
                            .color(v.getColor())
                            .ramGb(v.getRamGb())
                            .romGb(v.getRomGb())
                            .connectionType(v.getConnectionType())
                            .listPrice(v.getPrice())
                            .availableQty(availableQty)
                            .isActive(isActive)
                            .build();
                })
                .toList();

        return ProductDetailResponse.builder()
                .productId(product.getProductId())
                .name(product.getProductName())
                .brand(product.getBrand() != null ? ProductSummaryResponse.BrandSummary.builder()
                        .brandId(product.getBrand().getBrandId())
                        .name(product.getBrand().getBrandName())
                        .build() : null)
                .category(product.getCategory() != null ? ProductSummaryResponse.CategorySummary.builder()
                        .categoryId(product.getCategory().getCategoryId())
                        .name(product.getCategory().getCategoryName())
                        .build() : null)
                .description(product.getDescription())
                .warrantyPolicy(product.getWarrantyPolicy())
                .status(product.getStatus())
                .images(imageResponses)
                .specs(specGroupResponses)
                .variants(variantResponses)
                .build();
    }

    // =========================================================================
    // 4.4 GET /api/v1/products/{id}/related - Sản phẩm liên quan
    // =========================================================================
    @Transactional(readOnly = true)
    public List<ProductRelatedResponse> getRelatedProducts(Long id, int limit) {
        Product current = productRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND, "Không tìm thấy sản phẩm có ID " + id + "!"));

        int maxItems = limit > 0 ? limit : 4;
        Long catId = current.getCategory() != null ? current.getCategory().getCategoryId() : null;
        Long bId = current.getBrand() != null ? current.getBrand().getBrandId() : null;

        List<Product> related = productRepository.findRelatedProducts(id, catId, bId, PageRequest.of(0, maxItems));
        if (related.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> relIds = related.stream().map(Product::getProductId).toList();
        List<ProductVariant> variants = productVariantRepository.findByProduct_ProductIdInAndStatus(relIds, "ACTIVE");
        Map<Long, List<ProductVariant>> variantsByProduct = variants.stream()
                .collect(Collectors.groupingBy(v -> v.getProduct().getProductId()));

        List<ProductImage> images = productImageRepository.findByProduct_ProductIdInAndIsPrimaryTrue(relIds);
        Map<Long, String> primaryImageByProduct = images.stream()
                .collect(Collectors.toMap(img -> img.getProduct().getProductId(), ProductImage::getImageUrl, (a, b) -> a));

        List<ProductRelatedResponse> response = new ArrayList<>();
        for (Product p : related) {
            List<ProductVariant> pVars = variantsByProduct.getOrDefault(p.getProductId(), Collections.emptyList());
            BigDecimal minP = pVars.stream()
                    .map(ProductVariant::getPrice)
                    .filter(Objects::nonNull)
                    .min(BigDecimal::compareTo)
                    .orElse(p.getBasePrice());

            String img = primaryImageByProduct.getOrDefault(p.getProductId(), p.getThumbnailUrl());

            response.add(ProductRelatedResponse.builder()
                    .productId(p.getProductId())
                    .name(p.getProductName())
                    .primaryImage(img)
                    .minPrice(minP)
                    .brand(p.getBrand() != null ? p.getBrand().getBrandName() : null)
                    .build());
        }

        return response;
    }

    // =========================================================================
    // 4.5 POST /api/v1/admin/products - Tạo mới sản phẩm trọn gói
    // =========================================================================
    @Transactional(rollbackFor = Exception.class)
    public CreateProductResponse createProduct(CreateProductRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        Brand brand = brandRepository.findById(request.getBrandId())
                .orElseThrow(() -> new AppException(ErrorCode.BRAND_NOT_FOUND));

        // Validate duplicates within request and database
        Set<String> skuSet = new HashSet<>();
        for (CreateProductRequest.VariantItemRequest v : request.getVariants()) {
            String sku = v.getSku().trim();
            if (!skuSet.add(sku)) {
                throw new AppException(ErrorCode.DUPLICATE_SKU, "Mã SKU '" + sku + "' bị trùng lặp trong yêu cầu!");
            }
            if (productVariantRepository.existsBySku(sku)) {
                throw new AppException(ErrorCode.DUPLICATE_SKU, "Mã SKU '" + sku + "' đã tồn tại trong hệ thống!");
            }
        }

        // Determine base price and primary image
        BigDecimal minPrice = request.getVariants().stream()
                .map(CreateProductRequest.VariantItemRequest::getListPrice)
                .filter(Objects::nonNull)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        String primaryImageUrl = null;
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            primaryImageUrl = request.getImages().stream()
                    .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                    .map(CreateProductRequest.ImageItemRequest::getImageUrl)
                    .findFirst()
                    .orElse(request.getImages().get(0).getImageUrl());
        }

        String baseSlug = CategoriesAndBrandsService.toSlug(request.getName());
        String slug = generateUniqueSlug(baseSlug, null);

        String status = (request.getStatus() != null && !request.getStatus().isBlank())
                ? request.getStatus().toUpperCase()
                : "ACTIVE";

        Product product = Product.builder()
                .productName(request.getName().trim())
                .category(category)
                .brand(brand)
                .description(request.getDescription())
                .warrantyPolicy(request.getWarrantyPolicy())
                .basePrice(minPrice)
                .thumbnailUrl(primaryImageUrl)
                .slug(slug)
                .status(status)
                .build();

        Product savedProduct = productRepository.save(product);

        // Images
        int totalImages = 0;
        if (request.getImages() != null) {
            int order = 1;
            for (CreateProductRequest.ImageItemRequest img : request.getImages()) {
                ProductImage image = ProductImage.builder()
                        .product(savedProduct)
                        .imageUrl(img.getImageUrl())
                        .isPrimary(Boolean.TRUE.equals(img.getIsPrimary()))
                        .displayOrder(img.getSortOrder() != null ? img.getSortOrder() : order++)
                        .build();
                productImageRepository.save(image);
                totalImages++;
            }
        }

        // Specs
        int totalSpecs = 0;
        if (request.getSpecs() != null) {
            int order = 1;
            for (CreateProductRequest.SpecItemRequest specReq : request.getSpecs()) {
                ProductSpec spec = ProductSpec.builder()
                        .product(savedProduct)
                        .groupName(specReq.getGroupName())
                        .specName(specReq.getSpecName())
                        .specValue(specReq.getSpecValue())
                        .sortOrder(specReq.getSortOrder() != null ? specReq.getSortOrder() : order++)
                        .build();
                productSpecRepository.save(spec);
                totalSpecs++;
            }
        }

        // Variants & Inventories
        List<CreateProductResponse.CreatedVariantSummary> createdVariants = new ArrayList<>();
        for (CreateProductRequest.VariantItemRequest vReq : request.getVariants()) {
            String variantName = buildVariantName(savedProduct.getProductName(), vReq.getRomGb(), vReq.getColor());

            ProductVariant variant = ProductVariant.builder()
                    .product(savedProduct)
                    .sku(vReq.getSku().trim())
                    .variantName(variantName)
                    .price(vReq.getListPrice())
                    .compareAtPrice(vReq.getListPrice())
                    .color(vReq.getColor())
                    .ramGb(vReq.getRamGb())
                    .romGb(vReq.getRomGb())
                    .connectionType(vReq.getConnectionType())
                    .status("ACTIVE")
                    .imageUrl(primaryImageUrl)
                    .build();

            ProductVariant savedVariant = productVariantRepository.save(variant);

            int onHand = vReq.getInitialOnHandQty() != null ? vReq.getInitialOnHandQty() : 0;
            int threshold = vReq.getMinThreshold() != null ? vReq.getMinThreshold() : 5;

            Inventory inventory = Inventory.builder()
                    .variant(savedVariant)
                    .quantity(onHand)
                    .reservedQuantity(0)
                    .minThreshold(threshold)
                    .build();

            Inventory savedInventory = inventoryRepository.save(inventory);

            createdVariants.add(CreateProductResponse.CreatedVariantSummary.builder()
                    .variantId(savedVariant.getVariantId())
                    .sku(savedVariant.getSku())
                    .listPrice(savedVariant.getPrice())
                    .inventory(CreateProductResponse.InventorySummary.builder()
                            .inventoryId(savedInventory.getInventoryId())
                            .onHandQty(savedInventory.getOnHandQty())
                            .reservedQty(savedInventory.getReservedQty())
                            .availableQty(savedInventory.getAvailableQty())
                            .minThreshold(savedInventory.getMinThreshold())
                            .build())
                    .build());
        }

        // Audit Log
        try {
            User operator = securityHelper.getCurrentUser();
            auditLogRepository.log(operator, "CREATE_PRODUCT", "Product", savedProduct.getProductId(), null, savedProduct.getProductName());
        } catch (Exception e) {
            log.warn("Không thể ghi audit log cho createProduct: {}", e.getMessage());
        }

        return CreateProductResponse.builder()
                .productId(savedProduct.getProductId())
                .name(savedProduct.getProductName())
                .categoryId(savedProduct.getCategory().getCategoryId())
                .brandId(savedProduct.getBrand().getBrandId())
                .status(savedProduct.getStatus())
                .totalImages(totalImages)
                .totalSpecs(totalSpecs)
                .createdVariants(createdVariants)
                .createdAt(savedProduct.getCreatedAt() != null ? savedProduct.getCreatedAt().toString() : LocalDateTime.now().toString())
                .build();
    }

    // =========================================================================
    // 4.6 PUT /api/v1/admin/products/{id} - Cập nhật thông tin sản phẩm & Specs
    // =========================================================================
    @Transactional(rollbackFor = Exception.class)
    public UpdateProductResponse updateProduct(Long id, UpdateProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND, "Không tìm thấy sản phẩm có ID " + id + "!"));

        String oldName = product.getProductName();

        if (request.getName() != null && !request.getName().isBlank()) {
            product.setProductName(request.getName().trim());
            String baseSlug = CategoriesAndBrandsService.toSlug(request.getName());
            product.setSlug(generateUniqueSlug(baseSlug, product.getProductId()));
        }

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
            product.setCategory(category);
        }

        if (request.getBrandId() != null) {
            Brand brand = brandRepository.findById(request.getBrandId())
                    .orElseThrow(() -> new AppException(ErrorCode.BRAND_NOT_FOUND));
            product.setBrand(brand);
        }

        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }

        if (request.getWarrantyPolicy() != null) {
            product.setWarrantyPolicy(request.getWarrantyPolicy());
        }

        // Update images if provided
        if (request.getImages() != null) {
            productImageRepository.deleteByProduct_ProductId(id);
            int order = 1;
            String newPrimaryUrl = null;
            for (UpdateProductRequest.UpdateImageItem imgReq : request.getImages()) {
                ProductImage img = ProductImage.builder()
                        .product(product)
                        .imageUrl(imgReq.getImageUrl())
                        .isPrimary(Boolean.TRUE.equals(imgReq.getIsPrimary()))
                        .displayOrder(imgReq.getSortOrder() != null ? imgReq.getSortOrder() : order++)
                        .build();
                productImageRepository.save(img);
                if (Boolean.TRUE.equals(imgReq.getIsPrimary()) && newPrimaryUrl == null) {
                    newPrimaryUrl = imgReq.getImageUrl();
                }
            }
            if (newPrimaryUrl != null) {
                product.setThumbnailUrl(newPrimaryUrl);
            }
        }

        // Update specs if provided
        if (request.getSpecs() != null) {
            productSpecRepository.deleteByProduct_ProductId(id);
            int order = 1;
            for (UpdateProductRequest.UpdateSpecItem specReq : request.getSpecs()) {
                ProductSpec spec = ProductSpec.builder()
                        .product(product)
                        .groupName(specReq.getGroupName())
                        .specName(specReq.getSpecName())
                        .specValue(specReq.getSpecValue())
                        .sortOrder(specReq.getSortOrder() != null ? specReq.getSortOrder() : order++)
                        .build();
                productSpecRepository.save(spec);
            }
        }

        Product saved = productRepository.save(product);

        // Audit Log
        try {
            User operator = securityHelper.getCurrentUser();
            auditLogRepository.log(operator, "UPDATE_PRODUCT", "Product", saved.getProductId(), oldName, saved.getProductName());
        } catch (Exception e) {
            log.warn("Không thể ghi audit log cho updateProduct: {}", e.getMessage());
        }

        return UpdateProductResponse.builder()
                .productId(saved.getProductId())
                .name(saved.getProductName())
                .categoryId(saved.getCategory() != null ? saved.getCategory().getCategoryId() : null)
                .brandId(saved.getBrand() != null ? saved.getBrand().getBrandId() : null)
                .updatedAt(saved.getUpdatedAt() != null ? saved.getUpdatedAt().toString() : LocalDateTime.now().toString())
                .build();
    }

    // =========================================================================
    // 4.7 POST /api/v1/admin/products/{id}/variants - Thêm biến thể SKU mới
    // =========================================================================
    @Transactional(rollbackFor = Exception.class)
    public VariantCreatedResponse createVariant(Long productId, CreateVariantRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND, "Không tìm thấy sản phẩm có ID " + productId + "!"));

        String sku = request.getSku().trim();
        if (productVariantRepository.existsBySku(sku)) {
            throw new AppException(ErrorCode.DUPLICATE_SKU, "Mã SKU '" + sku + "' đã tồn tại trong hệ thống!");
        }

        String variantName = buildVariantName(product.getProductName(), request.getRomGb(), request.getColor());

        ProductVariant variant = ProductVariant.builder()
                .product(product)
                .sku(sku)
                .variantName(variantName)
                .color(request.getColor())
                .ramGb(request.getRamGb())
                .romGb(request.getRomGb())
                .connectionType(request.getConnectionType())
                .price(request.getListPrice())
                .compareAtPrice(request.getListPrice())
                .status("ACTIVE")
                .imageUrl(product.getThumbnailUrl())
                .build();

        ProductVariant savedVariant = productVariantRepository.save(variant);

        int onHand = request.getInitialOnHandQty() != null ? request.getInitialOnHandQty() : 0;
        int threshold = request.getMinThreshold() != null ? request.getMinThreshold() : 3;

        Inventory inventory = Inventory.builder()
                .variant(savedVariant)
                .quantity(onHand)
                .reservedQuantity(0)
                .minThreshold(threshold)
                .build();

        Inventory savedInventory = inventoryRepository.save(inventory);

        // Update product basePrice if new variant price is lower
        if (product.getBasePrice() == null || product.getBasePrice().compareTo(request.getListPrice()) > 0) {
            product.setBasePrice(request.getListPrice());
            productRepository.save(product);
        }

        // Audit Log
        try {
            User operator = securityHelper.getCurrentUser();
            auditLogRepository.log(operator, "CREATE_PRODUCT_VARIANT", "ProductVariant", savedVariant.getVariantId(), null, savedVariant.getSku());
        } catch (Exception e) {
            log.warn("Không thể ghi audit log cho createVariant: {}", e.getMessage());
        }

        return VariantCreatedResponse.builder()
                .variantId(savedVariant.getVariantId())
                .productId(product.getProductId())
                .sku(savedVariant.getSku())
                .color(savedVariant.getColor())
                .ramGb(savedVariant.getRamGb())
                .romGb(savedVariant.getRomGb())
                .listPrice(savedVariant.getPrice())
                .isActive(true)
                .inventory(CreateProductResponse.InventorySummary.builder()
                        .inventoryId(savedInventory.getInventoryId())
                        .onHandQty(savedInventory.getOnHandQty())
                        .reservedQty(savedInventory.getReservedQty())
                        .availableQty(savedInventory.getAvailableQty())
                        .minThreshold(savedInventory.getMinThreshold())
                        .build())
                .createdAt(savedVariant.getCreatedAt() != null ? savedVariant.getCreatedAt().toString() : LocalDateTime.now().toString())
                .build();
    }

    // =========================================================================
    // 4.8 PUT /api/v1/admin/products/variants/{varId} - Điều chỉnh giá / bật tắt SKU
    // =========================================================================
    @Transactional(rollbackFor = Exception.class)
    public UpdateVariantResponse updateVariant(Long varId, UpdateVariantRequest request) {
        ProductVariant variant = productVariantRepository.findById(varId)
                .orElseThrow(() -> new AppException(ErrorCode.VARIANT_NOT_FOUND, "Không tìm thấy biến thể có ID " + varId + "!"));

        BigDecimal oldPrice = variant.getPrice();
        String oldStatus = variant.getStatus();

        if (request.getListPrice() != null) {
            variant.setPrice(request.getListPrice());
        }

        if (request.getIsActive() != null) {
            variant.setStatus(request.getIsActive() ? "ACTIVE" : "INACTIVE");
        }

        ProductVariant updated = productVariantRepository.save(variant);

        // Audit Log
        try {
            User operator = securityHelper.getCurrentUser();
            auditLogRepository.log(operator, "UPDATE_PRODUCT_VARIANT", "ProductVariant", varId,
                    "price=" + oldPrice + ",status=" + oldStatus,
                    "price=" + updated.getPrice() + ",status=" + updated.getStatus());
        } catch (Exception e) {
            log.warn("Không thể ghi audit log cho updateVariant: {}", e.getMessage());
        }

        return UpdateVariantResponse.builder()
                .variantId(updated.getVariantId())
                .sku(updated.getSku())
                .oldListPrice(oldPrice)
                .newListPrice(updated.getPrice())
                .isActive("ACTIVE".equalsIgnoreCase(updated.getStatus()))
                .updatedAt(updated.getUpdatedAt() != null ? updated.getUpdatedAt().toString() : LocalDateTime.now().toString())
                .build();
    }

    // =========================================================================
    // 4.9 PATCH /api/v1/admin/products/{id}/status - Bật/tắt trạng thái kinh doanh
    // =========================================================================
    @Transactional(rollbackFor = Exception.class)
    public UpdateProductStatusResponse updateProductStatus(Long id, UpdateProductStatusRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND, "Không tìm thấy sản phẩm có ID " + id + "!"));

        String previousStatus = product.getStatus();
        String newStatus = request.getStatus() != null ? request.getStatus().trim().toUpperCase() : "";

        if (!List.of("ACTIVE", "INACTIVE", "DISCONTINUED").contains(newStatus)) {
            throw new AppException(ErrorCode.INVALID_PRODUCT_STATUS, "Trạng thái không hợp lệ. Chỉ chấp nhận: ACTIVE, INACTIVE, DISCONTINUED!");
        }

        product.setStatus(newStatus);
        Product saved = productRepository.save(product);

        String userEmail = "admin@phoneshop.vn";
        try {
            User operator = securityHelper.getCurrentUser();
            userEmail = operator.getEmail();
            String reasonText = request.getReason() != null ? " | Reason: " + request.getReason() : "";
            auditLogRepository.log(operator, "UPDATE_PRODUCT_STATUS", "Product", id, previousStatus, newStatus + reasonText);
        } catch (Exception e) {
            log.warn("Không thể lấy currentUser hoặc ghi audit log: {}", e.getMessage());
        }

        return UpdateProductStatusResponse.builder()
                .productId(saved.getProductId())
                .name(saved.getProductName())
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .reason(request.getReason())
                .updatedBy(userEmail)
                .updatedAt(saved.getUpdatedAt() != null ? saved.getUpdatedAt().toString() : LocalDateTime.now().toString())
                .build();
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================
    private String buildVariantName(String productName, Integer romGb, String color) {
        StringBuilder sb = new StringBuilder(productName != null ? productName : "");
        if (romGb != null && romGb > 0) {
            sb.append(" ").append(romGb).append("GB");
        }
        if (color != null && !color.isBlank()) {
            sb.append(" - ").append(color.trim());
        }
        return sb.toString();
    }

    private String generateUniqueSlug(String baseSlug, Long excludeProductId) {
        if (baseSlug == null || baseSlug.isBlank()) {
            baseSlug = "san-pham";
        }
        String slug = baseSlug;
        int count = 1;
        while (slugExists(slug, excludeProductId)) {
            slug = baseSlug + "-" + count;
            count++;
        }
        return slug;
    }

    private boolean slugExists(String slug, Long excludeProductId) {
        if (excludeProductId == null) {
            return productRepository.existsBySlug(slug);
        }
        return productRepository.existsBySlugAndProductIdNot(slug, excludeProductId);
    }
}
