package org.example.petcarebe.dto;

import lombok.Data;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Data
public class TimeSlotDTO {
    // Chuỗi thời gian với định dạng "HH:mm" cho frontend
    private String hour;
    
    // Đối tượng LocalTime cho backend, không gửi đến frontend
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
        // Không tự động set time từ hour nữa
    }

    public LocalTime getTime() {
        return time;
    }

    public void setTime(LocalTime time) {
        this.time = time;
        // Nếu time được set, cập nhật hour theo định dạng chuẩn
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