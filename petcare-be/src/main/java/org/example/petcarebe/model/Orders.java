package org.example.petcarebe.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Orders {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;

    @Temporal(TemporalType.DATE)
    private Date orderDate;
    @Column(name = "payment_status", columnDefinition = "NVARCHAR(255)")
    private String paymentStatus;
    private String paymentMethod;
    private String shippingAddress;
    private float shippingCost;
    private float totalAmount;

    @Column(name = "type", columnDefinition = "NVARCHAR(255)")
    private String type;
    private int pointEarned;
    private int pointUsed;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "status_id", nullable = true)
    private StatusOrder statusOrder;

    @ManyToOne
    @JoinColumn(name = "voucher_id", nullable = true)
    private Voucher voucher;

    @ManyToOne
    @JoinColumn(name = "point_id", nullable = true)
    private Point point;

    // ✅ Thêm quan hệ One-to-Many với OrderDetails
    @OneToMany(mappedBy = "orders", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderDetails> orderDetails;

}
