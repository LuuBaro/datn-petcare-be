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

    @Column(name = "time", nullable = false)
    private LocalTime time;

    @Enumerated(EnumType.STRING)
    @Column(name = "adjustment_type", nullable = false)
    private AdjustmentType adjustmentType;

    @Column(name = "slot_count", nullable = false)
    private int slotCount;

    @Column(name = "is_permanent", nullable = false)
    private boolean isPermanent;
}
