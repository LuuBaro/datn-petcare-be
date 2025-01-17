package org.example.petcarebe.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

@Data
@Entity
public class ProductImages {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productImageId;
    @URL(message = "Url không hợp lệ")
    private String imageUrl;

    @ManyToOne
    @JoinColumn(name = "productDetail_Id", nullable = false)
    private ProductDetails productDetails;

}
