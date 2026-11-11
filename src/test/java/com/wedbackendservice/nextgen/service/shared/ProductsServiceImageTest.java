package com.wedbackendservice.nextgen.service.shared;

import com.wedbackendservice.nextgen.dto.client.response.product.ProductDetailResponse;
import com.wedbackendservice.nextgen.dto.portal.request.product.CreateProductRequest;
import com.wedbackendservice.nextgen.dto.portal.request.product.UpdateProductRequest;
import com.wedbackendservice.nextgen.dto.portal.response.product.CreateProductResponse;
import com.wedbackendservice.nextgen.dto.session.UploadSessionData;
import com.wedbackendservice.nextgen.entity.*;
import com.wedbackendservice.nextgen.enums.StorageProvider;
import com.wedbackendservice.nextgen.enums.StorageType;
import com.wedbackendservice.nextgen.enums.UploadStatus;
import com.wedbackendservice.nextgen.exception.AppException;
import com.wedbackendservice.nextgen.exception.ErrorCode;
import com.wedbackendservice.nextgen.repository.*;
import com.wedbackendservice.nextgen.security.SecurityHelper;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.anyCollection;
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
    @Mock private SecurityHelper securityHelper;
    @Mock private StorageService storageService;
    @Mock private TokenRedisService tokenRedisService;

    @InjectMocks
    private ProductsService productsService;

    private Category category;
    private Brand brand;

    @BeforeEach
    void setUp() {
        category = Category.builder().categoryId(1L).categoryName("Phones").build();
        brand = Brand.builder().brandId(2L).brandName("Apple").build();
    }

    @Test
    void createProduct_attachesCompletedUploadSessionAndCleansRedisSession() {
        UploadSessionData completed = UploadSessionData.builder()
                .uploadId("up-100")
                .storageProvider(StorageProvider.MINIO)
                .storageType(StorageType.PRODUCTS)
                .bucketName("products")
                .objectKey("2026-11-11/abc-iphone.png")
                .originalFileName("iphone.png")
                .contentType("image/png")
                .actualFileSize(55000L)
                .eTag("etag-1")
                .status(UploadStatus.COMPLETED)
                .build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(brandRepository.findById(2L)).thenReturn(Optional.of(brand));
        when(productVariantRepository.existsBySku("IP16-256")).thenReturn(false);
        when(tokenRedisService.getUploadSession("up-100")).thenReturn(completed);
        when(storageService.getObjectUrl("products", completed.getObjectKey()))
                .thenReturn("http://localhost:9000/products/2026-11-11/abc-iphone.png");
        when(productRepository.existsBySlug(anyString())).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setProductId(50L);
            return p;
        });
        when(productVariantRepository.save(any(ProductVariant.class))).thenAnswer(inv -> {
            ProductVariant v = inv.getArgument(0);
            v.setVariantId(60L);
            return v;
        });
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(inv -> {
            Inventory i = inv.getArgument(0);
            i.setInventoryId(70L);
            return i;
        });

        CreateProductResponse response = productsService.createProduct(CreateProductRequest.builder()
                .categoryId(1L)
                .brandId(2L)
                .name("iPhone 16 Pro")
                .images(List.of(CreateProductRequest.ImageItemRequest.builder()
                        .uploadId("up-100")
                        .isPrimary(true)
                        .sortOrder(1)
                        .build()))
                .variants(List.of(CreateProductRequest.VariantItemRequest.builder()
                        .sku("IP16-256")
                        .listPrice(BigDecimal.valueOf(30000000))
                        .initialOnHandQty(10)
                        .build()))
                .build());

        assertThat(response.getTotalImages()).isEqualTo(1);
        ArgumentCaptor<List<ProductImage>> imagesCaptor = ArgumentCaptor.forClass(List.class);
        verify(productImageRepository).saveAll(imagesCaptor.capture());
        ProductImage savedImg = imagesCaptor.getValue().get(0);
        assertThat(savedImg.getStorageProvider()).isEqualTo(StorageProvider.MINIO);
        assertThat(savedImg.getStorageType()).isEqualTo(StorageType.PRODUCTS);
        assertThat(savedImg.getBucket()).isEqualTo("products");
        assertThat(savedImg.getObjectKey()).isEqualTo("2026-11-11/abc-iphone.png");
        verify(tokenRedisService).deleteUploadSession("up-100");
    }

    @Test
    void createProduct_rejectsPendingUploadSession() {
        UploadSessionData pending = UploadSessionData.builder()
                .uploadId("up-pending")
                .objectKey("pending.png")
                .status(UploadStatus.PENDING)
                .build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(brandRepository.findById(2L)).thenReturn(Optional.of(brand));
        when(productVariantRepository.existsBySku("IP16-256")).thenReturn(false);
        when(tokenRedisService.getUploadSession("up-pending")).thenReturn(pending);

        assertThatThrownBy(() -> productsService.createProduct(CreateProductRequest.builder()
                .categoryId(1L)
                .brandId(2L)
                .name("iPhone 16 Pro")
                .images(List.of(CreateProductRequest.ImageItemRequest.builder().uploadId("up-pending").build()))
                .variants(List.of(CreateProductRequest.VariantItemRequest.builder()
                        .sku("IP16-256")
                        .listPrice(BigDecimal.valueOf(30000000))
                        .initialOnHandQty(10)
                        .build()))
                .build()))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STORAGE_FILE_NOT_CONFIRMED);
    }

    @Test
    void deleteProductImage_queuesOrphanIfMinioDeleteFailsAfterDbDelete() {
        Product product = Product.builder()
                .productId(50L)
                .productName("iPhone 16 Pro")
                .thumbnailUrl("http://localhost:9000/products/50/1.png")
                .build();
        ProductImage primaryImg = ProductImage.builder()
                .imageId(1L)
                .product(product)
                .bucket("products")
                .objectKey("50/1.png")
                .imageUrl("http://localhost:9000/products/50/1.png")
                .isPrimary(true)
                .displayOrder(1)
                .build();
        ProductImage secondImg = ProductImage.builder()
                .imageId(2L)
                .product(product)
                .bucket("products")
                .objectKey("50/2.png")
                .imageUrl("http://localhost:9000/products/50/2.png")
                .isPrimary(false)
                .displayOrder(2)
                .build();

        when(productRepository.findById(50L)).thenReturn(Optional.of(product));
        when(productImageRepository.findById(1L)).thenReturn(Optional.of(primaryImg));
        when(productImageRepository.findByProduct_ProductIdOrderByDisplayOrderAsc(50L))
                .thenReturn(List.of(secondImg));
        doThrow(new AppException(ErrorCode.STORAGE_OPERATION_FAILED))
                .when(storageService).deleteObject("products", "50/1.png");

        productsService.deleteProductImage(50L, 1L);

        verify(productImageRepository).delete(primaryImg);
        verify(tokenRedisService).addOrphanObject("products", "50/1.png");
        assertThat(secondImg.getIsPrimary()).isTrue();
        assertThat(product.getThumbnailUrl()).isEqualTo("http://localhost:9000/products/50/2.png");
    }

    @Test
    void updateProduct_deletesOnlyRemovedStorageObjects() {
        Product existingProduct = Product.builder()
                .productId(50L)
                .productName("iPhone 16 Pro")
                .category(category)
                .brand(brand)
                .status("ACTIVE")
                .build();
        ProductImage keptImg = ProductImage.builder()
                .imageId(1L)
                .product(existingProduct)
                .objectKey("50/kept.png")
                .imageUrl("http://localhost:9000/products/50/kept.png")
                .storageProvider(StorageProvider.MINIO)
                .bucket("products")
                .build();
        ProductImage removedImg = ProductImage.builder()
                .imageId(2L)
                .product(existingProduct)
                .objectKey("50/removed.png")
                .imageUrl("http://localhost:9000/products/50/removed.png")
                .storageProvider(StorageProvider.MINIO)
                .bucket("products")
                .build();

        when(productRepository.findById(50L)).thenReturn(Optional.of(existingProduct));
        when(productImageRepository.findByProduct_ProductIdOrderByDisplayOrderAsc(50L))
                .thenReturn(List.of(keptImg, removedImg));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        productsService.updateProduct(50L, UpdateProductRequest.builder()
                .images(List.of(UpdateProductRequest.UpdateImageItem.builder()
                        .imageId(1L)
                        .isPrimary(true)
                        .sortOrder(1)
                        .build()))
                .build());

        verify(storageService).deleteObject("products", "50/removed.png");
        verify(storageService, never()).deleteObject("products", "50/kept.png");
    }

    @Test
    void getProductDetail_resolvesMinioAccessUrlAndKeepsLegacyFallback() {
        Product product = Product.builder()
                .productId(50L)
                .productName("iPhone 16 Pro")
                .category(category)
                .brand(brand)
                .status("ACTIVE")
                .build();
        ProductImage storedImg = ProductImage.builder()
                .imageId(1L)
                .objectKey("50/stored.png")
                .storageProvider(StorageProvider.MINIO)
                .bucket("products")
                .isPrimary(true)
                .displayOrder(1)
                .build();
        ProductImage legacyImg = ProductImage.builder()
                .imageId(2L)
                .imageUrl("https://cdn.nextgen.vn/legacy.png")
                .isPrimary(false)
                .displayOrder(2)
                .build();

        when(productRepository.findById(50L)).thenReturn(Optional.of(product));
        when(productImageRepository.findByProduct_ProductIdOrderByDisplayOrderAsc(50L))
                .thenReturn(List.of(storedImg, legacyImg));
        when(storageService.getAccessUrl("products", "50/stored.png"))
                .thenReturn("http://localhost:9000/products/50/stored.png?X-Amz-Signature=1");
        when(productSpecRepository.findByProduct_ProductIdOrderBySortOrderAsc(50L)).thenReturn(List.of());
        when(productVariantRepository.findByProduct_ProductId(50L)).thenReturn(List.of());
        when(orderItemRepository.countSoldByProductIds(anyCollection())).thenReturn(List.of());

        ProductDetailResponse detail = productsService.getProductDetail(50L);

        assertThat(detail.getImages()).hasSize(2);
        assertThat(detail.getImages().get(0).getImageUrl()).contains("X-Amz-Signature=1");
        assertThat(detail.getImages().get(0).getStorageProvider()).isEqualTo(StorageProvider.MINIO);
        assertThat(detail.getImages().get(1).getImageUrl()).isEqualTo("https://cdn.nextgen.vn/legacy.png");
        assertThat(detail.getImages().get(1).getStorageProvider()).isNull();
    }
}
