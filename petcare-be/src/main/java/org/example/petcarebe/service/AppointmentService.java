package org.example.petcarebe.service;

import org.example.petcarebe.dto.AppointmentRequest;
import org.example.petcarebe.dto.AppointmentResponse;
import org.example.petcarebe.dto.PetResponse;
import org.example.petcarebe.dto.SlotUpdateMessage;
import org.example.petcarebe.dto.NewAppointmentMessage;
import org.example.petcarebe.enums.AppointmentStatus;
import org.example.petcarebe.enums.PetType;
import org.example.petcarebe.enums.TransactionType;
import org.example.petcarebe.enums.TransactionStatus;
import org.example.petcarebe.model.Appointment;
import org.example.petcarebe.model.AppointmentSlot;
import org.example.petcarebe.model.DefaultTimeSlot;
import org.example.petcarebe.model.Pet;
import org.example.petcarebe.model.PetService;
import org.example.petcarebe.model.PetWeight;
import org.example.petcarebe.model.Transaction;
import org.example.petcarebe.repository.AppointmentRepository;
import org.example.petcarebe.repository.AppointmentSlotRepository;
import org.example.petcarebe.repository.DefaultTimeSlotRepository;
import org.example.petcarebe.repository.PetServiceRepository;
import org.example.petcarebe.repository.PetWeightRepository;
import org.example.petcarebe.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AppointmentService {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private AppointmentSlotRepository appointmentSlotRepository;

    @Autowired
    private PetServiceRepository petServiceRepository;

    @Autowired
    private PetWeightRepository petWeightRepository;

    @Autowired
    private DefaultTimeSlotRepository defaultTimeSlotRepo;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Transactional
    public Appointment createAppointment(AppointmentRequest request) {
        // Validate input
        if (request.getPets() == null || request.getPets().isEmpty()) {
            throw new IllegalArgumentException("Phải có ít nhất một thú cưng");
        }
        if (request.getCustomerName() == null || request.getCustomerName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên khách hàng không được để trống");
        }
        if (request.getPhone() == null || !request.getPhone().matches("0\\d{9}")) {
            throw new IllegalArgumentException("Số điện thoại không hợp lệ");
        }
        if (request.getDate() == null || request.getTime() == null) {
            throw new IllegalArgumentException("Ngày và giờ không được để trống");
        }

        // Parse date and time
        LocalDate date;
        LocalTime time;
        try {
            date = LocalDate.parse(request.getDate());
            time = LocalTime.parse(request.getTime());
        } catch (Exception e) {
            throw new IllegalArgumentException("Định dạng ngày hoặc giờ không hợp lệ");
        }

        // Find or create AppointmentSlot
        AppointmentSlot slot = appointmentSlotRepository.findByDateAndTime(date, time)
                .orElseGet(() -> {
                    DefaultTimeSlot defaultSlot = defaultTimeSlotRepo.findByTime(time)
                            .orElseThrow(() -> new IllegalArgumentException("Khung giờ không tồn tại: " + time));
                    AppointmentSlot newSlot = new AppointmentSlot();
                    newSlot.setDate(date);
                    newSlot.setTime(time);
                    newSlot.setTotalSlots(defaultSlot.getTotalSlots());
                    newSlot.setBookedSlots(0);
                    newSlot.setAvailableSlots(defaultSlot.getTotalSlots());
                    newSlot.setIsActive(true);
                    newSlot.setDefaultTimeSlot(defaultSlot);
                    return newSlot;
                });

        // Check available slots
        int requiredSlots = request.getPets().size();
        if (slot.getAvailableSlots() < requiredSlots) {
            throw new IllegalArgumentException("Không đủ slot trống cho khung giờ này");
        }

        // Create Appointment
        Appointment appointment = new Appointment();
        appointment.setCustomerName(request.getCustomerName());
        appointment.setPhone(request.getPhone());
        appointment.setStatus(AppointmentStatus.PAID);
        appointment.setDate(date);
        appointment.setTime(time);
        appointment.setDepositAmount(request.getDepositAmount());
        appointment.setTotalAmount(request.getTotalAmount());
        appointment.setPaidAmount(request.getDepositAmount());
        appointment.setAppointmentSlots(new ArrayList<>());
        appointment.setPets(new ArrayList<>());

        // Create Pets
        List<Pet> pets = new ArrayList<>();
        for (AppointmentRequest.PetRequest petRequest : request.getPets()) {
            if (petRequest.getPetServiceId() == null || petRequest.getPetWeightId() == null) {
                throw new IllegalArgumentException("Dịch vụ hoặc cân nặng của thú cưng không được để trống");
            }
            PetService petService = petServiceRepository.findById(petRequest.getPetServiceId())
                    .orElseThrow(() -> new IllegalArgumentException("Dịch vụ không tồn tại: " + petRequest.getPetServiceId()));
            PetWeight petWeight = petWeightRepository.findById(petRequest.getPetWeightId())
                    .orElseThrow(() -> new IllegalArgumentException("Cân nặng không tồn tại: " + petRequest.getPetWeightId()));

            Pet pet = new Pet();
            pet.setNamePet(petRequest.getName());
            pet.setPetType(PetType.valueOf(petRequest.getPetType().toUpperCase()));
            pet.setPetService(petService);
            pet.setPetWeight(petWeight);
            pet.setNote(petRequest.getNote());
            pet.setPrice(petRequest.getPrice());
            pet.setAppointment(appointment);
            pet.setDepositAmount(request.getDepositAmount() / requiredSlots);
            pet.setPaidAmount(pet.getDepositAmount());
            pets.add(pet);
        }
        appointment.setPets(pets);

        // Update AppointmentSlot
        slot.setAppointment(appointment);
        appointment.getAppointmentSlots().add(slot);

        // Save to DB
        Appointment savedAppointment = appointmentRepository.save(appointment);
        appointmentSlotRepository.save(slot);

        // Lưu giao dịch cọc
        Transaction depositTransaction = new Transaction();
        depositTransaction.setAppointment(savedAppointment);
        depositTransaction.setAmount(request.getDepositAmount());
        depositTransaction.setType(TransactionType.DEPOSIT);
        depositTransaction.setStatus(TransactionStatus.COMPLETED);
        transactionRepository.save(depositTransaction);

        // Notify WebSocket clients about slot update
        SlotUpdateMessage slotMessage = new SlotUpdateMessage(
                request.getDate(),
                request.getTime(),
                request.getPets().size()
        );
        messagingTemplate.convertAndSend("/topic/slots", slotMessage);

        // Notify WebSocket clients about new appointment
        Map<String, Object> messagePayload = new HashMap<>();
        messagePayload.put("type", "NEW_APPOINTMENT");
        messagePayload.put("appointment", new NewAppointmentMessage(
                savedAppointment.getAppointmentId(),
                savedAppointment.getCustomerName(),
                savedAppointment.getDate().toString(),
                savedAppointment.getTime().toString(),
                savedAppointment.getPaidAmount()
        ));
        messagingTemplate.convertAndSend("/topic/new-appointment", messagePayload);

        return savedAppointment;
    }

    public List<AppointmentResponse> getPendingAppointments() {
        List<Appointment> pendingAppointments = appointmentRepository.findByStatus(AppointmentStatus.PAID);
        return pendingAppointments.stream().map(appointment -> new AppointmentResponse(
                appointment.getAppointmentId(),
                appointment.getCustomerName(),
                appointment.getPhone(),
                appointment.getDate().toString(),
                appointment.getTime().toString(),
                appointment.getPaidAmount(),
                appointment.getTotalAmount(),
                appointment.getDepositAmount(),
                appointment.getPets().size()
        )).collect(Collectors.toList());
    }

    public List<PetResponse> getPetsByAppointmentId(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Lịch hẹn không tồn tại: " + appointmentId));
        return appointment.getPets().stream().map(pet -> new PetResponse(
                pet.getId(),
                pet.getNamePet(),
                pet.getPetType().toString(),
                pet.getAge(),
                pet.getPetWeight().getWeightRange(),
                pet.getPetService().getServiceName(),
                pet.getPrice()
        )).collect(Collectors.toList());
    }

    @Transactional
    public void confirmAppointments(List<Long> appointmentIds) {
        for (Long appointmentId : appointmentIds) {
            Appointment appointment = appointmentRepository.findById(appointmentId)
                    .orElseThrow(() -> new IllegalArgumentException("Lịch hẹn không tồn tại: " + appointmentId));
            if (appointment.getStatus() != AppointmentStatus.PAID) {
                throw new IllegalStateException("Lịch hẹn #" + appointmentId + " không ở trạng thái PAID");
            }
            appointment.setStatus(AppointmentStatus.CONFIRMED);
            appointmentRepository.save(appointment);

            // Notify WebSocket clients about slot update
            SlotUpdateMessage message = new SlotUpdateMessage(
                    appointment.getDate().toString(),
                    appointment.getTime().toString(),
                    appointment.getPets().size()
            );
            messagingTemplate.convertAndSend("/topic/slots", message);
        }
    }

    @Transactional
    public Map<String, Object> cancelAppointment(Long appointmentId, String reason) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Lịch hẹn không tồn tại: " + appointmentId));

        // Kiểm tra trạng thái trước khi hủy
        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new IllegalStateException("Lịch hẹn #" + appointmentId + " đã bị hủy trước đó");
        }
        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new IllegalStateException("Lịch hẹn #" + appointmentId + " đã hoàn thành, không thể hủy");
        }

        // Kiểm tra thời gian hủy
        LocalDateTime appointmentTime = LocalDateTime.of(appointment.getDate(), appointment.getTime());
        LocalDateTime now = LocalDateTime.now();
        long hoursUntilAppointment = ChronoUnit.HOURS.between(now, appointmentTime);
        boolean isBefore12Hours = hoursUntilAppointment >= 12;

        // Tính toán hoàn tiền
        double refundAmount = 0;
        double nonRefundedDeposit = 0;
        if (isBefore12Hours) {
            // Hoàn toàn bộ tiền
            refundAmount = appointment.getPaidAmount(); // depositAmount hoặc totalAmount
        } else {
            // Sau 12 tiếng
            nonRefundedDeposit = appointment.getDepositAmount();
            if (appointment.getPaidAmount() > appointment.getDepositAmount()) {
                refundAmount = appointment.getPaidAmount() - appointment.getDepositAmount();
            }
        }

        // Lưu giao dịch
        if (nonRefundedDeposit > 0) {
            Transaction nonRefundedTransaction = new Transaction();
            nonRefundedTransaction.setAppointment(appointment);
            nonRefundedTransaction.setAmount(nonRefundedDeposit);
            nonRefundedTransaction.setType(TransactionType.NON_REFUNDED_DEPOSIT);
            nonRefundedTransaction.setStatus(TransactionStatus.COMPLETED);
            transactionRepository.save(nonRefundedTransaction);
        }
        if (refundAmount > 0) {
            Transaction refundTransaction = new Transaction();
            refundTransaction.setAppointment(appointment);
            refundTransaction.setAmount(refundAmount);
            refundTransaction.setType(TransactionType.REFUNDED);
            refundTransaction.setStatus(TransactionStatus.COMPLETED);
            transactionRepository.save(refundTransaction);
        }

        // Cập nhật trạng thái và lý do hủy
        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancelReason(reason);
        appointmentRepository.save(appointment);

        // Cập nhật slot
        LocalDate date = appointment.getDate();
        LocalTime time = appointment.getTime();
        AppointmentSlot slot = appointmentSlotRepository.findByDateAndTime(date, time)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy slot cho lịch hẹn #" + appointmentId));
        int slotsToFree = appointment.getPets().size();
        slot.setBookedSlots(slot.getBookedSlots() - slotsToFree);
        slot.setAvailableSlots(slot.getAvailableSlots() + slotsToFree);
        appointmentSlotRepository.save(slot);

        // Gửi thông báo WebSocket
        SlotUpdateMessage slotMessage = new SlotUpdateMessage(
                date.toString(),
                time.toString(),
                slotsToFree
        );
        messagingTemplate.convertAndSend("/topic/slots", slotMessage);

        // Gửi thông báo hủy lịch
        Map<String, Object> cancelMessage = new HashMap<>();
        cancelMessage.put("type", "APPOINTMENT_CANCELLED");
        cancelMessage.put("appointmentId", appointmentId);
        cancelMessage.put("date", date.toString());
        cancelMessage.put("time", time.toString());
        cancelMessage.put("petCount", slotsToFree);
        cancelMessage.put("refundAmount", refundAmount);
        cancelMessage.put("nonRefundedDeposit", nonRefundedDeposit);
        messagingTemplate.convertAndSend("/topic/appointments", cancelMessage);

        // Trả về thông tin hủy
        Map<String, Object> response = new HashMap<>();
        response.put("appointmentId", appointmentId);
        response.put("status", "CANCELLED");
        response.put("refundAmount", refundAmount);
        response.put("nonRefundedDeposit", nonRefundedDeposit);
        response.put("message", "Hủy lịch hẹn thành công");
        return response;
    }

    @Transactional
    public List<Map<String, Object>> cancelAppointments(List<Long> appointmentIds, String reason) {
        List<Map<String, Object>> responses = new ArrayList<>();
        for (Long appointmentId : appointmentIds) {
            try {
                Map<String, Object> response = cancelAppointment(appointmentId, reason);
                responses.add(response);
            } catch (Exception e) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("appointmentId", appointmentId);
                errorResponse.put("status", "FAILED");
                errorResponse.put("message", e.getMessage());
                responses.add(errorResponse);
            }
        }
        return responses;
    }

    public List<AppointmentResponse> getConfirmedAppointmentsByDateAndTime(LocalDate date, LocalTime time) {
        List<Appointment> appointments = appointmentRepository.findByDateAndTimeAndStatus(date, time, AppointmentStatus.CONFIRMED);
        return appointments.stream().map(appointment -> new AppointmentResponse(
                appointment.getAppointmentId(),
                appointment.getCustomerName(),
                appointment.getPhone(),
                appointment.getDate().toString(),
                appointment.getTime().toString(),
                appointment.getPaidAmount(),
                appointment.getTotalAmount(),
                appointment.getDepositAmount(),
                appointment.getPets().size()
        )).collect(Collectors.toList());
    }

    public List<AppointmentResponse> getConfirmedAppointmentsByDate(String date) {
        LocalDate localDate = LocalDate.parse(date);
        List<Appointment> appointments = appointmentRepository.findByDateAndStatus(localDate, AppointmentStatus.CONFIRMED);
        return appointments.stream().map(appointment -> new AppointmentResponse(
                appointment.getAppointmentId(),
                appointment.getCustomerName(),
                appointment.getPhone(),
                appointment.getDate().toString(),
                appointment.getTime().toString(),
                appointment.getPaidAmount(),
                appointment.getTotalAmount(),
                appointment.getDepositAmount(),
                appointment.getPets().size()
        )).collect(Collectors.toList());
    }
}