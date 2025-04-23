package org.example.petcarebe.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderVetDetailDTO {
    private Long id;
    private Long orderId; // Chỉ lấy ID của Orders thay vì toàn bộ đối tượng
    private MedicalRecordDTO medicalRecord; // Ánh xạ MedicalRecord sang MedicalRecordDTO
    private Integer quantity;
    private Float price;
}