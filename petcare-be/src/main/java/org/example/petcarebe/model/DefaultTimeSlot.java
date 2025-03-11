package org.example.petcarebe.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalTime;

@Entity
@Table(name = "default_time_slots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DefaultTimeSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Khóa chính

    @Column(name = "time", nullable = false)
    private LocalTime time; // Thời gian slot (VD: 09:00, 10:00, ...)

    @Column(name = "slot_index", nullable = false)
    private int slotIndex; // Thứ tự của slot trong ngày (VD: 1, 2, 3,...)

    @Column(name = "default_slot_count", nullable = false)
    private int defaultSlotCount; // Số lượng slot mặc định cho khung giờ này

    @Column(name = "is_active", nullable = false)
    private boolean isActive; // true: đang sử dụng, false: ngừng hoạt động
}

