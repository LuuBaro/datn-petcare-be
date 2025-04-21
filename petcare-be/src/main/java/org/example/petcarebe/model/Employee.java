package org.example.petcarebe.model;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table (name = "employees")
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long employeeId;

    @Column(name = "full_name", columnDefinition = "NVARCHAR(255)", nullable = false)
    private String fullName;

    @Column(name = "phone", columnDefinition = "VARCHAR(20)", unique = true, nullable = false)
    private String phone;

    @Column(name = "employee_type", columnDefinition = "NVARCHAR(255)", nullable = false)
    private String employeeType;

    @Column(name = "status", columnDefinition = "VARCHAR(50)", nullable = false)
    private String status;

}
