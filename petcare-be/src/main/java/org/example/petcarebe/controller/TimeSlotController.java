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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/time-slots")
public class TimeSlotController {

    private static final Logger logger = Logger.getLogger(TimeSlotController.class.getName());

    @Autowired
    private AppointmentSlotRepository appointmentSlotRepository;

    @Autowired
    private DefaultTimeSlotRepository defaultTimeSlotRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @GetMapping
    public ResponseEntity<?> getAvailableSlots(@RequestParam("date") String date) {
        try {
            logger.info("Received date parameter: " + date);
            LocalDate localDate = LocalDate.parse(date);
            logger.info("Parsed LocalDate: " + localDate);

            forceCreateOrUpdateSlotsForDate(localDate);

            return ResponseEntity.ok(getSlotsForDate(localDate, true));
        } catch (Exception e) {
            logger.severe("Error processing date: " + date + " - " + e.getMessage());
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

    @GetMapping("/default-config")
    public ResponseEntity<?> getDefaultTimeSlotsConfig() {
        List<DefaultTimeSlot> defaultSlots = defaultTimeSlotRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();

        for (DefaultTimeSlot slot : defaultSlots) {
            Map<String, Object> slotInfo = new HashMap<>();
            slotInfo.put("id", slot.getId());
            slotInfo.put("time", slot.getTime().format(DateTimeFormatter.ofPattern("HH:mm")));
            slotInfo.put("slotIndex", slot.getSlotIndex());
            slotInfo.put("defaultSlotCount", slot.getDefaultSlotCount());
            slotInfo.put("totalSlots", slot.getTotalSlots());
            slotInfo.put("isActive", slot.isActive());
            slotInfo.put("isMorning", slot.isMorning());
            result.add(slotInfo);
        }

        return ResponseEntity.ok(result);
    }


    private void forceCreateOrUpdateSlotsForDate(LocalDate date) {
        List<DefaultTimeSlot> defaultSlots = defaultTimeSlotRepository.findAll();

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
                        return appointmentSlotRepository.save(newSlot);
                    });

            int bookedCount = 0;
            for (AppointmentStatus status : List.of(AppointmentStatus.PAID, AppointmentStatus.CONFIRMED)) {
                List<Appointment> appointments = appointmentRepository.findByDateAndTimeAndStatus(date, time, status);
                int statusBookedSlots = appointments.stream()
                        .mapToInt(appointment -> appointment.getPets().size())
                        .sum();
                bookedCount += statusBookedSlots;
            }

            slot.setTotalSlots(defaultSlot.getTotalSlots());
            slot.setBookedSlots(bookedCount);
            slot.setAvailableSlots(defaultSlot.getTotalSlots() - bookedCount);
            appointmentSlotRepository.save(slot);

            logger.info("Updated slot " + time + " on " + date + ": total=" +
                    slot.getTotalSlots() + ", booked=" + slot.getBookedSlots() +
                    ", available=" + slot.getAvailableSlots());
        }
    }

    private Map<String, List<TimeSlotDTO>> getSlotsForDate(LocalDate date, boolean includePaid) {
        List<DefaultTimeSlot> defaultSlots = defaultTimeSlotRepository.findAll();
        List<TimeSlotDTO> morningSlots = new ArrayList<>();
        List<TimeSlotDTO> afternoonSlots = new ArrayList<>();

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        for (DefaultTimeSlot defaultSlot : defaultSlots) {
            LocalTime time = defaultSlot.getTime();
            logger.info("Processing time slot: " + time.format(timeFormatter) + " for date: " + date);

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

            List<AppointmentStatus> statuses = includePaid
                    ? List.of(AppointmentStatus.PAID, AppointmentStatus.CONFIRMED)
                    : List.of(AppointmentStatus.CONFIRMED);
            int bookedSlots = 0;
            for (AppointmentStatus status : statuses) {
                List<Appointment> appointments = appointmentRepository.findByDateAndTimeAndStatus(date, time, status);
                int statusBookedSlots = appointments.stream()
                        .mapToInt(appointment -> appointment.getPets().size())
                        .sum();
                logger.info("Slot " + time.format(timeFormatter) + " has " + statusBookedSlots +
                        " pets booked with status " + status);
                bookedSlots += statusBookedSlots;
            }

            slot.setBookedSlots(bookedSlots);
            slot.setAvailableSlots(slot.getTotalSlots() - bookedSlots);

            if (slot.getAvailableSlots() < 0) {
                slot.setAvailableSlots(0);
            }

            logger.info("Slot " + time.format(timeFormatter) + " total: " + slot.getTotalSlots() +
                    ", booked: " + slot.getBookedSlots() + ", available: " + slot.getAvailableSlots());

            TimeSlotDTO slotDTO = new TimeSlotDTO();
            slotDTO.setTime(time);
            slotDTO.setTotalSlots(slot.getTotalSlots());
            slotDTO.setBookedSlots(slot.getBookedSlots());
            slotDTO.setAvailableSlots(slot.getAvailableSlots());
            slotDTO.setActive(slot.getIsActive());
            slotDTO.setMorning(defaultSlot.isMorning());

            if (defaultSlot.isMorning()) {
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