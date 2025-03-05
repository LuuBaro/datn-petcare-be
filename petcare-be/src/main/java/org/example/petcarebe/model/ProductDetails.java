package org.example.petcarebe.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Data
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class ProductDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productDetailId;

    @Min(value = 1, message = "Số lượng phải lớn hơn 0")
    private int quantity;

    @Min(value = 1, message = "Giá phải lớn hơn 0")
    private float price;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Products products;

    @ManyToOne
    @JoinColumn(name = "weight_id", nullable = false)
    private Weights weights;

    @ManyToOne
    @JoinColumn(name = "product_size_id", nullable = false)
    private ProductSizes productSizes;

    @ManyToOne
    @JoinColumn(name = "product_color_id", nullable = false)
    private ProductColors productColors;


}
