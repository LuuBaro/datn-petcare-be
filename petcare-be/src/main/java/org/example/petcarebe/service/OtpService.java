package org.example.petcarebe.service;


import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.example.petcarebe.dto.request.RegisterRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {

    private final Map<String, RegisterRequest> registrationRequests = new HashMap<>();
    private final Map<String, String> otpStorage = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> otpExpiryStorage = new ConcurrentHashMap<>();
    private final int OTP_EXPIRATION_MINUTES = 1;

    @Autowired
    private EmailService emailService;

    public enum OTPType {
        REGISTRATION,
        FORGOT_PASSWORD
    }

    @Async
    public void generateOtp(String email, OTPType otpType) {
        String otp = String.valueOf(new Random().nextInt(899999) + 100000); // Tạo OTP ngẫu nhiên 6 chữ số
        String key = email + "_" + otpType.name();
        otpStorage.put(key, otp);
        otpExpiryStorage.put(email, LocalDateTime.now().plusMinutes(OTP_EXPIRATION_MINUTES));

        // Gửi OTP qua email
        sendOtpEmail(email, otp, otpType);
        System.out.println("OTP generated: " + otp);
    }

    public boolean validateOtp(String email, String otp, OTPType otpType) {
        String storedOtp = otpStorage.get(email + "_" + otpType);
        if (storedOtp == null || !storedOtp.equals(otp)) {
            return false;
        }

        LocalDateTime expiryTime = otpExpiryStorage.get(email);
        if (expiryTime != null && LocalDateTime.now().isAfter(expiryTime)) {
            clearOtp(email);
            return false;
        }
        return true;
    }

    public void clearOtp(String email) {
        otpStorage.remove(email);
        otpExpiryStorage.remove(email);
    }

    public void saveRegistrationRequest(RegisterRequest request) {
        registrationRequests.put(request.getEmail(), request);
    }

    public RegisterRequest getRegistrationRequest(String email) {
        return registrationRequests.get(email);
    }

    public void removeRegistrationRequest(String email) {
        registrationRequests.remove(email);
    }

//    private void sendOtpEmail(String email, String otp) {
//        String subject = "Your OTP Code - PetCare";
//        String message = buildOtpEmailContent(otp);
//        emailService.sendEmail(email, subject, message);
//    }

    @Async
    public void sendOtpEmail(String email, String otp, OTPType otpType) {
        MimeMessage message = emailService.createMimeMessage();
        MimeMessageHelper helper;

        try {
            helper = new MimeMessageHelper(message, true);
            helper.setTo(email);
            helper.setSubject("Mã Xác Thực OTP từ PetCare");

            // Tạo URL chứa mã OTP
            String otpLink = "http://localhost:5173/verify-otp?email=" + email + "&otp=" + otp + "&type=" + otpType.name();

            // Nội dung email với thiết kế nâng cấp
            String emailBody = """
                    <div style="background-color: #f8f8f8; padding: 20px; font-family: Arial, sans-serif;">
                        <div style="max-width: 580px; margin: 0 auto; background-color: #ffffff; 
                                    border-radius: 10px; overflow: hidden; 
                                    box-shadow: 0px 4px 12px rgba(0, 0, 0, 0.1);">
                    
                            <!-- Header -->
                            <div style="background: linear-gradient(to right, #fbb321, #f89406); 
                                        padding: 18px; text-align: center;">
                                <h1 style="color: #ffffff; margin: 0; font-size: 26px;">PetCare</h1>
                            </div>
                    
                            <!-- Nội dung -->
                            <div style="padding: 28px;">
                                <h2 style="color: #333; text-align: center;">Xác Thực OTP</h2>
                                <p style="font-size: 15px; color: #555; text-align: center;">
                                    Cảm ơn bạn đã sử dụng dịch vụ của <strong>PetCare</strong>.
                                </p>
                                <p style="font-size: 15px; color: #555; text-align: center;">
                                    Mã OTP của bạn là:
                                </p>
                    
                                <!-- Mã OTP với hiệu ứng gradient -->
                                <div style="text-align: center; margin: 22px 0;">
                                    <span style="display: inline-block; background: linear-gradient(135deg, #fbb321, #f89406);
                                                 color: #ffffff; font-size: 24px; font-weight: bold; 
                                                 padding: 12px 24px; border-radius: 6px; 
                                                 box-shadow: 0px 3px 8px rgba(251, 179, 33, 0.3); letter-spacing: 2px;">
                                        %s
                                    </span>
                                </div>
                    
                                <!-- Nút xác thực nhỏ gọn hơn -->
                                <div style="text-align: center; margin-top: 18px;">
                                    <a href="%s" style="background-color: #fbb321; color: #ffffff; 
                                                        padding: 10px 22px; font-size: 16px; 
                                                        border-radius: 4px; text-decoration: none; 
                                                        display: inline-block; font-weight: bold;
                                                        box-shadow: 0px 2px 6px rgba(251, 179, 33, 0.3);">
                                        Xác Thực Ngay
                                    </a>
                                </div>
                    
                                <p style="font-size: 15px; color: #555; margin-top: 18px; text-align: center;">
                                    Mã OTP có giá trị trong <strong>%d phút</strong>.
                                </p>
                    
                                <p style="font-size: 13px; color: #888; margin-top: 12px; text-align: center;">
                                    Nếu bạn không yêu cầu mã này, vui lòng bỏ qua email này.
                                </p>
                            </div>
                    
                            <!-- Footer -->
                            <div style="background-color: #f8f8f8; padding: 14px; text-align: center; font-size: 12px; color: #888;">
                                © 2025 PetCare. Mọi quyền được bảo lưu.
                            </div>
                        </div>
                    </div>
                    """.formatted(otp, otpLink, OTP_EXPIRATION_MINUTES);

            helper.setText(emailBody, true);
            emailService.send(message);
        } catch (MessagingException e) {
            System.err.println("Error sending OTP email: " + e.getMessage());
        }
    }


}
