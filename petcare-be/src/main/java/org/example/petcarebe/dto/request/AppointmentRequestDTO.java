// AppointmentRequestDTO.java
package org.example.petcarebe.dto.request;

import lombok.Data;
import org.example.petcarebe.enums.PetType;
import org.example.petcarebe.model.PetService;
import org.example.petcarebe.model.PetWeight;

import java.util.List;

@Data
public class AppointmentRequestDTO {
    private String date;
    private String time;
    private String customerName;
    private String phone;
    private float depositAmount;
    private List<PetDTO> pets;

    @Data
    public static class PetDTO {
        private PetType petType;
        private PetService petService;
        private PetWeight petWeight;
        private String note;
        private float price;
    }
}