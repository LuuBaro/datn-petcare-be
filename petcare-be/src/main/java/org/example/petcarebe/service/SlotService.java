package org.example.petcarebe.service;

import org.example.petcarebe.dto.request.BookingRequestDTO;
import org.example.petcarebe.dto.response.BookingResponseDTO;
import org.example.petcarebe.dto.TimeSlotDTO;
import org.example.petcarebe.model.Appointment;
import org.example.petcarebe.model.DefaultTimeSlot;
import org.example.petcarebe.model.SlotAdjustment;
import org.example.petcarebe.model.Orders;
import org.example.petcarebe.model.User;
import org.example.petcarebe.repository.AppointmentRepository;
import org.example.petcarebe.repository.DefaultTimeSlotRepository;
import org.example.petcarebe.repository.SlotAdjustmentRepository;
import org.example.petcarebe.repository.OrderRepository;
import org.example.petcarebe.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class SlotService {

    @Autowired
    private DefaultTimeSlotRepository defaultTimeSlotRepo;

    @Autowired
    private SlotAdjustmentRepository slotAdjustmentRepo;

    @Autowired
    private AppointmentRepository appointmentRepo;

    @Autowired
    private OrderRepository ordersRepo;

    @Autowired
    private UserRepository userRepo;

    // Lấy danh sách khung giờ và trạng thái slot theo ngày
    public Map<String, List<TimeSlotDTO>> getTimeSlotsByDate(LocalDate date) {
        // Chỉ lấy các slot đang hoạt động
        List<DefaultTimeSlot> defaultSlots = defaultTimeSlotRepo.findByIsActiveTrue();

        List<TimeSlotDTO> morningSlots = new ArrayList<>();
        List<TimeSlotDTO> afternoonSlots = new ArrayList<>();

        for (DefaultTimeSlot slot : defaultSlots) {
            LocalTime time = slot.getTime();
            int slotIndex = slot.getSlotIndex();
            int totalSlots = slot.getDefaultSlotCount();

            // Áp dụng điều chỉnh từ SlotAdjustments
            Optional<SlotAdjustment> adjustment = slotAdjustmentRepo.findByDateAndTimeAndIsPermanent(date, time, true);
            if (adjustment.isPresent()) {
                switch (adjustment.get().getAdjustmentType()) {
                    case ADD:
                        totalSlots += adjustment.get().getSlotCount();
                        break;
                    case REMOVE:
                        totalSlots = Math.max(0, totalSlots - adjustment.get().getSlotCount());
                        break;
                    case SET:
                        totalSlots = slot.getDefaultSlotCount(); // Đặt lại về giá trị mặc định (4)
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown adjustment type: " + adjustment.get().getAdjustmentType());
                }
            }

            // Đếm số slot đã đặt (không dùng slotIndex nữa)
            long bookedCount = appointmentRepo.countByDateAndTime(date, time);

            // Tạo danh sách slot chi tiết
            List<TimeSlotDTO.SlotDetail> slotDetails = new ArrayList<>();
            for (int i = 0; i < totalSlots; i++) {
                boolean isBooked = i < bookedCount;
                slotDetails.add(new TimeSlotDTO.SlotDetail(i, isBooked));
            }

            TimeSlotDTO dto = new TimeSlotDTO();
            dto.setHour(time.toString());
            dto.setSlotIndex(slotIndex);
            dto.setTotalSlots(totalSlots);
            dto.setBookedSlots((int) bookedCount);
            dto.setSlots(slotDetails);

            // Phân loại buổi sáng/chiều
            if (time.isBefore(LocalTime.of(14, 0))) {
                morningSlots.add(dto);
            } else {
                afternoonSlots.add(dto);
            }
        }

        Map<String, List<TimeSlotDTO>> result = new HashMap<>();
        result.put("morning", morningSlots);
        result.put("afternoon", afternoonSlots);
        return result;
    }

    // Đặt lịch
    public BookingResponseDTO bookSlot(BookingRequestDTO request) {
        LocalDate date = request.getDate();
        LocalTime time = request.getTime();

        // Tìm slot mặc định
        Optional<DefaultTimeSlot> defaultSlotOpt = defaultTimeSlotRepo.findByTimeAndSlotIndex(time, 0); // slotIndex không còn dùng, tạm để 0
        if (defaultSlotOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid time");
        }

        DefaultTimeSlot defaultSlot = defaultSlotOpt.get();
        int totalSlots = defaultSlot.getDefaultSlotCount();

        // Áp dụng điều chỉnh từ SlotAdjustments
        Optional<SlotAdjustment> adjustment = slotAdjustmentRepo.findByDateAndTimeAndIsPermanent(date, time, true);
        if (adjustment.isPresent()) {
            switch (adjustment.get().getAdjustmentType()) {
                case ADD:
                    totalSlots += adjustment.get().getSlotCount();
                    break;
                case REMOVE:
                    totalSlots = Math.max(0, totalSlots - adjustment.get().getSlotCount());
                    break;
                case SET:
                    totalSlots = defaultSlot.getDefaultSlotCount(); // Đặt lại về 4
                    break;
            }
        }

        // Kiểm tra slot còn trống
        long bookedCount = appointmentRepo.countByDateAndTime(date, time);
        if (bookedCount >= totalSlots) {
            throw new IllegalStateException("No available slots for this time");
        }

        // Tìm Orders và User
        Optional<Orders> orderOpt = ordersRepo.findById(request.getOrderId());
        if (orderOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid order ID");
        }

        Optional<User> userOpt = userRepo.findById(request.getUserId());
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid user ID");
        }

        // Lưu lịch đặt
        Appointment appointment = new Appointment();
        appointment.setDate(date);
        appointment.setTime(time);
        appointment.setCustomerName(request.getCustomerName());
        appointment.setPhone(request.getPhone());
        appointment.setDepositAmount(request.getDepositAmount());
        appointment.setStatus(request.getStatus());
        appointment.setUser(userOpt.get());
        appointmentRepo.save(appointment);

        BookingResponseDTO response = new BookingResponseDTO();
        response.setAppointmentId(appointment.getAppointmentId());
        response.setMessage("Booking successful");
        return response;
    }
}