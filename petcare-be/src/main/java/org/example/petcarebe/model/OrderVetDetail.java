package org.example.petcarebe.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "OrderVetDetail")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderVetDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Orders orderId;

    @ManyToOne
    @JoinColumn(name = "medical_record_id")
    private MedicalRecord MedicalRecord;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "price", nullable = false)
    private Float price;
}
