package org.example.petcarebe.service;

import org.example.petcarebe.dto.AppointmentHistoryDTO;
import org.example.petcarebe.enums.AppointmentStatus;
import org.example.petcarebe.model.Appointment;
import org.example.petcarebe.model.AppointmentHistory;
import org.example.petcarebe.model.AppointmentSlot;
import org.example.petcarebe.model.Role;
import org.example.petcarebe.model.User;
import org.example.petcarebe.repository.AppointmentHistoryRepository;
import org.example.petcarebe.repository.AppointmentRepository;
import org.example.petcarebe.repository.AppointmentSlotRepository;
import org.example.petcarebe.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AppointmentHistoryService {

    @Autowired
    private AppointmentHistoryRepository appointmentHistoryRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AppointmentSlotRepository appointmentSlotRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(Long appointmentId, Long userId, String action, AppointmentStatus oldStatus, AppointmentStatus newStatus, String reason) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Lịch hẹn không tồn tại với ID: " + appointmentId));

        if (userId == null) {
            throw new IllegalArgumentException("userId không được phép null khi ghi lịch sử hành động");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại với ID: " + userId));

        AppointmentHistory history = new AppointmentHistory();
        history.setAppointment(appointment);
        history.setUser(user);
        history.setAction(action);
        history.setOldStatus(oldStatus);
        history.setNewStatus(newStatus);
        history.setReason(reason);

        appointmentHistoryRepository.save(history);
    }

    public List<AppointmentHistoryDTO> findHistoryByPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            throw new IllegalArgumentException("Số điện thoại không được để trống");
        }
        return appointmentHistoryRepository.findByPhone(phone).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<AppointmentHistoryDTO> findAllHistory() {
        return appointmentHistoryRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private AppointmentHistoryDTO mapToDTO(AppointmentHistory history) {
        AppointmentHistoryDTO dto = new AppointmentHistoryDTO();
        dto.setId(history.getId());
        dto.setAction(history.getAction());
        dto.setReason(history.getReason());
        dto.setTimestamp(history.getTimestamp());
        dto.setAppointmentId(history.getAppointment().getAppointmentId());
        dto.setUserId(history.getUser().getUserId());
        dto.setPhone(history.getAppointment().getPhone());
        dto.setUserName(history.getUser().getFullName());
        Set<Role> userRoles = history.getUser().getUserRoles();
        String userRole = userRoles != null && !userRoles.isEmpty() ?
                userRoles.iterator().next().getRoleName() : "Unknown";
        dto.setUserRole(userRole);
        dto.setCustomerName(history.getAppointment().getCustomerName());
        dto.setPetName("Unknown");
        dto.setService("Unknown");
        List<AppointmentSlot> slots = appointmentSlotRepository.findByAppointmentId(history.getAppointment().getAppointmentId());
        String date;
        String time;
        if (slots.isEmpty()) {
            LocalDate appointmentDate = history.getAppointment().getDate();
            if (appointmentDate != null) {
                date = appointmentDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            } else {
                date = "Không xác định";
            }
            LocalTime appointmentTime = history.getAppointment().getTime();
            if (appointmentTime != null) {
                time = appointmentTime.format(DateTimeFormatter.ofPattern("HH:mm"));
            } else {
                time = "Không xác định";
            }
        } else {
            date = slots.get(0).getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            time = slots.get(0).getTime().format(DateTimeFormatter.ofPattern("HH:mm"));
        }
        dto.setDate(date);
        dto.setTime(time);
        return dto;
    }
}