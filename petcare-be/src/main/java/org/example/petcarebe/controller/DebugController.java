package org.example.petcarebe.controller;

import org.example.petcarebe.enums.AppointmentStatus;
import org.example.petcarebe.model.Appointment;
import org.example.petcarebe.model.AppointmentSlot;
import org.example.petcarebe.model.DefaultTimeSlot;
import org.example.petcarebe.model.Pet;
import org.example.petcarebe.repository.AppointmentRepository;
import org.example.petcarebe.repository.AppointmentSlotRepository;
import org.example.petcarebe.repository.DefaultTimeSlotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/debug")
public class DebugController {
    
    private static final Logger logger = Logger.getLogger(DebugController.class.getName());
    
    @Autowired
    private AppointmentRepository appointmentRepository;
    
    @Autowired
    private AppointmentSlotRepository appointmentSlotRepository;
    
    @Autowired
    private DefaultTimeSlotRepository defaultTimeSlotRepository;
    
    @GetMapping("/slot-info")
    public ResponseEntity<?> getSlotInfo(@RequestParam("date") String dateStr, 
                                        @RequestParam("time") String timeStr) {
        try {
            LocalDate date = LocalDate.parse(dateStr);
            LocalTime time = LocalTime.parse(timeStr);
            
            Map<String, Object> result = new HashMap<>();
            result.put("date", date.toString());
            result.put("time", time.format(DateTimeFormatter.ofPattern("HH:mm")));
            
            // Lấy thông tin DefaultTimeSlot
            DefaultTimeSlot defaultSlot = defaultTimeSlotRepository.findByTime(time).orElse(null);
            if (defaultSlot != null) {
                Map<String, Object> defaultSlotInfo = new HashMap<>();
                defaultSlotInfo.put("id", defaultSlot.getId());
                defaultSlotInfo.put("time", defaultSlot.getTime().format(DateTimeFormatter.ofPattern("HH:mm")));
                defaultSlotInfo.put("totalSlots", defaultSlot.getTotalSlots());
                defaultSlotInfo.put("defaultSlotCount", defaultSlot.getDefaultSlotCount());
                defaultSlotInfo.put("isActive", defaultSlot.isActive());
                result.put("defaultSlot", defaultSlotInfo);
            } else {
                result.put("defaultSlot", "Not found");
            }
            
            // Lấy thông tin AppointmentSlot
            AppointmentSlot slot = appointmentSlotRepository.findByDateAndTime(date, time).orElse(null);
            if (slot != null) {
                Map<String, Object> slotInfo = new HashMap<>();
                slotInfo.put("id", slot.getId());
                slotInfo.put("totalSlots", slot.getTotalSlots());
                slotInfo.put("bookedSlots", slot.getBookedSlots());
                slotInfo.put("availableSlots", slot.getAvailableSlots());
                slotInfo.put("isActive", slot.getIsActive());
                result.put("appointmentSlot", slotInfo);
            } else {
                result.put("appointmentSlot", "Not found");
            }
            
            // Lấy danh sách tất cả các appointment cho slot này
            List<Map<String, Object>> appointmentsList = new ArrayList<>();
            for (AppointmentStatus status : AppointmentStatus.values()) {
                List<Appointment> appointments = appointmentRepository.findByDateAndTimeAndStatus(date, time, status);
                for (Appointment appointment : appointments) {
                    Map<String, Object> appointmentInfo = new HashMap<>();
                    appointmentInfo.put("id", appointment.getAppointmentId());
                    appointmentInfo.put("customerName", appointment.getCustomerName());
                    appointmentInfo.put("status", appointment.getStatus());
                    
                    // Thông tin về pets
                    List<Map<String, Object>> petsList = new ArrayList<>();
                    for (Pet pet : appointment.getPets()) {
                        Map<String, Object> petInfo = new HashMap<>();
                        petInfo.put("id", pet.getId());
                        petInfo.put("name", pet.getNamePet());
                        petInfo.put("type", pet.getPetType());
                        petsList.add(petInfo);
                    }
                    
                    appointmentInfo.put("petCount", appointment.getPets().size());
                    appointmentInfo.put("pets", petsList);
                    appointmentsList.add(appointmentInfo);
                }
            }
            
            result.put("appointments", appointmentsList);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.severe("Error in slot-info: " + e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    @GetMapping("/fix-slots")
    public ResponseEntity<?> fixSlots(@RequestParam("date") String dateStr) {
        try {
            LocalDate date = LocalDate.parse(dateStr);
            List<DefaultTimeSlot> defaultSlots = defaultTimeSlotRepository.findAll();
            List<Map<String, Object>> result = new ArrayList<>();
            
            for (DefaultTimeSlot defaultSlot : defaultSlots) {
                LocalTime time = defaultSlot.getTime();
                Map<String, Object> slotResult = new HashMap<>();
                slotResult.put("time", time.format(DateTimeFormatter.ofPattern("HH:mm")));
                
                // Kiểm tra slot tồn tại chưa
                AppointmentSlot slot = appointmentSlotRepository.findByDateAndTime(date, time).orElse(null);
                
                if (slot == null) {
                    // Tạo mới slot
                    slot = new AppointmentSlot();
                    slot.setDate(date);
                    slot.setTime(time);
                    slot.setTotalSlots(defaultSlot.getTotalSlots());
                    slot.setBookedSlots(0);
                    slot.setAvailableSlots(defaultSlot.getTotalSlots());
                    slot.setIsActive(true);
                    slot.setDefaultTimeSlot(defaultSlot);
                    appointmentSlotRepository.save(slot);
                    slotResult.put("action", "Created new slot");
                } else {
                    // Cập nhật lại các thông số của slot
                    // Xem có bao nhiêu appointments
                    int bookedCount = 0;
                    for (AppointmentStatus status : List.of(AppointmentStatus.PAID, AppointmentStatus.CONFIRMED)) {
                        List<Appointment> appointments = appointmentRepository.findByDateAndTimeAndStatus(date, time, status);
                        bookedCount += appointments.stream()
                                .mapToInt(appointment -> appointment.getPets().size())
                                .sum();
                    }
                    
                    slot.setTotalSlots(defaultSlot.getTotalSlots());
                    slot.setBookedSlots(bookedCount);
                    slot.setAvailableSlots(slot.getTotalSlots() - bookedCount);
                    appointmentSlotRepository.save(slot);
                    slotResult.put("action", "Updated slot");
                }
                
                slotResult.put("totalSlots", slot.getTotalSlots());
                slotResult.put("bookedSlots", slot.getBookedSlots());
                slotResult.put("availableSlots", slot.getAvailableSlots());
                
                result.add(slotResult);
            }
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.severe("Error in fix-slots: " + e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    @PostMapping("/reset-slot")
    public ResponseEntity<?> resetSlot(@RequestParam("date") String dateStr, 
                                       @RequestParam("time") String timeStr) {
        try {
            LocalDate date = LocalDate.parse(dateStr);
            LocalTime time = LocalTime.parse(timeStr);
            
            Map<String, Object> result = new HashMap<>();
            result.put("date", date.toString());
            result.put("time", time.format(DateTimeFormatter.ofPattern("HH:mm")));
            
            // Kiểm tra DefaultTimeSlot tồn tại
            DefaultTimeSlot defaultSlot = defaultTimeSlotRepository.findByTime(time)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khung giờ mặc định: " + time));
            
            // Tìm AppointmentSlot hiện tại
            AppointmentSlot slot = appointmentSlotRepository.findByDateAndTime(date, time).orElse(null);
            
            if (slot != null) {
                // Xóa slot cũ và tạo mới
                appointmentSlotRepository.delete(slot);
                result.put("oldSlot", "Deleted");
            }
            
            // Tạo slot mới
            AppointmentSlot newSlot = new AppointmentSlot();
            newSlot.setDate(date);
            newSlot.setTime(time);
            newSlot.setTotalSlots(defaultSlot.getTotalSlots());
            
            // Tính toán lại số lượng slots đã đặt
            int bookedCount = 0;
            for (AppointmentStatus status : List.of(AppointmentStatus.PAID, AppointmentStatus.CONFIRMED)) {
                List<Appointment> appointments = appointmentRepository.findByDateAndTimeAndStatus(date, time, status);
                bookedCount += appointments.stream()
                        .mapToInt(appointment -> appointment.getPets().size())
                        .sum();
            }
            
            newSlot.setBookedSlots(bookedCount);
            newSlot.setAvailableSlots(defaultSlot.getTotalSlots() - bookedCount);
            newSlot.setIsActive(true);
            newSlot.setDefaultTimeSlot(defaultSlot);
            
            appointmentSlotRepository.save(newSlot);
            
            result.put("newSlot", Map.of(
                "totalSlots", newSlot.getTotalSlots(),
                "bookedSlots", newSlot.getBookedSlots(),
                "availableSlots", newSlot.getAvailableSlots()
            ));
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.severe("Error in reset-slot: " + e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
} 