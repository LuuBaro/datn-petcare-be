package org.example.petcarebe.model;
import jakarta.persistence.*;
import lombok.*;
import org.example.petcarebe.enums.AdjustmentType;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "slot_adjustments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SlotAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_time_slot_id", nullable = false)
    private DefaultTimeSlot defaultTimeSlot;

    @Enumerated(EnumType.STRING)
    @Column(name = "adjustment_type", nullable = false)
    private AdjustmentType adjustmentType; // Enum: ADD, REMOVE

    @Column(name = "slot_count", nullable = false)
    private int slotCount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "adjusted_by", nullable = false)
    private User adjustedBy; // Tham chiếu đến bảng users

}
