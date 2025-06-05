package org.example.petcarebe.model;

import jakarta.persistence.*;
import lombok.*;
import org.example.petcarebe.enums.AppointmentStatus;
import org.example.petcarebe.enums.RefundMethod;

import java.time.LocalDate;
import java.time.ZoneId;
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

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AppointmentStatus status;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "time", nullable = false)
    private LocalTime time;

    @OneToMany(mappedBy = "appointment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Pet> pets = new ArrayList<>();

    @OneToMany(mappedBy = "appointment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AppointmentSlot> appointmentSlots = new ArrayList<>();

    @OneToMany(mappedBy = "appointment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Transaction> transactions = new ArrayList<>();

    @Column(name = "deposit_amount")
    private double depositAmount;

    @Column(name = "total_amount")
    private double totalAmount;

    @Column(name = "paid_amount")
    private double paidAmount;

    @Column(name = "cancel_reason", length = 255)
    private String cancelReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "refund_status")
    private String refundStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "refund_method")
    private RefundMethod refundMethod;

    @Column(name = "refund_note")
    private String refundNote;


    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
    }

    public void addPet(Pet pet) {
        pets.add(pet);
        pet.setAppointment(this);
        updateTotalAmount();
    }

    public void removePet(Pet pet) {
        pets.remove(pet);
        pet.setAppointment(null);
        updateTotalAmount();
    }

    public void updateTotalAmount() {
        this.totalAmount = pets.stream().mapToDouble(Pet::getPrice).sum();
    }

    public List<Transaction> getTransactions() {
        return this.transactions;
    }
}