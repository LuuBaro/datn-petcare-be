package org.example.petcarebe.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.Date;

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
    private int paymentStatus;
    private String paymentMethod;
    private String shippingAddress;
    private float shippingCost;
    private float totalAmount;
    private int type;
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

}
