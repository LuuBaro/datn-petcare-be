package org.example.petcarebe.model;

import jakarta.persistence.*;
import lombok.*;

@Data
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Point {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long pointId;
    private int totalPoint;
    private String name;
    private String phone;


    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
