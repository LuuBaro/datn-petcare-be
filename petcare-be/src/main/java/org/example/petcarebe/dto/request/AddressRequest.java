package org.example.petcarebe.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddressRequest {
    private Long userId;
    private String province;

    private String district;

    private String ward;

    private String street;
}
