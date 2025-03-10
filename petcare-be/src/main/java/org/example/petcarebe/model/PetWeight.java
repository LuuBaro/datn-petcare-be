package org.example.petcarebe.model;

import jakarta.persistence.*;
import lombok.*;
import org.example.petcarebe.enums.PetType;

@Entity
@Table(name = "pet_weights")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PetWeight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long petWeightId;

    @Enumerated(EnumType.STRING)
    @Column(name = "pet_type", nullable = false)
    private PetType petType;

    @Column(name = "weight_range", nullable = false)
    private String weightRange;

    @Column(name = "price_multiplier", nullable = false)
    private float priceMultiplier;
}
