package org.example.petcarebe.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Autowired
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // ✅ Tạo một MimeMessage mới
    public MimeMessage createMimeMessage() {
        return mailSender.createMimeMessage();
    }

    // ✅ Gửi email thông thường
    @Async
    public CompletableFuture<Boolean> sendEmail(String to, String subject, String content) {
        try {
            log.info("Bắt đầu gửi email đến: {}", to);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true); // true: email hỗ trợ HTML
            mailSender.send(message);
            log.info("Đã gửi email thành công đến: {}", to);
            return CompletableFuture.completedFuture(true);
        } catch (Exception e) {
            log.error("Lỗi khi gửi email đến {}: {}", to, e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        }
    }

    // ✅ Gửi email trong nền (không block thread chính)
    @Async
    public void send(MimeMessage message) {
        try {
            log.info("Bắt đầu gửi email async");
            mailSender.send(message);
            log.info("Đã gửi email async thành công");
        } catch (Exception e) {
            log.error("Lỗi khi gửi email async: {}", e.getMessage(), e);
            // Không throw exception vì đây là phương thức @Async
        }
    }
    
    // ✅ Gửi nhiều email cùng lúc
    @Async
    public CompletableFuture<Boolean> sendMultipleEmails(MimeMessage[] messages) {
        try {
            log.info("Bắt đầu gửi {} email", messages.length);
            mailSender.send(messages);
            log.info("Đã gửi tất cả {} email thành công", messages.length);
            return CompletableFuture.completedFuture(true);
        } catch (Exception e) {
            log.error("Lỗi khi gửi nhiều email: {}", e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        }
    }
}