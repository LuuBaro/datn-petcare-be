package org.example.petcarebe.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "booking_enables")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookingEnabled {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Khóa chính

    @Column(name = "setting_name", nullable = false, length = 255)
    private String settingName; // Tên cài đặt (ví dụ: "ENABLE_BOOKING")

    @Column(name = "setting_value", nullable = false)
    private boolean settingValue; // true: bật, false: tắt

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt; // Thời gian cập nhật

    @ManyToOne
    @JoinColumn(name = "updated_by", nullable = false)
    private User user;
}
