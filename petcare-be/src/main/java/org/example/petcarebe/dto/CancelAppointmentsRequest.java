package org.example.petcarebe.dto;

import java.util.List;

public class CancelAppointmentsRequest {
    private List<Long> appointmentIds;
    private String reason;
    private Object userId; // Có thể là Number hoặc String (email)

    // Getters và Setters
    public List<Long> getAppointmentIds() {
        return appointmentIds;
    }

    public void setAppointmentIds(List<Long> appointmentIds) {
        this.appointmentIds = appointmentIds;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Object getUserId() {
        return userId;
    }

    public void setUserId(Object userId) {
        this.userId = userId;
    }
}