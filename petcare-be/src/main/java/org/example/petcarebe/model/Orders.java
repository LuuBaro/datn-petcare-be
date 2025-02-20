package org.example.petcarebe.model;

import jakarta.persistence.Entity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Orders {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;

    @Temporal(TemporalType.DATE)
    private Date orderDate = new Date();

    @Column(name = "payment_status", columnDefinition = "NVARCHAR(255)")
    @NotBlank(message = "Trạng thái thanh toán không được để trống")
    private String paymentStatus;


    @NotBlank(message = "Phương thức thanh toán không được để trống")
    private String paymentMethod;

    @NotBlank(message = "Địa chỉ giao hàng không được để trống")
    private String shippingAddress;

    @Min(value = 0, message = "Phí giao hàng không hợp lệ")
    private float shippingCost;

    @Min(value = 1, message = "Tổng tiền phải lớn hơn 0")
    private float totalAmount;

    @Column(name = "type", columnDefinition = "NVARCHAR(255)")
    private String type;

    private int pointEarned;
    private int pointUsed;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "status_id")
    private StatusOrder statusOrder;

    @ManyToOne
    @JoinColumn(name = "voucher_id")
    private Voucher voucher;

    @ManyToOne
    @JoinColumn(name = "point_id")
    private Point point;

    @OneToMany(mappedBy = "orders", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderDetails> orderDetails;



}
