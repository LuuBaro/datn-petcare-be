package org.example.petcarebe.dto;

import lombok.Data;

@Data
public class NewAppointmentMessage {
    private Long appointmentId;
    private String customerName;
    private String date;
    private String time;
    private float paidAmount;

    public NewAppointmentMessage(Long appointmentId, String customerName, String date, String time, float paidAmount) {
        this.appointmentId = appointmentId;
        this.customerName = customerName;
        this.date = date;
        this.time = time;
        this.paidAmount = paidAmount;
    }
}