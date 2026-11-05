package com.wedbackendservice.nextgen.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình chuyển khoản ngân hàng nội địa.
 * Khách chuyển đúng nội dung (transferPrefix + transactionRef) để đối soát.
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
    String hotline;
    int expireMinutes = 1440;

    public boolean isConfigured() {
        return accountNumber != null && !accountNumber.isBlank()
                && accountName != null && !accountName.isBlank();
    }

    public String buildTransferContent(String transactionRef) {
        String prefix = transferPrefix == null || transferPrefix.isBlank() ? "NG" : transferPrefix.trim();
        return prefix + " " + transactionRef;
    }
}
