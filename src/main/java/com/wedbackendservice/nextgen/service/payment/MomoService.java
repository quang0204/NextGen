package com.wedbackendservice.nextgen.service.payment;

import com.wedbackendservice.nextgen.configuration.MomoConfig;
import com.wedbackendservice.nextgen.entity.Order;
import com.wedbackendservice.nextgen.entity.Payment;
import com.wedbackendservice.nextgen.exception.AppException;
import com.wedbackendservice.nextgen.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MomoService {

    private final MomoConfig momoConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Gọi API MoMo để lấy payUrl (ví MoMo / QR).
     */
    public String createPaymentUrl(Payment payment, Order order, String returnUrlOverride) {
        requireConfigured();

        String requestId = payment.getTransactionRef() + "-" + System.currentTimeMillis();
        String orderId = payment.getTransactionRef();
        String orderInfo = "Thanh toan don hang " + safe(order.getOrderCode());
        long amount = payment.getAmount() == null ? 0L : payment.getAmount().longValue();
        String extraData = "";
        String redirectUrl = firstNonBlank(returnUrlOverride, momoConfig.getReturnUrl());
        String requestType = momoConfig.getRequestType();

        String rawSignature = "accessKey=" + momoConfig.getAccessKey()
                + "&amount=" + amount
                + "&extraData=" + extraData
                + "&ipnUrl=" + momoConfig.getIpnUrl()
                + "&orderId=" + orderId
                + "&orderInfo=" + orderInfo
                + "&partnerCode=" + momoConfig.getPartnerCode()
                + "&redirectUrl=" + redirectUrl
                + "&requestId=" + requestId
                + "&requestType=" + requestType;
        String signature = hmacSHA256(momoConfig.getSecretKey(), rawSignature);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("partnerCode", momoConfig.getPartnerCode());
        body.put("partnerName", momoConfig.getPartnerName());
        body.put("storeId", momoConfig.getStoreId());
        body.put("requestId", requestId);
        body.put("amount", amount);
        body.put("orderId", orderId);
        body.put("orderInfo", orderInfo);
        body.put("redirectUrl", redirectUrl);
        body.put("ipnUrl", momoConfig.getIpnUrl());
        body.put("lang", momoConfig.getLang());
        body.put("requestType", requestType);
        body.put("extraData", extraData);
        body.put("signature", signature);

        try {
            String response = RestClient.create()
                    .post()
                    .uri(momoConfig.getEndpoint())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode json = objectMapper.readTree(response == null ? "{}" : response);
            JsonNode resultCodeNode = json.get("resultCode");
            int resultCode = resultCodeNode != null && resultCodeNode.isNumber() ? resultCodeNode.asInt() : -1;
            JsonNode payUrlNode = json.get("payUrl");
            String payUrl = payUrlNode != null && !payUrlNode.isNull() ? payUrlNode.asText() : null;
            if (resultCode != 0 || payUrl == null || payUrl.isBlank()) {
                JsonNode messageNode = json.get("message");
                String message = messageNode != null && !messageNode.isNull()
                        ? messageNode.asText()
                        : "Không tạo được giao dịch MoMo.";
                log.warn("MoMo create payment failed: resultCode={}, message={}", resultCode, message);
                throw new AppException(ErrorCode.INVALID_REQUEST, "MoMo: " + message);
            }
            return payUrl;
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Cannot call MoMo create payment API", e);
            throw new AppException(ErrorCode.UNCATEGORIZE_EXCEPTION, "Không kết nối được cổng thanh toán MoMo.");
        }
    }

    /**
     * Xác thực chữ ký IPN/Return của MoMo.
     */
    public boolean verifySignature(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return false;
        }
        String signature = params.get("signature");
        if (signature == null || signature.isBlank()) {
            return false;
        }
        String rawSignature = "accessKey=" + momoConfig.getAccessKey()
                + "&amount=" + value(params, "amount")
                + "&extraData=" + value(params, "extraData")
                + "&message=" + value(params, "message")
                + "&orderId=" + value(params, "orderId")
                + "&orderInfo=" + value(params, "orderInfo")
                + "&orderType=" + value(params, "orderType")
                + "&partnerCode=" + value(params, "partnerCode")
                + "&payType=" + value(params, "payType")
                + "&requestId=" + value(params, "requestId")
                + "&responseTime=" + value(params, "responseTime")
                + "&resultCode=" + value(params, "resultCode")
                + "&transId=" + value(params, "transId");
        String expected = hmacSHA256(momoConfig.getSecretKey(), rawSignature);
        return expected.equalsIgnoreCase(signature);
    }

    public boolean isSuccessResult(Map<String, String> params) {
        return params != null && "0".equals(params.get("resultCode"));
    }

    private static String hmacSHA256(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new AppException(ErrorCode.UNCATEGORIZE_EXCEPTION);
        }
    }

    private void requireConfigured() {
        if (!momoConfig.isConfigured()) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "Chưa cấu hình MoMo (payment.momo.partner-code / access-key / secret-key).");
        }
    }

    private static String value(Map<String, String> params, String key) {
        String v = params.get(key);
        return v == null ? "" : v;
    }

    private static String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
