package com.wedbackendservice.nextgen.controller.client;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.wedbackendservice.nextgen.dto.common.ApiResponse;
import com.wedbackendservice.nextgen.service.shared.MinioStorageService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class BannersControllerTest {

  private MinioStorageService minioStorageService;
  private BannersController bannersController;

  @BeforeEach
  void setUp() {
    minioStorageService = Mockito.mock(MinioStorageService.class);
    when(minioStorageService.getBannersBucket()).thenReturn("banner");
    when(minioStorageService.getAccessUrl(eq("banner"), eq("banner1.png")))
        .thenReturn("http://localhost:9000/banner/banner1.png");
    when(minioStorageService.getAccessUrl(eq("banner"), eq("banner2.png")))
        .thenReturn("http://localhost:9000/banner/banner2.png");

    bannersController = new BannersController(minioStorageService);
  }

  @Test
  @DisplayName("Lấy danh sách 2 banner với URL MinIO và nút CTA hợp lệ")
  void testGetActiveBanners() {
    ApiResponse<List<BannersController.BannerItemDto>> response =
        bannersController.getActiveBanners();

    assertNotNull(response);
    assertEquals(200, response.getStatusCode());
    List<BannersController.BannerItemDto> banners = response.getData();
    assertEquals(2, banners.size());

    // Banner 1: Flash Sale
    BannersController.BannerItemDto b1 = banners.get(0);
    assertEquals(1L, b1.getId());
    assertEquals("FLASH_SALE", b1.getType());
    assertEquals("http://localhost:9000/banner/banner1.png", b1.getImageUrl());
    assertEquals("/products?sale=true", b1.getTargetUrl());
    assertEquals("Săn Deal Flash Sale", b1.getCtaText());

    // Banner 2: Laptop/PC
    BannersController.BannerItemDto b2 = banners.get(1);
    assertEquals(2L, b2.getId());
    assertEquals("LAPTOP_PC", b2.getType());
    assertEquals("http://localhost:9000/banner/banner2.png", b2.getImageUrl());
    assertEquals("/products?category=laptop", b2.getTargetUrl());
    assertEquals("Khám Phá Laptop & PC", b2.getCtaText());
  }
}
