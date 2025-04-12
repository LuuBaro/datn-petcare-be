package org.example.petcarebe.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    
    @Autowired
    private JavaMailSender mailSender;
    
    /**
     * Gửi email HTML với nội dung định dạng
     * 
     * @param to Địa chỉ email người nhận
     * @param subject Tiêu đề email
     * @param htmlContent Nội dung email dạng HTML
     * @return true nếu gửi thành công, false nếu có lỗi
     */
    public boolean sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            logger.info("Bắt đầu gửi email tới '{}' với tiêu đề: '{}'", to, subject);
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setFrom("petcarefptct@gmail.com");
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            logger.info("Email đã được gửi thành công tới: {}", to);
            return true;
        } catch (MessagingException e) {
            logger.error("Lỗi khi gửi email tới '{}': {}", to, e.getMessage());
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            logger.error("Lỗi không mong muốn khi gửi email: {}", e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // ✅ Tạo một MimeMessage mới
    public MimeMessage createMimeMessage() {
        return mailSender.createMimeMessage();
    }

    // ✅ Gửi email thông thường
    public void sendEmail(String to, String subject, String content) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(content, true); // true: email hỗ trợ HTML
        mailSender.send(message);
    }

    // ✅ Gửi email trong nền (không block thread chính)
    @Async
    public void send(MimeMessage message) {
        mailSender.send(message);
    }
}