package org.example.petcarebe.dto;

import lombok.Data;
import java.time.LocalTime;

@Data
public class TimeSlotDTO {
    private String hour;
    private LocalTime time;
    private int totalSlots;
    private int bookedSlots;
    private int availableSlots;
    private boolean active = true;
    private boolean morning; // Changed from isMorning to morning to follow Java bean convention

    public String getHour() {
        return hour;
    }

    public void setHour(String hour) {
        this.hour = hour;
        // Nếu time là null, convert từ hour
        if (this.time == null && hour != null) {
            try {
                this.time = LocalTime.parse(hour);
            } catch (Exception e) {
                // Ignore parsing errors
            }
        }
    }

    public void setTime(LocalTime time) {
        this.time = time;
        if (time != null) {
            this.hour = time.toString();
        }
    }

    public boolean isMorning() {
        return morning;
    }

    public void setMorning(boolean morning) {
        this.morning = morning;
    }
}