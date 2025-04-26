package org.example.petcarebe.controller;

import org.example.petcarebe.dto.AppointmentRequest;
import org.example.petcarebe.dto.AppointmentResponse;
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
            Appointment appointment = appointmentService.createAppointment(request);
            return ResponseEntity.ok(new AppointmentResponse(appointment.getAppointmentId(), "PAID", "Lịch hẹn đã được lưu"));
        } catch (Exception e) {
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
            LocalTime localTime = LocalTime.parse(time);
            List<AppointmentResponse> appointments = appointmentService.getConfirmedAppointmentsByDateAndTime(localDate, localTime);
            return ResponseEntity.ok(appointments);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }
    @PutMapping("/cancel")
    public ResponseEntity<?> cancelAppointments(@RequestBody Map<String, Object> request) {
        try {
            List<Long> appointmentIds = (List<Long>) request.get("appointmentIds");
            String reason = (String) request.get("reason");
            List<Map<String, Object>> responses = appointmentService.cancelAppointments(appointmentIds, reason);
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new AppointmentResponse(null, "FAILED", e.getMessage()));
        }
    }
}