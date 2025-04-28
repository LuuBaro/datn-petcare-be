package org.example.petcarebe.dto;

import lombok.Data;

import java.util.List;

@Data
public class CancelAppointmentsRequest {
    private List<Long> appointmentIds;
    private String reason;
}