package org.example.petcarebe.controller;

import org.example.petcarebe.dto.request.BookingRequestDTO;
import org.example.petcarebe.dto.response.BookingResponseDTO;
import org.example.petcarebe.dto.TimeSlotDTO;
import org.example.petcarebe.service.SlotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/timeslots")
public class TimeSlotController {

    @Autowired
    private SlotService slotService;

    @GetMapping
    public ResponseEntity<Map<String, List<TimeSlotDTO>>> getTimeSlots(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            Map<String, List<TimeSlotDTO>> timeSlots = slotService.getTimeSlotsByDate(date);
            return ResponseEntity.ok(timeSlots);
        } catch (Exception e) {
            System.err.println("Error in getTimeSlots: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }

    @PostMapping("/book")
    public ResponseEntity<BookingResponseDTO> bookAppointment(@RequestBody BookingRequestDTO request) {
        try {
            BookingResponseDTO response = slotService.bookSlot(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error in bookAppointment: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }
}