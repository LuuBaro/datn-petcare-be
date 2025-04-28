package org.example.petcarebe.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VetOrderDTO {
    private Long id;
    private Long userId; // ID của người dùng (User)
    private Date orderDate; // Ngày đặt hàng
    private String paymentMethod; // Phương thức thanh toán (CASH, MOMO, v.v.)
    private String paymentStatus; // Trạng thái thanh toán (Đã thanh toán, Thất bại)
    private String type; // Loại đơn hàng (VET_SERVICE)
    private Float totalAmount; // Tổng tiền
    private List<OrderVetDetailDTO> orderVetDetails; // Danh sách chi tiết đơn hàng
}