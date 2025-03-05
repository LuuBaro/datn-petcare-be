package org.example.petcarebe.controller;

import org.example.petcarebe.dto.UpdateUserDTO;
import org.example.petcarebe.dto.request.UserUpdateRequest;
import org.example.petcarebe.model.User;
import org.example.petcarebe.service.JwtService;
import org.example.petcarebe.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")

public class UserController {
    @Autowired
    private final UserService userService;

    @Autowired
    private JwtService jwtUtil;
    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Lấy danh sách tất cả người dùng
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        User user = userService.findById(id);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        return ResponseEntity.ok(user);
    }

    // Cập nhật thông tin người dùng bên account
    @PutMapping("/update/{id}")
    public ResponseEntity<Map<String, Object>> updateUser(
            @PathVariable Long id,
            @RequestBody UserUpdateRequest updateRequest,
            Authentication authentication) {

        String email = authentication.getName();
        User user = userService.findByEmail(email);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        try {
            if (!id.equals(user.getUserId())) {
                throw new AccessDeniedException("You are not authorized to update this user's information");
            }

            // Cập nhật thông tin người dùng
            User updatedUser = userService.updateUserAccount(id, updateRequest, user.getUserId().toString());

            // Tạo token mới chứa thông tin đã cập nhật
            String newToken = jwtUtil.generateToken(userService.loadUserByUsername(email), updatedUser);

            // Trả về thông tin người dùng mới + token mới
            Map<String, Object> response = new HashMap<>();
            response.put("user", updatedUser);
            response.put("token", newToken);

            return ResponseEntity.ok(response);
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // Cập nhật avatar người dùng
    @PutMapping("/update/avatar/{userId}")
    public ResponseEntity<User> updateAvatar(@PathVariable Long userId, @RequestBody UpdateUserDTO imageUrl) {
        User user = userService.updateAvatar(userId, imageUrl);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        return ResponseEntity.ok(user);
    }


    @GetMapping("/staff")
    public ResponseEntity<List<User>> getAllStaff() {
        List<User> staffList = userService.getAllStaff();
        return ResponseEntity.ok(staffList);
    }

    @PostMapping("/create-staff")
    public ResponseEntity<?> createStaff(@RequestBody User staff) {
        try {
            if (staff.getEmail() == null || staff.getEmail().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Email không được để trống."));
            }
            if (staff.getPassword() == null || staff.getPassword().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Mật khẩu không được để trống."));
            }

            User savedStaff = userService.saveStaff(staff);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedStaff);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Đã xảy ra lỗi hệ thống."));
        }
    }

    @PutMapping("/update-staff/{userId}")
    public ResponseEntity<?> updateStaff(@PathVariable Long userId, @RequestBody User staff) {
        try {
            if (staff.getEmail() == null || staff.getEmail().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Email không được để trống."));
            }

            User updatedStaff = userService.updateStaff(userId, staff);
            return ResponseEntity.ok(updatedStaff);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Đã xảy ra lỗi hệ thống."));
        }
    }
}
