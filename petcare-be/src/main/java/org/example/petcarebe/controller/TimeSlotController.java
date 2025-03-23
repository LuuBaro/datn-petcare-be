// TimeSlotController.java
package org.example.petcarebe.controller;

import org.example.petcarebe.dto.TimeSlotDTO;
import org.example.petcarebe.service.TimeSlotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/time-slots")
public class TimeSlotController {

    @Autowired
    private TimeSlotService timeSlotService;

    @GetMapping
    public ResponseEntity<Map<String, List<TimeSlotDTO>>> getTimeSlots(@RequestParam("date") String date) {
        LocalDate localDate = LocalDate.parse(date);
        Map<String, List<TimeSlotDTO>> timeSlots = timeSlotService.getTimeSlots(localDate);
        return ResponseEntity.ok(timeSlots);
    }
}