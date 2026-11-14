package com.wedbackendservice.nextgen.service.shared;

import com.wedbackendservice.nextgen.dto.client.response.category.VariantSchemaField;
import com.wedbackendservice.nextgen.dto.client.response.product.*;
import com.wedbackendservice.nextgen.dto.portal.request.product.*;
import com.wedbackendservice.nextgen.dto.portal.response.product.*;
import com.wedbackendservice.nextgen.dto.session.UploadSessionData;
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
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final SecurityHelper securityHelper;
    private final TokenRedisService tokenRedisService;
    private final MinioStorageService minioStorageService;

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    private Map<Long, Integer> getSoldCountMap(Collection<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) return Map.of();
        return orderItemRepository.countSoldByProductIds(productIds).stream()
                .filter(r -> r != null && r[0] != null && r[1] != null)
                .collect(Collectors.toMap(r -> (Long) r[0], r -> ((Number) r[1]).intValue(), (a, b) -> a));
    }

    private ProductDetailResponse.VariantDetailResponse mapVariantDetailResponse(ProductVariant v, Inventory inv) {
        int availableQty = inv != null ? inv.getAvailableQty() : 0;
        boolean isActive = "ACTIVE".equalsIgnoreCase(v.getStatus());
        BigDecimal curPrice = v.getPrice();
        BigDecimal origPrice = v.getCompareAtPrice() != null && v.getCompareAtPrice().compareTo(BigDecimal.ZERO) > 0
                ? v.getCompareAtPrice()
                : curPrice;

        return ProductDetailResponse.VariantDetailResponse.builder()
                .variantId(v.getVariantId())
                .sku(v.getSku())
                .variantName(v.getVariantName())
                .color(v.getColor())
                .ramGb(v.getRamGb())
                .romGb(v.getRomGb())
                .connectionType(v.getConnectionType())
                .attributes(v.getAttributes())
                .listPrice(origPrice)
                .salePrice(curPrice)
                .compareAtPrice(origPrice)
                .price(curPrice)
                .imageUrl(v.getImageUrl())
                .availableQty(availableQty)
                .isActive(isActive)
                .build();
    }

    private static String stripAccents(String input) {
        if (input == null) return "";
        String normalized = java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace('đ', 'd').replace('Đ', 'D')
                .toLowerCase().trim();
    }

    private record SmartSearchIntent(
            Long detectedCategoryId,
            BigDecimal detectedMinPrice,
            BigDecimal detectedMaxPrice,
            Integer detectedRam,
            Integer detectedRom,
            String suggestedSort,
            List<String> requirementKeywords,
            List<String> coreTokens
    ) {}

    private Long resolveCategoryIdByKeyword(String keywordType, Long fallbackId) {
        try {
            List<Category> activeCategories = new ArrayList<>(categoryRepository.findAllActive());
            activeCategories.sort(Comparator.comparing(c -> c.getParent() != null));
            for (Category c : activeCategories) {
                String haystack = stripAccents((c.getCategoryName() != null ? c.getCategoryName() : "") + " " + (c.getSlug() != null ? c.getSlug() : ""));
                if ("phone".equals(keywordType) && !haystack.contains("phu kien") && (haystack.contains("dien thoai") || haystack.contains("phone"))) {
                    return c.getCategoryId();
                }
                if ("tablet".equals(keywordType) && !haystack.contains("phu kien") && (haystack.contains("may tinh bang") || haystack.contains("tablet") || haystack.contains("ipad"))) {
                    return c.getCategoryId();
                }
                if ("laptop".equals(keywordType) && !haystack.contains("phu kien") && (haystack.contains("laptop") || haystack.contains("may tinh xach tay") || haystack.contains("macbook"))) {
                    return c.getCategoryId();
                }
                if ("accessory".equals(keywordType) && (haystack.contains("phu kien") || haystack.contains("am thanh") || haystack.contains("accessory"))) {
                    return c.getCategoryId();
                }
            }
        } catch (Exception ignored) {
        }
        return fallbackId;
    }

    private SmartSearchIntent parseSmartSearch(String rawSearch) {
        if (rawSearch == null || rawSearch.isBlank()) {
            return new SmartSearchIntent(null, null, null, null, null, null, List.of(), List.of());
        }
        String text = rawSearch.toLowerCase().trim();
        String ascii = stripAccents(text);

        Long catId = null;
        BigDecimal minP = null;
        BigDecimal maxP = null;
        Integer ram = null;
        Integer rom = null;
        String sort = null;
        List<String> reqKeywords = new ArrayList<>();

        // 1. Nhận diện Danh mục (tra cứu động từ DB, fallback về ID chuẩn nếu chưa seed)
        if (ascii.contains("dien thoai") || ascii.contains("smartphone") || ascii.matches(".*\\bphone\\b.*")) {
            catId = resolveCategoryIdByKeyword("phone", 1L);
            text = text.replaceAll("(?i)(điện thoại di động|điện thoại|dien thoai di dong|dien thoai|smartphone|\\bphone\\b)", " ");
        } else if (ascii.contains("may tinh bang") || ascii.contains("tablet")) {
            catId = resolveCategoryIdByKeyword("tablet", 2L);
            text = text.replaceAll("(?i)(máy tính bảng|may tinh bang|tablet)", " ");
        } else if (ascii.contains("laptop") || ascii.contains("may tinh xach tay") || ascii.contains("macbook")) {
            catId = resolveCategoryIdByKeyword("laptop", 3L);
            text = text.replaceAll("(?i)(máy tính xách tay|may tinh xach tay|laptop)", " ");
        } else if (ascii.contains("phu kien") || ascii.contains("tai nghe") || ascii.contains("cu sac") || ascii.contains("pin du phong") || ascii.contains("sac du phong") || ascii.contains("loa ")) {
            catId = resolveCategoryIdByKeyword("accessory", 4L);
            text = text.replaceAll("(?i)(phụ kiện di động|phụ kiện|phu kien di dong|phu kien)", " ");
        }

        // 2. Nhận diện Khoảng giá cụ thể (từ X đến Y triệu, dưới X triệu, trên X triệu)
        String asciiClean = stripAccents(text);
        java.util.regex.Matcher rangeM = java.util.regex.Pattern
                .compile("(?:tu\\s*)?(\\d+(?:[.,]\\d+)?)\\s*(?:trieu|tr|cu)?\\s*(?:den|-|toi)\\s*(\\d+(?:[.,]\\d+)?)\\s*(trieu|tr|cu)")
                .matcher(asciiClean);
        if (rangeM.find()) {
            minP = BigDecimal.valueOf(Double.parseDouble(rangeM.group(1).replace(',', '.')) * 1_000_000);
            maxP = BigDecimal.valueOf(Double.parseDouble(rangeM.group(2).replace(',', '.')) * 1_000_000);
            text = text.replaceAll("(?i)(từ|tu)?\\s*\\d+([.,]\\d+)?\\s*(triệu|trieu|tr|củ|cu)?\\s*(đến|den|-|tới|toi)\\s*\\d+([.,]\\d+)?\\s*(triệu|trieu|tr|củ|cu)", " ");
        } else {
            java.util.regex.Matcher underM = java.util.regex.Pattern
                    .compile("(?:duoi|nho hon|<|<=)\\s*(\\d+(?:[.,]\\d+)?)\\s*(trieu|tr|cu|k|nghin|ngan)")
                    .matcher(asciiClean);
            if (underM.find()) {
                double val = Double.parseDouble(underM.group(1).replace(',', '.'));
                String unit = underM.group(2);
                double mult = (unit.startsWith("k") || unit.startsWith("ngh") || unit.startsWith("nga")) ? 1_000 : 1_000_000;
                maxP = BigDecimal.valueOf(val * mult);
                text = text.replaceAll("(?i)(dưới|duoi|nhỏ hơn|nho hon|<=|<)\\s*\\d+([.,]\\d+)?\\s*(triệu|trieu|tr|củ|cu|k|nghìn|nghin|ngàn|ngan)", " ");
            }
            java.util.regex.Matcher overM = java.util.regex.Pattern
                    .compile("(?:tren|lon hon|>|>=|tu)\\s*(\\d+(?:[.,]\\d+)?)\\s*(trieu|tr|cu)")
                    .matcher(asciiClean);
            if (overM.find()) {
                double val = Double.parseDouble(overM.group(1).replace(',', '.'));
                minP = BigDecimal.valueOf(val * 1_000_000);
                text = text.replaceAll("(?i)(trên|tren|lớn hơn|lon hon|>=|>|từ|tu)\\s*\\d+([.,]\\d+)?\\s*(triệu|trieu|tr|củ|cu)", " ");
            }
        }

        // 3. Nhận diện Ý định Giá rẻ / Cao cấp
        asciiClean = stripAccents(text);
        if (asciiClean.matches(".*\\b(gia re|re nhat|binh dan|tiet kiem|gia tot|gia thap|sinh vien|re)\\b.*")) {
            sort = "priceAsc";
            text = text.replaceAll("(?i)(giá rẻ nhất|giá rẻ|rẻ nhất|bình dân|tiết kiệm|giá tốt|giá thấp|sinh viên|gia re nhat|gia re|re nhat|binh dan|tiet kiem|gia tot|gia thap|sinh vien|\\brẻ\\b|\\bre\\b)", " ");
        } else if (asciiClean.matches(".*\\b(cao cap|flagship|dat nhat|gia cao|sang trong|hang sang)\\b.*")) {
            sort = "priceDesc";
            text = text.replaceAll("(?i)(cao cấp|cao cap|flagship|đắt nhất|dat nhat|giá cao|gia cao|sang trọng|sang trong|hạng sang|hang sang)", " ");
        }

        // 4. Nhận diện RAM / ROM
        asciiClean = stripAccents(text);
        java.util.regex.Matcher ramM = java.util.regex.Pattern.compile("ram\\s*(\\d{1,2})\\s*(?:gb|g)?").matcher(asciiClean);
        if (ramM.find()) {
            ram = Integer.parseInt(ramM.group(1));
            text = text.replaceAll("(?i)ram\\s*\\d{1,2}\\s*(gb|g)?", " ");
        }
        java.util.regex.Matcher romM = java.util.regex.Pattern.compile("(?:rom|ssd|bo nho)?\\s*(64|128|256|512|1024)\\s*(?:gb|g)|\\b(1)\\s*tb\\b").matcher(asciiClean);
        if (romM.find()) {
            rom = romM.group(1) != null ? Integer.parseInt(romM.group(1)) : 1024;
            text = text.replaceAll("(?i)(rom|ssd|bộ nhớ|bo nho)?\\s*(64|128|256|512|1024)\\s*(gb|g)|\\b1\\s*tb\\b", " ");
        }

        // 5. Nhận diện Nhu cầu sử dụng (Gaming, Văn phòng, Đồ họa, Camera, Pin trâu, Chống ồn, Màn gập)
        asciiClean = stripAccents(text);
        if (asciiClean.matches(".*\\b(gaming|choi game|chien game|cau hinh manh|game)\\b.*")) {
            reqKeywords.addAll(List.of("gaming", "rog", "tuf", "legion", "rtx", "120hz", "144hz", "a17 pro", "a18 pro", "snapdragon 8", "m3 pro", "m3 max"));
            text = text.replaceAll("(?i)(chơi game|choi game|chiến game|chien game|cấu hình mạnh|cau hinh manh|gaming|\\bgame\\b)", " ");
        }
        if (asciiClean.matches(".*\\b(van phong|mong nhe|hoc tap)\\b.*")) {
            reqKeywords.addAll(List.of("văn phòng", "mỏng nhẹ", "air", "macbook", "ipad", "zenbook", "vivobook"));
            text = text.replaceAll("(?i)(văn phòng|van phong|mỏng nhẹ|mong nhe|học tập|hoc tap)", " ");
        }
        if (asciiClean.matches(".*\\b(do hoa|ky thuat|render|thiet ke)\\b.*")) {
            reqKeywords.addAll(List.of("đồ họa", "pro", "ultra", "m3", "rtx", "xdr", "amoled"));
            text = text.replaceAll("(?i)(thiết kế đồ họa|đồ họa|do hoa|kỹ thuật|ky thuat|render)", " ");
        }
        if (asciiClean.matches(".*\\b(chup anh|camera|nhiep anh|zoom|quay phim)\\b.*")) {
            reqKeywords.addAll(List.of("camera", "leica", "zoom", "48mp", "50mp", "200mp", "nhiếp ảnh", "ultra", "pro"));
            text = text.replaceAll("(?i)(chụp ảnh đẹp|chụp ảnh|chup anh dep|chup anh|nhiếp ảnh|nhiep anh|camera đẹp|camera|zoom quang|zoom|quay phim)", " ");
        }
        if (asciiClean.matches(".*\\b(pin trau|pin khung|pin lau|sac nhanh)\\b.*")) {
            reqKeywords.addAll(List.of("5000", "6000", "30 giờ", "sạc nhanh", "gan", "ultra", "pro max"));
            text = text.replaceAll("(?i)(pin trâu|pin trau|pin khủng|pin khung|pin lâu|pin lau|sạc nhanh|sac nhanh)", " ");
        }
        if (asciiClean.matches(".*\\b(man hinh gap|gap)\\b.*")) {
            reqKeywords.addAll(List.of("fold", "flip", "gập"));
            text = text.replaceAll("(?i)(màn hình gập|man hinh gap|điện thoại gập|dien thoai gap|\\bgập\\b|\\bgap\\b)", " ");
        }
        if (asciiClean.matches(".*\\b(chong on|anc)\\b.*")) {
            reqKeywords.addAll(List.of("chống ồn", "anc", "xm5", "airpods pro"));
            text = text.replaceAll("(?i)(chống ồn chủ động|chống ồn|chong on|anc)", " ");
        }
        text = text.replaceAll("(?i)(giảm giá|giam gia|khuyến mãi|khuyen mai|flash sale|\\bsale\\b|trả góp 0%|trả góp|tra gop|chính hãng|chinh hang|mới nhất|moi nhat|tốt nhất|tot nhat|đẹp|dep)", " ");

        // 6. Tách từ khóa gốc còn lại (bỏ stop-words tiếng Việt)
        Set<String> stopWords = Set.of("cua", "cho", "loai", "dong", "may", "can", "mua", "tim", "co", "voi", "va", "hoac", "nhung", "cac", "chiec", "con");
        List<String> tokens = Arrays.stream(text.trim().split("\\s+"))
                .map(String::trim)
                .filter(t -> t.length() >= 2 && !stopWords.contains(stripAccents(t)))
                .toList();

        return new SmartSearchIntent(catId, minP, maxP, ram, rom, sort, reqKeywords, tokens);
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

        SmartSearchIntent parsed = parseSmartSearch(search);
        Long effectiveCategoryId = categoryId != null ? categoryId : parsed.detectedCategoryId();
        BigDecimal effectiveMinPrice = minPrice != null ? minPrice : parsed.detectedMinPrice();
        BigDecimal effectiveMaxPrice = maxPrice != null ? maxPrice : parsed.detectedMaxPrice();
        Integer effectiveRam = ram != null ? ram : parsed.detectedRam();
        Integer effectiveRom = rom != null ? rom : parsed.detectedRom();
        String effectiveSortBy = ((sortBy == null || "newest".equalsIgnoreCase(sortBy)) && parsed.suggestedSort() != null)
                ? parsed.suggestedSort()
                : sortBy;

        Specification<Product> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), "ACTIVE"));

            var categoryJoin = root.join("category", jakarta.persistence.criteria.JoinType.LEFT);
            var parentCategoryJoin = categoryJoin.join("parent", jakarta.persistence.criteria.JoinType.LEFT);
            var brandJoin = root.join("brand", jakarta.persistence.criteria.JoinType.LEFT);

            if (brandId != null) {
                predicates.add(cb.equal(brandJoin.get("brandId"), brandId));
            }
            if (effectiveCategoryId != null) {
                predicates.add(cb.or(
                        cb.equal(categoryJoin.get("categoryId"), effectiveCategoryId),
                        cb.equal(parentCategoryJoin.get("categoryId"), effectiveCategoryId)
                ));
            }

            // Nhu cầu sử dụng (Gaming, Văn phòng, Đồ họa, Camera, Pin...)
            if (!parsed.requirementKeywords().isEmpty()) {
                List<Predicate> reqOrPredicates = new ArrayList<>();
                for (String kw : parsed.requirementKeywords()) {
                    String pattern = "%" + kw.toLowerCase() + "%";
                    reqOrPredicates.add(cb.like(cb.lower(root.get("productName")), pattern));
                    reqOrPredicates.add(cb.like(cb.lower(cb.coalesce(root.get("description"), "")), pattern));
                    reqOrPredicates.add(cb.like(cb.lower(cb.coalesce(categoryJoin.get("categoryName"), "")), pattern));

                    Subquery<Long> specSub = query.subquery(Long.class);
                    var specRoot = specSub.from(ProductSpec.class);
                    specSub.select(specRoot.get("product").get("productId"))
                            .where(
                                    cb.equal(specRoot.get("product").get("productId"), root.get("productId")),
                                    cb.or(
                                            cb.like(cb.lower(cb.coalesce(specRoot.get("specName"), "")), pattern),
                                            cb.like(cb.lower(cb.coalesce(specRoot.get("specValue"), "")), pattern)
                                    )
                            );
                    reqOrPredicates.add(cb.exists(specSub));
                }
                predicates.add(cb.or(reqOrPredicates.toArray(new Predicate[0])));
            }

            // Tìm theo các từ khóa gốc còn lại (Tên SP, Slug, Mô tả, Hãng, Danh mục, SKU, Specs)
            if (!parsed.coreTokens().isEmpty()) {
                for (String token : parsed.coreTokens()) {
                    String pattern = "%" + token.toLowerCase() + "%";
                    String asciiPattern = "%" + stripAccents(token) + "%";

                    Subquery<Long> varSub = query.subquery(Long.class);
                    var varRoot = varSub.from(ProductVariant.class);
                    varSub.select(varRoot.get("product").get("productId"))
                            .where(
                                    cb.equal(varRoot.get("product").get("productId"), root.get("productId")),
                                    cb.or(
                                            cb.like(cb.lower(cb.coalesce(varRoot.get("sku"), "")), pattern),
                                            cb.like(cb.lower(cb.coalesce(varRoot.get("color"), "")), pattern),
                                            cb.like(cb.lower(cb.coalesce(varRoot.get("variantName"), "")), pattern),
                                            cb.like(cb.lower(cb.coalesce(varRoot.get("connectionType"), "")), pattern)
                                    )
                            );

                    Subquery<Long> specSub = query.subquery(Long.class);
                    var specRoot = specSub.from(ProductSpec.class);
                    specSub.select(specRoot.get("product").get("productId"))
                            .where(
                                    cb.equal(specRoot.get("product").get("productId"), root.get("productId")),
                                    cb.or(
                                            cb.like(cb.lower(cb.coalesce(specRoot.get("specName"), "")), pattern),
                                            cb.like(cb.lower(cb.coalesce(specRoot.get("specValue"), "")), pattern)
                                    )
                            );

                    predicates.add(cb.or(
                            cb.like(cb.lower(root.get("productName")), pattern),
                            cb.like(cb.lower(cb.coalesce(root.get("slug"), "")), asciiPattern),
                            cb.like(cb.lower(cb.coalesce(root.get("description"), "")), pattern),
                            cb.like(cb.lower(cb.coalesce(brandJoin.get("brandName"), "")), pattern),
                            cb.like(cb.lower(cb.coalesce(brandJoin.get("slug"), "")), asciiPattern),
                            cb.like(cb.lower(cb.coalesce(categoryJoin.get("categoryName"), "")), pattern),
                            cb.like(cb.lower(cb.coalesce(categoryJoin.get("slug"), "")), asciiPattern),
                            cb.like(cb.lower(cb.coalesce(parentCategoryJoin.get("categoryName"), "")), pattern),
                            cb.like(cb.lower(cb.coalesce(parentCategoryJoin.get("slug"), "")), asciiPattern),
                            cb.exists(varSub),
                            cb.exists(specSub)
                    ));
                }
            }

            if (effectiveMinPrice != null || effectiveMaxPrice != null || effectiveRam != null || effectiveRom != null) {
                Subquery<Long> subquery = query.subquery(Long.class);
                var variantRoot = subquery.from(ProductVariant.class);
                List<Predicate> variantPredicates = new ArrayList<>();
                variantPredicates.add(cb.equal(variantRoot.get("product").get("productId"), root.get("productId")));
                variantPredicates.add(cb.equal(variantRoot.get("status"), "ACTIVE"));

                if (effectiveMinPrice != null) {
                    variantPredicates.add(cb.greaterThanOrEqualTo(variantRoot.get("price"), effectiveMinPrice));
                }
                if (effectiveMaxPrice != null) {
                    variantPredicates.add(cb.lessThanOrEqualTo(variantRoot.get("price"), effectiveMaxPrice));
                }
                if (effectiveRam != null) {
                    variantPredicates.add(cb.equal(variantRoot.get("ramGb"), effectiveRam));
                }
                if (effectiveRom != null) {
                    variantPredicates.add(cb.equal(variantRoot.get("romGb"), effectiveRom));
                }

                subquery.select(variantRoot.get("product").get("productId"))
                        .where(variantPredicates.toArray(new Predicate[0]));
                predicates.add(cb.exists(subquery));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        if ("priceAsc".equalsIgnoreCase(effectiveSortBy) || "price_asc".equalsIgnoreCase(effectiveSortBy)) {
            sort = Sort.by(Sort.Direction.ASC, "basePrice");
        } else if ("priceDesc".equalsIgnoreCase(effectiveSortBy) || "price_desc".equalsIgnoreCase(effectiveSortBy)) {
            sort = Sort.by(Sort.Direction.DESC, "basePrice");
        } else if ("newest".equalsIgnoreCase(effectiveSortBy)) {
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
                    .map(ProductImage::getImageUrl)
                    .findFirst()
                    .orElseGet(() -> pImages.stream()
                            .min(Comparator.comparingInt(img -> img.getDisplayOrder() != null ? img.getDisplayOrder() : 9999))
                            .map(ProductImage::getImageUrl)
                            .orElse(product.getThumbnailUrl()));

            List<ProductDetailResponse.VariantDetailResponse> variantSummaries = pVariants.stream()
                    .map(v -> mapVariantDetailResponse(v, inventoryByVariant.get(v.getVariantId())))
                    .toList();

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
                    .variants(variantSummaries)
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
                List<PromotionVariant> promoVariants = promotionVariantRepository.findByPromotionIdInWithVariantAndProduct(promoIds);

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
                .map(v -> mapVariantDetailResponse(v, invMap.get(v.getVariantId())))
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
                .variantSchema(resolveEffectiveVariantSchema(product.getCategory()))
                .variants(variantResponses)
                .build();
    }

    // =========================================================================
    // 4.3B GET /api/v1/admin/products/{id} - Chi tiết sản phẩm đầy đủ cho Admin (bất kể trạng thái)
    // =========================================================================
    @Transactional(readOnly = true)
    public AdminProductDetailResponse getAdminProductDetail(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND, "Không tìm thấy sản phẩm có ID " + id + "!"));

        List<VariantSchemaField> effectiveSchema = resolveEffectiveVariantSchema(product.getCategory());

        List<ProductImage> images = productImageRepository.findByProduct_ProductIdOrderByDisplayOrderAsc(id);
        List<ProductSpec> specs = productSpecRepository.findByProduct_ProductIdOrderBySortOrderAsc(id);
        List<ProductVariant> variants = productVariantRepository.findByProduct_ProductId(id);

        List<Long> varIds = variants.stream().map(ProductVariant::getVariantId).toList();
        Map<Long, Inventory> inventoryMap = varIds.isEmpty() ? Map.of() :
                inventoryRepository.findByVariant_VariantIdIn(varIds).stream()
                        .filter(i -> i.getVariant() != null)
                        .collect(Collectors.toMap(i -> i.getVariant().getVariantId(), i -> i, (a, b) -> a));

        List<AdminProductDetailResponse.AdminProductImageResponse> imageResponses = images.stream().map(img -> {
            String url = img.getImageUrl() != null ? img.getImageUrl() : "";
            String objectKey = img.getObjectKey();
            String bucket = img.getBucket();
            if (objectKey == null || objectKey.isBlank()) {
                objectKey = url;
                if (url.contains("/products/")) {
                    objectKey = url.substring(url.indexOf("/products/") + "/products/".length());
                    if (objectKey.contains("?")) {
                        objectKey = objectKey.substring(0, objectKey.indexOf("?"));
                    }
                }
            }
            if (bucket == null || bucket.isBlank()) {
                bucket = "products";
            }
            return AdminProductDetailResponse.AdminProductImageResponse.builder()
                    .imageId(img.getImageId())
                    .imageUrl(url)
                    .objectKey(objectKey)
                    .bucket(bucket)
                    .isPrimary(Boolean.TRUE.equals(img.getIsPrimary()))
                    .sortOrder(img.getSortOrder())
                    .build();
        }).toList();

        List<AdminProductDetailResponse.AdminProductSpecResponse> specResponses = specs.stream().map(s ->
                AdminProductDetailResponse.AdminProductSpecResponse.builder()
                        .specId(s.getSpecId())
                        .groupName(s.getGroupName())
                        .specName(s.getSpecName())
                        .specValue(s.getSpecValue())
                        .sortOrder(s.getSortOrder())
                        .build()
        ).toList();

        List<AdminProductDetailResponse.AdminProductVariantResponse> variantResponses = variants.stream().map(v -> {
            Inventory inv = inventoryMap.get(v.getVariantId());
            boolean hasTx = orderItemRepository.existsByVariant_VariantId(v.getVariantId())
                    || inventoryTransactionRepository.existsByVariant_VariantId(v.getVariantId());

            AdminProductDetailResponse.AdminVariantInventoryResponse invResp = null;
            if (inv != null) {
                invResp = AdminProductDetailResponse.AdminVariantInventoryResponse.builder()
                        .inventoryId(inv.getInventoryId())
                        .onHandQty(inv.getOnHandQty())
                        .reservedQty(inv.getReservedQty())
                        .availableQty(inv.getAvailableQty())
                        .minThreshold(inv.getMinThreshold() != null ? inv.getMinThreshold() : 5)
                        .build();
            }

            BigDecimal curPrice = v.getPrice();
            BigDecimal origPrice = v.getCompareAtPrice() != null && v.getCompareAtPrice().compareTo(BigDecimal.ZERO) > 0
                    ? v.getCompareAtPrice()
                    : curPrice;

            return AdminProductDetailResponse.AdminProductVariantResponse.builder()
                    .variantId(v.getVariantId())
                    .sku(v.getSku())
                    .variantName(v.getVariantName() != null ? v.getVariantName() : buildVariantNameFromSchema(product.getProductName(), v, effectiveSchema))
                    .color(v.getColor())
                    .ramGb(v.getRamGb())
                    .romGb(v.getRomGb())
                    .connectionType(v.getConnectionType())
                    .attributes(v.getAttributes())
                    .listPrice(origPrice)
                    .salePrice(curPrice)
                    .compareAtPrice(origPrice)
                    .price(curPrice)
                    .imageUrl(v.getImageUrl())
                    .isActive("ACTIVE".equalsIgnoreCase(v.getStatus()))
                    .hasTransactions(hasTx)
                    .inventory(invResp)
                    .build();
        }).toList();

        return AdminProductDetailResponse.builder()
                .productId(product.getProductId())
                .name(product.getProductName())
                .categoryId(product.getCategory() != null ? product.getCategory().getCategoryId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getCategoryName() : null)
                .brandId(product.getBrand() != null ? product.getBrand().getBrandId() : null)
                .brandName(product.getBrand() != null ? product.getBrand().getBrandName() : null)
                .description(product.getDescription())
                .warrantyPolicy(product.getWarrantyPolicy())
                .status(product.getStatus())
                .effectiveVariantSchema(effectiveSchema)
                .images(imageResponses)
                .specs(specResponses)
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

        List<ResolvedImageData> resolvedImages = new ArrayList<>();
        List<String> consumedUploadIds = new ArrayList<>();
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            int order = 1;
            for (CreateProductRequest.ImageItemRequest img : request.getImages()) {
                int sortOrder = img.getSortOrder() != null ? img.getSortOrder() : order++;
                boolean isPrimary = Boolean.TRUE.equals(img.getIsPrimary());
                if (img.getUploadId() != null && !img.getUploadId().isBlank()) {
                    String uploadId = img.getUploadId().trim();
                    UploadSessionData session = validateCompletedUploadSession(uploadId, null);
                    String resolvedUrl = minioStorageService.getFileUrl(session.getBucket(), session.getObjectKey());
                    resolvedImages.add(new ResolvedImageData(
                            resolvedUrl, session.getBucket(), session.getObjectKey(),
                            session.getOriginalFileName(), session.getContentType(),
                            session.getActualSize() != null ? session.getActualSize() : session.getExpectedSize(),
                            session.getEtag(), isPrimary, sortOrder));
                    consumedUploadIds.add(uploadId);
                } else {
                    validateImageUrlNotBase64(img.getImageUrl());
                    resolvedImages.add(new ResolvedImageData(
                            img.getImageUrl(), null, null, null, null, null, null, isPrimary, sortOrder));
                }
            }
        }

        String primaryImageUrl = null;
        if (!resolvedImages.isEmpty()) {
            primaryImageUrl = resolvedImages.stream()
                    .filter(ResolvedImageData::isPrimary)
                    .map(ResolvedImageData::imageUrl)
                    .findFirst()
                    .orElse(resolvedImages.get(0).imageUrl());
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
        for (ResolvedImageData img : resolvedImages) {
            ProductImage image = ProductImage.builder()
                    .product(savedProduct)
                    .imageUrl(img.imageUrl())
                    .bucket(img.bucket())
                    .objectKey(img.objectKey())
                    .originalFileName(img.originalFileName())
                    .contentType(img.contentType())
                    .fileSize(img.fileSize())
                    .etag(img.etag())
                    .isPrimary(img.isPrimary())
                    .displayOrder(img.sortOrder())
                    .build();
            productImageRepository.save(image);
            totalImages++;
        }

        if (!consumedUploadIds.isEmpty()) {
            List<String> idsToDelete = List.copyOf(consumedUploadIds);
            runAfterCommit(() -> idsToDelete.forEach(tokenRedisService::deleteUploadSession));
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
        List<VariantSchemaField> schema = resolveEffectiveVariantSchema(category);
        List<ProductVariant> createdVariantsList = new ArrayList<>();
        List<CreateProductResponse.CreatedVariantSummary> createdVariants = new ArrayList<>();
        for (CreateProductRequest.VariantItemRequest vReq : request.getVariants()) {
            Map<String, Object> sanitizedAttrs = validateAndSanitizeVariant(
                    vReq.getSku(), vReq.getColor(), vReq.getRamGb(), vReq.getRomGb(),
                    vReq.getConnectionType(), vReq.getAttributes(), schema
            );

            checkDuplicateVariantCombination(
                    savedProduct.getProductId(), vReq.getColor(), vReq.getRamGb(), vReq.getRomGb(),
                    vReq.getConnectionType(), sanitizedAttrs, createdVariantsList
            );

            String variantImageUrl = primaryImageUrl;
            if (vReq.getImageUrl() != null && !vReq.getImageUrl().isBlank()) {
                validateImageUrlNotBase64(vReq.getImageUrl());
                variantImageUrl = vReq.getImageUrl().trim();
            }

            ProductVariant variant = ProductVariant.builder()
                    .product(savedProduct)
                    .sku(vReq.getSku().trim())
                    .price(vReq.getListPrice())
                    .compareAtPrice(vReq.getListPrice())
                    .color(vReq.getColor())
                    .ramGb(vReq.getRamGb())
                    .romGb(vReq.getRomGb())
                    .connectionType(vReq.getConnectionType())
                    .attributes(sanitizedAttrs)
                    .status("ACTIVE")
                    .imageUrl(variantImageUrl)
                    .build();

            String variantName = buildVariantNameFromSchema(savedProduct.getProductName(), variant, schema);
            variant.setVariantName(variantName);

            ProductVariant savedVariant = productVariantRepository.save(variant);
            createdVariantsList.add(savedVariant);

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
            List<ProductImage> existingImages = productImageRepository.findByProduct_ProductIdOrderByDisplayOrderAsc(id);
            Map<Long, ProductImage> existingById = existingImages.stream()
                    .filter(img -> img.getImageId() != null)
                    .collect(Collectors.toMap(ProductImage::getImageId, img -> img, (a, b) -> a));
            Map<String, ProductImage> existingByUrl = existingImages.stream()
                    .filter(img -> img.getImageUrl() != null && !img.getImageUrl().isBlank())
                    .collect(Collectors.toMap(ProductImage::getImageUrl, img -> img, (a, b) -> a));

            List<ResolvedImageData> resolvedImages = new ArrayList<>();
            List<String> consumedUploadIds = new ArrayList<>();
            int order = 1;
            for (UpdateProductRequest.UpdateImageItem imgReq : request.getImages()) {
                int sortOrder = imgReq.getSortOrder() != null ? imgReq.getSortOrder() : order++;
                boolean isPrimary = Boolean.TRUE.equals(imgReq.getIsPrimary());
                if (imgReq.getUploadId() != null && !imgReq.getUploadId().isBlank()) {
                    String uploadId = imgReq.getUploadId().trim();
                    UploadSessionData session = validateCompletedUploadSession(uploadId, id);
                    String resolvedUrl = minioStorageService.getFileUrl(session.getBucket(), session.getObjectKey());
                    resolvedImages.add(new ResolvedImageData(
                            resolvedUrl, session.getBucket(), session.getObjectKey(),
                            session.getOriginalFileName(), session.getContentType(),
                            session.getActualSize() != null ? session.getActualSize() : session.getExpectedSize(),
                            session.getEtag(), isPrimary, sortOrder));
                    consumedUploadIds.add(uploadId);
                } else {
                    validateImageUrlNotBase64(imgReq.getImageUrl());
                    ProductImage matched = imgReq.getImageId() != null
                            ? existingById.get(imgReq.getImageId())
                            : (imgReq.getImageUrl() != null ? existingByUrl.get(imgReq.getImageUrl()) : null);
                    String resolvedUrl = (imgReq.getImageUrl() != null && !imgReq.getImageUrl().isBlank())
                            ? imgReq.getImageUrl()
                            : (matched != null ? matched.getImageUrl() : null);
                    validateImageUrlNotBase64(resolvedUrl);
                    resolvedImages.add(new ResolvedImageData(
                            resolvedUrl,
                            matched != null ? matched.getBucket() : null,
                            matched != null ? matched.getObjectKey() : null,
                            matched != null ? matched.getOriginalFileName() : null,
                            matched != null ? matched.getContentType() : null,
                            matched != null ? matched.getFileSize() : null,
                            matched != null ? matched.getEtag() : null,
                            isPrimary,
                            sortOrder));
                }
            }

            Set<String> retainedKeys = resolvedImages.stream()
                    .filter(r -> r.objectKey() != null && !r.objectKey().isBlank())
                    .map(r -> (r.bucket() != null ? r.bucket() : "products") + "/" + r.objectKey())
                    .collect(Collectors.toSet());

            List<MinioObjectRef> removedObjects = existingImages.stream()
                    .filter(old -> old.getObjectKey() != null && !old.getObjectKey().isBlank())
                    .filter(old -> {
                        String b = old.getBucket() != null && !old.getBucket().isBlank() ? old.getBucket() : "products";
                        return !retainedKeys.contains(b + "/" + old.getObjectKey());
                    })
                    .map(old -> new MinioObjectRef(
                            old.getBucket() != null && !old.getBucket().isBlank() ? old.getBucket() : "products",
                            old.getObjectKey()))
                    .toList();

            productImageRepository.deleteByProduct_ProductId(id);
            String newPrimaryUrl = null;
            for (ResolvedImageData resolved : resolvedImages) {
                ProductImage img = ProductImage.builder()
                        .product(product)
                        .imageUrl(resolved.imageUrl())
                        .bucket(resolved.bucket())
                        .objectKey(resolved.objectKey())
                        .originalFileName(resolved.originalFileName())
                        .contentType(resolved.contentType())
                        .fileSize(resolved.fileSize())
                        .etag(resolved.etag())
                        .isPrimary(resolved.isPrimary())
                        .displayOrder(resolved.sortOrder())
                        .build();
                productImageRepository.save(img);
                if (resolved.isPrimary() && newPrimaryUrl == null) {
                    newPrimaryUrl = resolved.imageUrl();
                }
            }
            if (newPrimaryUrl != null) {
                product.setThumbnailUrl(newPrimaryUrl);
            } else if (!resolvedImages.isEmpty()) {
                product.setThumbnailUrl(resolvedImages.get(0).imageUrl());
            } else {
                product.setThumbnailUrl(null);
            }

            if (!consumedUploadIds.isEmpty() || !removedObjects.isEmpty()) {
                List<String> idsToDelete = List.copyOf(consumedUploadIds);
                List<MinioObjectRef> objectsToDelete = List.copyOf(removedObjects);
                runAfterCommit(() -> {
                    idsToDelete.forEach(tokenRedisService::deleteUploadSession);
                    objectsToDelete.forEach(obj -> minioStorageService.deleteObject(obj.bucket(), obj.objectKey()));
                });
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

        List<VariantSchemaField> schema = resolveEffectiveVariantSchema(product.getCategory());
        Map<String, Object> sanitizedAttrs = validateAndSanitizeVariant(
                sku, request.getColor(), request.getRamGb(), request.getRomGb(),
                request.getConnectionType(), request.getAttributes(), schema
        );

        List<ProductVariant> existingVariants = productVariantRepository.findByProduct_ProductId(productId);
        checkDuplicateVariantCombination(
                productId, request.getColor(), request.getRamGb(), request.getRomGb(),
                request.getConnectionType(), sanitizedAttrs, existingVariants
        );

        BigDecimal salePrice = request.getSalePrice() != null && request.getSalePrice().compareTo(BigDecimal.ZERO) > 0
                ? request.getSalePrice()
                : request.getListPrice();
        BigDecimal compareAtPrice = request.getListPrice() != null && request.getListPrice().compareTo(BigDecimal.ZERO) > 0
                ? request.getListPrice()
                : salePrice;

        String variantImageUrl = product.getThumbnailUrl();
        if (request.getImageUrl() != null && !request.getImageUrl().isBlank()) {
            validateImageUrlNotBase64(request.getImageUrl());
            variantImageUrl = request.getImageUrl().trim();
        }

        ProductVariant variant = ProductVariant.builder()
                .product(product)
                .sku(sku)
                .color(request.getColor())
                .ramGb(request.getRamGb())
                .romGb(request.getRomGb())
                .connectionType(request.getConnectionType())
                .attributes(sanitizedAttrs)
                .price(salePrice)
                .compareAtPrice(compareAtPrice)
                .status("ACTIVE")
                .imageUrl(variantImageUrl)
                .build();

        String variantName = buildVariantNameFromSchema(product.getProductName(), variant, schema);
        variant.setVariantName(variantName);

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
                .imageUrl(savedVariant.getImageUrl())
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
    // 4.8 PUT /api/v1/admin/products/variants/{varId} - Điều chỉnh giá / bật tắt SKU / cập nhật thông số
    // =========================================================================
    @Transactional(rollbackFor = Exception.class)
    public UpdateVariantResponse updateVariant(Long varId, UpdateVariantRequest request) {
        ProductVariant variant = productVariantRepository.findById(varId)
                .orElseThrow(() -> new AppException(ErrorCode.VARIANT_NOT_FOUND, "Không tìm thấy biến thể có ID " + varId + "!"));

        BigDecimal oldPrice = variant.getPrice();
        String oldStatus = variant.getStatus();

        // 1. Mã SKU là định danh cố định của biến thể, tuyệt đối không được sửa sau khi tạo
        if (request.getSku() != null && !request.getSku().trim().equalsIgnoreCase(variant.getSku())) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Mã SKU biến thể là mã định danh duy nhất cố định trong hệ thống, không được phép chỉnh sửa sau khi tạo!");
        }

        // 2. Giá bán hiện hành (price) & Giá niêm yết gốc (compareAtPrice)
        BigDecimal effectiveSalePrice = request.getSalePrice() != null ? request.getSalePrice()
                : (request.getPrice() != null ? request.getPrice() : request.getListPrice());
        BigDecimal effectiveListPrice = request.getCompareAtPrice() != null ? request.getCompareAtPrice()
                : (request.getListPrice() != null ? request.getListPrice() : effectiveSalePrice);

        if (effectiveSalePrice != null && effectiveSalePrice.compareTo(BigDecimal.ZERO) > 0) {
            variant.setPrice(effectiveSalePrice);
        }
        if (effectiveListPrice != null && effectiveListPrice.compareTo(BigDecimal.ZERO) > 0) {
            variant.setCompareAtPrice(effectiveListPrice);
        } else if (effectiveSalePrice != null && variant.getCompareAtPrice() == null) {
            variant.setCompareAtPrice(effectiveSalePrice);
        }

        // 3. Trạng thái bán
        if (request.getIsActive() != null) {
            variant.setStatus(request.getIsActive() ? "ACTIVE" : "INACTIVE");
        }

        // 4. Cột vật lý
        if (request.getColor() != null) {
            variant.setColor(request.getColor());
        }
        if (request.getRamGb() != null) {
            variant.setRamGb(request.getRamGb());
        }
        if (request.getRomGb() != null) {
            variant.setRomGb(request.getRomGb());
        }
        if (request.getConnectionType() != null) {
            variant.setConnectionType(request.getConnectionType());
        }
        if (request.getImageUrl() != null && !request.getImageUrl().isBlank()) {
            validateImageUrlNotBase64(request.getImageUrl());
            variant.setImageUrl(request.getImageUrl());
        }

        // 5. Attributes và schema validation
        List<VariantSchemaField> schema = resolveEffectiveVariantSchema(variant.getProduct().getCategory());
        Map<String, Object> currentAttrs = request.getAttributes() != null ? request.getAttributes() : variant.getAttributes();
        Map<String, Object> sanitized = validateAndSanitizeVariant(
                variant.getSku(), variant.getColor(), variant.getRamGb(), variant.getRomGb(),
                variant.getConnectionType(), currentAttrs, schema
        );
        variant.setAttributes(sanitized);

        // Kiểm tra trùng tổ hợp biến thể với các biến thể khác cùng sản phẩm
        List<ProductVariant> otherVariants = productVariantRepository.findByProduct_ProductId(variant.getProduct().getProductId())
                .stream().filter(v -> !v.getVariantId().equals(varId)).toList();
        checkDuplicateVariantCombination(variant.getProduct().getProductId(),
                variant.getColor(), variant.getRamGb(), variant.getRomGb(),
                variant.getConnectionType(), sanitized, otherVariants);

        variant.setVariantName(buildVariantNameFromSchema(variant.getProduct().getProductName(), variant, schema));

        ProductVariant updated = productVariantRepository.save(variant);

        // 6. Cập nhật minThreshold trên Inventory nếu có truyền
        if (request.getMinThreshold() != null) {
            inventoryRepository.findByVariant_VariantId(varId).ifPresent(inv -> {
                inv.setMinThreshold(request.getMinThreshold());
                inventoryRepository.save(inv);
            });
        }

        Integer currentMinThreshold = inventoryRepository.findByVariant_VariantId(varId)
                .map(Inventory::getMinThreshold)
                .orElse(5);

        // 7. Tính lại product.basePrice = MIN(price) của các variant ACTIVE
        List<ProductVariant> allVariants = productVariantRepository.findByProduct_ProductId(variant.getProduct().getProductId());
        BigDecimal minActivePrice = allVariants.stream()
                .filter(v -> "ACTIVE".equalsIgnoreCase(v.getStatus()))
                .map(ProductVariant::getPrice)
                .filter(Objects::nonNull)
                .min(BigDecimal::compareTo)
                .orElse(variant.getPrice());
        Product product = variant.getProduct();
        product.setBasePrice(minActivePrice);
        productRepository.save(product);

        // 8. Audit Log
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
                .variantName(updated.getVariantName())
                .color(updated.getColor())
                .ramGb(updated.getRamGb())
                .romGb(updated.getRomGb())
                .connectionType(updated.getConnectionType())
                .attributes(updated.getAttributes())
                .minThreshold(currentMinThreshold)
                .listPrice(updated.getCompareAtPrice() != null ? updated.getCompareAtPrice() : updated.getPrice())
                .salePrice(updated.getPrice())
                .imageUrl(updated.getImageUrl())
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

        if ("INACTIVE".equalsIgnoreCase(newStatus) || "DISCONTINUED".equalsIgnoreCase(newStatus)) {
            List<ProductVariant> variants = productVariantRepository.findByProduct_ProductId(id);
            for (ProductVariant v : variants) {
                v.setStatus("INACTIVE");
            }
            productVariantRepository.saveAll(variants);
        } else if ("ACTIVE".equalsIgnoreCase(newStatus)) {
            List<ProductVariant> variants = productVariantRepository.findByProduct_ProductId(id);
            for (ProductVariant v : variants) {
                v.setStatus("ACTIVE");
            }
            productVariantRepository.saveAll(variants);
        }

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
    // DELETE /api/v1/products/{productId}/images/{imageId} - Xóa ảnh sản phẩm & object MinIO sau commit
    // =========================================================================
    @Transactional(rollbackFor = Exception.class)
    public void deleteProductImage(Long productId, Long imageId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND, "Không tìm thấy sản phẩm có ID " + productId + "!"));

        ProductImage image = productImageRepository.findByImageIdAndProduct_ProductId(imageId, productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_IMAGE_NOT_FOUND));

        boolean wasPrimary = Boolean.TRUE.equals(image.getIsPrimary());
        String deletedUrl = image.getImageUrl();
        String bucket = image.getBucket() != null && !image.getBucket().isBlank() ? image.getBucket() : "products";
        String objectKey = image.getObjectKey();

        productImageRepository.delete(image);

        if (wasPrimary || Objects.equals(product.getThumbnailUrl(), deletedUrl)) {
            List<ProductImage> remaining = productImageRepository.findByProduct_ProductIdOrderByDisplayOrderAsc(productId)
                    .stream()
                    .filter(img -> !Objects.equals(img.getImageId(), imageId))
                    .toList();
            if (!remaining.isEmpty()) {
                ProductImage nextPrimary = remaining.get(0);
                nextPrimary.setIsPrimary(true);
                productImageRepository.save(nextPrimary);
                product.setThumbnailUrl(nextPrimary.getImageUrl());
            } else {
                product.setThumbnailUrl(null);
            }
            productRepository.save(product);
        }

        if (objectKey != null && !objectKey.isBlank()) {
            runAfterCommit(() -> minioStorageService.deleteObject(bucket, objectKey));
        }
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================
    private UploadSessionData validateCompletedUploadSession(String uploadId, Long expectedProductId) {
        UploadSessionData session = tokenRedisService.getUploadSession(uploadId);
        if (session == null) {
            throw new AppException(ErrorCode.STORAGE_SESSION_NOT_FOUND,
                    "Phiên tải ảnh '" + uploadId + "' không tồn tại hoặc đã hết hạn.");
        }
        if (!UploadSessionData.STATUS_COMPLETED.equals(session.getStatus())) {
            throw new AppException(ErrorCode.STORAGE_SESSION_INVALID,
                    "Phiên tải ảnh '" + uploadId + "' chưa được xác nhận (COMPLETED).");
        }
        if (session.getProductId() != null && !Objects.equals(session.getProductId(), expectedProductId)) {
            throw new AppException(ErrorCode.STORAGE_SESSION_INVALID,
                    "Phiên tải ảnh '" + uploadId + "' không thuộc sản phẩm hiện tại.");
        }
        User currentUser = null;
        try {
            currentUser = securityHelper.getCurrentUser();
        } catch (AppException e) {
            throw e;
        } catch (Exception ignored) {
        }
        if (currentUser != null && session.getOwnerId() != null
                && !Objects.equals(session.getOwnerId(), currentUser.getUserId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED,
                    "Bạn không có quyền sử dụng phiên tải ảnh của người dùng khác.");
        }
        return session;
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

    private record ResolvedImageData(
            String imageUrl,
            String bucket,
            String objectKey,
            String originalFileName,
            String contentType,
            Long fileSize,
            String etag,
            boolean isPrimary,
            int sortOrder
    ) {}

    private record MinioObjectRef(String bucket, String objectKey) {}
    public List<VariantSchemaField> resolveEffectiveVariantSchema(Category category) {
        if (category == null) return null;
        Category current = category;
        int depth = 0;
        while (current != null && depth < 10) {
            if (current.getVariantSchema() != null && !current.getVariantSchema().isEmpty()) {
                return current.getVariantSchema();
            }
            current = current.getParent();
            depth++;
        }
        return null;
    }

    private Map<String, Object> validateAndSanitizeVariant(
            String sku,
            String color,
            Integer ramGb,
            Integer romGb,
            String connectionType,
            Map<String, Object> attributes,
            List<VariantSchemaField> schema
    ) {
        if (schema == null || schema.isEmpty()) {
            return attributes != null ? new HashMap<>(attributes) : new HashMap<>();
        }

        Map<String, Object> sanitizedAttrs = new HashMap<>();

        for (VariantSchemaField field : schema) {
            String key = field.getKey();
            boolean isRequired = Boolean.TRUE.equals(field.getRequired());
            Object value = null;

            if ("COLUMN".equalsIgnoreCase(field.getStorage())) {
                if ("color".equalsIgnoreCase(key)) value = color;
                else if ("ramGb".equalsIgnoreCase(key)) value = ramGb;
                else if ("romGb".equalsIgnoreCase(key)) value = romGb;
                else if ("connectionType".equalsIgnoreCase(key)) value = connectionType;
            } else {
                if (attributes != null && attributes.containsKey(key)) {
                    value = attributes.get(key);
                    if (value != null && !value.toString().trim().isBlank()) {
                        sanitizedAttrs.put(key, value);
                    } else {
                        value = null;
                    }
                }
            }

            if (isRequired && (value == null || (value instanceof String && ((String) value).trim().isBlank()))) {
                throw new AppException(ErrorCode.INVALID_REQUEST, "Thiếu thông tin bắt buộc: " + field.getLabel());
            }
        }

        return sanitizedAttrs;
    }

    private void checkDuplicateVariantCombination(
            Long productId,
            String color,
            Integer ramGb,
            Integer romGb,
            String connectionType,
            Map<String, Object> attributes,
            List<ProductVariant> existingVariants
    ) {
        if (existingVariants == null || existingVariants.isEmpty()) return;

        for (ProductVariant existing : existingVariants) {
            boolean sameColor = Objects.equals(
                    existing.getColor() != null ? existing.getColor().trim().toLowerCase() : null,
                    color != null ? color.trim().toLowerCase() : null
            );
            boolean sameRam = Objects.equals(existing.getRamGb(), ramGb);
            boolean sameRom = Objects.equals(existing.getRomGb(), romGb);
            boolean sameConn = Objects.equals(
                    existing.getConnectionType() != null ? existing.getConnectionType().trim().toLowerCase() : null,
                    connectionType != null ? connectionType.trim().toLowerCase() : null
            );
            boolean sameAttrs = Objects.equals(existing.getAttributes(), attributes);

            if (sameColor && sameRam && sameRom && sameConn && sameAttrs) {
                throw new AppException(ErrorCode.INVALID_REQUEST, "Trùng tổ hợp biến thể trong cùng sản phẩm!");
            }
        }
    }

    private String buildVariantNameFromSchema(String productName, ProductVariant variant, List<VariantSchemaField> schema) {
        if (schema == null || schema.isEmpty()) {
            return buildVariantName(productName, variant.getRomGb(), variant.getColor());
        }

        List<String> parts = new ArrayList<>();
        String colorPart = null;

        for (VariantSchemaField field : schema) {
            String key = field.getKey();
            Object val = null;
            if ("COLUMN".equalsIgnoreCase(field.getStorage())) {
                if ("color".equalsIgnoreCase(key)) {
                    colorPart = variant.getColor();
                    continue;
                } else if ("ramGb".equalsIgnoreCase(key) && variant.getRamGb() != null) {
                    val = variant.getRamGb() + (field.getUnit() != null ? field.getUnit() : "GB");
                } else if ("romGb".equalsIgnoreCase(key) && variant.getRomGb() != null) {
                    val = variant.getRomGb() + (field.getUnit() != null ? field.getUnit() : "GB");
                } else if ("connectionType".equalsIgnoreCase(key) && variant.getConnectionType() != null) {
                    val = variant.getConnectionType();
                }
            } else if (variant.getAttributes() != null && variant.getAttributes().containsKey(key)) {
                Object raw = variant.getAttributes().get(key);
                if (raw != null && !raw.toString().isBlank()) {
                    val = raw.toString().trim() + (field.getUnit() != null ? field.getUnit() : "");
                }
            }

            if (val != null && !val.toString().isBlank()) {
                parts.add(val.toString());
            }
        }

        StringBuilder sb = new StringBuilder();
        if (!parts.isEmpty()) {
            sb.append(String.join(" / ", parts));
        }
        if (colorPart != null && !colorPart.isBlank()) {
            if (sb.length() > 0) {
                sb.append(" - ");
            }
            sb.append(colorPart.trim());
        }
        return sb.length() > 0 ? sb.toString() : (productName != null ? productName : "");
    }

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

    private void validateImageUrlNotBase64(String imageUrl) {
        if (imageUrl != null && imageUrl.trim().toLowerCase().startsWith("data:")) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "Không hỗ trợ lưu trực tiếp dữ liệu ảnh base64 (data URI). Vui lòng upload ảnh lên MinIO qua /api/v1/storage/presigned-urls hoặc dùng URL ảnh hợp lệ.");
        }
    }

    private boolean slugExists(String slug, Long excludeProductId) {
        if (excludeProductId == null) {
            return productRepository.existsBySlug(slug);
        }
        return productRepository.existsBySlugAndProductIdNot(slug, excludeProductId);
    }
}
