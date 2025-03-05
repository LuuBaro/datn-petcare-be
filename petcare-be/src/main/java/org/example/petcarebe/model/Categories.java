package org.example.petcarebe.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Categories {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long categoryId;
    @Column(name = "Category_name", columnDefinition = "nvarchar(255)")
    @NotBlank(message = "Tên danh mục không được để trống")
    @Size(min = 3, message = "Tên danh mục phải có ít nhất 3 ký tự")
    @Pattern(regexp = "^[a-zA-Z0-9\\p{L}\\p{Z}.,!?;:\"()\\-]+$",
            message = "Tên danh mục không được chứa ký tự đặc biệt")
    private String categoryName;
    private Boolean status;

}
