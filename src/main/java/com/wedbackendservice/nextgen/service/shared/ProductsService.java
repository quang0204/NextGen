package com.wedbackendservice.nextgen.service.shared;

import com.wedbackendservice.nextgen.dto.client.response.product.*;
import com.wedbackendservice.nextgen.dto.portal.request.product.*;
import com.wedbackendservice.nextgen.dto.portal.response.product.*;
import com.wedbackendservice.nextgen.dto.session.UploadSessionData;
import com.wedbackendservice.nextgen.entity.*;
import com.wedbackendservice.nextgen.enums.StorageType;
import com.wedbackendservice.nextgen.enums.UploadStatus;
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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
    private final OrderItemRepository orderItemRepository;
    private final SecurityHelper securityHelper;
    private final StorageService storageService;
    private final TokenRedisService tokenRedisService;

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    private Map<Long, Integer> getSoldCountMap(Collection<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) return Map.of();
        return orderItemRepository.countSoldByProductIds(productIds).stream()
                .filter(r -> r != null && r[0] != null && r[1] != null)
                .collect(Collectors.toMap(r -> (Long) r[0], r -> ((Number) r[1]).intValue(), (a, b) -> a));
    }

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

        Map<Long, Integer> soldMap = getSoldCountMap(productIds);

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
                    .map(this::resolveProductImageUrl)
                    .findFirst()
                    .orElseGet(() -> pImages.stream()
                            .min(Comparator.comparingInt(img -> img.getDisplayOrder() != null ? img.getDisplayOrder() : 9999))
                            .map(this::resolveProductImageUrl)
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
                    .soldCount(soldMap.getOrDefault(product.getProductId(), 0))
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

                Map<Long, String> promoPrimaryImageMap = promoVariantsByProduct.isEmpty() ? Map.of() :
                        getPrimaryImageUrlMap(promoVariantsByProduct.keySet());

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

                    String image = promoPrimaryImageMap.getOrDefault(product.getProductId(),
                            variant.getImageUrl() != null && !variant.getImageUrl().isBlank()
                                    ? variant.getImageUrl()
                                    : product.getThumbnailUrl());

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

        List<Long> activeProductIds = products.stream().map(Product::getProductId).toList();
        List<ProductVariant> allVariants = activeProductIds.isEmpty() ? List.of() :
                productVariantRepository.findByProduct_ProductIdInAndStatus(activeProductIds, "ACTIVE");
        Map<Long, List<ProductVariant>> variantsByProduct = allVariants.stream()
                .collect(Collectors.groupingBy(v -> v.getProduct().getProductId()));

        List<Long> firstVariantIds = products.stream()
                .map(p -> variantsByProduct.getOrDefault(p.getProductId(), List.of()))
                .filter(vars -> !vars.isEmpty())
                .map(vars -> vars.get(0).getVariantId())
                .toList();

        Map<Long, List<PromotionVariant>> promosByVariant = firstVariantIds.isEmpty() ? Map.of() :
                promotionVariantRepository.findActivePromotionsForVariantIds(firstVariantIds, now).stream()
                        .collect(Collectors.groupingBy(pv -> pv.getVariant().getVariantId()));

        Map<Long, Inventory> inventoryMap = firstVariantIds.isEmpty() ? Map.of() :
                inventoryRepository.findByVariant_VariantIdIn(firstVariantIds).stream()
                        .collect(Collectors.toMap(i -> i.getVariant().getVariantId(), i -> i, (a, b) -> a));

        Map<Long, String> primaryImageMap = getPrimaryImageUrlMap(activeProductIds);

        for (Product product : products) {
            List<ProductVariant> variants = variantsByProduct.getOrDefault(product.getProductId(), List.of());
            if (variants.isEmpty()) continue;

            ProductVariant firstVariant = variants.get(0);
            BigDecimal originalPrice = firstVariant.getPrice() != null ? firstVariant.getPrice() : product.getBasePrice();
            BigDecimal salePrice = originalPrice;
            Integer discountPercent = 0;
            ProductFeaturedResponse.PromotionSummary promoSummary = null;

            List<PromotionVariant> activePromos = promosByVariant.getOrDefault(firstVariant.getVariantId(), List.of());
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

            Inventory inv = inventoryMap.get(firstVariant.getVariantId());
            boolean inStock = inv != null && inv.getAvailableQty() > 0;

            result.add(ProductFeaturedResponse.builder()
                    .productId(product.getProductId())
                    .name(product.getProductName())
                    .primaryImage(primaryImageMap.getOrDefault(product.getProductId(), product.getThumbnailUrl()))
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
                        .imageUrl(resolveProductImageUrl(img))
                        .storageProvider(img.getStorageProvider())
                        .bucket(img.getBucket())
                        .objectKey(img.getObjectKey())
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
                .soldCount(getSoldCountMap(List.of(id)).getOrDefault(id, 0))
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
                .collect(Collectors.toMap(img -> img.getProduct().getProductId(), this::resolveProductImageUrl, (a, b) -> a));

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

        List<ImageInput> imageInputs = request.getImages() == null ? List.of() : request.getImages().stream()
                .map(img -> new ImageInput(null, img.getUploadId(), img.getObjectKey(), img.getImageUrl(), img.getIsPrimary(), img.getSortOrder()))
                .toList();
        List<String> consumedUploadIds = new ArrayList<>();
        List<ProductImage> builtImages = buildProductImages(null, imageInputs, Map.of(), consumedUploadIds);
        String primaryImageUrl = extractPrimaryImageUrl(builtImages);

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
        if (!builtImages.isEmpty()) {
            for (ProductImage image : builtImages) {
                image.setProduct(savedProduct);
            }
            productImageRepository.saveAll(builtImages);
            totalImages = builtImages.size();
        }
        cleanupConsumedUploadSessionsAfterCommit(consumedUploadIds);

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
        List<RemovedStorageObject> removedObjects = List.of();
        List<String> consumedUploadIds = new ArrayList<>();
        if (request.getImages() != null) {
            List<ProductImage> existingImages = productImageRepository.findByProduct_ProductIdOrderByDisplayOrderAsc(id);
            Map<Long, ProductImage> existingById = existingImages.stream()
                    .filter(img -> img.getImageId() != null)
                    .collect(Collectors.toMap(ProductImage::getImageId, img -> img, (a, b) -> a));

            List<ImageInput> imageInputs = request.getImages().stream()
                    .map(img -> new ImageInput(img.getImageId(), img.getUploadId(), img.getObjectKey(), img.getImageUrl(), img.getIsPrimary(), img.getSortOrder()))
                    .toList();
            List<ProductImage> newImages = buildProductImages(product, imageInputs, existingById, consumedUploadIds);

            Set<String> keptKeys = newImages.stream()
                    .map(ProductImage::getObjectKey)
                    .filter(k -> k != null && !k.isBlank())
                    .collect(Collectors.toSet());
            removedObjects = existingImages.stream()
                    .filter(old -> old.getObjectKey() != null && !old.getObjectKey().isBlank() && !keptKeys.contains(old.getObjectKey()))
                    .map(old -> new RemovedStorageObject(
                            old.getBucket() != null ? old.getBucket() : storageService.resolveBucket(StorageType.PRODUCTS),
                            old.getObjectKey()))
                    .distinct()
                    .toList();

            productImageRepository.deleteByProduct_ProductId(id);
            productImageRepository.flush();
            if (!newImages.isEmpty()) {
                productImageRepository.saveAll(newImages);
            }
            product.setThumbnailUrl(extractPrimaryImageUrl(newImages));
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
        cleanupConsumedUploadSessionsAfterCommit(consumedUploadIds);
        cleanupRemovedStorageObjectsAfterCommit(removedObjects);

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
    // DELETE /api/v1/products/{productId}/images/{imageId} - Xóa ảnh sản phẩm
    // =========================================================================
    @Transactional(rollbackFor = Exception.class)
    public void deleteProductImage(Long productId, Long imageId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND, "Không tìm thấy sản phẩm có ID " + productId + "!"));

        ProductImage image = productImageRepository.findById(imageId)
                .orElseThrow(() -> new AppException(ErrorCode.STORAGE_FILE_NOT_FOUND, "Không tìm thấy ảnh có ID " + imageId + "!"));

        if (image.getProduct() == null || !productId.equals(image.getProduct().getProductId())) {
            throw new AppException(ErrorCode.STORAGE_FILE_NOT_FOUND, "Ảnh không thuộc sản phẩm này!");
        }

        String objectKey = image.getObjectKey();
        String bucket = image.getBucket() != null ? image.getBucket() : storageService.resolveBucket(StorageType.PRODUCTS);
        boolean wasPrimary = Boolean.TRUE.equals(image.getIsPrimary());
        String deletedUrl = image.getImageUrl();

        productImageRepository.delete(image);
        productImageRepository.flush();

        if (wasPrimary || Objects.equals(deletedUrl, product.getThumbnailUrl())) {
            List<ProductImage> remaining = productImageRepository.findByProduct_ProductIdOrderByDisplayOrderAsc(productId);
            if (remaining.isEmpty()) {
                product.setThumbnailUrl(null);
            } else {
                ProductImage nextPrimary = remaining.stream()
                        .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                        .findFirst()
                        .orElseGet(() -> {
                            ProductImage first = remaining.get(0);
                            first.setIsPrimary(true);
                            productImageRepository.save(first);
                            return first;
                        });
                product.setThumbnailUrl(nextPrimary.getImageUrl());
            }
            productRepository.save(product);
        }

        if (objectKey != null && !objectKey.isBlank()) {
            cleanupRemovedStorageObjectsAfterCommit(List.of(new RemovedStorageObject(bucket, objectKey)));
        }

        try {
            User operator = securityHelper.getCurrentUser();
            auditLogRepository.log(operator, "DELETE_PRODUCT_IMAGE", "ProductImage", imageId, objectKey != null ? objectKey : deletedUrl, null);
        } catch (Exception e) {
            log.warn("Không thể ghi audit log cho deleteProductImage: {}", e.getMessage());
        }
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

    private String resolveProductImageUrl(ProductImage image) {
        if (image == null) {
            return null;
        }
        if (image.getObjectKey() != null && !image.getObjectKey().isBlank()) {
            String bucket = image.getBucket() != null && !image.getBucket().isBlank()
                    ? image.getBucket()
                    : storageService.resolveBucket(StorageType.PRODUCTS);
            return storageService.getAccessUrl(bucket, image.getObjectKey());
        }
        return image.getImageUrl();
    }

    private Map<Long, String> getPrimaryImageUrlMap(Collection<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Map.of();
        }
        return productImageRepository.findByProduct_ProductIdInAndIsPrimaryTrue(productIds).stream()
                .filter(img -> img.getProduct() != null)
                .collect(Collectors.toMap(
                        img -> img.getProduct().getProductId(),
                        this::resolveProductImageUrl,
                        (a, b) -> a));
    }

    private List<ProductImage> buildProductImages(
            Product product,
            List<ImageInput> inputs,
            Map<Long, ProductImage> existingById,
            List<String> consumedUploadIds) {
        if (inputs == null || inputs.isEmpty()) {
            return List.of();
        }

        List<ProductImage> result = new ArrayList<>(inputs.size());
        int order = 1;
        for (ImageInput input : inputs) {
            int displayOrder = input.sortOrder() != null ? input.sortOrder() : order++;
            boolean isPrimary = Boolean.TRUE.equals(input.isPrimary());

            if (input.uploadId() != null && !input.uploadId().isBlank()) {
                String uploadId = input.uploadId().trim();
                UploadSessionData session = tokenRedisService.getUploadSession(uploadId);
                if (session == null) {
                    throw new AppException(ErrorCode.STORAGE_UPLOAD_SESSION_EXPIRED);
                }
                if (session.getStatus() != UploadStatus.COMPLETED) {
                    throw new AppException(ErrorCode.STORAGE_FILE_NOT_CONFIRMED);
                }
                if (input.objectKey() != null && !input.objectKey().isBlank()
                        && !input.objectKey().trim().equals(session.getObjectKey())) {
                    throw new AppException(ErrorCode.STORAGE_OBJECT_KEY_MISMATCH);
                }
                consumedUploadIds.add(uploadId);
                result.add(ProductImage.builder()
                        .product(product)
                        .imageUrl(storageService.getObjectUrl(session.getBucketName(), session.getObjectKey()))
                        .storageProvider(session.getStorageProvider())
                        .storageType(session.getStorageType())
                        .bucket(session.getBucketName())
                        .objectKey(session.getObjectKey())
                        .originalFileName(session.getOriginalFileName())
                        .contentType(session.getContentType())
                        .fileSize(session.getActualFileSize() != null ? session.getActualFileSize() : session.getExpectedFileSize())
                        .eTag(session.getETag())
                        .isPrimary(isPrimary)
                        .displayOrder(displayOrder)
                        .build());
                continue;
            }

            if (input.imageId() != null && existingById.containsKey(input.imageId())) {
                ProductImage prev = existingById.get(input.imageId());
                result.add(ProductImage.builder()
                        .product(product)
                        .imageUrl(prev.getImageUrl())
                        .storageProvider(prev.getStorageProvider())
                        .storageType(prev.getStorageType())
                        .bucket(prev.getBucket())
                        .objectKey(prev.getObjectKey())
                        .originalFileName(prev.getOriginalFileName())
                        .contentType(prev.getContentType())
                        .fileSize(prev.getFileSize())
                        .eTag(prev.getETag())
                        .isPrimary(isPrimary)
                        .displayOrder(displayOrder)
                        .build());
                continue;
            }

            if (input.imageUrl() != null && !input.imageUrl().isBlank()) {
                result.add(ProductImage.builder()
                        .product(product)
                        .imageUrl(input.imageUrl().trim())
                        .isPrimary(isPrimary)
                        .displayOrder(displayOrder)
                        .build());
                continue;
            }

            throw new AppException(ErrorCode.INVALID_REQUEST, "Mỗi ảnh sản phẩm phải có uploadId, imageId hoặc imageUrl.");
        }
        return result;
    }

    private String extractPrimaryImageUrl(List<ProductImage> images) {
        if (images == null || images.isEmpty()) {
            return null;
        }
        return images.stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                .map(ProductImage::getImageUrl)
                .findFirst()
                .orElse(images.get(0).getImageUrl());
    }

    private void cleanupConsumedUploadSessionsAfterCommit(List<String> uploadIds) {
        if (uploadIds == null || uploadIds.isEmpty()) {
            return;
        }
        runAfterCommit(() -> {
            for (String uploadId : uploadIds) {
                try {
                    tokenRedisService.deleteUploadSession(uploadId);
                } catch (Exception e) {
                    log.warn("Không thể xóa upload session {}: {}", uploadId, e.getMessage());
                }
            }
        });
    }

    private void cleanupRemovedStorageObjectsAfterCommit(List<RemovedStorageObject> removedObjects) {
        if (removedObjects == null || removedObjects.isEmpty()) {
            return;
        }
        runAfterCommit(() -> {
            for (RemovedStorageObject item : removedObjects) {
                try {
                    storageService.deleteObject(item.bucket(), item.objectKey());
                } catch (Exception e) {
                    log.error("ORPHAN_STORAGE_OBJECT cleanup failed bucket={}, key={}: {}",
                            item.bucket(), item.objectKey(), e.getMessage());
                    try {
                        tokenRedisService.addOrphanObject(item.bucket(), item.objectKey());
                    } catch (Exception ex) {
                        log.error("Không thể ghi orphan queue cho bucket={}, key={}: {}",
                                item.bucket(), item.objectKey(), ex.getMessage());
                    }
                }
            }
        });
    }

    private void runAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }

    private record ImageInput(
            Long imageId,
            String uploadId,
            String objectKey,
            String imageUrl,
            Boolean isPrimary,
            Integer sortOrder
    ) {
    }

    private record RemovedStorageObject(String bucket, String objectKey) {
    }
}
