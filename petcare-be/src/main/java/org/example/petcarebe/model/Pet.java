package org.example.petcarebe.model;
import jakarta.persistence.*;
import lombok.*;
import org.example.petcarebe.enums.PetType;
import org.example.petcarebe.model.*;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "pets")
public class Pet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Khóa chính


    @ManyToOne
    @JoinColumn(name = "weight_id", nullable = false)
    private PetWeight petWeight; // FK đến bảng cân nặng

    @ManyToOne
    @JoinColumn(name = "service_id", nullable = false)
    private Service service; // FK đến bảng dịch vụ

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = true)
    private Employee employee; // FK đến bảng nhân viên (nếu có)

    @ManyToOne
    @JoinColumn(name = "slot_id", nullable = false)
    private Slot slot; // FK đến bảng slot thời gian

    @Enumerated(EnumType.STRING)
    @Column(name = "pet_type", nullable = false)
    private PetType petType; // Enum Chó/Mèo

    @Column(columnDefinition = "TEXT")
    private String note; // Ghi chú thêm

    private float price; // Giá dịch vụ sau khi tính theo cân nặng
}
