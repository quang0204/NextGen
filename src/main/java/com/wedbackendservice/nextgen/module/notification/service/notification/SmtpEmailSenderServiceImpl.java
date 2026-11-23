package com.wedbackendservice.nextgen.module.notification.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@Primary
public class SmtpEmailSenderServiceImpl implements EmailSenderService {
  private final JavaMailSender mailSender;

  @Override
  public void sendEmailOtp(String email, String otpCode) {
    String masked = maskEmail(email);
    log.info("[SMTP Gateway] Sending OTP to email: {}", masked);
    try {
      SimpleMailMessage message = new SimpleMailMessage();
      message.setTo(email);
      message.setSubject("Mã xác thực đăng nhập NextGen");
      message.setText("Mã OTP của bạn là: " + otpCode + ". Mã có hiệu lực trong 2 phút.");
      mailSender.send(message);
      log.info("[SMTP Gateway] OTP sent successfully to email: {}", masked);
    } catch (Exception e) {
      log.error("[SMTP Gateway] Lỗi khi gửi email tới {}: {}", masked, e.getMessage());
      throw e;
    }
  }

  private String maskEmail(String email) {
    if (email == null || !email.contains("@")) return "***";
    int atIdx = email.indexOf('@');
    if (atIdx <= 2) return "***" + email.substring(atIdx);
    return email.substring(0, 2) + "***" + email.substring(atIdx);
  }
}
