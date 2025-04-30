package org.example.petcarebe.controller;

import org.example.petcarebe.dto.AdditionalFeeRequest;
import org.example.petcarebe.model.Transaction;
import org.example.petcarebe.service.TransactionService;
import org.example.petcarebe.service.AppointmentHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/appointments/{appointmentId}/fees")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private AppointmentHistoryService appointmentHistoryService;

    @PostMapping
    public ResponseEntity<Transaction> createAdditionalFee(
            @PathVariable Long appointmentId,
            @RequestBody AdditionalFeeRequest request) {
        try {
            System.out.println("TransactionController - Received request to create additional fee:");
            System.out.println("  Appointment ID: " + appointmentId);
            System.out.println("  Pet ID: " + request.getPetId());
            System.out.println("  Amount: " + request.getAmount());
            System.out.println("  Reason: " + request.getReason());
            System.out.println("  User ID: " + request.getUserId());
            
            // Tạo giao dịch ADDITIONAL_FEE
            Transaction transaction = transactionService.createAdditionalFee(
                    appointmentId,
                    request.getPetId(),
                    request.getAmount(),
                    request.getReason()
            );
            
            System.out.println("TransactionController - Additional fee created successfully:");
            System.out.println("  Transaction ID: " + transaction.getId());
            System.out.println("  Transaction Amount: " + transaction.getAmount());
            System.out.println("  Transaction Type: " + transaction.getType());

            // Lưu lịch sử hành động CREATE_ADDITIONAL_FEE
            appointmentHistoryService.logAction(
                    appointmentId,
                    request.getUserId(),
                    "CREATE_ADDITIONAL_FEE",
                    null,
                    null,
                    request.getReason()
            );
            
            System.out.println("TransactionController - History logged for CREATE_ADDITIONAL_FEE action");

            return ResponseEntity.ok(transaction);
        } catch (Exception e) {
            System.err.println("TransactionController - Error creating additional fee: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(null);
        }
    }
}