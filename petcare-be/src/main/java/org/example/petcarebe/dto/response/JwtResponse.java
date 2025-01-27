package org.example.petcarebe.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class JwtResponse {
    private String accessToken;
    private Long userId;
    private String fullName; // Add this field
    private String roleName;
    private String errorMessage; // Trường mới để chứa thông tin lỗi
    private String phone;
    private String email;
    private boolean isStatus;
    private String imageUrl;
    private LocalDate registration_date;
    private double totalSpent;
}
