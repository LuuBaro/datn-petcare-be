package org.example.petcarebe.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "appointments")
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long appointmentId;

    @Column(name = "customer_name", columnDefinition = "NVARCHAR(255)")
    private String customerName;

    private String phone;

    private String status;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "time", nullable = false)
    private LocalTime time;

    @OneToMany(mappedBy = "appointment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Pet> pets = new ArrayList<>();

    private float depositAmount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @ManyToOne
    @JoinColumn(name = "staff_id", nullable = false)
    private User user;


    public void addPet(Pet pet) {
        pets.add(pet);
        pet.setAppointment(this);
    }
}