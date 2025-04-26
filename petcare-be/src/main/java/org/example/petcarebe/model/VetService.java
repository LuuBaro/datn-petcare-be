package org.example.petcarebe.model;

import jakarta.persistence.*;
import lombok.*;
import org.example.petcarebe.enums.PetType;

@Entity
@Table(name = "VetService")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VetService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, columnDefinition = "NVARCHAR(255)")
    private String name;

    @Column(name = "description", columnDefinition = "NVARCHAR(255)")
    private String description;

    @Column(name = "price", nullable = false)
    private Float priceBase;

    @Enumerated(EnumType.STRING)
    @Column(name = "pet_type", nullable = false)
    private PetType petType;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

}
