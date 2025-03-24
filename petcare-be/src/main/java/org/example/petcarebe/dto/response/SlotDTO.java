package org.example.petcarebe.dto.response;


import lombok.Data;
import org.example.petcarebe.enums.SlotStatus;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class SlotDTO {
    private Long slotId;
    private LocalDate date;
    private LocalTime time;
    private int slotIndex;
    private SlotStatus status;
}
