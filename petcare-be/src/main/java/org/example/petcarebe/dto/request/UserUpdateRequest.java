package org.example.petcarebe.dto.request;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UserUpdateRequest {
    private String fullName;
    @Pattern(regexp = "^(\\+84|0)[3|5|7|8|9]\\d{8}$", message = "Số điện thoại không hợp lệ")
    private String phone;
}
