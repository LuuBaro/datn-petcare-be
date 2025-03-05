package org.example.petcarebe.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
    @JsonIgnore
    @ToString.Exclude
    private Orders orders;

    @ManyToOne
    @JoinColumn(name = "product_detail_id", nullable = true)
    @JsonIgnore
    @ToString.Exclude
    private ProductDetails productDetails;

    public void setProductDetail(ProductDetails productDetail) {

    }
}
