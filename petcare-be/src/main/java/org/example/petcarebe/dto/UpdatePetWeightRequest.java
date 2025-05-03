package org.example.petcarebe.dto;

public class UpdatePetWeightRequest {
    private Long petWeightId;
    private double price;
    private Long appointmentId;
    private String reason;
    private Long userId;

    public Long getPetWeightId() {
        return petWeightId;
    }

    public void setPetWeightId(Long petWeightId) {
        this.petWeightId = petWeightId;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public Long getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(Long appointmentId) {
        this.appointmentId = appointmentId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}