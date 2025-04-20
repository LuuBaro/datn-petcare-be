package org.example.petcarebe.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class MedicalRecordRequestDTO {
    private Long petId;
    private Long vaccineId;
    private Long vetServiceId;
    private LocalDateTime examDate;
    private String symptoms;
    private String diagnosis;
    private String treatment;
    private String note;
}