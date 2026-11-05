package com.wedbackendservice.nextgen.enums;

/**
 * Phương thức thanh toán được hỗ trợ.
 * Payment entity lưu method dạng String nên khi ghi DB dùng {@link #name()}.
 */
public enum PaymentMethod {
    COD,
    VNPAY,
    MOMO,
    BANK_TRANSFER;

    public boolean isOnline() {
        return this == VNPAY || this == MOMO;
    }

    public boolean isCod() {
        return this == COD;
    }
}
