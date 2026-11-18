package com.wedbackendservice.nextgen.module.notification.service.notification;

public interface EmailSenderService {
  void sendEmailOtp(String email, String otpCode);
}
