package org.example.petcarebe.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentRequest {
    private String date;
    private String time;
    private String customerName;
    private String phone;
    private float depositAmount;
    private double totalAmount;
    private List<PetRequest> pets;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PetRequest {
        private String name;
        private String petType;
        private Long petServiceId;
        private Long petWeightId;
        private String note;
        private float price;
    }
}