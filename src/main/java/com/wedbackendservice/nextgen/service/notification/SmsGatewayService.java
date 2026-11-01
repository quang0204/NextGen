package com.wedbackendservice.nextgen.service.notification;

public interface SmsGatewayService {
    void sendSmsOtp(String phoneNumber, String otpCode);
}
