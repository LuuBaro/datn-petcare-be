package org.example.petcarebe.service;

import org.example.petcarebe.dto.TimeSlotDTO;
import org.example.petcarebe.model.Appointment;
import org.example.petcarebe.model.AppointmentSlot;
import org.example.petcarebe.model.DefaultTimeSlot;
import org.example.petcarebe.model.SlotAdjustment;
import org.example.petcarebe.repository.AppointmentRepository;
import org.example.petcarebe.repository.AppointmentSlotRepository;
import org.example.petcarebe.repository.DefaultTimeSlotRepository;
import org.example.petcarebe.repository.SlotAdjustmentRepository;
import org.example.petcarebe.enums.AdjustmentType;
import org.example.petcarebe.enums.AppointmentStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TimeSlotService {

    @Autowired
    private DefaultTimeSlotRepository defaultTimeSlotRepo;

    @Autowired
    private SlotAdjustmentRepository slotAdjustmentRepo;

    @Autowired
    private AppointmentSlotRepository appointmentSlotRepo;

    @Autowired
    private AppointmentRepository appointmentRepository;

    public Map<String, List<TimeSlotDTO>> getConfirmedSlots(LocalDate date) {
        List<DefaultTimeSlot> defaultSlots = defaultTimeSlotRepo.findAll();
        System.out.println("Default slots from DB: " + defaultSlots);

        List<AppointmentSlot> appointmentSlots = appointmentSlotRepo.findByDate(date);
        System.out.println("Appointment slots for date " + date + ": " + appointmentSlots);

        Map<LocalTime, AppointmentSlot> appointmentSlotMap = appointmentSlots.stream()
            .collect(Collectors.toMap(
                AppointmentSlot::getTime,
                slot -> slot,
                (s1, s2) -> s1
            ));

        List<TimeSlotDTO> morningSlots = new ArrayList<>();
        List<TimeSlotDTO> afternoonSlots = new ArrayList<>();

        for (DefaultTimeSlot slot : defaultSlots) {
            TimeSlotDTO dto = new TimeSlotDTO();
            LocalTime time = slot.getTime();
            dto.setTime(time);
            dto.setHour(time.toString());
            dto.setActive(slot.isActive());
            dto.setMorning(slot.isMorning());

            int totalSlots = slot.getTotalSlots();
            dto.setTotalSlots(totalSlots);

            int bookedSlots = 0;
            List<Appointment> appointments = appointmentRepository.findByDateAndTimeAndStatus(date, time, AppointmentStatus.CONFIRMED);
            for (Appointment appt : appointments) {
                bookedSlots += appt.getPets() != null ? appt.getPets().size() : 0;
            }

            dto.setBookedSlots(bookedSlots);
            dto.setAvailableSlots(Math.max(0, totalSlots - bookedSlots));

            if (slot.isMorning()) {
                morningSlots.add(dto);
            } else {
                afternoonSlots.add(dto);
            }
        }

        System.out.println("Morning slots (CONFIRMED) before return: " + morningSlots);
        System.out.println("Afternoon slots (CONFIRMED) before return: " + afternoonSlots);

        Map<String, List<TimeSlotDTO>> result = new HashMap<>();
        result.put("morning", morningSlots);
        result.put("afternoon", afternoonSlots);
        return result;
    }

    public Map<String, List<TimeSlotDTO>> getInProgressSlots(LocalDate date) {
        List<DefaultTimeSlot> defaultSlots = defaultTimeSlotRepo.findAll();
        System.out.println("Default slots from DB: " + defaultSlots);

        List<AppointmentSlot> appointmentSlots = appointmentSlotRepo.findByDate(date);
        System.out.println("Appointment slots for date " + date + ": " + appointmentSlots);

        Map<LocalTime, AppointmentSlot> appointmentSlotMap = appointmentSlots.stream()
            .collect(Collectors.toMap(
                AppointmentSlot::getTime,
                slot -> slot,
                (s1, s2) -> s1
            ));

        List<TimeSlotDTO> morningSlots = new ArrayList<>();
        List<TimeSlotDTO> afternoonSlots = new ArrayList<>();

        for (DefaultTimeSlot slot : defaultSlots) {
            TimeSlotDTO dto = new TimeSlotDTO();
            LocalTime time = slot.getTime();
            dto.setTime(time);
            dto.setHour(time.toString());
            dto.setActive(slot.isActive());
            dto.setMorning(slot.isMorning());

            int totalSlots = slot.getTotalSlots();
            dto.setTotalSlots(totalSlots);

            int bookedSlots = 0;
            List<Appointment> appointments = appointmentRepository.findByDateAndTimeAndStatus(date, time, AppointmentStatus.IN_PROGRESS);
            for (Appointment appt : appointments) {
                bookedSlots += appt.getPets() != null ? appt.getPets().size() : 0;
            }

            dto.setBookedSlots(bookedSlots);
            dto.setAvailableSlots(Math.max(0, totalSlots - bookedSlots));

            if (slot.isMorning()) {
                morningSlots.add(dto);
            } else {
                afternoonSlots.add(dto);
            }
        }

        System.out.println("Morning slots (IN_PROGRESS) before return: " + morningSlots);
        System.out.println("Afternoon slots (IN_PROGRESS) before return: " + afternoonSlots);

        Map<String, List<TimeSlotDTO>> result = new HashMap<>();
        result.put("morning", morningSlots);
        result.put("afternoon", afternoonSlots);
        return result;
    }

    public Map<String, List<TimeSlotDTO>> getCompletedSlots(LocalDate date) {
        List<DefaultTimeSlot> defaultSlots = defaultTimeSlotRepo.findAll();
        System.out.println("Default slots from DB: " + defaultSlots);

        List<AppointmentSlot> appointmentSlots = appointmentSlotRepo.findByDate(date);
        System.out.println("Appointment slots for date " + date + ": " + appointmentSlots);

        Map<LocalTime, AppointmentSlot> appointmentSlotMap = appointmentSlots.stream()
            .collect(Collectors.toMap(
                AppointmentSlot::getTime,
                slot -> slot,
                (s1, s2) -> s1
            ));

        List<TimeSlotDTO> morningSlots = new ArrayList<>();
        List<TimeSlotDTO> afternoonSlots = new ArrayList<>();

        for (DefaultTimeSlot slot : defaultSlots) {
            TimeSlotDTO dto = new TimeSlotDTO();
            LocalTime time = slot.getTime();
            dto.setTime(time);
            dto.setHour(time.toString());
            dto.setActive(slot.isActive());
            dto.setMorning(slot.isMorning());

            int totalSlots = slot.getTotalSlots();
            dto.setTotalSlots(totalSlots);

            int bookedSlots = 0;
            List<Appointment> appointments = appointmentRepository.findByDateAndTimeAndStatus(date, time, AppointmentStatus.COMPLETED);
            for (Appointment appt : appointments) {
                bookedSlots += appt.getPets() != null ? appt.getPets().size() : 0;
            }

            dto.setBookedSlots(bookedSlots);
            dto.setAvailableSlots(Math.max(0, totalSlots - bookedSlots));

            if (slot.isMorning()) {
                morningSlots.add(dto);
            } else {
                afternoonSlots.add(dto);
            }
        }

        System.out.println("Morning slots (COMPLETED) before return: " + morningSlots);
        System.out.println("Afternoon slots (COMPLETED) before return: " + afternoonSlots);

        Map<String, List<TimeSlotDTO>> result = new HashMap<>();
        result.put("morning", morningSlots);
        result.put("afternoon", afternoonSlots);
        return result;
    }

    public Map<String, List<TimeSlotDTO>> getConfirmedSlotsForStaff(LocalDate date) {
        List<DefaultTimeSlot> defaultSlots = defaultTimeSlotRepo.findAll();
        System.out.println("Default slots from DB: " + defaultSlots);

        List<SlotAdjustment> adjustments = slotAdjustmentRepo.findByDate(date);
        System.out.println("Adjustments for date " + date + ": " + adjustments);

        Map<LocalTime, Integer> adjustmentMap = adjustments.stream()
                .collect(Collectors.toMap(
                        adj -> adj.getDefaultTimeSlot().getTime(),
                        adj -> adj.getAdjustmentType() == AdjustmentType.ADD ? adj.getSlotCount() : -adj.getSlotCount(),
                        Integer::sum
                ));

        List<TimeSlotDTO> morningSlots = new ArrayList<>();
        List<TimeSlotDTO> afternoonSlots = new ArrayList<>();

        for (DefaultTimeSlot slot : defaultSlots) {
            TimeSlotDTO dto = new TimeSlotDTO();
            LocalTime time = slot.getTime();
            dto.setTime(time);
            dto.setHour(time.toString());
            dto.setActive(slot.isActive());
            dto.setMorning(slot.isMorning());

            int totalSlots = slot.getTotalSlots();
            if (adjustmentMap.containsKey(slot.getTime())) {
                totalSlots += adjustmentMap.get(slot.getTime());
                totalSlots = Math.max(0, totalSlots);
            }
            dto.setTotalSlots(totalSlots);

            // Tính bookedSlots dựa trên các trạng thái CONFIRMED, IN_PROGRESS và COMPLETED
            int bookedSlots = 0;
            List<AppointmentStatus> statuses = Arrays.asList(
                AppointmentStatus.CONFIRMED,
                AppointmentStatus.IN_PROGRESS,
                AppointmentStatus.COMPLETED
            );
            for (AppointmentStatus status : statuses) {
                List<Appointment> appointments = appointmentRepository.findByDateAndTimeAndStatus(date, time, status);
                for (Appointment appt : appointments) {
                    bookedSlots += appt.getPets() != null ? appt.getPets().size() : 0;
                }
            }
            dto.setBookedSlots(bookedSlots);
            dto.setAvailableSlots(Math.max(0, totalSlots - bookedSlots));

            if (slot.isMorning()) {
                morningSlots.add(dto);
            } else {
                afternoonSlots.add(dto);
            }
        }

        System.out.println("Morning slots before return (staff): " + morningSlots);
        System.out.println("Afternoon slots before return (staff): " + afternoonSlots);

        Map<String, List<TimeSlotDTO>> result = new HashMap<>();
        result.put("morning", morningSlots);
        result.put("afternoon", afternoonSlots);
        return result;
    }

    public boolean getBookingStatus() {
        return true;
    }
}