package org.example.petcarebe.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.Set;

@Data
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;
    private String email;
    private String password;

    @Column(name = "fullName", columnDefinition = "NVARCHAR(255)")
    private String fullName;

    private String phone;
    private LocalDate registration_date;
    private double totalSpent;
    @JsonProperty("isStatus") // Ép buộc tên trường JSON là "isStatus"
    private boolean isStatus;
    private String imageUrl;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "userId"),
            inverseJoinColumns = @JoinColumn(name = "roleId")
    )
    private Set<Role> userRoles; // Hoặc List<Role>

}
