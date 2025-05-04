package org.example.petcarebe.service;

import org.example.petcarebe.enums.TransactionType;
import org.example.petcarebe.enums.TransactionStatus;
import org.example.petcarebe.enums.PaymentMethod;
import org.example.petcarebe.model.Appointment;
import org.example.petcarebe.model.Pet;
import org.example.petcarebe.model.Transaction;
import org.example.petcarebe.repository.AppointmentRepository;
import org.example.petcarebe.repository.PetRepository;
import org.example.petcarebe.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private PetRepository petRepository;

    public Transaction createAdditionalFee(Long appointmentId, Long petId, double amount, String reason, String transactionType) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Lịch hẹn không tồn tại với ID: " + appointmentId));

        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new RuntimeException("Thú cưng không tồn tại với ID: " + petId));

        Transaction transaction = new Transaction();
        transaction.setAppointment(appointment);
        transaction.setPet(pet);
        transaction.setAmount(amount);

        // Xác định type dựa trên transactionType
        if ("REFUNDED".equalsIgnoreCase(transactionType)) {
            transaction.setType(TransactionType.REFUNDED);
        } else {
            transaction.setType(TransactionType.PAYMENT);
        }

        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setPaymentMethod(PaymentMethod.ONLINE);
        transaction.setPaymentChannel(null);

        return transactionRepository.save(transaction);
    }
}