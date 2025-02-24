package org.example.petcarebe.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class PasswordEmailService {

    @Autowired
    private EmailService emailService;

    @Autowired
    private JavaMailSender mailSender;

    /**
     * 📩 Gửi email khi mật khẩu của người dùng thay đổi thành công.
     * @param email Email người dùng.
     */
    @Async
    public void sendPasswordChangeAlert(String email) {
        MimeMessage message = emailService.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(email);
            helper.setSubject("Thông Báo: Mật khẩu của bạn đã được thay đổi!");

            // Nội dung email cảnh báo đổi mật khẩu
            String emailContent = """
                    <div style="background-color: #f8f8f8; padding: 20px; font-family: Arial, sans-serif;">
                        <div style="max-width: 600px; margin: 0 auto; background-color: #ffffff; 
                                    border-radius: 10px; overflow: hidden; 
                                    box-shadow: 0px 4px 12px rgba(0, 0, 0, 0.1);">
                    
                            <!-- Header -->
                            <div style="background: linear-gradient(to right, #fbb321, #f89406); 
                                        padding: 18px; text-align: center;">
                                <h1 style="color: #ffffff; margin: 0; font-size: 26px;">PetCare</h1>
                            </div>
                    
                            <!-- Nội dung -->
                            <div style="padding: 28px;">
                                <h2 style="color: #333; text-align: center;">Mật khẩu của bạn đã được thay đổi</h2>
                                <p style="font-size: 15px; color: #555; text-align: center;">
                                    Nếu bạn đã thay đổi mật khẩu, bạn có thể bỏ qua email này.
                                </p>
                    
                                <p style="font-size: 15px; color: #555; text-align: center;">
                                    Nếu bạn <b>không</b> thay đổi mật khẩu, hãy <b>đặt lại mật khẩu ngay</b> bằng cách nhấp vào liên kết bên dưới:
                                </p>
                    
                                <!-- Nút đặt lại mật khẩu -->
                                <div style="text-align: center; margin-top: 18px;">
                                    <a href="http://localhost:5173/resetPassword"
                                       style="background-color: #fbb321; color: #ffffff; 
                                              padding: 10px 22px; font-size: 16px; 
                                              border-radius: 4px; text-decoration: none; 
                                              display: inline-block; font-weight: bold;
                                              box-shadow: 0px 2px 6px rgba(251, 179, 33, 0.3);">
                                        Đặt lại mật khẩu ngay
                                    </a>
                                </div>
                    
                                <p style="font-size: 13px; color: #888; margin-top: 12px; text-align: center;">
                                    Nếu liên kết trên không hoạt động, vui lòng truy cập: <br>
                                    <a href="http://localhost:5173/resetPassword">http://localhost:5173/resetPassword</a>
                                </p>
                            </div>
                    
                            <!-- Footer -->
                            <div style="background-color: #f8f8f8; padding: 14px; text-align: center; font-size: 12px; color: #888;">
                                © 2025 PetCare. Mọi quyền được bảo lưu.
                            </div>
                        </div>
                    </div>
                    """;

            helper.setText(emailContent, true);
            emailService.send(message);
        } catch (MessagingException e) {
            System.err.println("❌ Lỗi gửi email đổi mật khẩu: " + e.getMessage());
        }
    }

    @Async
    public void sendResetPasswordEmail(String email) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(email);
            helper.setSubject("Yêu cầu đặt lại mật khẩu từ PetCare");

            // 📩 Nội dung email đặt lại mật khẩu
            String emailBody = """
                <div style="background-color: #f8f8f8; padding: 20px;">
                    <h2>Đặt lại mật khẩu của bạn</h2>
                    <p>Bạn đã yêu cầu đặt lại mật khẩu cho tài khoản của mình. Nhấn vào nút bên dưới để tiếp tục:</p>
                    <a href="http://localhost:5173/resetPassword?email=%s" 
                       style="display: inline-block; background-color: #fbb321; color: #ffffff; 
                              padding: 10px 20px; border-radius: 5px; text-decoration: none;">
                        Đặt lại mật khẩu
                    </a>
                    <p>Nếu bạn không yêu cầu, vui lòng bỏ qua email này.</p>
                </div>
            """.formatted(email);

            helper.setText(emailBody, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            System.err.println("Lỗi gửi email reset mật khẩu: " + e.getMessage());
        }
    }
}