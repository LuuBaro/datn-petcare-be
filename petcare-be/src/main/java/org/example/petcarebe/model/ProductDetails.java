package org.example.petcarebe.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

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

    @NotBlank(message = "Số lượng không được để trống")
    @Min(value = 1, message = "Số lượng phải lớn hơn 0")
    private int quantity;

    @NotBlank(message = "Giá không được để trống")
    @Min(value = 1, message = "Giá phải lớn hơn 0")
    @Pattern(regexp = "^[0-9]*\\.?[0-9]+$", message = "Giá phải là số hợp lệ")
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

//    @OneToMany(mappedBy = "productDetails")  // This should be a one-to-many relationship
//    private List<ProductImages> productImages;
}
