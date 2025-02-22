package org.example.petcarebe.controller;

import org.example.petcarebe.dto.request.OtpRequest;
import org.example.petcarebe.dto.request.RegisterRequest;
import org.example.petcarebe.dto.request.ResetPasswordRequest;
import org.example.petcarebe.service.OtpService;
import org.example.petcarebe.service.UserService;
import org.example.petcarebe.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/otp")
public class OtpController {

    @Autowired
    private OtpService otpService;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder; // Thêm mã hóa mật khẩu

    /**
     * 📌 Gửi OTP cho đăng ký tài khoản
     */
    @PostMapping("/send-registration-otp")
    public ResponseEntity<String> sendRegistrationOtp(@RequestBody RegisterRequest registerRequest) {
        if (userService.existsByEmail(registerRequest.getEmail())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Error: Email đã được sử dụng.");
        }
        otpService.saveRegistrationRequest(registerRequest);
        otpService.generateOtp(registerRequest.getEmail(), OtpService.OTPType.REGISTRATION);
        return ResponseEntity.ok("OTP đã được gửi đến " + registerRequest.getEmail());
    }

    /**
     * 📌 Xác thực OTP đăng ký
     */
    @PostMapping("/verify-registration-otp")
    public ResponseEntity<String> verifyRegistrationOtp(@RequestBody OtpRequest otpRequest) {
        String email = otpRequest.getEmail();
        String otp = otpRequest.getOtp();

        if (otpService.validateOtp(email, otp, OtpService.OTPType.REGISTRATION)) {
            RegisterRequest registrationRequest = otpService.getRegistrationRequest(email);
            if (registrationRequest == null) {
                return ResponseEntity.badRequest().body("Không tìm thấy yêu cầu đăng ký.");
            }

            // Mã hóa mật khẩu trước khi lưu
            User newUser = new User();
            newUser.setEmail(registrationRequest.getEmail());
            newUser.setPassword(passwordEncoder.encode(registrationRequest.getPassword()));
            newUser.setFullName(registrationRequest.getFullName());
            userService.saveUser(newUser);

            otpService.clearOtp(email);
            otpService.removeRegistrationRequest(email);
            return ResponseEntity.ok("Tạo tài khoản thành công!");
        }
        return ResponseEntity.badRequest().body("OTP không hợp lệ hoặc đã hết hạn.");
    }

    /**
     * 📌 Gửi OTP quên mật khẩu
     */
    @PostMapping("/send-reset-password-otp")
    public ResponseEntity<String> sendResetPasswordOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (!userService.existsByEmail(email)) {
            return ResponseEntity.badRequest().body("Email không tồn tại.");
        }
        otpService.generateOtp(email, OtpService.OTPType.FORGOT_PASSWORD);
        return ResponseEntity.ok("OTP đặt lại mật khẩu đã được gửi.");
    }

    /**
     * 📌 Xác thực OTP quên mật khẩu
     */
    @PostMapping("/verify-reset-password-otp")
    public ResponseEntity<String> verifyResetPasswordOtp(@RequestBody OtpRequest otpRequest) {
        if (otpService.validateOtp(otpRequest.getEmail(), otpRequest.getOtp(), OtpService.OTPType.FORGOT_PASSWORD)) {
            return ResponseEntity.ok("OTP hợp lệ. Bạn có thể đặt lại mật khẩu.");
        }
        return ResponseEntity.badRequest().body("OTP không hợp lệ hoặc đã hết hạn.");
    }

    /**
     * 📌 Gửi lại OTP (Resend OTP)
     */
    @PostMapping("/resend-otp")
    public ResponseEntity<String> resendOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String otpTypeStr = request.get("otpType");

        // Kiểm tra nếu otpType bị null hoặc không hợp lệ
        OtpService.OTPType otpType;
        try {
            otpType = OtpService.OTPType.valueOf(otpTypeStr);
        } catch (IllegalArgumentException | NullPointerException e) {
            return ResponseEntity.badRequest().body("Loại OTP không hợp lệ.");
        }

        if (!userService.existsByEmail(email)) {
            return ResponseEntity.badRequest().body("Email không tồn tại.");
        }

        otpService.generateOtp(email, otpType);
        return ResponseEntity.ok("OTP đã được gửi lại thành công.");
    }
}