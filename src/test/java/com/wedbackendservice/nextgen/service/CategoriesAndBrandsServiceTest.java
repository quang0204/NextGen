package com.wedbackendservice.nextgen.service;

import com.wedbackendservice.nextgen.dto.request.brand.BrandRequest;
import com.wedbackendservice.nextgen.dto.request.category.CategoryRequest;
import com.wedbackendservice.nextgen.dto.response.brand.BrandResponse;
import com.wedbackendservice.nextgen.dto.response.category.CategoryResponse;
import com.wedbackendservice.nextgen.entity.AuditLog;
import com.wedbackendservice.nextgen.entity.Brand;
import com.wedbackendservice.nextgen.entity.Category;
import com.wedbackendservice.nextgen.entity.User;
import com.wedbackendservice.nextgen.enums.Role;
import com.wedbackendservice.nextgen.enums.UserStatus;
import com.wedbackendservice.nextgen.exception.AppException;
import com.wedbackendservice.nextgen.exception.ErrorCode;
import com.wedbackendservice.nextgen.repository.AuditLogRepository;
import com.wedbackendservice.nextgen.repository.BrandRepository;
import com.wedbackendservice.nextgen.repository.CategoryRepository;
import com.wedbackendservice.nextgen.repository.ProductRepository;
import com.wedbackendservice.nextgen.security.SecurityHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CategoriesAndBrandsServiceTest {

    @Mock
    private SecurityHelper securityHelper;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private BrandRepository brandRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private CategoriesAndBrandsService service;

    private User adminUser;
    private Category rootCat;
    private Brand brand;

    @BeforeEach
    void setUp() {
        adminUser = User.builder()
                .userId(1L)
                .fullName("Quản Trị Viên")
                .role(Role.ADMIN)
                .status(UserStatus.ACTIVE)
                .build();

        rootCat = Category.builder()
                .categoryId(1L)
                .categoryName("Điện thoại")
                .slug("dien-thoai")
                .description("Điện thoại di động")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        brand = Brand.builder()
                .brandId(1L)
                .brandName("Apple")
                .slug("apple")
                .description("Hãng công nghệ hàng đầu")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testGetCategoriesTree_Success() {
        when(categoryRepository.findAllActive()).thenReturn(List.of(rootCat));

        List<CategoryResponse> tree = service.getCategoriesTree(false);

        assertNotNull(tree);
        assertEquals(1, tree.size());
        assertEquals(1L, tree.get(0).getCategoryId());
        assertEquals("Điện thoại", tree.get(0).getName());
    }

    @Test
    void testGetCategoryById_Success() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(rootCat));
        when(productRepository.countByCategory_CategoryId(1L)).thenReturn(10L);

        CategoryResponse resp = service.getCategoryById(1L);

        assertNotNull(resp);
        assertEquals(1L, resp.getCategoryId());
        assertEquals(10, resp.getProductCount());
    }

    @Test
    void testGetCategoryById_NotFound() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> service.getCategoryById(99L));
        assertEquals(ErrorCode.CATEGORY_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void testCreateCategory_Success() {
        when(securityHelper.authenticateStaffOrAdmin("Bearer token")).thenReturn(adminUser);
        when(categoryRepository.existsByCategoryNameIgnoreCaseAndParentIsNull("Tablet")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(Category.builder()
                .categoryId(2L)
                .categoryName("Tablet")
                .isActive(true)
                .build());

        CategoryRequest req = CategoryRequest.builder()
                .name("Tablet")
                .description("Máy tính bảng")
                .isVisible(true)
                .build();

        CategoryResponse resp = service.createCategory("Bearer token", req);

        assertNotNull(resp);
        assertEquals(2L, resp.getCategoryId());
        verify(categoryRepository, times(1)).save(any(Category.class));
        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    void testCreateCategory_Duplicate_ThrowsConflict() {
        when(securityHelper.authenticateStaffOrAdmin("Bearer token")).thenReturn(adminUser);
        when(categoryRepository.existsByCategoryNameIgnoreCaseAndParentIsNull("Điện thoại")).thenReturn(true);

        CategoryRequest req = CategoryRequest.builder()
                .name("Điện thoại")
                .build();

        AppException ex = assertThrows(AppException.class, () -> service.createCategory("Bearer token", req));
        assertEquals(ErrorCode.CATEGORY_ALREADY_EXISTS, ex.getErrorCode());
    }

    @Test
    void testGetBrands_Success() {
        when(brandRepository.findAllActive()).thenReturn(List.of(brand));

        List<BrandResponse> brands = service.getBrands();

        assertNotNull(brands);
        assertEquals(1, brands.size());
        assertEquals(1L, brands.get(0).getBrandId());
        assertEquals("Apple", brands.get(0).getName());
    }

    @Test
    void testCreateBrand_Success() {
        when(securityHelper.authenticateStaffOrAdmin("Bearer token")).thenReturn(adminUser);
        when(brandRepository.existsByBrandNameIgnoreCase("Samsung")).thenReturn(false);
        when(brandRepository.save(any(Brand.class))).thenReturn(Brand.builder()
                .brandId(2L)
                .brandName("Samsung")
                .isActive(true)
                .build());

        BrandRequest req = BrandRequest.builder()
                .name("Samsung")
                .description("Điện thoại Samsung")
                .isVisible(true)
                .build();

        BrandResponse resp = service.createBrand("Bearer token", req);

        assertNotNull(resp);
        assertEquals(2L, resp.getBrandId());
        verify(brandRepository, times(1)).save(any(Brand.class));
        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    void testCreateBrand_Duplicate_ThrowsConflict() {
        when(securityHelper.authenticateStaffOrAdmin("Bearer token")).thenReturn(adminUser);
        when(brandRepository.existsByBrandNameIgnoreCase("Apple")).thenReturn(true);

        BrandRequest req = BrandRequest.builder()
                .name("Apple")
                .build();

        AppException ex = assertThrows(AppException.class, () -> service.createBrand("Bearer token", req));
        assertEquals(ErrorCode.BRAND_ALREADY_EXISTS, ex.getErrorCode());
    }
}
