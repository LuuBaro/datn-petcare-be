package org.example.petcarebe.dto;

import lombok.Data;

import java.util.List;

@Data
public class TimeSlotDTO {
    private String hour;
    private int slotIndex;
    private int totalSlots;
    private int bookedSlots;
    private List<SlotDetail> slots;

    @Data
    public static class SlotDetail {
        private int slotIndex;
        private boolean isBooked;

        // Thêm constructor để khớp với cú pháp new SlotDetail(i, isBooked)
        public SlotDetail(int slotIndex, boolean isBooked) {
            this.slotIndex = slotIndex;
            this.isBooked = isBooked;
        }
    }
}