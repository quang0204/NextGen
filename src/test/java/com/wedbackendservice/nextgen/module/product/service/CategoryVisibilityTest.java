package com.wedbackendservice.nextgen.module.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.wedbackendservice.nextgen.core.exception.AppException;
import com.wedbackendservice.nextgen.core.exception.ErrorCode;
import com.wedbackendservice.nextgen.core.security.SecurityHelper;
import com.wedbackendservice.nextgen.core.security.TokenRedisService;
import com.wedbackendservice.nextgen.module.cart.dto.client.request.CartItemAddRequest;
import com.wedbackendservice.nextgen.module.cart.dto.client.request.CartItemUpdateRequest;
import com.wedbackendservice.nextgen.module.cart.dto.client.request.CartSyncRequest;
import com.wedbackendservice.nextgen.module.cart.dto.client.response.CartResponse;
import com.wedbackendservice.nextgen.module.cart.dto.client.response.CartSyncResponse;
import com.wedbackendservice.nextgen.module.cart.entity.Cart;
import com.wedbackendservice.nextgen.module.cart.entity.CartItem;
import com.wedbackendservice.nextgen.module.cart.repository.CartItemRepository;
import com.wedbackendservice.nextgen.module.cart.repository.CartRepository;
import com.wedbackendservice.nextgen.module.cart.service.client.CartService;
import com.wedbackendservice.nextgen.module.inventory.entity.Inventory;
import com.wedbackendservice.nextgen.module.inventory.repository.InventoryRepository;
import com.wedbackendservice.nextgen.module.inventory.repository.InventoryTransactionRepository;
import com.wedbackendservice.nextgen.module.notification.repository.AuditLogRepository;
import com.wedbackendservice.nextgen.module.notification.service.MinioStorageService;
import com.wedbackendservice.nextgen.module.notification.service.StorageFileService;
import com.wedbackendservice.nextgen.module.order.adapter.out.persistence.ProductCatalogPersistenceAdapter;
import com.wedbackendservice.nextgen.module.order.repository.OrderItemRepository;
import com.wedbackendservice.nextgen.module.product.dto.client.response.CategoryResponse;
import com.wedbackendservice.nextgen.module.product.dto.client.response.ProductDetailResponse;
import com.wedbackendservice.nextgen.module.product.dto.portal.response.AdminProductDetailResponse;
import com.wedbackendservice.nextgen.module.product.entity.Category;
import com.wedbackendservice.nextgen.module.product.entity.Product;
import com.wedbackendservice.nextgen.module.product.entity.ProductVariant;
import com.wedbackendservice.nextgen.module.product.mapper.BrandMapper;
import com.wedbackendservice.nextgen.module.product.mapper.CategoryMapper;
import com.wedbackendservice.nextgen.module.product.mapper.ProductVariantMapper;
import com.wedbackendservice.nextgen.module.product.repository.BrandRepository;
import com.wedbackendservice.nextgen.module.product.repository.CategoryRepository;
import com.wedbackendservice.nextgen.module.product.repository.ProductImageRepository;
import com.wedbackendservice.nextgen.module.product.repository.ProductRepository;
import com.wedbackendservice.nextgen.module.product.repository.ProductSpecRepository;
import com.wedbackendservice.nextgen.module.product.repository.ProductVariantRepository;
import com.wedbackendservice.nextgen.module.product.repository.ReviewRepository;
import com.wedbackendservice.nextgen.module.user.entity.User;
import com.wedbackendservice.nextgen.module.user.enums.Role;
import com.wedbackendservice.nextgen.module.voucher.repository.PromotionRepository;
import com.wedbackendservice.nextgen.module.voucher.repository.PromotionVariantRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CategoryVisibilityTest {

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
  @Mock private ReviewRepository reviewRepository;
  @Mock private StorageFileService storageFileService;
  @Mock private CartRepository cartRepository;
  @Mock private CartItemRepository cartItemRepository;

  private ProductsService productsService;
  private CategoriesAndBrandsService categoriesAndBrandsService;
  private CartService cartService;
  private ProductCatalogPersistenceAdapter catalogAdapter;

  private Category rootPhone;
  private Category midIphone;
  private Category leafIphone17;
  private Category midSamsung;
  private Category leafGalaxyA;
  private Product activePhone17;
  private Product inactivePhone16;
  private ProductVariant activeVariant17;
  private User customerUser;
  private User adminUser;

  @BeforeEach
  void setUp() {
    ProductVariantMapper productVariantMapper = Mappers.getMapper(ProductVariantMapper.class);
    CategoryMapper categoryMapper = Mappers.getMapper(CategoryMapper.class);
    BrandMapper brandMapper = Mappers.getMapper(BrandMapper.class);

    productsService =
        new ProductsService(
            productRepository,
            productVariantRepository,
            productImageRepository,
            productSpecRepository,
            inventoryRepository,
            categoryRepository,
            brandRepository,
            promotionRepository,
            promotionVariantRepository,
            auditLogRepository,
            orderItemRepository,
            reviewRepository,
            inventoryTransactionRepository,
            securityHelper,
            tokenRedisService,
            minioStorageService,
            productVariantMapper);

    categoriesAndBrandsService =
        new CategoriesAndBrandsService(
            securityHelper,
            categoryRepository,
            brandRepository,
            productRepository,
            auditLogRepository,
            categoryMapper,
            brandMapper,
            storageFileService);

    cartService =
        new CartService(
            securityHelper,
            cartRepository,
            cartItemRepository,
            productVariantRepository,
            inventoryRepository,
            promotionVariantRepository,
            productImageRepository);

    catalogAdapter =
        new ProductCatalogPersistenceAdapter(productVariantRepository, promotionVariantRepository);

    rootPhone = Category.builder().categoryId(1L).categoryName("Điện thoại").isActive(true).build();
    midIphone =
        Category.builder()
            .categoryId(2L)
            .categoryName("iPhone")
            .parent(rootPhone)
            .isActive(true)
            .build();
    leafIphone17 =
        Category.builder()
            .categoryId(3L)
            .categoryName("iPhone 17")
            .parent(midIphone)
            .isActive(true)
            .build();
    midSamsung =
        Category.builder()
            .categoryId(4L)
            .categoryName("Samsung")
            .parent(rootPhone)
            .isActive(true)
            .build();
    leafGalaxyA =
        Category.builder()
            .categoryId(5L)
            .categoryName("Galaxy A")
            .parent(midSamsung)
            .isActive(false)
            .build();

    activePhone17 =
        Product.builder()
            .productId(101L)
            .productName("iPhone 17 Pro Max")
            .category(leafIphone17)
            .status("ACTIVE")
            .basePrice(BigDecimal.valueOf(34990000))
            .build();
    inactivePhone16 =
        Product.builder()
            .productId(102L)
            .productName("iPhone 16 cũ")
            .category(midIphone)
            .status("INACTIVE")
            .basePrice(BigDecimal.valueOf(19990000))
            .build();

    activeVariant17 =
        ProductVariant.builder()
            .variantId(1001L)
            .product(activePhone17)
            .sku("IP17PM-256")
            .status("ACTIVE")
            .price(BigDecimal.valueOf(34990000))
            .build();

    customerUser = User.builder().userId(10L).role(Role.CUSTOMER).build();
    adminUser = User.builder().userId(1L).role(Role.ADMIN).build();
  }

  @Test
  @DisplayName("Hiding parent category hides descendants without mutating own flags, and restoring keeps individually inactive ones hidden")
  void hierarchyVisibility_preservesOwnFlags() {
    List<Category> all = List.of(rootPhone, midIphone, leafIphone17, midSamsung, leafGalaxyA);

    rootPhone.setIsActive(false);

    assertThat(midIphone.getIsActive()).isTrue();
    assertThat(leafIphone17.getIsActive()).isTrue();
    assertThat(CategoryVisibilityHelper.isCategoryTreeVisible(leafIphone17)).isFalse();
    assertThat(CategoryVisibilityHelper.isProductEffectivelyVisible(activePhone17)).isFalse();
    assertThat(CategoryVisibilityHelper.resolveVisibleCategoryIds(all)).isEmpty();
    assertThat(CategoryVisibilityHelper.resolveSubtreeCategoryIds(all, 1L, true)).isEmpty();
    assertThat(CategoryVisibilityHelper.resolveSubtreeCategoryIds(all, 1L, false))
        .containsExactlyInAnyOrder(1L, 2L, 3L, 4L, 5L);

    rootPhone.setIsActive(true);

    Set<Long> visibleAfterRestore = CategoryVisibilityHelper.resolveVisibleCategoryIds(all);
    assertThat(visibleAfterRestore).containsExactlyInAnyOrder(1L, 2L, 3L, 4L);
    assertThat(CategoryVisibilityHelper.isProductEffectivelyVisible(activePhone17)).isTrue();
    assertThat(CategoryVisibilityHelper.isProductEffectivelyVisible(inactivePhone16)).isFalse();
    assertThat(CategoryVisibilityHelper.isCategoryTreeVisible(leafGalaxyA)).isFalse();
  }

  @Test
  @DisplayName("Public category tree and category detail hide subtree when root is hidden, while Admin still sees it")
  void categoryTreeAndDetail_respectsAncestorVisibility() {
    rootPhone.setIsActive(false);

    when(categoryRepository.findAllActive())
        .thenReturn(List.of(midIphone, leafIphone17, midSamsung));
    when(productRepository.countProductsGroupedByCategory()).thenReturn(List.of());

    List<CategoryResponse> publicTree = categoriesAndBrandsService.getCategoriesTree(false);
    assertThat(publicTree).isEmpty();

    when(categoryRepository.findById(3L)).thenReturn(Optional.of(leafIphone17));
    when(securityHelper.getCurrentUser()).thenReturn(customerUser);

    assertThatThrownBy(() -> categoriesAndBrandsService.getCategoryById(3L))
        .isInstanceOf(AppException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.CATEGORY_NOT_FOUND);

    when(securityHelper.getCurrentUser()).thenReturn(adminUser);
    CategoryResponse adminCat = categoriesAndBrandsService.getCategoryById(3L);
    assertThat(adminCat.getCategoryId()).isEqualTo(3L);
  }

  @Test
  @DisplayName("Public product detail blocks hidden-ancestor products with 404 while Admin portal detail remains accessible")
  void productDetail_blockedPublicButAccessibleInAdmin() {
    rootPhone.setIsActive(false);

    when(productRepository.findById(101L)).thenReturn(Optional.of(activePhone17));
    assertThatThrownBy(() -> productsService.getProductDetail(101L))
        .isInstanceOf(AppException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);

    when(securityHelper.authenticateStaffOrAdmin()).thenReturn(adminUser);
    when(productImageRepository.findByProduct_ProductIdOrderByDisplayOrderAsc(101L))
        .thenReturn(List.of());
    when(productSpecRepository.findByProduct_ProductIdOrderBySortOrderAsc(101L))
        .thenReturn(List.of());
    when(productVariantRepository.findByProduct_ProductId(101L))
        .thenReturn(List.of(activeVariant17));
    when(inventoryRepository.findByVariant_VariantIdIn(List.of(1001L))).thenReturn(List.of());

    AdminProductDetailResponse adminDetail = productsService.getAdminProductDetail(101L);
    assertThat(adminDetail.getStatus()).isEqualTo("ACTIVE");
    assertThat(adminDetail.getCategoryEffectiveVisible()).isFalse();

    rootPhone.setIsActive(true);
    when(reviewRepository.findRatingStatsByProductIds(any())).thenReturn(List.of());
    ProductDetailResponse restoredPublic = productsService.getProductDetail(101L);
    assertThat(restoredPublic.getProductId()).isEqualTo(101L);

    when(productRepository.findById(102L)).thenReturn(Optional.of(inactivePhone16));
    assertThatThrownBy(() -> productsService.getProductDetail(102L))
        .isInstanceOf(AppException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
  }

  @Test
  @DisplayName("Featured, smart search fallback, and related queries exclude products under hidden ancestors")
  void featuredAndRelated_excludeHiddenSubtree() {
    rootPhone.setIsActive(false);

    when(categoryRepository.findAll()).thenReturn(List.of(rootPhone, midIphone, leafIphone17));
    assertThat(productsService.getFeaturedProducts("BEST_SELLER", 8)).isEmpty();
    verify(productRepository, never()).findBestSellingProductIds(anyCollection(), any());

    when(productRepository.findById(101L)).thenReturn(Optional.of(activePhone17));
    assertThat(productsService.getRelatedProducts(101L, 4)).isEmpty();
    verify(productRepository, never()).findRelatedProducts(anyLong(), any(), any(), anyCollection(), any());
  }

  @Test
  @DisplayName("Cart and checkout reject products whose category ancestor is hidden and mark existing cart items unavailable")
  void cartAndCheckout_enforceAncestorVisibility() {
    rootPhone.setIsActive(false);
    when(securityHelper.getCurrentUser()).thenReturn(customerUser);
    when(productVariantRepository.findById(1001L)).thenReturn(Optional.of(activeVariant17));

    CartItemAddRequest addReq = new CartItemAddRequest();
    addReq.setVariantId(1001L);
    addReq.setQuantity(1);
    assertThatThrownBy(() -> cartService.addItemToCart(addReq))
        .isInstanceOf(AppException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.PRODUCT_INACTIVE);

    Cart cart = Cart.builder().cartId(50L).user(customerUser).build();
    CartItem existingItem =
        CartItem.builder()
            .cartItemId(500L)
            .cart(cart)
            .variant(activeVariant17)
            .quantity(1)
            .build();
    when(cartRepository.findByUserId(10L)).thenReturn(Optional.of(cart));
    when(cartItemRepository.findById(500L)).thenReturn(Optional.of(existingItem));

    CartItemUpdateRequest updateReq = new CartItemUpdateRequest();
    updateReq.setQuantity(2);
    assertThatThrownBy(() -> cartService.updateItemQuantity(500L, updateReq))
        .isInstanceOf(AppException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.PRODUCT_INACTIVE);

    when(cartItemRepository.findByCartId(50L)).thenReturn(List.of(existingItem));
    Inventory stock = Inventory.builder().variant(activeVariant17).quantity(5).reservedQuantity(0).build();
    when(inventoryRepository.findByVariant_VariantIdIn(List.of(1001L))).thenReturn(List.of(stock));
    when(promotionVariantRepository.findActivePromotionsForVariantIds(eq(List.of(1001L)), any()))
        .thenReturn(List.of());
    when(productImageRepository.findByProduct_ProductIdInAndIsPrimaryTrue(List.of(101L)))
        .thenReturn(List.of());

    CartResponse cartResponse = cartService.getCart();
    assertThat(cartResponse.getItems()).hasSize(1);
    assertThat(cartResponse.getItems().get(0).getIsAvailable()).isFalse();

    when(productVariantRepository.findAllById(List.of(1001L))).thenReturn(List.of(activeVariant17));
    CartSyncRequest syncReq = new CartSyncRequest();
    syncReq.setGuestItems(List.of(addReq));
    CartSyncResponse syncResponse = cartService.syncCart(syncReq);
    assertThat(syncResponse.getSyncedCount()).isZero();
    assertThat(syncResponse.getSkippedItems()).hasSize(1);

    assertThatThrownBy(
            () -> catalogAdapter.validateAndFetchPricing(List.of(1001L), LocalDateTime.now()))
        .isInstanceOf(AppException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.PRODUCT_INACTIVE);
  }
}
