package org.example.petcarebe.dto;

import lombok.*;
import org.example.petcarebe.enums.PetType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VetPetDTO {
    // Pet info
    private Long id;
    private String namePet;
    private Float age; // Changed to Float to match Pet entity
    private PetType petType;
    private String note;
    private Float price; // Changed to Float to match Pet entity
    private String phoneBoss;
    private String nameBoss;
    private Float depositAmount;
    private Float paidAmount;
    private boolean deleted;

    // Reference to PetWeight info
    private VetPetWeightDTO petWeight;
}