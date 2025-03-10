package org.example.petcarebe.dto;

import lombok.Data;

@Data
public class PointInfoDTO {
    private String name;
    private int totalPoints;

    public PointInfoDTO(String name, int totalPoints) {
        this.name = name;
        this.totalPoints = totalPoints;
    }
}