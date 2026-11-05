package com.wedbackendservice.nextgen.service.payment;

import com.wedbackendservice.nextgen.configuration.VNPayConfig;
import com.wedbackendservice.nextgen.entity.Order;
import com.wedbackendservice.nextgen.entity.Payment;
import com.wedbackendservice.nextgen.exception.AppException;
import com.wedbackendservice.nextgen.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class VNPayService {

    private static final DateTimeFormatter VNPAY_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String SUCCESS_CODE = "00";
    private static final long AMOUNT_MULTIPLIER = 100L;

    private final VNPayConfig vnPayConfig;

    /**
     * Tạo URL thanh toán VNPay (redirect khách sang cổng thanh toán).
     */
    public String buildPaymentUrl(Payment payment, Order order, String clientIp, String returnUrlOverride) {
        requireConfigured();

        LocalDateTime now = LocalDateTime.now();
        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Version", vnPayConfig.getVersion());
        params.put("vnp_Command", vnPayConfig.getCommand());
        params.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        params.put("vnp_Locale", vnPayConfig.getLocale());
        params.put("vnp_CurrCode", vnPayConfig.getCurrCode());
        params.put("vnp_TxnRef", payment.getTransactionRef());
        params.put("vnp_OrderInfo", "Thanh toan don hang " + safe(order.getOrderCode()));
        params.put("vnp_OrderType", vnPayConfig.getOrderType());
        params.put("vnp_Amount", String.valueOf(toVnpayAmount(payment.getAmount())));
        params.put("vnp_ReturnUrl", firstNonBlank(returnUrlOverride, vnPayConfig.getReturnUrl()));
        params.put("vnp_IpAddr", firstNonBlank(clientIp, "127.0.0.1"));
        params.put("vnp_CreateDate", now.format(VNPAY_TIME));
        params.put("vnp_ExpireDate", now.plusMinutes(Math.max(vnPayConfig.getExpireMinutes(), 1)).format(VNPAY_TIME));

        String hashData = buildHashData(params);
        String secureHash = hmacSHA512(vnPayConfig.getHashSecret(), hashData);
        return vnPayConfig.getPayUrl() + "?" + hashData + "&vnp_SecureHash=" + secureHash;
    }

    /**
     * Kiểm tra chữ ký params trả về từ VNPay (Return và IPN).
     */
    public boolean verifySignature(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return false;
        }
        String secureHash = params.get("vnp_SecureHash");
        if (secureHash == null || secureHash.isBlank()) {
            return false;
        }
        String expected = hmacSHA512(vnPayConfig.getHashSecret(), buildHashData(signableParams(params)));
        return expected.equalsIgnoreCase(secureHash);
    }

    public boolean isSuccessResponse(Map<String, String> params) {
        if (params == null) {
            return false;
        }
        String responseCode = params.get("vnp_ResponseCode");
        String transactionStatus = params.get("vnp_TransactionStatus");
        return SUCCESS_CODE.equals(responseCode)
                && (transactionStatus == null || SUCCESS_CODE.equals(transactionStatus));
    }

    public BigDecimal extractAmount(Map<String, String> params) {
        if (params == null || params.get("vnp_Amount") == null) {
            return null;
        }
        try {
            return new BigDecimal(params.get("vnp_Amount"))
                    .divide(BigDecimal.valueOf(AMOUNT_MULTIPLIER), 2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Map<String, String> signableParams(Map<String, String> params) {
        Map<String, String> result = new TreeMap<>();
        params.forEach((key, value) -> {
            if (value != null
                    && !"vnp_SecureHash".equals(key)
                    && !"vnp_SecureHashType".equals(key)) {
                result.put(key, value);
            }
        });
        return result;
    }

    private String buildHashData(Map<String, String> params) {
        StringBuilder builder = new StringBuilder();
        params.forEach((key, value) -> {
            if (value == null || value.isEmpty()) {
                return;
            }
            if (builder.length() > 0) {
                builder.append('&');
            }
            builder.append(key).append('=').append(urlEncode(value));
        });
        return builder.toString();
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String hmacSHA512(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            log.error("Cannot sign VNPay data", e);
            throw new AppException(ErrorCode.UNCATEGORIZE_EXCEPTION);
        }
    }

    private static long toVnpayAmount(BigDecimal amount) {
        if (amount == null) {
            return 0L;
        }
        return amount.multiply(BigDecimal.valueOf(AMOUNT_MULTIPLIER))
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();
    }

    private void requireConfigured() {
        if (!vnPayConfig.isConfigured()) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "Chưa cấu hình VNPay (payment.vnpay.tmn-code / hash-secret).");
        }
    }

    private static String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
