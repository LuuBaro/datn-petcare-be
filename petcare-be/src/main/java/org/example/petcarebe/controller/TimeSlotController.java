package org.example.petcarebe.controller;

import org.example.petcarebe.dto.TimeSlotDTO;
import org.example.petcarebe.enums.AppointmentStatus;
import org.example.petcarebe.model.Appointment;
import org.example.petcarebe.model.AppointmentSlot;
import org.example.petcarebe.model.DefaultTimeSlot;
import org.example.petcarebe.repository.AppointmentRepository;
import org.example.petcarebe.repository.AppointmentSlotRepository;
import org.example.petcarebe.repository.DefaultTimeSlotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/time-slots")
public class TimeSlotController {

    @Autowired
    private AppointmentSlotRepository appointmentSlotRepository;

    @Autowired
    private DefaultTimeSlotRepository defaultTimeSlotRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @GetMapping
    public ResponseEntity<?> getAvailableSlots(@RequestParam("date") String date) {
        try {
            LocalDate localDate = LocalDate.parse(date);
            return ResponseEntity.ok(getSlotsForDate(localDate, true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/confirmed")
    public ResponseEntity<?> getConfirmedSlots(@RequestParam("date") String date) {
        try {
            LocalDate localDate = LocalDate.parse(date);
            return ResponseEntity.ok(getSlotsForDate(localDate, false));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    private Map<String, List<TimeSlotDTO>> getSlotsForDate(LocalDate date, boolean includePaid) {
        List<DefaultTimeSlot> defaultSlots = defaultTimeSlotRepository.findAll();
        List<TimeSlotDTO> morningSlots = new ArrayList<>();
        List<TimeSlotDTO> afternoonSlots = new ArrayList<>();

        for (DefaultTimeSlot defaultSlot : defaultSlots) {
            LocalTime time = defaultSlot.getTime();
            AppointmentSlot slot = appointmentSlotRepository.findByDateAndTime(date, time)
                    .orElseGet(() -> {
                        AppointmentSlot newSlot = new AppointmentSlot();
                        newSlot.setDate(date);
                        newSlot.setTime(time);
                        newSlot.setTotalSlots(defaultSlot.getTotalSlots());
                        newSlot.setBookedSlots(0);
                        newSlot.setAvailableSlots(defaultSlot.getTotalSlots());
                        newSlot.setIsActive(true);
                        newSlot.setDefaultTimeSlot(defaultSlot);
                        return newSlot;
                    });

            // Tính bookedSlots
            List<AppointmentStatus> statuses = includePaid
                    ? List.of(AppointmentStatus.PAID, AppointmentStatus.CONFIRMED)
                    : List.of(AppointmentStatus.CONFIRMED);
            int bookedSlots = 0;
            for (AppointmentStatus status : statuses) {
                List<Appointment> appointments = appointmentRepository.findByDateAndTimeAndStatus(date, time, status);
                bookedSlots += appointments.stream()
                        .mapToInt(appointment -> appointment.getPets().size())
                        .sum();
            }

            slot.setBookedSlots(bookedSlots);
            slot.setAvailableSlots(slot.getTotalSlots() - bookedSlots);

            TimeSlotDTO slotDTO = new TimeSlotDTO();
            slotDTO.setHour(time.toString());
            slotDTO.setTime(time);
            slotDTO.setTotalSlots(slot.getTotalSlots());
            slotDTO.setBookedSlots(slot.getBookedSlots());
            slotDTO.setAvailableSlots(slot.getAvailableSlots());
            slotDTO.setActive(slot.getIsActive());
            slotDTO.setMorning(time.isBefore(LocalTime.of(12, 0)));

            if (time.isBefore(LocalTime.of(12, 0))) {
                morningSlots.add(slotDTO);
            } else {
                afternoonSlots.add(slotDTO);
            }
        }

        Map<String, List<TimeSlotDTO>> result = new HashMap<>();
        result.put("morning", morningSlots);
        result.put("afternoon", afternoonSlots);
        return result;
    }
}