package org.example.petcarebe.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.Date;

@Data
@Setter
@Getter
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
    private int type;
    private int pointEarned;
    private int pointUsed;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "status_id", nullable = false)
    private StatusOrder statusOrder;

    @ManyToOne
    @JoinColumn(name = "voucher_id", nullable = false)
    private Voucher voucher;

    @ManyToOne
    @JoinColumn(name = "point_id", nullable = false)
    private Point point;
}
