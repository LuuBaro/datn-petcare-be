package org.example.petcarebe.service;

import org.example.petcarebe.dto.TimeSlotDTO;
import org.example.petcarebe.model.AppointmentSlot;
import org.example.petcarebe.model.DefaultTimeSlot;
import org.example.petcarebe.model.SlotAdjustment;
import org.example.petcarebe.repository.AppointmentSlotRepository;
import org.example.petcarebe.repository.DefaultTimeSlotRepository;
import org.example.petcarebe.repository.SlotAdjustmentRepository;
import org.example.petcarebe.enums.AdjustmentType;
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

    public Map<String, List<TimeSlotDTO>> getTimeSlotsForDate(LocalDate date) {
        // Lấy danh sách DefaultTimeSlot
        List<DefaultTimeSlot> defaultSlots = defaultTimeSlotRepo.findAll();
        System.out.println("Default slots from DB: " + defaultSlots);

        // Lấy danh sách SlotAdjustment cho ngày cụ thể
        List<SlotAdjustment> adjustments = slotAdjustmentRepo.findByDate(date);
        System.out.println("Adjustments for date " + date + ": " + adjustments);

        // Lấy danh sách AppointmentSlot cho ngày cụ thể
        List<AppointmentSlot> appointmentSlots = appointmentSlotRepo.findByDate(date);
        System.out.println("Appointment slots for date " + date + ": " + appointmentSlots);

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
                        (s1, s2) -> s1 // Nếu có trùng thời gian, lấy slot đầu tiên
                ));

        // Chia slot thành morning và afternoon
        List<TimeSlotDTO> morningSlots = new ArrayList<>();
        List<TimeSlotDTO> afternoonSlots = new ArrayList<>();

        for (DefaultTimeSlot slot : defaultSlots) {
            TimeSlotDTO dto = new TimeSlotDTO();
            dto.setTime(slot.getTime());
            dto.setHour(slot.getTime().toString());
            dto.setActive(slot.isActive());
            dto.setMorning(slot.isMorning()); // Sử dụng lại logic cũ, dựa trên cột is_morning

            // Tính totalSlots: defaultSlotCount + adjustment (nếu có)
            int totalSlots = slot.getDefaultSlotCount();
            if (adjustmentMap.containsKey(slot.getTime())) {
                totalSlots += adjustmentMap.get(slot.getTime());
                totalSlots = Math.max(0, totalSlots); // Đảm bảo không âm
            }
            dto.setTotalSlots(totalSlots);

            // Lấy bookedSlots từ AppointmentSlot
            int bookedSlots = 0;
            if (appointmentSlotMap.containsKey(slot.getTime())) {
                AppointmentSlot appointmentSlot = appointmentSlotMap.get(slot.getTime());
                bookedSlots = appointmentSlot.getBookedSlots();
            }
            dto.setBookedSlots(bookedSlots);
            dto.setAvailableSlots(Math.max(0, totalSlots - bookedSlots));

            // Chia slot theo buổi dựa trên is_morning
            if (slot.isMorning()) {
                morningSlots.add(dto);
            } else {
                afternoonSlots.add(dto);
            }
        }

        // Log để kiểm tra dữ liệu trước khi trả về
        System.out.println("Morning slots before return: " + morningSlots);
        System.out.println("Afternoon slots before return: " + afternoonSlots);

        // Trả về map với morning và afternoon
        Map<String, List<TimeSlotDTO>> result = new HashMap<>();
        result.put("morning", morningSlots);
        result.put("afternoon", afternoonSlots);
        return result;
    }

    public boolean getBookingStatus() {
        return true;
    }
}