// AppointmentService.java
package org.example.petcarebe.service;

import org.example.petcarebe.dto.TimeSlotDTO;
import org.example.petcarebe.dto.request.AppointmentRequestDTO;
import org.example.petcarebe.enums.AppointmentStatus;
import org.example.petcarebe.model.Appointment;
import org.example.petcarebe.model.Pet;
import org.example.petcarebe.model.User;
import org.example.petcarebe.repository.AppointmentRepository;
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

    @Transactional
    public Appointment createAppointment(AppointmentRequestDTO request) {
        // Kiểm tra slot khả dụng
        LocalDate date = LocalDate.parse(request.getDate());
        LocalTime time = LocalTime.parse(request.getTime());
        long bookedSlots = appointmentRepository.countPetsByDateAndTime(date, time);

        // Tính số slot khả dụng
        Map<String, List<TimeSlotDTO>> timeSlots = timeSlotService.getTimeSlots(date);
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
            Pet pet = new Pet();
            pet.setPetType(petDTO.getPetType());
            pet.setPetService(petDTO.getPetService());
            pet.setPetWeight(petDTO.getPetWeight());
            pet.setNote(petDTO.getNote());
            pet.setPrice(petDTO.getPrice());
            appointment.addPet(pet);
        }

        return appointmentRepository.save(appointment);
    }

    @Transactional
    public Appointment updatePayment(Long id, float depositAmount) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lịch hẹn không tồn tại với ID: " + id));

        if (appointment.getStatus() != AppointmentStatus.PENDING) {
            throw new RuntimeException("Lịch hẹn không ở trạng thái PENDING để cập nhật thanh toán.");
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
}