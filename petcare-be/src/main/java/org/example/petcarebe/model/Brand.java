package org.example.petcarebe.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Entity
@Table
public class Brand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long brandId;

    @Column(name = "brandName", columnDefinition = "NVARCHAR(255)")
    @NotBlank(message = "Tên thương hiệu không được để trống")
    @Size(min = 3, message = "Tên thương hiệu phải có ít nhất 3 ký tự")
    @Pattern(regexp = "^[a-zA-Z0-9\\p{L}\\p{Z}.,!?;:\"()\\-]+$",
            message = "Tên thương hiệu không được chứa ký tự đặc biệt")
    private String brandName;
    private Boolean status;
}
