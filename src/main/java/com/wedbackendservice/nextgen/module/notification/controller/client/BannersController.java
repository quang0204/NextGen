package com.wedbackendservice.nextgen.module.notification.controller.client;

import com.wedbackendservice.nextgen.core.dto.ApiResponse;
import com.wedbackendservice.nextgen.module.notification.service.MinioStorageService;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/banners")
@RequiredArgsConstructor
public class BannersController {

  private final MinioStorageService minioStorageService;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class BannerItemDto {
    private Long id;
    private String title;
    private String subtitle;
    private String imageUrl;
    private String targetUrl;
    private String ctaText;
    private String type;
  }

  @GetMapping
  public ApiResponse<List<BannerItemDto>> getActiveBanners() {
    String bucket = minioStorageService.getBannersBucket();
    String banner1Url = minioStorageService.getAccessUrl(bucket, "banner1.png");
    String banner2Url = minioStorageService.getAccessUrl(bucket, "banner2.png");

    List<BannerItemDto> banners =
        List.of(
            BannerItemDto.builder()
                .id(1L)
                .title("GIỜ VÀNG FLASH SALE")
                .subtitle("Săn deal cực hot – Đón công nghệ đỉnh cao")
                .imageUrl(banner1Url)
                .targetUrl("/products?sale=true")
                .ctaText("Săn Deal Flash Sale")
                .type("FLASH_SALE")
                .build(),
            BannerItemDto.builder()
                .id(2L)
                .title("LAPTOP & PC CAO CẤP")
                .subtitle("Hiệu năng vượt trội • Kết nối tương lai")
                .imageUrl(banner2Url)
                .targetUrl("/products?category=laptop")
                .ctaText("Khám Phá Laptop & PC")
                .type("LAPTOP_PC")
                .build());

    return ApiResponse.ok("Lấy danh sách banner thành công", banners);
  }
}
