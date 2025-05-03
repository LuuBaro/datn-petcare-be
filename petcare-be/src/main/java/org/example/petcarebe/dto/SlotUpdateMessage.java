package org.example.petcarebe.dto;

import lombok.Data;

@Data
public class SlotUpdateMessage {
    private String date;
    private String time;
    private int bookedSlots;

    public SlotUpdateMessage(String date, String time, int bookedSlots) {
        this.date = date;
        this.time = time;
        this.bookedSlots = bookedSlots;
    }
}