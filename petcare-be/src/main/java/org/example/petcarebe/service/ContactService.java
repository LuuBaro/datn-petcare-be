package org.example.petcarebe.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.petcarebe.dto.ContactRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContactService {

    private final EmailService emailService;
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Value("${spring.mail.username}")
    private String adminEmail;

    public void processContactRequest(ContactRequest request) {
        log.info("Bắt đầu xử lý yêu cầu liên hệ từ {}", request.getEmail());
        
        try {
            // Gửi email trực tiếp cho khách hàng
            sendDirectCustomerEmail(request);
            log.info("Đã gửi email trực tiếp cho khách hàng: {}", request.getEmail());
            
            // Gửi email cho admin trong nền
            CompletableFuture.runAsync(() -> {
                try {
                    sendEmailToAdmin(request);
                    log.info("Đã gửi email thông báo cho admin");
                } catch (Exception e) {
                    log.error("Lỗi khi gửi email cho admin: {}", e.getMessage(), e);
                }
            });
        } catch (Exception e) {
            log.error("Lỗi khi gửi email cho khách hàng: {}", e.getMessage(), e);
        }
    }
    
    // Gửi email trực tiếp đến khách hàng không dùng async
    private void sendDirectCustomerEmail(ContactRequest contact) {
        try {
            log.info("Đang gửi email trực tiếp đến khách hàng: {}", contact.getEmail());
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(adminEmail);
            message.setTo(contact.getEmail());
            message.setSubject("Xác nhận thông tin liên hệ - PetCare");
            message.setText("Xin chào " + contact.getFullName() + ",\n\n" +
                    "Chúng tôi đã nhận được thông tin liên hệ của bạn và sẽ phản hồi trong thời gian sớm nhất có thể.\n\n" +
                    "Thông tin bạn đã gửi:\n" +
                    "- Họ và tên: " + contact.getFullName() + "\n" +
                    "- Email: " + contact.getEmail() + "\n" +
                    "- Số điện thoại: " + contact.getPhoneNumber() + "\n" +
                    "- Nội dung: " + contact.getMessage() + "\n\n" +
                    "Trân trọng,\n" +
                    "Đội ngũ PetCare");
            
            mailSender.send(message);
            log.info("Đã gửi email thành công cho khách hàng: {}", contact.getEmail());
        } catch (Exception e) {
            log.error("Không thể gửi email cho khách hàng {}: {}", contact.getEmail(), e.getMessage(), e);
            throw e;
        }
    }
    
    private void sendEmailToAdmin(ContactRequest contact) throws MessagingException {
        MimeMessage message = emailService.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        
        helper.setTo(adminEmail);
        helper.setSubject("Thông tin liên hệ mới từ khách hàng: " + contact.getFullName());
        
        String content = "<div style='font-family: Arial, sans-serif; padding: 20px;'>"
                + "<h2 style='color: #fbb321;'>Thông tin liên hệ mới</h2>"
                + "<p><strong>Họ và tên:</strong> " + contact.getFullName() + "</p>"
                + "<p><strong>Email:</strong> " + contact.getEmail() + "</p>"
                + "<p><strong>Số điện thoại:</strong> " + contact.getPhoneNumber() + "</p>"
                + "<p><strong>Nội dung:</strong> " + contact.getMessage() + "</p>"
                + "</div>";
        
        helper.setText(content, true);
        emailService.send(message);
    }
    
    private void sendConfirmationToCustomer(ContactRequest contact) throws MessagingException {
        MimeMessage message = emailService.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        
        helper.setTo(contact.getEmail());
        helper.setSubject("Xác nhận thông tin liên hệ - PetCare");
        
        String content = "<div style='font-family: Arial, sans-serif; padding: 20px;'>"
                + "<h2 style='color: #fbb321;'>Cảm ơn bạn đã liên hệ với PetCare</h2>"
                + "<p>Xin chào " + contact.getFullName() + ",</p>"
                + "<p>Chúng tôi đã nhận được thông tin liên hệ của bạn và sẽ phản hồi trong thời gian sớm nhất có thể.</p>"
                + "<p>Thông tin bạn đã gửi:</p>"
                + "<ul>"
                + "<li><strong>Họ và tên:</strong> " + contact.getFullName() + "</li>"
                + "<li><strong>Email:</strong> " + contact.getEmail() + "</li>"
                + "<li><strong>Số điện thoại:</strong> " + contact.getPhoneNumber() + "</li>"
                + "<li><strong>Nội dung:</strong> " + contact.getMessage() + "</li>"
                + "</ul>"
                + "<p>Trân trọng,<br/>Đội ngũ PetCare</p>"
                + "</div>";
        
        helper.setText(content, true);
        emailService.send(message);
    }
} 