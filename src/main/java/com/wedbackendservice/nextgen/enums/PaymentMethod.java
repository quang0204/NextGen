package com.wedbackendservice.nextgen.enums;

/**
 * Phương thức thanh toán của nhóm 10.
 * BANK_TRANSFER là thanh toán QR mô phỏng (khách quét mã / chuyển khoản).
 */
public enum PaymentMethod {
    COD,
    BANK_TRANSFER;

    public boolean isCod() {
        return this == COD;
    }
}
