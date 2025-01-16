package org.example.petcarebe.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Entity
public class ProductSizes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productSizeId;
    @Column(name = "size_value", columnDefinition = "nvarchar(255)")
    @NotBlank(message = "Kích thước không được để trống")
    @Size(min = 1, max = 255, message = "Kích thước phải có từ 1 đến 255 ký tự")
    private String sizeValue;
    private Boolean status;
}
