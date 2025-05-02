package org.example.petcarebe.dto;

import lombok.Data;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Data
public class TimeSlotDTO {

    private String hour;

    @JsonIgnore
    private LocalTime time;
    
    private int totalSlots;
    private int bookedSlots;
    private int availableSlots;
    private boolean active = true;

    @JsonProperty("isMorning")
    private boolean morning;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public String getHour() {
        return hour;
    }

    public void setHour(String hour) {
        this.hour = hour;

    }

    public LocalTime getTime() {
        return time;
    }

    public void setTime(LocalTime time) {
        this.time = time;

        if (time != null) {
            this.hour = time.format(TIME_FORMATTER);
        }
    }

    public boolean isMorning() {
        return morning;
    }

    public void setMorning(boolean morning) {
        this.morning = morning;
    }
}