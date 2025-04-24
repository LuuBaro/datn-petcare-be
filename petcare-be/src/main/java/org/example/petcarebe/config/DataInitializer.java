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
        // Initialize default time slots
        List<DefaultTimeSlot> existingSlots = defaultTimeSlotRepo.findAll();
        if (existingSlots.isEmpty()) {
            List<DefaultTimeSlot> defaultSlots = Arrays.asList(
                    new DefaultTimeSlot(null, LocalTime.parse("09:00:00"), 1, 4, true, true),
                    new DefaultTimeSlot(null, LocalTime.parse("10:00:00"), 2, 4, true, true),
                    new DefaultTimeSlot(null, LocalTime.parse("11:00:00"), 3, 4, true, true),
                    new DefaultTimeSlot(null, LocalTime.parse("12:00:00"), 4, 4, true, true),
                    new DefaultTimeSlot(null, LocalTime.parse("13:00:00"), 5, 4, true, true),
                    new DefaultTimeSlot(null, LocalTime.parse("14:00:00"), 6, 4, true, false),
                    new DefaultTimeSlot(null, LocalTime.parse("15:00:00"), 7, 4, true, false),
                    new DefaultTimeSlot(null, LocalTime.parse("16:00:00"), 8, 4, true, false),
                    new DefaultTimeSlot(null, LocalTime.parse("17:00:00"), 9, 4, true, false),
                    new DefaultTimeSlot(null, LocalTime.parse("18:00:00"), 10, 4, true, false),
                    new DefaultTimeSlot(null, LocalTime.parse("19:00:00"), 11, 4, true, false),
                    new DefaultTimeSlot(null, LocalTime.parse("20:00:00"), 12, 4, true, false)
            );
            defaultTimeSlotRepo.saveAll(defaultSlots);
            System.out.println("Initialized default time slots with 4 slots each.");
        } else {
            boolean updated = false;
            for (DefaultTimeSlot slot : existingSlots) {
                if (slot.getDefaultSlotCount() != 4) {
                    slot.setDefaultSlotCount(4);
                    slot.setTotalSlots(4);
                    updated = true;
                }
            }
            if (updated) {
                defaultTimeSlotRepo.saveAll(existingSlots);
                System.out.println("Updated default time slots to ensure 4 slots each.");
            } else {
                System.out.println("Default time slots already correct.");
            }
        }
    }
}