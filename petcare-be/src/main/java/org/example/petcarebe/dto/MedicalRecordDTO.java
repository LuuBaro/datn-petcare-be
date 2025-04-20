package org.example.petcarebe.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicalRecordDTO {
    private Long id;
    private LocalDateTime examDate;
    private String symptoms;
    private String diagnosis;
    private String treatment;
    private String note;
    private Long vetServiceId;
    private Long vaccineId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private VetPetDTO vetPetDTO;


}