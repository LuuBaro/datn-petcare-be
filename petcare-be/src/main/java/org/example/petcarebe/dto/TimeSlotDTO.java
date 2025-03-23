// TimeSlotDTO.java
package org.example.petcarebe.dto;

import lombok.Data;

@Data
public class TimeSlotDTO {
    private String hour;
    private int totalSlots;
    private int bookedSlots;
}