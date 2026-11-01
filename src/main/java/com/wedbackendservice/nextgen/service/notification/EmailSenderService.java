package com.wedbackendservice.nextgen.service.notification;

public interface EmailSenderService {
    void sendEmailOtp(String email, String otpCode);
}
