package org.example.petcarebe.service;

import org.example.petcarebe.dto.AppointmentRequest;
import org.example.petcarebe.dto.AppointmentResponse;
import org.example.petcarebe.dto.PetResponse;
import org.example.petcarebe.dto.SlotUpdateMessage;
import org.example.petcarebe.dto.NewAppointmentMessage;
import org.example.petcarebe.enums.*;
import org.example.petcarebe.model.Appointment;
import org.example.petcarebe.model.AppointmentHistory;
import org.example.petcarebe.model.AppointmentSlot;
import org.example.petcarebe.model.DefaultTimeSlot;
import org.example.petcarebe.model.Pet;
import org.example.petcarebe.model.PetService;
import org.example.petcarebe.model.PetWeight;
import org.example.petcarebe.model.Transaction;
import org.example.petcarebe.model.User;
import org.example.petcarebe.model.Employee;
import org.example.petcarebe.repository.AppointmentRepository;
import org.example.petcarebe.repository.AppointmentSlotRepository;
import org.example.petcarebe.repository.AppointmentHistoryRepository;
import org.example.petcarebe.repository.DefaultTimeSlotRepository;
import org.example.petcarebe.repository.PetServiceRepository;
import org.example.petcarebe.repository.PetWeightRepository;
import org.example.petcarebe.repository.TransactionRepository;
import org.example.petcarebe.repository.UserRepository;
import org.example.petcarebe.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

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
    private AppointmentHistoryService appointmentHistoryService;

    @Autowired
    private AppointmentHistoryRepository appointmentHistoryRepository;

    @Autowired
    private WebSocketService webSocketService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private PetManagementService petService;

    @Autowired
    private OrderSpaService orderSpaService;

    @Transactional
    public Appointment createAppointment(AppointmentRequest request) {
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

        LocalDate date;
        LocalTime time;
        try {
            date = LocalDate.parse(request.getDate());
            time = LocalTime.parse(request.getTime());
            System.out.println("Parsed date: " + date + ", time: " + time);
        } catch (Exception e) {
            System.err.println("Error parsing date/time: " + e.getMessage());
            throw new IllegalArgumentException("Định dạng ngày hoặc giờ không hợp lệ: " + e.getMessage());
        }

        DefaultTimeSlot defaultSlot = defaultTimeSlotRepo.findByTime(time)
                .orElseThrow(() -> new IllegalArgumentException("Khung giờ không tồn tại: " + time));

        AppointmentSlot slot = appointmentSlotRepository.findByDateAndTime(date, time)
                .orElseGet(() -> {
                    AppointmentSlot newSlot = new AppointmentSlot();
                    newSlot.setDate(date);
                    newSlot.setTime(time);
                    newSlot.setTotalSlots(defaultSlot.getTotalSlots());
                    newSlot.setBookedSlots(0);
                    newSlot.setAvailableSlots(defaultSlot.getTotalSlots());
                    newSlot.setIsActive(true);
                    newSlot.setDefaultTimeSlot(defaultSlot);
                    return appointmentSlotRepository.save(newSlot);
                });

        int actualBookedSlots = 0;
        for (AppointmentStatus status : List.of(AppointmentStatus.PAID, AppointmentStatus.CONFIRMED)) {
            List<Appointment> appointments = appointmentRepository.findByDateAndTimeAndStatus(date, time, status);
            int statusBookedSlots = 0;
            for (Appointment appt : appointments) {
                statusBookedSlots += appt.getPets() != null ? appt.getPets().size() : 0;
            }
            actualBookedSlots += statusBookedSlots;
        }

        slot.setBookedSlots(actualBookedSlots);
        slot.setAvailableSlots(slot.getTotalSlots() - actualBookedSlots);
        appointmentSlotRepository.save(slot);

        int requiredSlots = request.getPets().size();
        System.out.println("Requested slots: " + requiredSlots + ", Available slots: " + slot.getAvailableSlots() +
                ", Total slots: " + slot.getTotalSlots() + ", Booked slots: " + slot.getBookedSlots());

        if (slot.getAvailableSlots() < requiredSlots) {
            System.err.println("Không đủ slot trống cho khung giờ " + time + " ngày " + date +
                    ". Needed: " + requiredSlots + ", Available: " + slot.getAvailableSlots());
            throw new IllegalArgumentException("Không đủ slot trống cho khung giờ này");
        }

        Appointment appointment = new Appointment();
        appointment.setCustomerName(request.getCustomerName());
        appointment.setPhone(request.getPhone());
        appointment.setStatus(AppointmentStatus.PAID);
        appointment.setDate(date);
        appointment.setTime(time);
        appointment.setAppointmentSlots(new ArrayList<AppointmentSlot>());
        appointment.setPets(new ArrayList<Pet>());

        String paymentType = request.getPaymentType();
        double depositAmount = 50000 * requiredSlots;
        double totalAmount = request.getTotalAmount();
        double paidAmount;

        if (paymentType != null && paymentType.equals("full")) {
            paidAmount = totalAmount;
            depositAmount = 50000 * requiredSlots;
        } else {
            paidAmount = request.getDepositAmount();
            depositAmount = paidAmount;
        }

        appointment.setDepositAmount(depositAmount);
        appointment.setTotalAmount(totalAmount);
        appointment.setPaidAmount(paidAmount);

        List<Pet> pets = new ArrayList<Pet>();
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
            pet.setDepositAmount(50000d);
            pet.setPaidAmount(paymentType != null && paymentType.equals("full") ? pet.getPrice() : 50000d);
            pets.add(pet);
        }
        appointment.getPets().addAll(pets);

        slot.setAppointment(appointment);
        appointment.getAppointmentSlots().add(slot);

        slot.setBookedSlots(slot.getBookedSlots() + request.getPets().size());
        slot.setAvailableSlots(slot.getTotalSlots() - slot.getBookedSlots());

        Appointment savedAppointment = appointmentRepository.save(appointment);
        appointmentSlotRepository.save(slot);

        Transaction depositTransaction = new Transaction();
        depositTransaction.setAppointment(savedAppointment);
        depositTransaction.setAmount(paidAmount);
        depositTransaction.setType(paymentType != null && paymentType.equals("full") ? TransactionType.PAYMENT : TransactionType.DEPOSIT);
        depositTransaction.setStatus(TransactionStatus.COMPLETED);

        String paymentMethodStr = request.getPaymentMethod();
        if (paymentMethodStr != null && Arrays.stream(PaymentMethod.values())
                .anyMatch(e -> e.name().equals(paymentMethodStr))) {
            depositTransaction.setPaymentMethod(PaymentMethod.valueOf(paymentMethodStr));
        } else {
            throw new IllegalArgumentException("Phương thức thanh toán không hợp lệ hoặc thiếu");
        }

        String paymentChannelStr = request.getPaymentChannel();
        if (paymentChannelStr != null) {
            try {
                depositTransaction.setPaymentChannel(PaymentChannel.valueOf(paymentChannelStr));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Kênh thanh toán không hợp lệ: " + paymentChannelStr);
            }
        }
        transactionRepository.save(depositTransaction);

        SlotUpdateMessage slotMessage = new SlotUpdateMessage(
                request.getDate(),
                request.getTime(),
                request.getPets().size()
        );
        webSocketService.sendToTopic("/topic/slots", slotMessage.toString());

        Map<String, Object> messagePayload = new HashMap<String, Object>();
        messagePayload.put("type", "NEW_APPOINTMENT");
        messagePayload.put("appointment", new NewAppointmentMessage(
                savedAppointment.getAppointmentId(),
                savedAppointment.getCustomerName(),
                savedAppointment.getDate().toString(),
                savedAppointment.getTime().toString(),
                savedAppointment.getPaidAmount()
        ));
        webSocketService.sendToTopic("/topic/new-appointment", messagePayload.toString());

        return savedAppointment;
    }

    @Transactional
    public Appointment updateAppointment(Long appointmentId, AppointmentRequest request, Long userId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Lịch hẹn không tồn tại: " + appointmentId));

        if (appointment.getStatus() != AppointmentStatus.PAID) {
            throw new IllegalStateException("Chỉ có thể chuyển lịch hẹn ở trạng thái PAID");
        }

        LocalDate newDate;
        LocalTime newTime;
        try {
            newDate = LocalDate.parse(request.getDate());
            newTime = LocalTime.parse(request.getTime());
        } catch (Exception e) {
            throw new IllegalArgumentException("Định dạng ngày hoặc giờ không hợp lệ");
        }

        if (newDate.equals(appointment.getDate()) && newTime.equals(appointment.getTime())) {
            throw new IllegalArgumentException("Ngày và giờ mới phải khác với hiện tại");
        }

        int requiredSlots = appointment.getPets() != null ? appointment.getPets().size() : 0;
        AppointmentSlot newSlot = appointmentSlotRepository.findByDateAndTime(newDate, newTime)
                .orElseGet(() -> {
                    DefaultTimeSlot defaultSlot = defaultTimeSlotRepo.findByTime(newTime)
                            .orElseThrow(() -> new IllegalArgumentException("Khung giờ không tồn tại: " + newTime));
                    AppointmentSlot slot = new AppointmentSlot();
                    slot.setDate(newDate);
                    slot.setTime(newTime);
                    slot.setTotalSlots(defaultSlot.getTotalSlots());
                    slot.setBookedSlots(0);
                    slot.setAvailableSlots(defaultSlot.getTotalSlots());
                    slot.setIsActive(true);
                    slot.setDefaultTimeSlot(defaultSlot);
                    appointmentSlotRepository.save(slot);
                    return slot;
                });

        int newSlotBookedSlots = 0;
        for (AppointmentStatus status : List.of(AppointmentStatus.PAID, AppointmentStatus.CONFIRMED)) {
            List<Appointment> appointments = appointmentRepository.findByDateAndTimeAndStatus(newDate, newTime, status);
            for (Appointment appt : appointments) {
                newSlotBookedSlots += appt.getPets() != null ? appt.getPets().size() : 0;
            }
        }
        newSlot.setBookedSlots(newSlotBookedSlots);
        newSlot.setAvailableSlots(newSlot.getTotalSlots() - newSlotBookedSlots);
        appointmentSlotRepository.save(newSlot);

        if (newSlot.getAvailableSlots() < requiredSlots) {
            throw new IllegalArgumentException("Không đủ slot trống cho khung giờ mới");
        }

        AppointmentSlot oldSlot = appointmentSlotRepository.findByDateAndTime(appointment.getDate(), appointment.getTime())
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy slot cũ cho lịch hẹn #" + appointmentId));

        oldSlot.setBookedSlots(oldSlot.getBookedSlots() - requiredSlots);
        oldSlot.setAvailableSlots(oldSlot.getTotalSlots() - oldSlot.getBookedSlots());
        oldSlot.setAppointment(null);
        appointmentSlotRepository.save(oldSlot);
        appointmentSlotRepository.flush();

        if (newSlot.getAppointment() != null) {
            newSlot.setAppointment(null);
            appointmentSlotRepository.save(newSlot);
            appointmentSlotRepository.flush();
        }
        newSlot.setBookedSlots(newSlot.getBookedSlots() + requiredSlots);
        newSlot.setAvailableSlots(newSlot.getTotalSlots() - newSlot.getBookedSlots());
        newSlot.setAppointment(appointment);
        appointmentSlotRepository.save(newSlot);

        AppointmentStatus oldStatus = appointment.getStatus();
        appointment.setDate(newDate);
        appointment.setTime(newTime);

        if (appointment.getAppointmentSlots() == null) {
            appointment.setAppointmentSlots(new ArrayList<AppointmentSlot>());
        }

        appointment.getAppointmentSlots().clear();
        appointment.getAppointmentSlots().add(newSlot);
        Appointment updatedAppointment = appointmentRepository.save(appointment);

        appointmentHistoryService.logAction(
                appointmentId,
                userId,
                "RESCHEDULE",
                oldStatus,
                appointment.getStatus(),
                request.getNote() != null ? request.getNote() : "Chuyển lịch hẹn"
        );

        SlotUpdateMessage oldSlotMessage = new SlotUpdateMessage(
                oldSlot.getDate().toString(),
                oldSlot.getTime().toString(),
                -requiredSlots
        );
        webSocketService.sendToTopic("/topic/slots", oldSlotMessage.toString());

        SlotUpdateMessage newSlotMessage = new SlotUpdateMessage(
                newDate.toString(),
                newTime.toString(),
                requiredSlots
        );
        webSocketService.sendToTopic("/topic/slots", newSlotMessage.toString());

        Map<String, Object> updateMessage = new HashMap<String, Object>();
        updateMessage.put("type", "APPOINTMENT_UPDATED");
        updateMessage.put("appointmentId", appointmentId);
        updateMessage.put("date", newDate.toString());
        updateMessage.put("time", newTime.toString());
        updateMessage.put("petCount", requiredSlots);
        webSocketService.sendToTopic("/topic/appointments", updateMessage.toString());

        return updatedAppointment;
    }

    public List<AppointmentResponse> getPendingAppointments() {
        try {
            List<Appointment> pendingAppointments = appointmentRepository.findByStatus(AppointmentStatus.PAID);
            List<AppointmentResponse> responses = new ArrayList<AppointmentResponse>();
            for (Appointment appointment : pendingAppointments) {
                if (appointment == null) {
                    System.err.println("Found null appointment in pending appointments");
                    continue;
                }
                AppointmentResponse response = new AppointmentResponse(
                        appointment.getAppointmentId(),
                        appointment.getCustomerName(),
                        appointment.getPhone(),
                        appointment.getDate() != null ? appointment.getDate().toString() : null,
                        appointment.getTime() != null ? appointment.getTime().toString() : null,
                        appointment.getPaidAmount(),
                        appointment.getTotalAmount(),
                        appointment.getDepositAmount(),
                        appointment.getPets() != null ? appointment.getPets().size() : 0
                );
                response.setStatus(appointment.getStatus() != null ? appointment.getStatus().toString() : "UNKNOWN");
                responses.add(response);
            }
            return responses;
        } catch (Exception e) {
            System.err.println("Error fetching pending appointments: " + e.getMessage());
            throw new RuntimeException("Không thể tải danh sách lịch hẹn: " + e.getMessage());
        }
    }

    public List<PetResponse> getPetsByAppointmentId(Long appointmentId) {
        try {
            Appointment appointment = appointmentRepository.findById(appointmentId)
                    .orElseThrow(() -> new IllegalArgumentException("Lịch hẹn không tồn tại: " + appointmentId));

            List<PetResponse> responses = new ArrayList<>();
            List<Pet> pets = appointment.getPets() != null ? appointment.getPets() : new ArrayList<>();
            for (Pet pet : pets) {
                if (pet == null) {
                    System.err.println("Found null pet in appointment ID: " + appointmentId);
                    continue;
                }
                PetResponse petResponse = new PetResponse(
                        pet.getId(),
                        pet.getNamePet() != null ? pet.getNamePet() : "Không có tên",
                        pet.getPetType() != null ? pet.getPetType().toString() : "Không xác định",
                        pet.getAge(),
                        pet.getPetWeight() != null ? pet.getPetWeight().getWeightRange() : "Không xác định",
                        pet.getPetService() != null ? pet.getPetService().getServiceName() : "Không có dịch vụ",
                        pet.getPetService() != null ? pet.getPetService().getId() : null,
                        pet.getPrice(),
                        pet.getWeightUpdateCount(),
                        pet.getEmployee() != null ? pet.getEmployee().getEmployeeId() : null,
                        pet.getEmployee() != null ? pet.getEmployee().getFullName() : null,
                        pet.getPaidAmount()
                );
                responses.add(petResponse);
            }
            return responses;
        } catch (IllegalArgumentException e) {
            System.err.println("Error fetching pets by appointment ID " + appointmentId + ": " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("Unexpected error fetching pets by appointment ID " + appointmentId + ": " + e.getMessage());
            throw new RuntimeException("Không thể tải thông tin thú cưng: " + e.getMessage());
        }
    }

    @Transactional
    public void confirmAppointments(List<Long> appointmentIds, Long userId) {
        for (Long appointmentId : appointmentIds) {
            Appointment appointment = appointmentRepository.findById(appointmentId)
                    .orElseThrow(() -> new IllegalArgumentException("Lịch hẹn không tồn tại: " + appointmentId));
            if (appointment.getStatus() != AppointmentStatus.PAID) {
                throw new IllegalStateException("Lịch hẹn #" + appointmentId + " không ở trạng thái PAID");
            }
            AppointmentStatus oldStatus = appointment.getStatus();
            appointment.setStatus(AppointmentStatus.CONFIRMED);
            appointmentRepository.save(appointment);

            appointmentHistoryService.logAction(
                    appointmentId,
                    userId,
                    "CONFIRM",
                    oldStatus,
                    AppointmentStatus.CONFIRMED,
                    "Xác nhận lịch hẹn"
            );

            Map<String, Object> confirmMessage = new HashMap<>();
            confirmMessage.put("type", "APPOINTMENT_CONFIRMED");
            confirmMessage.put("appointmentId", appointmentId);
            confirmMessage.put("date", appointment.getDate() != null ? appointment.getDate().toString() : null);
            confirmMessage.put("time", appointment.getTime() != null ? appointment.getTime().toString() : null);
            webSocketService.sendToTopic("/topic/appointments", confirmMessage.toString());
        }
    }

    @Transactional
    public void startService(Long appointmentId, Long userId, Map<Long, Long> petAssignments) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Lịch hẹn không tồn tại: " + appointmentId));

        if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new IllegalStateException("Lịch hẹn #" + appointmentId + " không ở trạng thái CONFIRMED");
        }

        AppointmentStatus oldStatus = appointment.getStatus();
        appointment.setStatus(AppointmentStatus.IN_PROGRESS);
        appointmentRepository.save(appointment);

        List<Pet> pets = appointment.getPets();
        for (Pet pet : pets) {
            Long staffId = petAssignments.get(pet.getId());
            if (staffId == null) {
                throw new IllegalArgumentException("Chưa gán nhân viên cho thú cưng #" + pet.getId());
            }
            Employee employee = employeeRepository.findById(staffId)
                    .orElseThrow(() -> new IllegalArgumentException("Nhân viên không tồn tại: " + staffId));
            pet.setEmployee(employee);
        }
        appointmentRepository.save(appointment);

        appointmentHistoryService.logAction(
                appointmentId,
                userId,
                "START_SERVICE",
                oldStatus,
                AppointmentStatus.IN_PROGRESS,
                "Bắt đầu dịch vụ"
        );

        Map<String, Object> startMessage = new HashMap<>();
        startMessage.put("type", "APPOINTMENT_STARTED");
        startMessage.put("appointmentId", appointmentId);
        startMessage.put("status", AppointmentStatus.IN_PROGRESS.toString());
        webSocketService.sendToTopic("/topic/appointments", startMessage.toString());
    }

    @Transactional
    public void completeService(Long appointmentId, Long userId, Map<String, Object> payload) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Lịch hẹn không tồn tại: " + appointmentId));

        System.out.println("Processing payment for appointment #" + appointmentId + ", current status: " + appointment.getStatus());

        AppointmentStatus oldStatus = appointment.getStatus();
        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointmentRepository.save(appointment);

        LocalDate date = appointment.getDate();
        LocalTime time = appointment.getTime();
        AppointmentSlot slot = appointmentSlotRepository.findByDateAndTime(date, time)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy slot cho lịch hẹn #" + appointmentId));
        int slotsToFree = appointment.getPets() != null ? appointment.getPets().size() : 0;
        slot.setBookedSlots(slot.getBookedSlots() - slotsToFree);
        slot.setAvailableSlots(slot.getTotalSlots() - slot.getBookedSlots());
        appointmentSlotRepository.save(slot);

        processPaymentAndOrder(appointmentId, userId, payload);

        appointmentHistoryService.logAction(
                appointmentId,
                userId,
                "COMPLETE_SERVICE",
                oldStatus,
                AppointmentStatus.COMPLETED,
                "Hoàn thành dịch vụ và thanh toán"
        );

        Map<String, Object> completeMessage = new HashMap<>();
        completeMessage.put("type", "APPOINTMENT_COMPLETED");
        completeMessage.put("appointmentId", appointmentId);
        completeMessage.put("status", AppointmentStatus.COMPLETED.toString());
        webSocketService.sendToTopic("/topic/appointments", completeMessage.toString());

        SlotUpdateMessage slotMessage = new SlotUpdateMessage(
                date.toString(),
                time.toString(),
                -slotsToFree
        );
        webSocketService.sendToTopic("/topic/slots", slotMessage.toString());
    }

    @Transactional
    private void processPaymentAndOrder(Long appointmentId, Long userId, Map<String, Object> payload) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Lịch hẹn không tồn tại: " + appointmentId));

        // Kiểm tra dữ liệu appointment
        if (appointment.getTotalAmount() <= 0) {
            throw new IllegalStateException("Tổng số tiền của lịch hẹn #" + appointmentId + " không hợp lệ: " + appointment.getTotalAmount());
        }

        List<Pet> pets = appointment.getPets();
        if (pets == null || pets.isEmpty()) {
            throw new IllegalStateException("Lịch hẹn #" + appointmentId + " không có thú cưng nào để tạo hóa đơn");
        }

        double amountToRecord = ((Number) payload.get("amount")).doubleValue();
        String requestedPaymentMethod = (String) payload.get("paymentMethod");
        String paymentChannel = (String) payload.get("paymentChannel");

        // Xác định paymentMethod
        String finalPaymentMethod;
        if (amountToRecord > 0) {
            // Trường hợp có số tiền còn lại cần thanh toán
            if ("CASH".equalsIgnoreCase(requestedPaymentMethod)) {
                finalPaymentMethod = "MIXED"; // Vì cọc là ONLINE, thanh toán còn lại bằng CASH
            } else {
                finalPaymentMethod = "ONLINE"; // Chuyển khoản, MoMo, VNPay
            }
        } else {
            // Trường hợp đã thanh toán toàn bộ trước đó
            finalPaymentMethod = "ONLINE"; // Thanh toán toàn bộ ở bước đặt lịch là ONLINE
        }

        // Tạo Transaction chỉ khi có chênh lệch
        if (amountToRecord > 0) {
            Transaction paymentTransaction = new Transaction();
            paymentTransaction.setAppointment(appointment);
            paymentTransaction.setAmount(amountToRecord);
            paymentTransaction.setType(TransactionType.PAYMENT);
            paymentTransaction.setStatus(TransactionStatus.COMPLETED);
            paymentTransaction.setPaymentMethod(PaymentMethod.valueOf(finalPaymentMethod));
            if (paymentChannel != null) {
                paymentTransaction.setPaymentChannel(PaymentChannel.valueOf(paymentChannel));
            }
            transactionRepository.save(paymentTransaction);
        }

        // Tạo Orders và OrderSpa
        orderSpaService.createSpaOrder(appointmentId, userId, finalPaymentMethod, paymentChannel);
    }

    @Transactional
    public void completeService(Long appointmentId, Long userId) {
        throw new UnsupportedOperationException("Phương thức này không được sử dụng. Vui lòng sử dụng completeService với payload.");
    }

    public AppointmentResponse getAppointmentById(Long appointmentId) {
        try {
            Appointment appointment = appointmentRepository.findById(appointmentId)
                    .orElseThrow(() -> new IllegalArgumentException("Lịch hẹn không tồn tại: " + appointmentId));
            AppointmentResponse response = new AppointmentResponse(
                    appointment.getAppointmentId(),
                    appointment.getCustomerName(),
                    appointment.getPhone(),
                    appointment.getDate() != null ? appointment.getDate().toString() : null,
                    appointment.getTime() != null ? appointment.getTime().toString() : null,
                    appointment.getPaidAmount(),
                    appointment.getTotalAmount(),
                    appointment.getDepositAmount(),
                    appointment.getPets() != null ? appointment.getPets().size() : 0
            );
            response.setStatus(appointment.getStatus() != null ? appointment.getStatus().toString() : "UNKNOWN");
            return response;
        } catch (IllegalArgumentException e) {
            System.err.println("Error fetching appointment by ID " + appointmentId + ": " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("Unexpected error fetching appointment by ID " + appointmentId + ": " + e.getMessage());
            throw new RuntimeException("Không thể tải thông tin lịch hẹn: " + e.getMessage());
        }
    }

    private PaymentMethod getPaymentMethodFromDepositTransaction(Appointment appointment) {
        List<Transaction> transactions = transactionRepository.findByAppointmentAppointmentId(appointment.getAppointmentId());
        for (Transaction transaction : transactions) {
            if (transaction.getType() == TransactionType.DEPOSIT) {
                return transaction.getPaymentMethod();
            }
        }
        return PaymentMethod.ONLINE;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void saveCancellationTransactions(Appointment appointment, double nonRefundedDeposit, double refundAmount) {
        try {
            System.out.println("Saving cancellation transactions for appointment #" + appointment.getAppointmentId());

            PaymentMethod paymentMethod = getPaymentMethodFromDepositTransaction(appointment);

            if (nonRefundedDeposit > 0) {
                Transaction nonRefundedTransaction = new Transaction();
                nonRefundedTransaction.setAppointment(appointment);
                nonRefundedTransaction.setAmount(nonRefundedDeposit);
                nonRefundedTransaction.setType(TransactionType.NON_REFUNDED_DEPOSIT);
                nonRefundedTransaction.setStatus(TransactionStatus.COMPLETED);
                nonRefundedTransaction.setPaymentMethod(paymentMethod);
                nonRefundedTransaction.setPaymentChannel(null);
                transactionRepository.save(nonRefundedTransaction);
                System.out.println("Đã lưu giao dịch không hoàn cọc: " + nonRefundedDeposit);
            }

            if (refundAmount > 0) {
                Transaction refundTransaction = new Transaction();
                refundTransaction.setAppointment(appointment);
                refundTransaction.setAmount(refundAmount);
                refundTransaction.setType(TransactionType.REFUNDED);
                refundTransaction.setStatus(TransactionStatus.PENDING);
                refundTransaction.setPaymentMethod(paymentMethod);
                refundTransaction.setPaymentChannel(null);
                transactionRepository.save(refundTransaction);
                System.out.println("Đã lưu giao dịch hoàn tiền: " + refundAmount);
            }
        } catch (Exception e) {
            System.err.println("Error saving cancellation transactions for appointment #" + appointment.getAppointmentId() + ": " + e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private AppointmentResponse cancelAppointmentsForPaid(Long appointmentId, String reason, Long userId, Appointment appointment) {
        try {
            System.out.println("Bắt đầu hủy lịch hẹn PAID #" + appointmentId);

            LocalDateTime appointmentTime = LocalDateTime.of(
                    appointment.getDate() != null ? appointment.getDate() : LocalDate.now(),
                    appointment.getTime() != null ? appointment.getTime() : LocalTime.now()
            );
            LocalDateTime now = LocalDateTime.now();
            long hoursUntilAppointment = ChronoUnit.HOURS.between(now, appointmentTime);
            boolean isBefore12Hours = hoursUntilAppointment >= 12;

            double refundAmount = 0;
            double nonRefundedDeposit = 0;
            if (isBefore12Hours) {
                refundAmount = appointment.getPaidAmount();
            } else {
                nonRefundedDeposit = appointment.getDepositAmount();
                if (appointment.getPaidAmount() > appointment.getDepositAmount()) {
                    refundAmount = appointment.getPaidAmount() - appointment.getDepositAmount();
                }
            }
            System.out.println("Hoàn tiền: " + refundAmount + ", Không hoàn cọc: " + nonRefundedDeposit);

            AppointmentStatus oldStatus = appointment.getStatus();
            appointment.setStatus(AppointmentStatus.CANCELLED);
            appointment.setCancelReason(reason);
            appointmentRepository.save(appointment);
            System.out.println("Đã cập nhật trạng thái lịch hẹn thành CANCELLED");

            LocalDate date = appointment.getDate();
            LocalTime time = appointment.getTime();
            AppointmentSlot slot = appointmentSlotRepository.findByDateAndTime(date, time)
                    .orElseThrow(() -> new IllegalStateException("Không tìm thấy slot cho lịch hẹn #" + appointmentId));
            int slotsToFree = appointment.getPets() != null ? appointment.getPets().size() : 0;
            slot.setBookedSlots(slot.getBookedSlots() - slotsToFree);
            slot.setAvailableSlots(slot.getTotalSlots() - slot.getBookedSlots());
            slot.setAppointment(null);
            appointmentSlotRepository.save(slot);
            System.out.println("Đã cập nhật slot: bookedSlots = " + slot.getBookedSlots() + ", availableSlots = " + slot.getAvailableSlots());

            String employeeName = "Quản trị viên";
            try {
                User employee = userRepository.findById(userId)
                        .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhân viên với ID: " + userId));
                employeeName = employee.getFullName() != null ? employee.getFullName() : "Quản trị viên";
                System.out.println("Tên nhân viên: " + employeeName);
            } catch (Exception e) {
                System.err.println("Error fetching employee name for userId " + userId + ": " + e.getMessage());
            }

            String historyReason = "Hủy bởi " + employeeName;
            try {
                System.out.println("Bắt đầu ghi lịch sử hành động...");
                appointmentHistoryService.logAction(
                        appointmentId,
                        userId,
                        "CANCEL",
                        oldStatus,
                        AppointmentStatus.CANCELLED,
                        historyReason
                );
                System.out.println("Đã ghi lịch sử hành động thành công");
            } catch (Exception e) {
                System.err.println("Error logging action for appointment #" + appointmentId + ": " + e.getMessage());
            }

            SlotUpdateMessage slotMessage = new SlotUpdateMessage(
                    date.toString(),
                    time.toString(),
                    -slotsToFree
            );
            Map<String, Object> cancelMessage = new HashMap<>();
            cancelMessage.put("type", "APPOINTMENT_CANCELLED");
            cancelMessage.put("appointmentId", appointmentId);
            cancelMessage.put("date", date.toString());
            cancelMessage.put("time", time.toString());
            cancelMessage.put("petCount", slotsToFree);
            cancelMessage.put("refundAmount", refundAmount);
            cancelMessage.put("nonRefundedDeposit", nonRefundedDeposit);

            try {
                webSocketService.sendToTopic("/topic/slots", slotMessage.toString());
                webSocketService.sendToTopic("/topic/appointments", cancelMessage.toString());
                System.out.println("Đã gửi thông báo WebSocket");
            } catch (Exception e) {
                System.err.println("Error sending WebSocket message: " + e.getMessage());
            }

            saveCancellationTransactions(appointment, nonRefundedDeposit, refundAmount);

            AppointmentResponse response = new AppointmentResponse(
                    appointmentId,
                    appointment.getCustomerName(),
                    appointment.getPhone(),
                    appointment.getDate() != null ? appointment.getDate().toString() : null,
                    appointment.getTime() != null ? appointment.getTime().toString() : null,
                    refundAmount,
                    nonRefundedDeposit,
                    null,
                    null,
                    null,
                    appointment.getCancelReason()
            );
            response.setStatus("CANCELLED");
            response.setMessage("Hủy lịch hẹn thành công");
            return response;
        } catch (Exception e) {
            System.err.println("Error canceling PAID appointment #" + appointmentId + ": " + e.getMessage());
            throw new RuntimeException("Không thể hủy lịch hẹn: " + e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private AppointmentResponse cancelAppointmentsForConfirmed(Long appointmentId, String reason, Long userId, Appointment appointment) {
        try {
            System.out.println("Bắt đầu hủy lịch hẹn CONFIRMED #" + appointmentId);

            LocalDateTime appointmentTime = LocalDateTime.of(
                    appointment.getDate() != null ? appointment.getDate() : LocalDate.now(),
                    appointment.getTime() != null ? appointment.getTime() : LocalTime.now()
            );
            LocalDateTime now = LocalDateTime.now();
            long hoursUntilAppointment = ChronoUnit.HOURS.between(now, appointmentTime);
            boolean isBefore12Hours = hoursUntilAppointment >= 12;

            double refundAmount = 0;
            double nonRefundedDeposit = 0;
            if (isBefore12Hours) {
                refundAmount = appointment.getPaidAmount();
            } else {
                nonRefundedDeposit = appointment.getDepositAmount();
                if (appointment.getPaidAmount() > appointment.getDepositAmount()) {
                    refundAmount = appointment.getPaidAmount() - appointment.getDepositAmount();
                }
            }
            System.out.println("Hoàn tiền: " + refundAmount + ", Không hoàn cọc: " + nonRefundedDeposit);

            AppointmentStatus oldStatus = appointment.getStatus();
            appointment.setStatus(AppointmentStatus.CANCELLED);
            appointment.setCancelReason(reason);
            appointmentRepository.save(appointment);
            System.out.println("Đã cập nhật trạng thái lịch hẹn thành CANCELLED");

            LocalDate date = appointment.getDate();
            LocalTime time = appointment.getTime();
            AppointmentSlot slot = appointmentSlotRepository.findByDateAndTime(date, time)
                    .orElseThrow(() -> new IllegalStateException("Không tìm thấy slot cho lịch hẹn #" + appointmentId));
            int slotsToFree = appointment.getPets() != null ? appointment.getPets().size() : 0;
            slot.setBookedSlots(slot.getBookedSlots() - slotsToFree);
            slot.setAvailableSlots(slot.getTotalSlots() - slot.getBookedSlots());
            slot.setAppointment(null);
            appointmentSlotRepository.save(slot);
            System.out.println("Đã cập nhật slot: bookedSlots = " + slot.getBookedSlots() + ", availableSlots = " + slot.getAvailableSlots());

            String employeeName = "Quản trị viên";
            try {
                User employee = userRepository.findById(userId)
                        .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhân viên với ID: " + userId));
                employeeName = employee.getFullName() != null ? employee.getFullName() : "Quản trị viên";
                System.out.println("Tên nhân viên: " + employeeName);
            } catch (Exception e) {
                System.err.println("Error fetching employee name for userId " + userId + ": " + e.getMessage());
            }

            String historyReason = "Hủy bởi " + employeeName + ", lý do: Hủy bởi quản trị viên";
            try {
                System.out.println("Bắt đầu ghi lịch sử hành động...");
                appointmentHistoryService.logAction(
                        appointmentId,
                        userId,
                        "CANCEL",
                        oldStatus,
                        AppointmentStatus.CANCELLED,
                        historyReason
                );
                System.out.println("Đã ghi lịch sử hành động thành công");
            } catch (Exception e) {
                System.err.println("Error logging action for appointment #" + appointmentId + ": " + e.getMessage());
            }

            SlotUpdateMessage slotMessage = new SlotUpdateMessage(
                    date.toString(),
                    time.toString(),
                    -slotsToFree
            );
            Map<String, Object> cancelMessage = new HashMap<>();
            cancelMessage.put("type", "APPOINTMENT_CANCELLED");
            cancelMessage.put("appointmentId", appointmentId);
            cancelMessage.put("date", date.toString());
            cancelMessage.put("time", time.toString());
            cancelMessage.put("petCount", slotsToFree);
            cancelMessage.put("refundAmount", refundAmount);
            cancelMessage.put("nonRefundedDeposit", nonRefundedDeposit);

            try {
                webSocketService.sendToTopic("/topic/slots", slotMessage.toString());
                webSocketService.sendToTopic("/topic/appointments", cancelMessage.toString());
                System.out.println("Đã gửi thông báo WebSocket");
            } catch (Exception e) {
                System.err.println("Error sending WebSocket message: " + e.getMessage());
            }

            saveCancellationTransactions(appointment, nonRefundedDeposit, refundAmount);

            AppointmentResponse response = new AppointmentResponse(
                    appointmentId,
                    appointment.getCustomerName(),
                    appointment.getPhone(),
                    appointment.getDate() != null ? appointment.getDate().toString() : null,
                    appointment.getTime() != null ? appointment.getTime().toString() : null,
                    refundAmount,
                    nonRefundedDeposit,
                    null,
                    null,
                    null,
                    appointment.getCancelReason()
            );
            response.setStatus("CANCELLED");
            response.setMessage("Hủy lịch hẹn thành công");
            return response;
        } catch (Exception e) {
            System.err.println("Error canceling CONFIRMED appointment #" + appointmentId + ": " + e.getMessage());
            throw new RuntimeException("Không thể hủy lịch hẹn: " + e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<AppointmentResponse> cancelAppointments(List<Long> appointmentIds, String reason, Long userId) {
        List<AppointmentResponse> responses = new ArrayList<>();
        for (Long appointmentId : appointmentIds) {
            try {
                Appointment appointment = appointmentRepository.findById(appointmentId)
                        .orElseThrow(() -> new IllegalArgumentException("Lịch hẹn không tồn tại: " + appointmentId));

                if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
                    throw new IllegalStateException("Lịch hẹn #" + appointmentId + " đã bị hủy trước đó");
                }
                if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
                    throw new IllegalStateException("Lịch hẹn #" + appointmentId + " đã hoàn thành, không thể hủy");
                }
                if (appointment.getStatus() == AppointmentStatus.IN_PROGRESS) {
                    throw new IllegalStateException("Lịch hẹn #" + appointmentId + " đang thực hiện, không thể hủy");
                }
                if (appointment.getStatus() != AppointmentStatus.PAID && appointment.getStatus() != AppointmentStatus.CONFIRMED) {
                    throw new IllegalStateException("Lịch hẹn #" + appointmentId + " phải ở trạng thái PAID hoặc CONFIRMED để hủy");
                }

                AppointmentResponse response;
                if (appointment.getStatus() == AppointmentStatus.PAID) {
                    response = cancelAppointmentsForPaid(appointmentId, reason, userId, appointment);
                } else {
                    response = cancelAppointmentsForConfirmed(appointmentId, reason, userId, appointment);
                }
                responses.add(response);
            } catch (Exception e) {
                System.err.println("Error canceling appointment #" + appointmentId + ": " + e.getMessage());
                AppointmentResponse response = new AppointmentResponse(appointmentId, "FAILED", e.getMessage());
                responses.add(response);
            }
        }
        return responses;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<AppointmentResponse> cancelPaidAppointments(List<Long> appointmentIds, String reason, Long userId) {
        List<AppointmentResponse> responses = new ArrayList<>();
        for (Long appointmentId : appointmentIds) {
            try {
                Appointment appointment = appointmentRepository.findById(appointmentId)
                        .orElseThrow(() -> new IllegalArgumentException("Lịch hẹn không tồn tại: " + appointmentId));
                AppointmentResponse response = cancelAppointmentsForPaid(appointmentId, reason, userId, appointment);
                responses.add(response);
            } catch (Exception e) {
                System.err.println("Error canceling PAID appointment #" + appointmentId + ": " + e.getMessage());
                AppointmentResponse response = new AppointmentResponse(appointmentId, "FAILED", e.getMessage());
                responses.add(response);
            }
        }
        return responses;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<AppointmentResponse> cancelConfirmedAppointments(List<Long> appointmentIds, String reason, Long userId) {
        List<AppointmentResponse> responses = new ArrayList<>();
        for (Long appointmentId : appointmentIds) {
            try {
                Appointment appointment = appointmentRepository.findById(appointmentId)
                        .orElseThrow(() -> new IllegalArgumentException("Lịch hẹn không tồn tại: " + appointmentId));
                AppointmentResponse response = cancelAppointmentsForConfirmed(appointmentId, reason, userId, appointment);
                responses.add(response);
            } catch (Exception e) {
                System.err.println("Error canceling CONFIRMED appointment #" + appointmentId + ": " + e.getMessage());
                AppointmentResponse response = new AppointmentResponse(appointmentId, "FAILED", e.getMessage());
                responses.add(response);
            }
        }
        return responses;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private AppointmentResponse removePetForPaid(Long appointmentId, Long petId, Long userId, Appointment appointment, Pet petToRemove) {
        try {
            LocalDateTime appointmentTime = LocalDateTime.of(
                    appointment.getDate() != null ? appointment.getDate() : LocalDate.now(),
                    appointment.getTime() != null ? appointment.getTime() : LocalTime.now()
            );
            LocalDateTime now = LocalDateTime.now();
            long hoursUntilAppointment = ChronoUnit.HOURS.between(now, appointmentTime);
            boolean isBefore12Hours = hoursUntilAppointment >= 12;

            double petRefundAmount = 0;
            double petNonRefundedDeposit = 0;
            double petDeposit = 50000;
            double petPrice = petToRemove.getPrice();

            if (isBefore12Hours) {
                petRefundAmount = petPrice;
            } else {
                petNonRefundedDeposit = petDeposit;
                if (petPrice > petDeposit) {
                    petRefundAmount = petPrice - petDeposit;
                }
            }

            appointment.setDepositAmount(appointment.getDepositAmount() - petDeposit);
            appointment.setTotalAmount(appointment.getTotalAmount() - petPrice);

            appointment.getPets().remove(petToRemove);
            appointmentRepository.save(appointment);

            LocalDate date = appointment.getDate();
            LocalTime time = appointment.getTime();
            AppointmentSlot slot = appointmentSlotRepository.findByDateAndTime(date, time)
                    .orElseThrow(() -> new IllegalStateException("Không tìm thấy slot cho lịch hẹn #" + appointmentId));
            slot.setBookedSlots(slot.getBookedSlots() - 1);
            slot.setAvailableSlots(slot.getTotalSlots() - slot.getBookedSlots());
            appointmentSlotRepository.save(slot);

            String employeeName = "Quản trị viên";
            try {
                User employee = userRepository.findById(userId)
                        .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhân viên với ID: " + userId));
                employeeName = employee.getFullName() != null ? employee.getFullName() : "Quản trị viên";
            } catch (Exception e) {
                System.err.println("Error fetching employee name for userId " + userId + ": " + e.getMessage());
            }

            String historyReason = "Xóa thú cưng #" + petId + " khỏi lịch hẹn bởi " + employeeName;
            try {
                appointmentHistoryService.logAction(
                        appointmentId,
                        userId,
                        "REMOVE_PET",
                        appointment.getStatus(),
                        appointment.getStatus(),
                        historyReason
                );
            } catch (Exception e) {
                System.err.println("Error logging action for appointment #" + appointmentId + ": " + e.getMessage());
            }

            SlotUpdateMessage slotMessage = new SlotUpdateMessage(
                    date.toString(),
                    time.toString(),
                    -1
            );
            Map<String, Object> petRemovedMessage = new HashMap<>();
            petRemovedMessage.put("type", "PET_REMOVED");
            petRemovedMessage.put("appointmentId", appointmentId);
            petRemovedMessage.put("petId", petId);
            petRemovedMessage.put("date", date.toString());
            petRemovedMessage.put("time", time.toString());
            petRemovedMessage.put("refundAmount", petRefundAmount);
            petRemovedMessage.put("nonRefundedDeposit", petNonRefundedDeposit);

            try {
                webSocketService.sendToTopic("/topic/slots", slotMessage.toString());
                webSocketService.sendToTopic("/topic/appointments", petRemovedMessage.toString());
            } catch (Exception e) {
                System.err.println("Error sending WebSocket message: " + e.getMessage());
            }

            saveCancellationTransactions(appointment, petNonRefundedDeposit, petRefundAmount);

            return new AppointmentResponse(
                    appointment.getAppointmentId(),
                    appointment.getCustomerName(),
                    appointment.getPhone(),
                    appointment.getDate() != null ? appointment.getDate().toString() : null,
                    appointment.getTime() != null ? appointment.getTime().toString() : null,
                    petRefundAmount,
                    petNonRefundedDeposit,
                    null,
                    null,
                    null,
                    appointment.getCancelReason()
            );
        } catch (Exception e) {
            System.err.println("Error removing pet for PAID appointment #" + appointmentId + ": " + e.getMessage());
            throw new RuntimeException("Không thể xóa thú cưng khỏi lịch hẹn: " + e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private AppointmentResponse removePetForConfirmed(Long appointmentId, Long petId, Long userId, Appointment appointment, Pet petToRemove) {
        try {
            LocalDateTime appointmentTime = LocalDateTime.of(
                    appointment.getDate() != null ? appointment.getDate() : LocalDate.now(),
                    appointment.getTime() != null ? appointment.getTime() : LocalTime.now()
            );
            LocalDateTime now = LocalDateTime.now();
            long hoursUntilAppointment = ChronoUnit.HOURS.between(now, appointmentTime);
            boolean isBefore12Hours = hoursUntilAppointment >= 12;

            double petRefundAmount = 0;
            double petNonRefundedDeposit = 0;
            double petDeposit = 50000;
            double petPrice = petToRemove.getPrice();

            if (isBefore12Hours) {
                petRefundAmount = petPrice;
            } else {
                petNonRefundedDeposit = petDeposit;
                if (petPrice > petDeposit) {
                    petRefundAmount = petPrice - petDeposit;
                }
            }

            appointment.setDepositAmount(appointment.getDepositAmount() - petDeposit);
            appointment.setTotalAmount(appointment.getTotalAmount() - petPrice);

            appointment.getPets().remove(petToRemove);
            appointmentRepository.save(appointment);

            LocalDate date = appointment.getDate();
            LocalTime time = appointment.getTime();
            AppointmentSlot slot = appointmentSlotRepository.findByDateAndTime(date, time)
                    .orElseThrow(() -> new IllegalStateException("Không tìm thấy slot cho lịch hẹn #" + appointmentId));
            slot.setBookedSlots(slot.getBookedSlots() - 1);
            slot.setAvailableSlots(slot.getTotalSlots() - slot.getBookedSlots());
            appointmentSlotRepository.save(slot);

            String employeeName = "Quản trị viên";
            try {
                User employee = userRepository.findById(userId)
                        .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhân viên với ID: " + userId));
                employeeName = employee.getFullName() != null ? employee.getFullName() : "Quản trị viên";
            } catch (Exception e) {
                System.err.println("Error fetching employee name for userId " + userId + ": " + e.getMessage());
            }

            String historyReason = "Xóa thú cưng #" + petId + " khỏi lịch hẹn bởi " + employeeName;
            try {
                appointmentHistoryService.logAction(
                        appointmentId,
                        userId,
                        "REMOVE_PET",
                        appointment.getStatus(),
                        appointment.getStatus(),
                        historyReason
                );
            } catch (Exception e) {
                System.err.println("Error logging action for appointment #" + appointmentId + ": " + e.getMessage());
            }

            SlotUpdateMessage slotMessage = new SlotUpdateMessage(
                    date.toString(),
                    time.toString(),
                    -1
            );
            Map<String, Object> petRemovedMessage = new HashMap<>();
            petRemovedMessage.put("type", "PET_REMOVED");
            petRemovedMessage.put("appointmentId", appointmentId);
            petRemovedMessage.put("petId", petId);
            petRemovedMessage.put("date", date.toString());
            petRemovedMessage.put("time", time.toString());
            petRemovedMessage.put("refundAmount", petRefundAmount);
            petRemovedMessage.put("nonRefundedDeposit", petNonRefundedDeposit);

            try {
                webSocketService.sendToTopic("/topic/slots", slotMessage.toString());
                webSocketService.sendToTopic("/topic/appointments", petRemovedMessage.toString());
            } catch (Exception e) {
                System.err.println("Error sending WebSocket message: " + e.getMessage());
            }

            saveCancellationTransactions(appointment, petNonRefundedDeposit, petRefundAmount);

            return new AppointmentResponse(
                    appointment.getAppointmentId(),
                    appointment.getCustomerName(),
                    appointment.getPhone(),
                    appointment.getDate() != null ? appointment.getDate().toString() : null,
                    appointment.getTime() != null ? appointment.getTime().toString() : null,
                    petRefundAmount,
                    petNonRefundedDeposit,
                    null,
                    null,
                    null,
                    appointment.getCancelReason()
            );
        } catch (Exception e) {
            System.err.println("Error removing pet for CONFIRMED appointment #" + appointmentId + ": " + e.getMessage());
            throw new RuntimeException("Không thể xóa thú cưng khỏi lịch hẹn: " + e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AppointmentResponse removePetFromAppointment(Long appointmentId, Long petId, Long userId) {
        try {
            Appointment appointment = appointmentRepository.findById(appointmentId)
                    .orElseThrow(() -> new IllegalArgumentException("Lịch hẹn không tồn tại: " + appointmentId));

            if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
                throw new IllegalStateException("Lịch hẹn #" + appointmentId + " đã bị hủy");
            }
            if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
                throw new IllegalStateException("Lịch hẹn #" + appointmentId + " đã hoàn thành, không thể chỉnh sửa");
            }

            Pet petToRemove = appointment.getPets().stream()
                    .filter(pet -> pet.getId().equals(petId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Thú cưng #" + petId + " không thuộc lịch hẹn #" + appointmentId));

            if (appointment.getPets().size() == 1) {
                List<AppointmentResponse> cancellationResults;
                if (appointment.getStatus() == AppointmentStatus.PAID) {
                    cancellationResults = cancelAppointments(Collections.singletonList(appointmentId), "Hủy vì xóa hết thú cưng", userId);
                } else {
                    cancellationResults = cancelAppointments(Collections.singletonList(appointmentId), "Hủy vì xóa hết thú cưng", userId);
                }
                return cancellationResults.get(0);
            }

            if (appointment.getStatus() == AppointmentStatus.PAID) {
                return removePetForPaid(appointmentId, petId, userId, appointment, petToRemove);
            } else {
                return removePetForConfirmed(appointmentId, petId, userId, appointment, petToRemove);
            }
        } catch (Exception e) {
            System.err.println("Error removing pet from appointment ID " + appointmentId + ": " + e.getMessage());
            throw new RuntimeException("Không thể xóa thú cưng khỏi lịch hẹn: " + e.getMessage());
        }
    }

    @Transactional
    public List<AppointmentResponse> getRefundedAppointments(String filter) {
        try {
            List<Appointment> refundedAppointments = appointmentRepository.findByStatusAndRefundStatusIsNotNull(AppointmentStatus.CANCELLED);
            System.out.println("Found " + refundedAppointments.size() + " CANCELLED appointments with refundStatus not null");
            List<AppointmentResponse> responses = new ArrayList<>();

            for (Appointment appointment : refundedAppointments) {
                if (appointment == null) {
                    System.err.println("Found null appointment in refunded appointments");
                    continue;
                }

                double refundAmount = 0;
                double nonRefundedDeposit = 0;
                boolean hasPendingRefundedTransaction = false;

                List<Transaction> transactions = transactionRepository.findByAppointmentAppointmentId(appointment.getAppointmentId());
                System.out.println("Appointment #" + appointment.getAppointmentId() + " has " + transactions.size() + " transactions");

                for (Transaction transaction : transactions) {
                    if (transaction == null) continue;
                    if (transaction.getType() == TransactionType.REFUNDED) {
                        refundAmount = transaction.getAmount();
                        if (transaction.getStatus() == TransactionStatus.PENDING) {
                            hasPendingRefundedTransaction = true;
                        }
                    } else if (transaction.getType() == TransactionType.NON_REFUNDED_DEPOSIT) {
                        nonRefundedDeposit = transaction.getAmount();
                    }
                }

                if (filter != null && filter.equals("pending") && !hasPendingRefundedTransaction) {
                    System.out.println("Skipping appointment #" + appointment.getAppointmentId() + " (no PENDING REFUNDED transaction)");
                    continue;
                }

                AppointmentResponse response = new AppointmentResponse(
                        appointment.getAppointmentId(),
                        appointment.getCustomerName(),
                        appointment.getPhone(),
                        appointment.getDate() != null ? appointment.getDate().toString() : null,
                        appointment.getTime() != null ? appointment.getTime().toString() : null,
                        refundAmount,
                        nonRefundedDeposit,
                        appointment.getRefundStatus(),
                        appointment.getRefundMethod() != null ? appointment.getRefundMethod().toString() : null,
                        appointment.getRefundNote(),
                        appointment.getCancelReason()
                );
                response.setStatus(appointment.getStatus() != null ? appointment.getStatus().toString() : "UNKNOWN");
                responses.add(response);
            }
            System.out.println("Returning " + responses.size() + " refunded appointments");
            return responses;
        } catch (Exception e) {
            System.err.println("Error fetching refunded appointments: " + e.getMessage());
            throw new RuntimeException("Không thể tải danh sách lịch hẹn hoàn tiền: " + e.getMessage());
        }
    }

    @Transactional
    public long getRefundedAppointmentsPendingCount() {
        try {
            return appointmentRepository.countByStatusAndRefundStatus(AppointmentStatus.CANCELLED, "PENDING");
        } catch (Exception e) {
            System.err.println("Error fetching refunded appointments pending count: " + e.getMessage());
            throw new RuntimeException("Không thể tải số lượng lịch hẹn hoàn tiền đang chờ: " + e.getMessage());
        }
    }

    @Transactional
    public void updateRefundStatus(Long appointmentId, String refundStatus, String refundMethod, String refundNote, Long userId) {
        try {
            Appointment appointment = appointmentRepository.findById(appointmentId)
                    .orElseThrow(() -> new IllegalArgumentException("Lịch hẹn không tồn tại: " + appointmentId));

            if (appointment.getStatus() != AppointmentStatus.CANCELLED) {
                throw new IllegalStateException("Lịch hẹn #" + appointmentId + " không ở trạng thái CANCELLED");
            }
            if (appointment.getRefundStatus() == null) {
                throw new IllegalStateException("Lịch hẹn #" + appointmentId + " không có trạng thái hoàn tiền");
            }
            if (appointment.getRefundStatus().equals("COMPLETED")) {
                throw new IllegalStateException("Lịch hẹn #" + appointmentId + " đã được hoàn tiền");
            }

            appointment.setRefundStatus(refundStatus);
            if (refundMethod != null) {
                appointment.setRefundMethod(RefundMethod.valueOf(refundMethod));
            }
            appointment.setRefundNote(refundNote);
            appointmentRepository.save(appointment);

            appointmentHistoryService.logAction(
                    appointmentId,
                    userId,
                    "REFUND_COMPLETED",
                    AppointmentStatus.CANCELLED,
                    AppointmentStatus.CANCELLED,
                    "Đã hoàn tiền cho khách hàng"
            );

            Map<String, Object> refundMessage = new HashMap<>();
            refundMessage.put("type", "REFUND_STATUS_UPDATED");
            refundMessage.put("appointmentId", appointmentId);
            webSocketService.sendToTopic("/topic/appointments", refundMessage.toString());
        } catch (Exception e) {
            System.err.println("Error updating refund status for appointment ID " + appointmentId + ": " + e.getMessage());
            throw new RuntimeException("Không thể cập nhật trạng thái hoàn tiền: " + e.getMessage());
        }
    }

    public List<AppointmentResponse> getConfirmedAppointmentsByDateAndTime(LocalDate date, LocalTime time) {
        try {
            List<Appointment> appointments = appointmentRepository.findByDateAndTimeAndStatus(date, time, AppointmentStatus.CONFIRMED);
            List<AppointmentResponse> responses = new ArrayList<AppointmentResponse>();
            for (Appointment appointment : appointments) {
                if (appointment == null) {
                    System.err.println("Found null appointment in date: " + date + ", time: " + time);
                    continue;
                }
                AppointmentResponse response = new AppointmentResponse(
                        appointment.getAppointmentId(),
                        appointment.getCustomerName(),
                        appointment.getPhone(),
                        appointment.getDate() != null ? appointment.getDate().toString() : null,
                        appointment.getTime() != null ? appointment.getTime().toString() : null,
                        appointment.getPaidAmount(),
                        appointment.getTotalAmount(),
                        appointment.getDepositAmount(),
                        appointment.getPets() != null ? appointment.getPets().size() : 0
                );
                response.setStatus(appointment.getStatus() != null ? appointment.getStatus().toString() : "UNKNOWN");
                responses.add(response);
            }
            return responses;
        } catch (Exception e) {
            System.err.println("Error fetching confirmed appointments by date and time: date=" + date + ", time=" + time + ", error: " + e.getMessage());
            throw new RuntimeException("Không thể tải danh sách lịch hẹn đã xác nhận: " + e.getMessage());
        }
    }

    public List<AppointmentResponse> getConfirmedAppointmentsByDate(String date) {
        try {
            LocalDate localDate = LocalDate.parse(date);
            List<Appointment> appointments = appointmentRepository.findByDateAndStatus(localDate, AppointmentStatus.CONFIRMED);
            List<AppointmentResponse> responses = new ArrayList<AppointmentResponse>();
            for (Appointment appointment : appointments) {
                if (appointment == null) {
                    System.err.println("Found null appointment in date: " + date);
                    continue;
                }
                AppointmentResponse response = new AppointmentResponse(
                        appointment.getAppointmentId(),
                        appointment.getCustomerName(),
                        appointment.getPhone(),
                        appointment.getDate() != null ? appointment.getDate().toString() : null,
                        appointment.getTime() != null ? appointment.getTime().toString() : null,
                        appointment.getPaidAmount(),
                        appointment.getTotalAmount(),
                        appointment.getDepositAmount(),
                        appointment.getPets() != null ? appointment.getPets().size() : 0
                );
                response.setStatus(appointment.getStatus() != null ? appointment.getStatus().toString() : "UNKNOWN");
                responses.add(response);
            }
            return responses;
        } catch (Exception e) {
            System.err.println("Error fetching confirmed appointments by date '" + date + "': " + e.getMessage());
            throw new RuntimeException("Không thể tải danh sách lịch hẹn đã xác nhận: " + e.getMessage());
        }
    }

    public List<Map<String, Object>> getAppointmentHistory() {
        try {
            List<AppointmentHistory> historyRecords = appointmentHistoryRepository.findAll();
            List<Map<String, Object>> historyList = new ArrayList<Map<String, Object>>();
            for (AppointmentHistory record : historyRecords) {
                if (record == null) {
                    System.err.println("Found null appointment history record");
                    continue;
                }
                Map<String, Object> historyMap = new HashMap<String, Object>();
                historyMap.put("id", record.getId());
                historyMap.put("timestamp", record.getTimestamp() != null ? record.getTimestamp().toString() : null);
                historyMap.put("user_id", record.getUser() != null ? record.getUser().getUserId() : null);
                historyMap.put("userName", record.getUser() != null ? record.getUser().getFullName() : null);
                String userRole = "";
                if (record.getUser() != null && record.getUser().getUserRoles() != null) {
                    for (Object role : record.getUser().getUserRoles()) {
                        try {
                            java.lang.reflect.Method getRoleName = role.getClass().getMethod("getRoleName");
                            userRole += (userRole.isEmpty() ? "" : ", ") + (String) getRoleName.invoke(role);
                        } catch (Exception e) {
                            System.err.println("Error getting role name: " + e.getMessage());
                        }
                    }
                }
                historyMap.put("userRole", userRole);
                historyMap.put("action", record.getAction());
                historyMap.put("old_status", record.getOldStatus() != null ? record.getOldStatus().toString() : null);
                historyMap.put("new_status", record.getNewStatus() != null ? record.getNewStatus().toString() : null);
                historyMap.put("appointment_id", record.getAppointment() != null ? record.getAppointment().getAppointmentId() : null);
                historyMap.put("customerName", record.getAppointment() != null ? record.getAppointment().getCustomerName() : null);
                List<Pet> pets = record.getAppointment() != null && record.getAppointment().getPets() != null ? record.getAppointment().getPets() : new ArrayList<>();
                historyMap.put("petName", pets.isEmpty() ? "Không có" : pets.get(0).getNamePet());
                historyMap.put("service", pets.isEmpty() ? "Không có" : pets.get(0).getPetService() != null ? pets.get(0).getPetService().getServiceName() : "Không có dịch vụ");
                historyMap.put("reason", record.getReason());
                historyList.add(historyMap);
            }
            return historyList;
        } catch (Exception e) {
            System.err.println("Error fetching appointment history: " + e.getMessage());
            throw new RuntimeException("Không thể tải lịch sử lịch hẹn: " + e.getMessage());
        }
    }

    public List<AppointmentResponse> getActiveAppointmentsByDate(String date) {
        try {
            LocalDate localDate = LocalDate.parse(date);
            List<AppointmentStatus> statuses = Arrays.asList(
                    AppointmentStatus.CONFIRMED,
                    AppointmentStatus.IN_PROGRESS,
                    AppointmentStatus.COMPLETED
            );
            List<Appointment> appointments = new ArrayList<>();
            for (AppointmentStatus status : statuses) {
                appointments.addAll(appointmentRepository.findByDateAndStatus(localDate, status));
            }
            List<AppointmentResponse> responses = new ArrayList<>();
            for (Appointment appointment : appointments) {
                if (appointment == null) {
                    System.err.println("Found null appointment in date: " + date);
                    continue;
                }
                AppointmentResponse response = new AppointmentResponse(
                        appointment.getAppointmentId(),
                        appointment.getCustomerName(),
                        appointment.getPhone(),
                        appointment.getDate() != null ? appointment.getDate().toString() : null,
                        appointment.getTime() != null ? appointment.getTime().toString() : null,
                        appointment.getPaidAmount(),
                        appointment.getTotalAmount(),
                        appointment.getDepositAmount(),
                        appointment.getPets() != null ? appointment.getPets().size() : 0
                );
                response.setStatus(appointment.getStatus() != null ? appointment.getStatus().toString() : "UNKNOWN");
                responses.add(response);
            }
            return responses;
        } catch (Exception e) {
            System.err.println("Error fetching active appointments by date '" + date + "': " + e.getMessage());
            throw new RuntimeException("Không thể tải danh sách lịch hẹn hoạt động: " + e.getMessage());
        }
    }

    public List<AppointmentResponse> getActiveAppointmentsByDateAndTime(LocalDate date, LocalTime time) {
        try {
            List<AppointmentStatus> statuses = Arrays.asList(
                    AppointmentStatus.CONFIRMED,
                    AppointmentStatus.IN_PROGRESS,
                    AppointmentStatus.COMPLETED
            );
            List<Appointment> appointments = new ArrayList<>();
            for (AppointmentStatus status : statuses) {
                appointments.addAll(appointmentRepository.findByDateAndTimeAndStatus(date, time, status));
            }
            List<AppointmentResponse> responses = new ArrayList<>();
            for (Appointment appointment : appointments) {
                if (appointment == null) {
                    System.err.println("Found null appointment in date: " + date + ", time: " + time);
                    continue;
                }
                AppointmentResponse response = new AppointmentResponse(
                        appointment.getAppointmentId(),
                        appointment.getCustomerName(),
                        appointment.getPhone(),
                        appointment.getDate() != null ? appointment.getDate().toString() : null,
                        appointment.getTime() != null ? appointment.getTime().toString() : null,
                        appointment.getPaidAmount(),
                        appointment.getTotalAmount(),
                        appointment.getDepositAmount(),
                        appointment.getPets() != null ? appointment.getPets().size() : 0
                );
                response.setStatus(appointment.getStatus() != null ? appointment.getStatus().toString() : "UNKNOWN");
                responses.add(response);
            }
            return responses;
        } catch (Exception e) {
            System.err.println("Error fetching active appointments by date and time: date=" + date + ", time=" + time + ", error: " + e.getMessage());
            throw new RuntimeException("Không thể tải danh sách lịch hẹn hoạt động: " + e.getMessage());
        }
    }

    public List<Transaction> getTransactionsByAppointmentId(Long appointmentId) {
        try {
            Appointment appointment = appointmentRepository.findById(appointmentId)
                    .orElseThrow(() -> new IllegalArgumentException("Lịch hẹn không tồn tại: " + appointmentId));
            List<Transaction> transactions = transactionRepository.findByAppointmentAppointmentId(appointmentId);
            return transactions != null ? transactions : new ArrayList<>();
        } catch (IllegalArgumentException e) {
            System.err.println("Error fetching transactions by appointment ID " + appointmentId + ": " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("Unexpected error fetching transactions by appointment ID " + appointmentId + ": " + e.getMessage());
            throw new RuntimeException("Không thể tải danh sách giao dịch: " + e.getMessage());
        }
    }
}