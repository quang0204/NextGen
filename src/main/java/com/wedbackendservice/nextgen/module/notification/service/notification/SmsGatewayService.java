package com.wedbackendservice.nextgen.module.notification.service.notification;

public interface SmsGatewayService {
  void sendSmsOtp(String phoneNumber, String otpCode);
}
