package org.example.petcarebe.dto;

import lombok.*;
import org.example.petcarebe.enums.PetType;
import org.example.petcarebe.enums.StatusType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VetPetWeightDTO {
    private Long petWeightId;
    private String weightRange;
    private float priceMultiplier;
    private StatusType statusType;
    private PetType petType;
}