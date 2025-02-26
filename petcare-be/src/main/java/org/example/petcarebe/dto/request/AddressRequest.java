package org.example.petcarebe.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AddressRequest {
    @NotNull(message = "User ID không được để trống")
    private Long userId;

    @NotBlank(message = "Tỉnh/Thành phố không được để trống")
    private String province;

    @NotBlank(message = "Quận/Huyện không được để trống")
    private String district;

    @NotBlank(message = "Xã/Phường không được để trống")
    private String ward;

    @NotBlank(message = "Đường không được để trống")
    private String street;
}

