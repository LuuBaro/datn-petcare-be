package org.example.petcarebe.dto.request;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class BookingRequestDTO {
    private LocalDate date;
    private LocalTime time;
    private String customerName;
    private String phone;
    private float depositAmount;
    private String status;
    private Long orderId;
    private Long userId; // Thay cho staffId
}