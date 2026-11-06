package com.wedbackendservice.nextgen.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình tài khoản nhận chuyển khoản / QR mô phỏng.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "payment.bank-transfer")
public class BankTransferConfig {
    String bankName;
    String accountNumber;
    String accountName;
    String branch;
    String transferPrefix = "NG";
    int expireMinutes = 30;

    public boolean isConfigured() {
        return accountNumber != null && !accountNumber.isBlank()
                && accountName != null && !accountName.isBlank();
    }

    public String buildTransferContent(String transactionRef) {
        String prefix = transferPrefix == null || transferPrefix.isBlank() ? "NG" : transferPrefix.trim();
        return prefix + " " + transactionRef;
    }

    public String buildQrContent(String transactionRef) {
        return "BANK|" + safe(bankName) + "|" + safe(accountNumber) + "|" + buildTransferContent(transactionRef);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
