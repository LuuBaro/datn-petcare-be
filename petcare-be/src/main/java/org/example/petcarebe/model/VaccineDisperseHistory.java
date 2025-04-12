package org.example.petcarebe.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "VaccineDisperseHistory")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VaccineDisperseHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "vaccine_id")
    private Vaccine vaccine;

    @ManyToOne
    @JoinColumn(name = "User_id")
    private User userId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "cancellation_Date")
    private LocalDate cancellationDate;

    @Column(name = "reason", columnDefinition = "NVARCHAR(255)")
    private String reason;

    @Column(name = "note", columnDefinition = "NVARCHAR(255)")
    private String note;
}
