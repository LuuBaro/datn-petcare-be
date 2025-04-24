package org.example.petcarebe.controller;

import org.example.petcarebe.dto.TimeSlotDTO;
import org.example.petcarebe.service.TimeSlotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/time-slots")
public class TimeSlotController {

    @Autowired
    private TimeSlotService timeSlotService;

    @GetMapping
    public ResponseEntity<Map<String, List<TimeSlotDTO>>> getTimeSlotsForDate(@RequestParam("date") String date) {
        try {
            LocalDate localDate = LocalDate.parse(date);
            Map<String, List<TimeSlotDTO>> timeSlots = timeSlotService.getTimeSlotsForDate(localDate);
            return ResponseEntity.ok(timeSlots);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }
}