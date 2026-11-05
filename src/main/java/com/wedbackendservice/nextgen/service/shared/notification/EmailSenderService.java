package com.wedbackendservice.nextgen.service.shared.notification;

public interface EmailSenderService {
    void sendEmailOtp(String email, String otpCode);
}
