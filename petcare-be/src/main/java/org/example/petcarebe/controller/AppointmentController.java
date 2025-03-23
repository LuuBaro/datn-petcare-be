// AppointmentController.java
package org.example.petcarebe.controller;

import org.example.petcarebe.dto.request.AppointmentRequestDTO;
import org.example.petcarebe.enums.AppointmentStatus;
import org.example.petcarebe.model.Appointment;
import org.example.petcarebe.service.AppointmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    // Sử dụng constructor injection
    @Autowired
    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping
    public ResponseEntity<Appointment> createAppointment(@RequestBody AppointmentRequestDTO request) {
        Appointment appointment = appointmentService.createAppointment(request);
        return ResponseEntity.ok(appointment);
    }

    @PutMapping("/{id}/pay")
    public ResponseEntity<Appointment> updatePayment(@PathVariable Long id, @RequestParam float depositAmount) {
        Appointment appointment = appointmentService.updatePayment(id, depositAmount);
        return ResponseEntity.ok(appointment);
    }

    @PutMapping("/{id}/confirm")
    public ResponseEntity<Appointment> confirmAppointment(@PathVariable Long id, @RequestParam Long staffId) {
        Appointment appointment = appointmentService.confirmAppointment(id, staffId);
        return ResponseEntity.ok(appointment);
    }

    @PutMapping("/{id}/update-status")
    public ResponseEntity<Appointment> updateStatus(@PathVariable Long id, @RequestParam AppointmentStatus status) {
        Appointment appointment = appointmentService.updateStatus(id, status);
        return ResponseEntity.ok(appointment);
    }
}