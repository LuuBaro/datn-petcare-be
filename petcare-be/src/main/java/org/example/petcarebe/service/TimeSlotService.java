package org.example.petcarebe.service;

import org.example.petcarebe.dto.TimeSlotDTO;
import org.example.petcarebe.model.AppointmentSlot;
import org.example.petcarebe.model.DefaultTimeSlot;
import org.example.petcarebe.model.SlotAdjustment;
import org.example.petcarebe.repository.AppointmentSlotRepository;
import org.example.petcarebe.repository.DefaultTimeSlotRepository;
import org.example.petcarebe.repository.SlotAdjustmentRepository;
import org.example.petcarebe.enums.AdjustmentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(TimeSlotService.class);

    @Autowired
    private DefaultTimeSlotRepository defaultTimeSlotRepo;

    @Autowired
    private SlotAdjustmentRepository slotAdjustmentRepo;

    @Autowired
    private AppointmentSlotRepository appointmentSlotRepo;

    public Map<String, List<TimeSlotDTO>> getTimeSlotsForDate(LocalDate date) {
        logger.info("Bắt đầu lấy khung giờ cho ngày: {}", date);

        // Lấy danh sách DefaultTimeSlot
        List<DefaultTimeSlot> defaultSlots = defaultTimeSlotRepo.findAll();
        logger.info("Số khung giờ mặc định từ DB: {}", defaultSlots.size());
        logger.info("Danh sách khung giờ mặc định từ DB: {}", defaultSlots);

        // Kiểm tra từng slot
        for (DefaultTimeSlot slot : defaultSlots) {
            LocalTime time = slot.getTime();
            logger.info("Slot: time={}, hour={}, isMorning={}", time, time.getHour(), slot.isMorning());
        }

        // Lấy danh sách SlotAdjustment cho ngày cụ thể
        List<SlotAdjustment> adjustments = slotAdjustmentRepo.findByDate(date);
        logger.info("Điều chỉnh slot cho ngày {}: {}", date, adjustments);

        // Lấy danh sách AppointmentSlot cho ngày cụ thể
        List<AppointmentSlot> appointmentSlots = appointmentSlotRepo.findByDate(date);
        logger.info("Slot đã đặt cho ngày {}: {}", date, appointmentSlots);

        // Tạo map để lưu số slot điều chỉnh theo thời gian
        Map<LocalTime, Integer> adjustmentMap = adjustments.stream()
                .collect(Collectors.toMap(
                        adj -> adj.getDefaultTimeSlot().getTime(),
                        adj -> adj.getAdjustmentType() == AdjustmentType.ADD ? adj.getSlotCount() : -adj.getSlotCount(),
                        Integer::sum
                ));

        // Tạo map để lưu số slot đã đặt từ AppointmentSlot
        Map<LocalTime, AppointmentSlot> appointmentSlotMap = appointmentSlots.stream()
                .collect(Collectors.toMap(
                        AppointmentSlot::getTime,
                        slot -> slot,
                        (s1, s2) -> s1
                ));

        // Chia slot thành morning và afternoon
        List<TimeSlotDTO> morningSlots = new ArrayList<>();
        List<TimeSlotDTO> afternoonSlots = new ArrayList<>();

        for (DefaultTimeSlot slot : defaultSlots) {
            TimeSlotDTO dto = new TimeSlotDTO();
            dto.setTime(slot.getTime());
            dto.setHour(slot.getTime().toString());
            dto.setActive(slot.isActive());

            LocalTime time = slot.getTime();
            boolean isMorning = time.getHour() >= 9 && time.getHour() < 14;
            logger.info("Phân loại slot: time={}, hour={}, isMorning={}", time, time.getHour(), isMorning);
            dto.setMorning(isMorning);

            int totalSlots = slot.getDefaultSlotCount();
            if (adjustmentMap.containsKey(slot.getTime())) {
                totalSlots += adjustmentMap.get(slot.getTime());
                totalSlots = Math.max(0, totalSlots);
            }
            dto.setTotalSlots(totalSlots);

            int bookedSlots = 0;
            if (appointmentSlotMap.containsKey(slot.getTime())) {
                AppointmentSlot appointmentSlot = appointmentSlotMap.get(slot.getTime());
                bookedSlots = appointmentSlot.getBookedSlots();
            }
            dto.setBookedSlots(bookedSlots);
            dto.setAvailableSlots(Math.max(0, totalSlots - bookedSlots));

            if (isMorning) {
                morningSlots.add(dto);
            } else {
                afternoonSlots.add(dto);
            }
        }

        logger.info("Số khung giờ buổi sáng trước khi trả về: {}", morningSlots.size());
        logger.info("Danh sách khung giờ buổi sáng: {}", morningSlots);
        logger.info("Số khung giờ buổi chiều trước khi trả về: {}", afternoonSlots.size());
        logger.info("Danh sách khung giờ buổi chiều: {}", afternoonSlots);

        Map<String, List<TimeSlotDTO>> result = new HashMap<>();
        result.put("morning", morningSlots);
        result.put("afternoon", afternoonSlots);
        return result;
    }

    public boolean getBookingStatus() {
        return true;
    }
}