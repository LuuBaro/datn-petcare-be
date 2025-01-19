package org.example.petcarebe.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.validator.constraints.URL;

import java.util.List;

@Data
@Entity
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Products {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productId;

    @Column(name = "product_name", columnDefinition = "nvarchar(255)")
    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(min = 3, message = "Tên sản phẩm phải có ít nhất 3 ký tự")
    @Pattern(regexp = "^[a-zA-Z0-9\\p{L}\\p{Z}.,!?;:\"()\\-]+$",
            message = "Tên sản phẩm không được chứa ký tự đặc biệt")
    private String productName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @URL(message = "URL hình ảnh không hợp lệ")
    private String image;

    @ManyToOne
    @JoinColumn(name = "categorie_id", nullable = false)
    private Categories categories;

    @ManyToOne
    @JoinColumn(name = "brand_id", nullable = false)
    private Brand brand;

    @OneToMany(mappedBy = "products")
    private List<ProductDetails> productDetails;




}
