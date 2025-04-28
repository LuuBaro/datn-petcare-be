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
    private double depositAmount; 
    private double totalAmount;   
    private double paidAmount;    
    private String paymentType;
    private List<PetRequest> pets;
    private String paymentMethod;
    private String paymentChannel;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PetRequest {
        private String name;
        private String petType;
        private Long petServiceId;
        private Long petWeightId;
        private String note;
        private double price; // Chuyển sang double
    }
}