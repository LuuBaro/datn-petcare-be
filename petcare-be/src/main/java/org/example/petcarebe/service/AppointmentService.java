package org.example.petcarebe.service;

import org.example.petcarebe.dto.AppointmentRequest;
import org.example.petcarebe.dto.AppointmentResponse;
import org.example.petcarebe.dto.PetResponse;
import org.example.petcarebe.enums.AppointmentStatus;
import org.example.petcarebe.enums.PetType;
import org.example.petcarebe.model.Appointment;
import org.example.petcarebe.model.AppointmentSlot;
import org.example.petcarebe.model.DefaultTimeSlot;
import org.example.petcarebe.model.Pet;
import org.example.petcarebe.model.PetService;
import org.example.petcarebe.model.PetWeight;
import org.example.petcarebe.repository.AppointmentRepository;
import org.example.petcarebe.repository.AppointmentSlotRepository;
import org.example.petcarebe.repository.DefaultTimeSlotRepository;
import org.example.petcarebe.repository.PetServiceRepository;
import org.example.petcarebe.repository.PetWeightRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
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
                    // Nếu không có AppointmentSlot, tạo mới dựa trên DefaultTimeSlot
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
        appointment.setStatus(AppointmentStatus.PAID); // Thanh toán mặc định thành công
        appointment.setDate(date);
        appointment.setTime(time);
        appointment.setDepositAmount(request.getDepositAmount());
        appointment.setTotalAmount(request.getTotalAmount());
        appointment.setPaidAmount(request.getDepositAmount()); // Cập nhật paid_amount
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
            pet.setDepositAmount(request.getDepositAmount() / requiredSlots); // Chia đều cọc cho mỗi pet
            pet.setPaidAmount(pet.getDepositAmount()); // Cập nhật paid_amount cho pet
            pets.add(pet);
        }
        appointment.setPets(pets);

        // Update AppointmentSlot
        slot.setAppointment(appointment);
        slot.setBookedSlots(slot.getBookedSlots() + requiredSlots);
        slot.setAvailableSlots(slot.getTotalSlots() - slot.getBookedSlots());
        appointment.getAppointmentSlots().add(slot);

        // Save to DB
        Appointment savedAppointment = appointmentRepository.save(appointment);
        appointmentSlotRepository.save(slot);

        // Notify WebSocket clients
        messagingTemplate.convertAndSend("/topic/slots", request.getDate());

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
        }
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
}