package org.example.petcarebe.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddressRequest {
    private Long userId;
    @NotBlank(message = "Thành phố không được trống")
    private String province;

    @NotBlank(message = "Quận/Huyện không được trống")
    private String district;

    @NotBlank(message = "Phường/Xã không được trống")
    private String ward;

    @NotBlank(message = "Đường không được trống")
    private String street;
}
