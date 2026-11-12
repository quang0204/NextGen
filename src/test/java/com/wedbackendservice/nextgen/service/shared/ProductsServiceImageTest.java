package com.wedbackendservice.nextgen.service.shared;

import com.wedbackendservice.nextgen.dto.client.response.product.ProductDetailResponse;
import com.wedbackendservice.nextgen.dto.portal.request.product.CreateProductRequest;
import com.wedbackendservice.nextgen.dto.portal.request.product.UpdateProductRequest;
import com.wedbackendservice.nextgen.dto.portal.response.product.AdminProductDetailResponse;
import com.wedbackendservice.nextgen.dto.portal.response.product.CreateProductResponse;
import com.wedbackendservice.nextgen.dto.session.UploadSessionData;
import com.wedbackendservice.nextgen.entity.*;
import com.wedbackendservice.nextgen.enums.Role;
import com.wedbackendservice.nextgen.exception.AppException;
import com.wedbackendservice.nextgen.exception.ErrorCode;
import com.wedbackendservice.nextgen.repository.*;
import com.wedbackendservice.nextgen.security.SecurityHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductsServiceImageTest {

    @Mock private ProductRepository productRepository;
    @Mock private ProductVariantRepository productVariantRepository;
    @Mock private ProductImageRepository productImageRepository;
    @Mock private ProductSpecRepository productSpecRepository;
    @Mock private InventoryRepository inventoryRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private BrandRepository brandRepository;
    @Mock private PromotionRepository promotionRepository;
    @Mock private PromotionVariantRepository promotionVariantRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private InventoryTransactionRepository inventoryTransactionRepository;
    @Mock private SecurityHelper securityHelper;
    @Mock private TokenRedisService tokenRedisService;
    @Mock private MinioStorageService minioStorageService;

    @InjectMocks
    private ProductsService productsService;

    private Category category;
    private Brand brand;
    private User adminUser;

    @BeforeEach
    void setUp() {
        category = Category.builder().categoryId(1L).categoryName("Laptop").build();
        brand = Brand.builder().brandId(2L).brandName("Apple").build();
        adminUser = User.builder().userId(1L).role(Role.ADMIN).email("admin@nextgen.vn").build();
    }

    @Test
    @DisplayName("createProduct resolves COMPLETED uploadId from Redis, saves MinIO metadata, and deletes session")
    void createProduct_withUploadId_success() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(brandRepository.findById(2L)).thenReturn(Optional.of(brand));
        when(productVariantRepository.existsBySku("MBP-14")).thenReturn(false);
        when(securityHelper.getCurrentUser()).thenReturn(adminUser);

        UploadSessionData session = UploadSessionData.builder()
                .uploadId("up-1")
                .ownerId(1L)
                .bucket("products")
                .objectKey("temp/1/up-1-mbp.png")
                .originalFileName("mbp.png")
                .contentType("image/png")
                .actualSize(12345L)
                .etag("etag-1")
                .status(UploadSessionData.STATUS_COMPLETED)
                .build();
        when(tokenRedisService.getUploadSession("up-1")).thenReturn(session);
        when(minioStorageService.getFileUrl("products", "temp/1/up-1-mbp.png"))
                .thenReturn("http://localhost:9000/products/temp/1/up-1-mbp.png");

        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setProductId(50L);
            return p;
        });
        when(productVariantRepository.save(any(ProductVariant.class))).thenAnswer(inv -> {
            ProductVariant v = inv.getArgument(0);
            v.setVariantId(301L);
            return v;
        });
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(inv -> {
            Inventory i = inv.getArgument(0);
            i.setInventoryId(401L);
            return i;
        });

        CreateProductRequest request = CreateProductRequest.builder()
                .categoryId(1L)
                .brandId(2L)
                .name("MacBook Pro 14")
                .images(List.of(CreateProductRequest.ImageItemRequest.builder()
                        .uploadId("up-1")
                        .isPrimary(true)
                        .sortOrder(1)
                        .build()))
                .variants(List.of(CreateProductRequest.VariantItemRequest.builder()
                        .sku("MBP-14")
                        .color("Space Black")
                        .listPrice(BigDecimal.valueOf(49990000))
                        .initialOnHandQty(5)
                        .build()))
                .build();

        CreateProductResponse response = productsService.createProduct(request);

        assertThat(response.getProductId()).isEqualTo(50L);
        assertThat(response.getTotalImages()).isEqualTo(1);

        ArgumentCaptor<ProductImage> imageCaptor = ArgumentCaptor.forClass(ProductImage.class);
        verify(productImageRepository).save(imageCaptor.capture());
        ProductImage savedImg = imageCaptor.getValue();
        assertThat(savedImg.getImageUrl()).isEqualTo("http://localhost:9000/products/temp/1/up-1-mbp.png");
        assertThat(savedImg.getBucket()).isEqualTo("products");
        assertThat(savedImg.getObjectKey()).isEqualTo("temp/1/up-1-mbp.png");
        assertThat(savedImg.getFileSize()).isEqualTo(12345L);

        verify(tokenRedisService).deleteUploadSession("up-1");
    }

    @Test
    @DisplayName("createProduct preserves legacy imageUrl behavior when uploadId is absent")
    void createProduct_legacyImageUrl_preserved() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(brandRepository.findById(2L)).thenReturn(Optional.of(brand));
        when(productVariantRepository.existsBySku("LEGACY-1")).thenReturn(false);
        when(securityHelper.getCurrentUser()).thenReturn(adminUser);

        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setProductId(51L);
            return p;
        });
        when(productVariantRepository.save(any(ProductVariant.class))).thenAnswer(inv -> {
            ProductVariant v = inv.getArgument(0);
            v.setVariantId(302L);
            return v;
        });
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(inv -> {
            Inventory i = inv.getArgument(0);
            i.setInventoryId(402L);
            return i;
        });

        CreateProductRequest request = CreateProductRequest.builder()
                .categoryId(1L)
                .brandId(2L)
                .name("Legacy Product")
                .images(List.of(CreateProductRequest.ImageItemRequest.builder()
                        .imageUrl("https://cdn.nextgen.vn/products/legacy.png")
                        .isPrimary(true)
                        .build()))
                .variants(List.of(CreateProductRequest.VariantItemRequest.builder()
                        .sku("LEGACY-1")
                        .listPrice(BigDecimal.valueOf(1000000))
                        .initialOnHandQty(1)
                        .build()))
                .build();

        CreateProductResponse response = productsService.createProduct(request);

        assertThat(response.getProductId()).isEqualTo(51L);
        ArgumentCaptor<ProductImage> imageCaptor = ArgumentCaptor.forClass(ProductImage.class);
        verify(productImageRepository).save(imageCaptor.capture());
        assertThat(imageCaptor.getValue().getImageUrl()).isEqualTo("https://cdn.nextgen.vn/products/legacy.png");
        assertThat(imageCaptor.getValue().getObjectKey()).isNull();
        verifyNoInteractions(minioStorageService);
        verifyNoInteractions(tokenRedisService);
    }

    @Test
    @DisplayName("createProduct rejects unconfirmed PENDING uploadId")
    void createProduct_pendingUploadId_throwsException() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(brandRepository.findById(2L)).thenReturn(Optional.of(brand));
        when(productVariantRepository.existsBySku("MBP-14")).thenReturn(false);
        when(tokenRedisService.getUploadSession("up-pending")).thenReturn(UploadSessionData.builder()
                .uploadId("up-pending")
                .status(UploadSessionData.STATUS_PENDING)
                .build());

        CreateProductRequest request = CreateProductRequest.builder()
                .categoryId(1L)
                .brandId(2L)
                .name("MacBook Pro 14")
                .images(List.of(CreateProductRequest.ImageItemRequest.builder()
                        .uploadId("up-pending")
                        .isPrimary(true)
                        .build()))
                .variants(List.of(CreateProductRequest.VariantItemRequest.builder()
                        .sku("MBP-14")
                        .listPrice(BigDecimal.valueOf(49990000))
                        .initialOnHandQty(5)
                        .build()))
                .build();

        assertThatThrownBy(() -> productsService.createProduct(request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.STORAGE_SESSION_INVALID);
    }

    @Test
    @DisplayName("updateProduct removes omitted MinIO image objects and consumes new uploadIds")
    void updateProduct_replacesAndDeletesRemovedMinioObjects() {
        Product product = Product.builder()
                .productId(50L)
                .productName("MacBook Pro")
                .thumbnailUrl("http://localhost:9000/products/50/old-removed.png")
                .build();

        ProductImage keptImg = ProductImage.builder()
                .imageId(10L)
                .product(product)
                .imageUrl("http://localhost:9000/products/50/kept.png")
                .bucket("products")
                .objectKey("50/kept.png")
                .isPrimary(false)
                .displayOrder(1)
                .build();
        ProductImage removedImg = ProductImage.builder()
                .imageId(11L)
                .product(product)
                .imageUrl("http://localhost:9000/products/50/old-removed.png")
                .bucket("products")
                .objectKey("50/old-removed.png")
                .isPrimary(true)
                .displayOrder(2)
                .build();

        when(productRepository.findById(50L)).thenReturn(Optional.of(product));
        when(productImageRepository.findByProduct_ProductIdOrderByDisplayOrderAsc(50L))
                .thenReturn(List.of(keptImg, removedImg));
        when(tokenRedisService.getUploadSession("up-new")).thenReturn(UploadSessionData.builder()
                .uploadId("up-new")
                .ownerId(1L)
                .bucket("products")
                .objectKey("50/up-new-hero.png")
                .originalFileName("hero.png")
                .contentType("image/png")
                .actualSize(5000L)
                .etag("etag-new")
                .status(UploadSessionData.STATUS_COMPLETED)
                .build());
        when(minioStorageService.getFileUrl("products", "50/up-new-hero.png"))
                .thenReturn("http://localhost:9000/products/50/up-new-hero.png");
        when(securityHelper.getCurrentUser()).thenReturn(adminUser);
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateProductRequest request = UpdateProductRequest.builder()
                .images(List.of(
                        UpdateProductRequest.UpdateImageItem.builder()
                                .imageId(10L)
                                .isPrimary(false)
                                .sortOrder(1)
                                .build(),
                        UpdateProductRequest.UpdateImageItem.builder()
                                .uploadId("up-new")
                                .isPrimary(true)
                                .sortOrder(2)
                                .build()
                ))
                .build();

        productsService.updateProduct(50L, request);

        assertThat(product.getThumbnailUrl()).isEqualTo("http://localhost:9000/products/50/up-new-hero.png");
        verify(productImageRepository).deleteByProduct_ProductId(50L);
        verify(productImageRepository, times(2)).save(any(ProductImage.class));
        verify(tokenRedisService).deleteUploadSession("up-new");
        verify(minioStorageService).deleteObject("products", "50/old-removed.png");
        verify(minioStorageService, never()).deleteObject("products", "50/kept.png");
    }

    @Test
    @DisplayName("deleteProductImage deletes DB row, promotes next primary image, and deletes MinIO object")
    void deleteProductImage_promotesNextPrimaryAndDeletesMinio() {
        Product product = Product.builder()
                .productId(50L)
                .productName("MacBook Pro")
                .thumbnailUrl("http://localhost:9000/products/50/img1.png")
                .build();
        ProductImage primaryImg = ProductImage.builder()
                .imageId(101L)
                .product(product)
                .imageUrl("http://localhost:9000/products/50/img1.png")
                .bucket("products")
                .objectKey("50/img1.png")
                .isPrimary(true)
                .displayOrder(1)
                .build();
        ProductImage secondImg = ProductImage.builder()
                .imageId(102L)
                .product(product)
                .imageUrl("http://localhost:9000/products/50/img2.png")
                .bucket("products")
                .objectKey("50/img2.png")
                .isPrimary(false)
                .displayOrder(2)
                .build();

        when(productRepository.findById(50L)).thenReturn(Optional.of(product));
        when(productImageRepository.findByImageIdAndProduct_ProductId(101L, 50L))
                .thenReturn(Optional.of(primaryImg));
        when(productImageRepository.findByProduct_ProductIdOrderByDisplayOrderAsc(50L))
                .thenReturn(List.of(secondImg));

        productsService.deleteProductImage(50L, 101L);

        verify(productImageRepository).delete(primaryImg);
        assertThat(secondImg.getIsPrimary()).isTrue();
        assertThat(product.getThumbnailUrl()).isEqualTo("http://localhost:9000/products/50/img2.png");
        verify(productImageRepository).save(secondImg);
        verify(productRepository).save(product);
        verify(minioStorageService).deleteObject("products", "50/img1.png");
    }

    @Test
    @DisplayName("getAdminProductDetail uses stored objectKey/bucket and falls back for legacy URLs")
    void getAdminProductDetail_returnsStoredAndLegacyImageMetadata() {
        Product product = Product.builder().productId(50L).productName("MacBook Pro").status("ACTIVE").build();
        ProductImage minioImg = ProductImage.builder()
                .imageId(1L)
                .product(product)
                .imageUrl("http://localhost:9000/products/50/uuid-mbp.png")
                .bucket("products")
                .objectKey("50/uuid-mbp.png")
                .isPrimary(true)
                .displayOrder(1)
                .build();
        ProductImage legacyImg = ProductImage.builder()
                .imageId(2L)
                .product(product)
                .imageUrl("https://cdn.nextgen.vn/products/legacy.png")
                .isPrimary(false)
                .displayOrder(2)
                .build();

        when(productRepository.findById(50L)).thenReturn(Optional.of(product));
        when(productImageRepository.findByProduct_ProductIdOrderByDisplayOrderAsc(50L))
                .thenReturn(List.of(minioImg, legacyImg));
        when(productSpecRepository.findByProduct_ProductIdOrderBySortOrderAsc(50L)).thenReturn(List.of());
        when(productVariantRepository.findByProduct_ProductId(50L)).thenReturn(List.of());

        AdminProductDetailResponse detail = productsService.getAdminProductDetail(50L);

        assertThat(detail.getImages()).hasSize(2);
        assertThat(detail.getImages().get(0).getObjectKey()).isEqualTo("50/uuid-mbp.png");
        assertThat(detail.getImages().get(0).getBucket()).isEqualTo("products");
        assertThat(detail.getImages().get(1).getObjectKey()).isEqualTo("legacy.png");
        assertThat(detail.getImages().get(1).getBucket()).isEqualTo("products");
    }

    @Test
    @DisplayName("createProduct rejects base64 data URI imageUrl with INVALID_REQUEST instead of failing at DB")
    void createProduct_base64DataUri_throwsInvalidRequest() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(brandRepository.findById(2L)).thenReturn(Optional.of(brand));
        when(productVariantRepository.existsBySku("BASE64-1")).thenReturn(false);

        CreateProductRequest request = CreateProductRequest.builder()
                .categoryId(1L)
                .brandId(2L)
                .name("Base64 Reject Product")
                .images(List.of(CreateProductRequest.ImageItemRequest.builder()
                        .imageUrl("data:image/png;base64,iVBORw0KGgoAAAANSUhEUg==")
                        .isPrimary(true)
                        .build()))
                .variants(List.of(CreateProductRequest.VariantItemRequest.builder()
                        .sku("BASE64-1")
                        .listPrice(BigDecimal.valueOf(1000000))
                        .initialOnHandQty(1)
                        .build()))
                .build();

        assertThatThrownBy(() -> productsService.createProduct(request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_REQUEST);

        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("getProductDetail and getAdminProductDetail return saved variant imageUrl")
    void productDetails_returnVariantImageUrl() {
        Product product = Product.builder()
                .productId(55L)
                .productName("iPhone 16 Pro")
                .status("ACTIVE")
                .thumbnailUrl("http://localhost:9000/products/55/main.png")
                .build();
        ProductVariant variant = ProductVariant.builder()
                .variantId(550L)
                .product(product)
                .sku("IP16P-TITAN")
                .color("Titan")
                .price(BigDecimal.valueOf(28990000))
                .compareAtPrice(BigDecimal.valueOf(29990000))
                .imageUrl("http://localhost:9000/products/temp/1/var-titan.png")
                .status("ACTIVE")
                .build();

        when(productRepository.findById(55L)).thenReturn(Optional.of(product));
        when(productImageRepository.findByProduct_ProductIdOrderByDisplayOrderAsc(55L)).thenReturn(List.of());
        when(productSpecRepository.findByProduct_ProductIdOrderBySortOrderAsc(55L)).thenReturn(List.of());
        when(productVariantRepository.findByProduct_ProductId(55L)).thenReturn(List.of(variant));
        when(inventoryRepository.findByVariant_VariantIdIn(List.of(550L))).thenReturn(List.of());

        AdminProductDetailResponse adminDetail = productsService.getAdminProductDetail(55L);
        ProductDetailResponse clientDetail = productsService.getProductDetail(55L);

        assertThat(adminDetail.getVariants()).hasSize(1);
        assertThat(adminDetail.getVariants().get(0).getImageUrl())
                .isEqualTo("http://localhost:9000/products/temp/1/var-titan.png");
        assertThat(clientDetail.getVariants()).hasSize(1);
        assertThat(clientDetail.getVariants().get(0).getImageUrl())
                .isEqualTo("http://localhost:9000/products/temp/1/var-titan.png");
    }
}
