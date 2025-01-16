package org.example.petcarebe.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class Weights {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long weightId;

    @NotNull(message = "Giá trị trọng lượng không được để trống")

    private float weightValue;
    private Boolean status;
}
