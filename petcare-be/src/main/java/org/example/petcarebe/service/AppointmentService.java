// AppointmentService.java
package org.example.petcarebe.service;

import org.example.petcarebe.dto.request.AppointmentRequestDTO;
import org.example.petcarebe.enums.AppointmentStatus;
import org.example.petcarebe.model.*;
import org.example.petcarebe.repository.AppointmentRepository;
import org.example.petcarebe.repository.PetServiceRepository;
import org.example.petcarebe.repository.PetWeightRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Service
public class AppointmentService {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private TimeSlotService timeSlotService;

    @Autowired
    private PetServiceRepository petServiceRepository;

    @Autowired
    private PetWeightRepository petWeightRepository;

    @Transactional
    public Appointment createAppointment(AppointmentRequestDTO request) {
        // Kiểm tra dữ liệu đầu vào
        if (request.getDate() == null || request.getTime() == null) {
            throw new IllegalArgumentException("Ngày và thời gian không được để trống.");
        }
        if (request.getCustomerName() == null || request.getCustomerName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên khách hàng không được để trống.");
        }
        if (request.getPhone() == null || request.getPhone().trim().isEmpty()) {
            throw new IllegalArgumentException("Số điện thoại không được để trống.");
        }
        if (request.getPets() == null || request.getPets().isEmpty()) {
            throw new IllegalArgumentException("Danh sách thú cưng không được để trống.");
        }

        // Kiểm tra slot khả dụng
        LocalDate date;
        LocalTime time;
        try {
            date = LocalDate.parse(request.getDate());
            time = LocalTime.parse(request.getTime());
        } catch (Exception e) {
            throw new IllegalArgumentException("Định dạng ngày hoặc thời gian không hợp lệ.");
        }

        long bookedSlots = appointmentRepository.countPetsByDateAndTime(date, time);

        // Tính số slot khả dụng
        Map<String, List<org.example.petcarebe.dto.TimeSlotDTO>> timeSlots = timeSlotService.getTimeSlots(date);
        long totalSlots = timeSlots.get("morning").stream()
                .filter(slot -> slot.getHour().equals(time.toString()))
                .findFirst()
                .map(org.example.petcarebe.dto.TimeSlotDTO::getTotalSlots)
                .orElseGet(() -> timeSlots.get("afternoon").stream()
                        .filter(slot -> slot.getHour().equals(time.toString()))
                        .findFirst()
                        .map(org.example.petcarebe.dto.TimeSlotDTO::getTotalSlots)
                        .orElse(0));

        long availableSlots = totalSlots - bookedSlots;
        if (availableSlots < request.getPets().size()) {
            throw new RuntimeException("Không đủ slot khả dụng cho khung giờ này.");
        }

        // Tạo Appointment
        Appointment appointment = new Appointment();
        appointment.setCustomerName(request.getCustomerName());
        appointment.setPhone(request.getPhone());
        appointment.setStatus(AppointmentStatus.PENDING);
        appointment.setDate(date);
        appointment.setTime(time);
        appointment.setDepositAmount(request.getDepositAmount());
        appointment.setTotalAmount(0); // Sẽ được cập nhật sau khi thêm Pet

        // Thêm Pet
        for (AppointmentRequestDTO.PetDTO petDTO : request.getPets()) {
            // Kiểm tra petService
            if (petDTO.getPetService() == null || petDTO.getPetService().getId() == null) {
                throw new IllegalArgumentException("Dịch vụ không được để trống.");
            }
            PetService petService = petServiceRepository.findById(petDTO.getPetService().getId())
                    .orElseThrow(() -> new RuntimeException("Dịch vụ không tồn tại với ID: " + petDTO.getPetService().getId()));

            // Kiểm tra petWeight
            if (petDTO.getPetWeight() == null || petDTO.getPetWeight().getPetWeightId() == null) {
                throw new IllegalArgumentException("Cân nặng không được để trống.");
            }
            PetWeight petWeight = petWeightRepository.findById(petDTO.getPetWeight().getPetWeightId())
                    .orElseThrow(() -> new RuntimeException("Cân nặng không tồn tại với ID: " + petDTO.getPetWeight().getPetWeightId()));

            Pet pet = new Pet();
            pet.setPetType(petDTO.getPetType());
            pet.setPetService(petService);
            pet.setPetWeight(petWeight);
            pet.setNote(petDTO.getNote());
            pet.setPrice(petDTO.getPrice());
            appointment.addPet(pet);
        }

        return appointmentRepository.save(appointment);
    }

    @Transactional
    public Appointment updatePayment(Long id, float depositAmount) {
        // Kiểm tra id
        if (id == null) {
            throw new IllegalArgumentException("ID lịch hẹn không được để trống.");
        }

        // Kiểm tra depositAmount
        if (depositAmount < 0) {
            throw new IllegalArgumentException("Số tiền cọc không được nhỏ hơn 0.");
        }

        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lịch hẹn không tồn tại với ID: " + id));

        if (appointment.getStatus() != AppointmentStatus.PENDING) {
            throw new RuntimeException("Lịch hẹn không ở trạng thái PENDING để cập nhật thanh toán. Trạng thái hiện tại: " + appointment.getStatus());
        }

        appointment.setDepositAmount(depositAmount);
        appointment.setStatus(AppointmentStatus.PAID);
        return appointmentRepository.save(appointment);
    }

    @Transactional
    public Appointment confirmAppointment(Long id, Long staffId) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lịch hẹn không tồn tại với ID: " + id));

        if (appointment.getStatus() != AppointmentStatus.PAID) {
            throw new RuntimeException("Lịch hẹn chưa được thanh toán để xác nhận.");
        }

        appointment.setStatus(AppointmentStatus.CONFIRMED);
        // Gán staff (User) cho Appointment
        User staff = new User();
        staff.setUserId(staffId);
        appointment.setUser(staff);

        return appointmentRepository.save(appointment);
    }

    @Transactional
    public Appointment updateStatus(Long id, AppointmentStatus newStatus) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lịch hẹn không tồn tại với ID: " + id));

        // Kiểm tra trình tự trạng thái
        if (appointment.getStatus() == AppointmentStatus.IN_PROGRESS) {
            if (newStatus != AppointmentStatus.COMPLETED && newStatus != AppointmentStatus.CANCELLED) {
                throw new RuntimeException("Từ trạng thái IN_PROGRESS, chỉ có thể chuyển sang COMPLETED hoặc CANCELLED.");
            }
        } else if (appointment.getStatus() == AppointmentStatus.COMPLETED || appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new RuntimeException("Không thể cập nhật trạng thái từ " + appointment.getStatus());
        }

        appointment.setStatus(newStatus);
        return appointmentRepository.save(appointment);
    }

    @Transactional
    public void deleteAppointment(Long id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lịch hẹn không tồn tại với ID: " + id));
        appointmentRepository.delete(appointment);
    }
}