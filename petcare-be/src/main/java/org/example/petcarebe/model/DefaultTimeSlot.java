package org.example.petcarebe.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;
import java.util.logging.Logger;

@Entity
@Table(name = "default_time_slots")
@Getter
@Setter
@NoArgsConstructor
public class DefaultTimeSlot {
    private static final Logger logger = Logger.getLogger(DefaultTimeSlot.class.getName());

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "time", nullable = false)
    private LocalTime time;

    @Column(name = "slot_index", nullable = false)
    private int slotIndex;

    @Column(name = "default_slot_count", nullable = false)
    private int defaultSlotCount;

    @Column(name = "total_slots", nullable = false)
    private int totalSlots = 0;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "is_morning", nullable = false)
    private boolean isMorning;

    public DefaultTimeSlot(Long id, LocalTime time, int slotIndex, int defaultSlotCount, boolean isActive, boolean isMorning) {
        this.id = id;
        this.time = time;
        this.slotIndex = slotIndex;
        this.defaultSlotCount = defaultSlotCount;
        this.totalSlots = defaultSlotCount;
        this.isActive = isActive;
        this.isMorning = isMorning;
    }

    public int getDefaultSlotCount() {
        logger.info("Truy xuất defaultSlotCount cho time=" + time + ", giá trị=" + defaultSlotCount);
        return defaultSlotCount;
    }
}