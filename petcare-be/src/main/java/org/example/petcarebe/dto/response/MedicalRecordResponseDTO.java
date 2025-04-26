package org.example.petcarebe.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class MedicalRecordResponseDTO {
    private Long id;
    private Long petId;
    private String petName;
    private Long vaccineId;
    private String vaccineName;
    private Long vetServiceId;
    private String vetServiceName;
    private LocalDateTime examDate;
    private String symptoms;
    private String diagnosis;
    private String treatment;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}