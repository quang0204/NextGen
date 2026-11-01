package com.wedbackendservice.nextgen.service.notification;

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
        log.info("[SMTP/SendGrid Gateway] Sending OTP [{}] to email: {}", otpCode, email);
         SimpleMailMessage message = new SimpleMailMessage();
         message.setTo(email);
         message.setSubject("Mã xác thực đăng nhập NextGen");
         message.setText("Mã OTP của bạn là: " + otpCode + ". Mã có hiệu lực trong 2 phút.");
         mailSender.send(message);
    }
}
