package org.example.petcarebe.model;

import jakarta.persistence.*;
import lombok.*;
import org.example.petcarebe.enums.PetType;

@Entity
@Table(name = "pets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Pet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "weight_id", nullable = false)

    private PetWeight petWeight;

    @ManyToOne
    @JoinColumn(name = "service_id", nullable = false)
    private PetService petService;

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = true)
    private Employee employee;

    @ManyToOne
    @JoinColumn(name = "appointment_id", nullable = false)
    private Appointment appointment;

    @Enumerated(EnumType.STRING)
    @Column(name = "pet_type", nullable = false)
    private PetType petType;

    @Column(columnDefinition = "TEXT")
    private String note;

    private float price;


    @Column(name = "name_pet", columnDefinition = "NVARCHAR(255)")
    private String namePet;

    private float age;

    @Column(name = "phone_boss", columnDefinition = "NVARCHAR(255)")
    private String phoneBoss;

    @Column(name = "name_boss", columnDefinition = "NVARCHAR(255)")
    private String nameBoss;

}