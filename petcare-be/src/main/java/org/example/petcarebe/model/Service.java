package org.example.petcarebe.model;

import jakarta.persistence.*;
import lombok.*;
import org.example.petcarebe.enums.PetType;

@Data
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table (name = "services")
public class Service {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String serviceName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private float basePrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PetType petType;
}
