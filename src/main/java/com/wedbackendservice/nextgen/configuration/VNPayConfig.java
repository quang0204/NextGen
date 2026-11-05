package com.wedbackendservice.nextgen.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình cổng thanh toán VNPay.
 * Giá trị thật đặt qua biến môi trường, không commit secret vào repo.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "payment.vnpay")
public class VNPayConfig {
    String tmnCode;
    String hashSecret;
    String payUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    String returnUrl = "http://localhost:8080/nextgen/api/v1/payments/vnpay/return";
    String ipnUrl = "http://localhost:8080/nextgen/api/v1/payments/vnpay/ipn";
    String version = "2.1.0";
    String command = "pay";
    String currCode = "VND";
    String locale = "vn";
    String orderType = "other";
    int expireMinutes = 15;

    public boolean isConfigured() {
        return tmnCode != null && !tmnCode.isBlank()
                && hashSecret != null && !hashSecret.isBlank();
    }
}
