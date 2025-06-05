package org.example.petcarebe.config;

import jakarta.annotation.PostConstruct;
import org.example.petcarebe.model.DefaultTimeSlot;
import org.example.petcarebe.repository.DefaultTimeSlotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

@Configuration
public class DataInitializer {

    @Autowired
    private DefaultTimeSlotRepository defaultTimeSlotRepo;

    @PostConstruct
    public void initData() {
        List<DefaultTimeSlot> existingSlots = defaultTimeSlotRepo.findAll();
        if (existingSlots.isEmpty()) {
            List<DefaultTimeSlot> defaultSlots = Arrays.asList(
                    new DefaultTimeSlot(null, LocalTime.parse("09:00:00"), 0, 4, true, true),
                    new DefaultTimeSlot(null, LocalTime.parse("10:00:00"), 0, 4, true, true),
                    new DefaultTimeSlot(null, LocalTime.parse("11:00:00"), 0, 4, true, true),
                    new DefaultTimeSlot(null, LocalTime.parse("12:00:00"), 0, 4, true, true),
                    new DefaultTimeSlot(null, LocalTime.parse("13:00:00"), 0, 4, true, true),
                    new DefaultTimeSlot(null, LocalTime.parse("14:00:00"), 0, 4, true, false),
                    new DefaultTimeSlot(null, LocalTime.parse("15:00:00"), 0, 4, true, false),
                    new DefaultTimeSlot(null, LocalTime.parse("16:00:00"), 0, 4, true, false),
                    new DefaultTimeSlot(null, LocalTime.parse("17:00:00"), 0, 4, true, false),
                    new DefaultTimeSlot(null, LocalTime.parse("18:00:00"), 0, 4, true, false),
                    new DefaultTimeSlot(null, LocalTime.parse("19:00:00"), 0, 4, true, false),
                    new DefaultTimeSlot(null, LocalTime.parse("20:00:00"), 0, 4, true, false)
            );
            defaultTimeSlotRepo.saveAll(defaultSlots);
            System.out.println("Khởi tạo các slot mặc định với slotIndex = 0 và 4 slot mỗi khung giờ.");
        } else {
            boolean updated = false;
            for (DefaultTimeSlot slot : existingSlots) {
                if (slot.getSlotIndex() != 0) {
                    slot.setSlotIndex(0);
                    updated = true;
                }
                if (slot.getDefaultSlotCount() != 4) {
                    slot.setDefaultSlotCount(4);
                    slot.setTotalSlots(4);
                    updated = true;
                }
            }
            if (updated) {
                defaultTimeSlotRepo.saveAll(existingSlots);
                System.out.println("Cập nhật các slot mặc định để slotIndex = 0 và đảm bảo 4 slot mỗi khung giờ.");
            } else {
                System.out.println("Các slot mặc định đã đúng cấu hình.");
            }
        }
    }
}