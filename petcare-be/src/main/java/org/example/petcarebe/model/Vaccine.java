package org.example.petcarebe.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "Vaccine")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Vaccine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "origin", length = 255)
    private String origin;

    @Column(name = "manufacturing_Date")
    private LocalDate manufacturingDate;

    @Column(name = "expiry_Date")
    private LocalDate expiryDate;

    @Column(name = "entry_Date")
    private LocalDate entryDate;

    @Column(name = "type", nullable = false, columnDefinition = "NVARCHAR(50)")
    private String type;

    @Column(name = "status", nullable = false)
    private Boolean status = true;

    @Column(name = "selling_Price")
    private Float sellingPrice;

    @Column(name = "import_Price")
    private Float importPrice;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "note", columnDefinition = "NVARCHAR(255)")
    private String note;


}
