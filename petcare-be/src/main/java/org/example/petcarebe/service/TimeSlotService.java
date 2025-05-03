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

        List<SlotAdjustment> adjustments = slotAdjustmentRepo.findByDate(date);
        System.out.println("Adjustments for date " + date + ": " + adjustments);

        List<AppointmentSlot> appointmentSlots = appointmentSlotRepo.findByDate(date);
        System.out.println("Appointment slots for date " + date + ": " + appointmentSlots);

        Map<LocalTime, Integer> adjustmentMap = adjustments.stream()
                .collect(Collectors.toMap(
                        adj -> adj.getDefaultTimeSlot().getTime(),
                        adj -> adj.getAdjustmentType() == AdjustmentType.ADD ? adj.getSlotCount() : -adj.getSlotCount(),
                        Integer::sum
                ));

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

            int totalSlots = slot.getDefaultSlotCount();
            if (adjustmentMap.containsKey(slot.getTime())) {
                totalSlots += adjustmentMap.get(slot.getTime());
                totalSlots = Math.max(0, totalSlots);
            }
            dto.setTotalSlots(totalSlots);

            int bookedSlots = 0;
            int availableSlots = totalSlots;
            if (appointmentSlotMap.containsKey(slot.getTime())) {
                AppointmentSlot appointmentSlot = appointmentSlotMap.get(slot.getTime());
                bookedSlots = appointmentSlot.getBookedSlots();
                availableSlots = Math.max(0, totalSlots - bookedSlots);
                appointmentSlot.setTotalSlots(totalSlots);
                appointmentSlot.setAvailableSlots(availableSlots);
                appointmentSlotRepo.save(appointmentSlot);
            } else {
                AppointmentSlot newSlot = new AppointmentSlot();
                newSlot.setDate(date);
                newSlot.setTime(time);
                newSlot.setTotalSlots(totalSlots);
                newSlot.setBookedSlots(0);
                newSlot.setAvailableSlots(totalSlots);
                newSlot.setIsActive(true);
                newSlot.setDefaultTimeSlot(slot);
                appointmentSlotRepo.save(newSlot);
            }
            dto.setBookedSlots(bookedSlots);
            dto.setAvailableSlots(availableSlots);

            if (slot.isMorning()) {
                morningSlots.add(dto);
            } else {
                afternoonSlots.add(dto);
            }
        }

        System.out.println("Morning slots before return: " + morningSlots);
        System.out.println("Afternoon slots before return: " + afternoonSlots);

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

            int totalSlots = slot.getDefaultSlotCount();
            if (adjustmentMap.containsKey(slot.getTime())) {
                totalSlots += adjustmentMap.get(slot.getTime());
                totalSlots = Math.max(0, totalSlots);
            }
            dto.setTotalSlots(totalSlots);

            // Tính bookedSlots dựa trên trạng thái CONFIRMED
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