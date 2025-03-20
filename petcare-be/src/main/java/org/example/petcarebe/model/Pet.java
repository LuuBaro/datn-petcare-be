package org.example.petcarebe.model;

import jakarta.persistence.*;
import lombok.*;

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
    private PetWeight weight;

    private String namePet;
    private float age;
    private String phoneBoss;
    private String nameBoss;

    @ManyToOne
    @JoinColumn(name = "service_id", nullable = false)
    private Service service;

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee; // FK - Nhân viên thực hiện

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}