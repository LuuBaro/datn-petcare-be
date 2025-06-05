package org.example.petcarebe.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "appointment_slots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "time", nullable = false)
    private LocalTime time;

    @Column(name = "total_slots", nullable = false)
    private Integer totalSlots = 0;

    @Column(name = "booked_slots", nullable = false)
    private Integer bookedSlots = 0;

    @Column(name = "available_slots", nullable = false)
    private Integer availableSlots = 0;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_time_slot_id")
    private DefaultTimeSlot defaultTimeSlot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", unique = true)
    private Appointment appointment;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "date", referencedColumnName = "date"),
            @JoinColumn(name = "time", referencedColumnName = "time")
    })
    private List<SlotAdjustment> slotAdjustments = new ArrayList<>(); // Thêm mối quan hệ
}