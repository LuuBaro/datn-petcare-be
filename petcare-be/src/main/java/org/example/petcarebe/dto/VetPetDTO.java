package org.example.petcarebe.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import org.example.petcarebe.enums.PetType;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VetPetDTO {
    private Long id;

    @NotBlank(message = "Tên thú cưng không được để trống")
    private String namePet;

    @Positive(message = "Tuổi phải là số dương")
    private Float age;

    private String note;

    @NotBlank(message = "Số điện thoại chủ không được để trống")
    private String phoneBoss;

    @NotBlank(message = "Tên chủ không được để trống")
    private String nameBoss;

    @NotNull(message = "Loại thú cưng không được để trống")
    private PetType petType;

    private boolean deleted;

    @NotNull(message = "Loại cân nặng không được để trống")
    private VetPetWeightDTO petWeight;


    @JsonIgnore // Prevent circular reference when serialized within MedicalRecordDTO
    private List<MedicalRecordDTO> medicalRecords = new ArrayList<>();


}