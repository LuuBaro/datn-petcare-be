package org.example.petcarebe.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@Entity
@Table(name = "vouchers")
public class Voucher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long voucherId;

    @NotBlank(message = "Tên voucher không được để trống")
    private String name;

    @Temporal(TemporalType.DATE)
    @NotNull(message = "Ngày bắt đầu không được để trống")
    private Date startDate;

    @Temporal(TemporalType.DATE)
    @NotNull(message = "Ngày kết thúc không được để trống")
    @Future(message = "Ngày kết thúc phải lớn hơn thời điểm hiện tại")
    private Date endDate;

    @Min(value = 0, message = "Số lượng không được nhỏ hơn 0")
    @Max(value = Integer.MAX_VALUE, message = "Số lượng phải là số hợp lệ")
    private int quantity;

    @DecimalMax(value = "70.0", message = "Giảm giá không được vượt quá 70%")
    @DecimalMin(value = "0.0", message = "Giảm giá không được nhỏ hơn 0%")
    private double percents;

    @PositiveOrZero(message = "Điều kiện không được nhỏ hơn 0")
    @Digits(integer = 10, fraction = 2, message = "Điều kiện phải là số hợp lệ")
    @Column(name = "condition_value") // Đổi tên cột
    private BigDecimal condition;

    @AssertTrue(message = "Ngày bắt đầu phải nhỏ hơn ngày kết thúc")
    public boolean isValidDateRange() {
        if (startDate == null || endDate == null) {
            return false;
        }
        return startDate.before(endDate);
    }
}
