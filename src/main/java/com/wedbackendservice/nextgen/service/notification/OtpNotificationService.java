package com.wedbackendservice.nextgen.service.notification;

import com.wedbackendservice.nextgen.enums.OtpChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpNotificationService {
    private final SmsGatewayService smsGatewayService;
    private final EmailSenderService emailSenderService;

    public void sendOtp(OtpChannel channel, String target, String otpCode) {
        if (channel.equals(OtpChannel.PHONE)) {
            smsGatewayService.sendSmsOtp(target, otpCode);
        } else if (channel.equals(OtpChannel.EMAIL)) {
            emailSenderService.sendEmailOtp(target, otpCode);
        } else {
            log.warn("Kenh gui OTP khong hop le: {}", channel);
        }
    }
}
