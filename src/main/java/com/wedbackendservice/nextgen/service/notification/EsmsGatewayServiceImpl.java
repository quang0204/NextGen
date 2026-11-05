package com.wedbackendservice.nextgen.service.notification;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.wedbackendservice.nextgen.exception.AppException;
import com.wedbackendservice.nextgen.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Gửi SMS OTP qua eSMS.
 */
@Service
@Slf4j
@Primary
public class EsmsGatewayServiceImpl implements SmsGatewayService {

    private static final String SUCCESS_CODE = "100";

    private final RestClient restClient;

    @Value("${app.sms.esms.api-url}")
    private String apiUrl;

    @Value("${app.sms.esms.api-key}")
    private String apiKey;

    @Value("${app.sms.esms.secret-key}")
    private String secretKey;

    @Value("${app.sms.esms.sandbox:0}")
    private String sandbox;

    @Value("${app.sms.esms.brandname:}")
    private String brandname;

    @Value("${app.sms.esms.type:2}")
    private String smsType;

    @Value("${app.sms.esms.otp-template}")
    private String otpTemplate;

    public record EsmsResponse(
            @JsonProperty("CodeResult") String codeResult,
            @JsonProperty("ErrorMessage") String errorMessage,
            @JsonProperty("SMSID") String smsId
    ) {}

    public EsmsGatewayServiceImpl(
            @Value("${app.sms.esms.connect-timeout-ms:5000}") int connectTimeoutMs,
            @Value("${app.sms.esms.read-timeout-ms:10000}") int readTimeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(java.time.Duration.ofMillis(connectTimeoutMs));
        factory.setReadTimeout(java.time.Duration.ofMillis(readTimeoutMs));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public void sendSmsOtp(String phone, String content) {
        content = String.format(otpTemplate, content);
        if (secretKey.isEmpty() || apiKey.isEmpty()) {
            log.error("Chưa cấu hình app.sms.esms.api-key / secret-key");
            throw new AppException(ErrorCode.SMS_SEND_FAILED);
        }

        String normalizedPhone = normalizePhone(phone);
        boolean sandboxOn = "1".equals(sandbox);

        Map<String, String> body = new HashMap<>();
        body.put("ApiKey", apiKey);
        body.put("SecretKey", secretKey);
        body.put("Phone", normalizedPhone);
        body.put("Content", content);
        body.put("SmsType", smsType);
        body.put("IsUnicode", "0");
        body.put("Sandbox", sandbox);
        body.put("RequestId", UUID.randomUUID().toString());
        if (brandname != null && !brandname.isBlank()) {
            body.put("Brandname", brandname);
        }

        log.info("eSMS request: phone={}, type='{}', brandname='{}', sandbox='{}'",
                maskPhone(normalizedPhone), smsType, brandname, sandbox);

        EsmsResponse response;
        try {
            response = restClient.post()
                    .uri(apiUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(EsmsResponse.class);
        } catch (Exception e) {
            log.error("Không gọi được eSMS: {}", e.getMessage());
            throw new AppException(ErrorCode.SMS_SEND_FAILED);
        }

        if (response == null || !SUCCESS_CODE.equals(response.codeResult())) {
            String code = response != null ? response.codeResult() : "EMPTY";
            String err = response != null ? response.errorMessage() : "Phản hồi rỗng";
            log.error("eSMS lỗi {}: {} ({})", code, describeError(code), err);
            throw new AppException(ErrorCode.SMS_SEND_FAILED);
        }

        log.info("eSMS đã nhận yêu cầu gửi tới {} (SMSID={}, sandbox={})",
                maskPhone(normalizedPhone), response.smsId(), sandboxOn);
    }

    private String normalizePhone(String phone) {
        String p = phone == null ? "" : phone.trim();
        return p.startsWith("+84") ? "0" + p.substring(3) : p;
    }

    private String maskPhone(String p) {
        return p.length() < 8 ? "***" : p.substring(0, 3) + "***" + p.substring(p.length() - 4);
    }

    private String describeError(String code) {
        return switch (code) {
            case "101" -> "Sai ApiKey hoặc SecretKey";
            case "103" -> "Số dư tài khoản không đủ";
            case "104" -> "Brandname chưa đăng ký hoặc chưa hoạt động";
            case "118" -> "Loại tin nhắn không hợp lệ";
            case "132" -> "Tài khoản chưa có quyền gửi đầu số cố định";
            case "140" -> "IP chưa được whitelist";
            case "146" -> "Template chưa đăng ký hoặc nội dung không khớp template";
            default -> "Lỗi không xác định";
        };
    }
}
