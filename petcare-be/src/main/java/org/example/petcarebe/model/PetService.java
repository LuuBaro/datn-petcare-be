package org.example.petcarebe.model;

import jakarta.persistence.*;
import lombok.*;
import org.example.petcarebe.enums.PetType;
import org.example.petcarebe.enums.StatusType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;

@Data
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "pet_services")
public class PetService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Tên dịch vụ không được để trống")
    @Column(nullable = false, length = 255)
    private String serviceName;

    @Column(length = 255)
    private String description;

    @NotNull(message = "Giá cơ bản không được để trống")
    @Min(value = 0, message = "Giá cơ bản phải lớn hơn hoặc bằng 0")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @NotNull(message = "Loại thú cưng không được để trống")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PetType petType;

    @NotNull(message = "Trạng thái không được để trống")
    @Enumerated(EnumType.STRING)
    @Column(name = "status_type", nullable = false)
    private StatusType statusType;
}