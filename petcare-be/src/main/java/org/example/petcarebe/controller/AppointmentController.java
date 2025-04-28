package org.example.petcarebe.controller;

import org.example.petcarebe.dto.AppointmentRequest;
import org.example.petcarebe.dto.AppointmentResponse;
import org.example.petcarebe.dto.CancelAppointmentsRequest;
import org.example.petcarebe.dto.PetResponse;
import org.example.petcarebe.model.Appointment;
import org.example.petcarebe.service.AppointmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    @Autowired
    private AppointmentService appointmentService;

    @PostMapping
    public ResponseEntity<?> createAppointment(@RequestBody AppointmentRequest request) {
        try {
            // Log the request for debugging
            System.out.println("Creating appointment with request: " + request.toString());
            
            // Ensure time is in HH:mm format
            if (request.getTime() != null && !request.getTime().contains(":")) {
                request.setTime(request.getTime() + ":00");
            }
            
            Appointment appointment = appointmentService.createAppointment(request);
            return ResponseEntity.ok(new AppointmentResponse(appointment.getAppointmentId(), "PAID", "Lịch hẹn đã được lưu"));
        } catch (Exception e) {
            e.printStackTrace(); // Log full error for debugging
            return ResponseEntity.badRequest().body(new AppointmentResponse(null, "FAILED", e.getMessage()));
        }
    }

    @GetMapping("/pending")
    public ResponseEntity<List<AppointmentResponse>> getPendingAppointments() {
        try {
            List<AppointmentResponse> pendingAppointments = appointmentService.getPendingAppointments();
            return ResponseEntity.ok(pendingAppointments);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/{appointmentId}/pets")
    public ResponseEntity<List<PetResponse>> getPetsByAppointmentId(@PathVariable Long appointmentId) {
        try {
            List<PetResponse> pets = appointmentService.getPetsByAppointmentId(appointmentId);
            return ResponseEntity.ok(pets);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PostMapping("/confirm")
    public ResponseEntity<?> confirmAppointments(@RequestBody List<Long> appointmentIds) {
        try {
            appointmentService.confirmAppointments(appointmentIds);
            return ResponseEntity.ok(new AppointmentResponse(null, "CONFIRMED", "Đã xác nhận các lịch hẹn"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new AppointmentResponse(null, "FAILED", e.getMessage()));
        }
    }

    @GetMapping("/confirmed-by-date-and-time")
    public ResponseEntity<List<AppointmentResponse>> getConfirmedAppointmentsByDateAndTime(
            @RequestParam("date") String date,
            @RequestParam("time") String time) {
        try {
            LocalDate localDate = LocalDate.parse(date);
            
            // More robust time parsing
            LocalTime localTime;
            try {
                // Try to parse time directly
                localTime = LocalTime.parse(time);
            } catch (Exception e) {
                // If that fails, try to add ":00" if needed
                if (!time.contains(":")) {
                    try {
                        localTime = LocalTime.parse(time + ":00");
                    } catch (Exception e2) {
                        // If that fails too, try with leading zero
                        if (time.length() == 1) {
                            localTime = LocalTime.parse("0" + time + ":00");
                        } else {
                            throw new IllegalArgumentException("Invalid time format: " + time);
                        }
                    }
                } else {
                    throw new IllegalArgumentException("Invalid time format: " + time);
                }
            }
            
            List<AppointmentResponse> appointments = appointmentService.getConfirmedAppointmentsByDateAndTime(localDate, localTime);
            return ResponseEntity.ok(appointments);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PutMapping("/cancel")
    public ResponseEntity<?> cancelAppointments(@RequestBody CancelAppointmentsRequest request) {
        try {
            List<Map<String, Object>> responses = appointmentService.cancelAppointments(request.getAppointmentIds(), request.getReason());
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new AppointmentResponse(null, "FAILED", e.getMessage()));
        }
    }

    @PutMapping("/{appointmentId}")
    public ResponseEntity<?> updateAppointment(@PathVariable Long appointmentId, @RequestBody AppointmentRequest request) {
        try {
            // Log the incoming request for debugging
            System.out.println("Updating appointment: " + appointmentId);
            System.out.println("Request: " + request.toString());
            
            // Add additional validation/formatting here if needed
            if (request.getTime() != null && !request.getTime().contains(":")) {
                request.setTime(request.getTime() + ":00");
            }
            
            Appointment updatedAppointment = appointmentService.updateAppointment(appointmentId, request);
            return ResponseEntity.ok(new AppointmentResponse(updatedAppointment.getAppointmentId(), "UPDATED", "Cập nhật lịch hẹn thành công"));
        } catch (Exception e) {
            e.printStackTrace(); // Log the full error for debugging
            return ResponseEntity.badRequest().body(new AppointmentResponse(null, "FAILED", e.getMessage()));
        }
    }
}