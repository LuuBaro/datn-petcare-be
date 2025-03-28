package org.example.petcarebe.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.petcarebe.dto.ContactRequest;
import org.example.petcarebe.service.ContactService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/contacts")
@RequiredArgsConstructor
@Slf4j
public class ContactController {

    private final ContactService contactService;

    @PostMapping
    public ResponseEntity<?> submitContact(@Validated @RequestBody ContactRequest request) {
        log.info("Nhận yêu cầu liên hệ từ: {}", request.getEmail());
        
        try {
            // Gửi email - phương thức đã được cập nhật để gửi trực tiếp
            contactService.processContactRequest(request);
            
            // Trả về phản hồi thành công
            return ResponseEntity.ok().body("Thông tin liên hệ đã được gửi thành công! Bạn sẽ nhận được email xác nhận sớm.");
        } catch (Exception e) {
            log.error("Lỗi xử lý yêu cầu liên hệ: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Có lỗi xảy ra khi gửi thông tin liên hệ: " + e.getMessage());
        }
    }
} 