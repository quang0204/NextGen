package com.wedbackendservice.nextgen.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình cổng thanh toán MoMo (API v2).
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "payment.momo")
public class MomoConfig {
    String partnerCode;
    String accessKey;
    String secretKey;
    String endpoint = "https://test-payment.momo.vn/v2/gateway/api/create";
    String returnUrl = "http://localhost:8080/nextgen/api/v1/payments/momo/return";
    String ipnUrl = "http://localhost:8080/nextgen/api/v1/payments/momo/ipn";
    String requestType = "captureWallet";
    String partnerName = "NextGen";
    String storeId = "NextGenStore";
    String lang = "vi";
    int expireMinutes = 15;

    public boolean isConfigured() {
        return partnerCode != null && !partnerCode.isBlank()
                && accessKey != null && !accessKey.isBlank()
                && secretKey != null && !secretKey.isBlank();
    }
}
