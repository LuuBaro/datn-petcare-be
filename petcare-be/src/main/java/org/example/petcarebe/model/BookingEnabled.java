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
    private Long id;

    @Column(name = "setting_name", nullable = false, length = 255)
    private String settingName;

    @Column(name = "setting_value", nullable = false)
    private boolean settingValue;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.EAGER) // Thay đổi từ LAZY sang EAGER
    @JoinColumn(name = "updated_by", nullable = false)
    private User user;
}