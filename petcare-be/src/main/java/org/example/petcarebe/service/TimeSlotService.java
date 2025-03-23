// TimeSlotService.java
package org.example.petcarebe.service;

import org.example.petcarebe.dto.TimeSlotDTO;
import org.example.petcarebe.enums.AdjustmentType;
import org.example.petcarebe.model.DefaultTimeSlot;
import org.example.petcarebe.model.SlotAdjustment;
import org.example.petcarebe.repository.AppointmentRepository;
import org.example.petcarebe.repository.DefaultTimeSlotRepository;
import org.example.petcarebe.repository.SlotAdjustmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TimeSlotService {

    @Autowired
    private DefaultTimeSlotRepository defaultTimeSlotRepository;

    @Autowired
    private SlotAdjustmentRepository slotAdjustmentRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    public Map<String, List<TimeSlotDTO>> getTimeSlots(LocalDate date) {
        // Lấy danh sách khung giờ mặc định
        List<DefaultTimeSlot> defaultSlots = defaultTimeSlotRepository.findByIsActiveTrueOrderBySlotIndexAsc();

        List<TimeSlotDTO> morningSlots = new ArrayList<>();
        List<TimeSlotDTO> afternoonSlots = new ArrayList<>();

        for (DefaultTimeSlot defaultSlot : defaultSlots) {
            LocalTime time = defaultSlot.getTime();
            int defaultSlotCount = defaultSlot.getDefaultSlotCount();

            // Tính số slot sau điều chỉnh
            int totalSlots = defaultSlotCount;

            // Kiểm tra điều chỉnh vĩnh viễn
            List<SlotAdjustment> permanentAdjustments = slotAdjustmentRepository.findByTimeAndIsPermanentTrue(time);
            for (SlotAdjustment adjustment : permanentAdjustments) {
                totalSlots = applyAdjustment(totalSlots, adjustment);
            }

            // Kiểm tra điều chỉnh tạm thời cho ngày cụ thể
            List<SlotAdjustment> temporaryAdjustments = slotAdjustmentRepository.findByDateAndTimeAndIsPermanentFalse(date, time);
            for (SlotAdjustment adjustment : temporaryAdjustments) {
                totalSlots = applyAdjustment(totalSlots, adjustment);
            }

            // Đếm số slot đã đặt (chỉ tính các Appointment ở trạng thái PAID hoặc CONFIRMED)
            long bookedSlots = appointmentRepository.countPetsByDateAndTimeAndStatus(date, time);

            TimeSlotDTO timeSlotDTO = new TimeSlotDTO();
            timeSlotDTO.setHour(time.toString());
            timeSlotDTO.setTotalSlots(totalSlots);
            timeSlotDTO.setBookedSlots((int) bookedSlots);

            // Phân chia buổi sáng và buổi chiều
            if (time.isBefore(LocalTime.of(14, 0))) {
                morningSlots.add(timeSlotDTO);
            } else {
                afternoonSlots.add(timeSlotDTO);
            }
        }

        Map<String, List<TimeSlotDTO>> result = new HashMap<>();
        result.put("morning", morningSlots);
        result.put("afternoon", afternoonSlots);
        return result;
    }

    private int applyAdjustment(int currentSlots, SlotAdjustment adjustment) {
        if (adjustment.getAdjustmentType() == AdjustmentType.ADD) {
            return currentSlots + adjustment.getSlotCount();
        } else if (adjustment.getAdjustmentType() == AdjustmentType.REMOVE) {
            return Math.max(0, currentSlots - adjustment.getSlotCount());
        } else if (adjustment.getAdjustmentType() == AdjustmentType.SET) {
            return adjustment.getSlotCount();
        }
        return currentSlots;
    }
}