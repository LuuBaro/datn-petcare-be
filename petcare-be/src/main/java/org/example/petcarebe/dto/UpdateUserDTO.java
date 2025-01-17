package org.example.petcarebe.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdateUserDTO {
    private String fullName;
    private String email;
    private String phone;
    private String password;
    private LocalDate registration_date;
    private double totalSpent;
    private boolean status;
    private String imageUrl;
}
