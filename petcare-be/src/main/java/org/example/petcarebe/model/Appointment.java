package org.example.petcarebe.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table (name = "appointments")
public class Appointment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long appointmentId;

    @Column(name = "customer_name", columnDefinition = "NVARCHAR(255)")
    private String customerName;
    private String phone;


    private float depositAmount;

    @Column(name = "status", columnDefinition = "NVARCHAR(255)")
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Column(name = "date", nullable = false, updatable = false)
    private LocalDate date;

    @Column(name = "time", nullable = false, updatable = false)
    private LocalTime time;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Orders order;

    @ManyToOne
    @JoinColumn(name = "staff_id", nullable = false)
    private User user;

}
