package org.example.petcarebe.model;

import jakarta.persistence.*;
import lombok.*;
import org.example.petcarebe.enums.SlotStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Data
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table (name = "slots")
public class Slot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long slotId;

    @Column(name = "slot_date", nullable = false)
    private LocalDate date;

    @Column(name = "slot_time", nullable = false)
    private LocalTime time;

    @Column(name = "slot_index", nullable = false)
    private int slotIndex;  // Ví dụ: 1, 2, 3, 4 cho các khung giờ trong ngày

    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "VARCHAR(50)", nullable = false)
    private SlotStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
