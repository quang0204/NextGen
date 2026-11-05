package com.wedbackendservice.nextgen.service.shared.notification;

public interface SmsGatewayService {
    void sendSmsOtp(String phoneNumber, String otpCode);
}
