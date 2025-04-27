package org.example.petcarebe.enums;

public enum AppointmentStatus {
    PENDING,      // Chưa thanh toán
    PAID,         // Đã thanh toán (cọc hoặc toàn bộ)
    CONFIRMED,    // Đã xác nhận
    IN_PROGRESS,  // Đang thực hiện
    COMPLETED,    // Hoàn thành
    CANCELLED     // Đã hủy
}