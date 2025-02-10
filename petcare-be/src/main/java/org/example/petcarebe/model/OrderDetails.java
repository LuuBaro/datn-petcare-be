package org.example.petcarebe.model;

import jakarta.persistence.*;
import lombok.*;

@Data
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class OrderDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderDetailsId;
    private int quantity;
    private float price;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = true)
    private Orders orders;

    @ManyToOne
    @JoinColumn(name = "product_detail_id", nullable = true)
    private ProductDetails productDetails;

    public void setProductDetail(ProductDetails productDetail) {

    }
}
