package org.example.petcarebe.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
public class ProductColors {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productColorId;

    @Column(name = "color_value", columnDefinition = "nvarchar(255)")
    @NotBlank(message = "Màu sắc không được để trống")
    @Size(min = 1, max = 255, message = "Màu sắc phải có từ 1 đến 255 ký tự")
    private String colorValue;
    private Boolean status;
}
