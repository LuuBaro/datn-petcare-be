package org.example.petcarebe.service;

import org.example.petcarebe.enums.AppointmentStatus;
import org.example.petcarebe.model.Appointment;
import org.example.petcarebe.model.AppointmentHistory;
import org.example.petcarebe.model.User;
import org.example.petcarebe.repository.AppointmentHistoryRepository;
import org.example.petcarebe.repository.AppointmentRepository;
import org.example.petcarebe.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

@Service
public class AppointmentHistoryService {

    @Autowired
    private AppointmentHistoryRepository appointmentHistoryRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private UserRepository userRepository;

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
}