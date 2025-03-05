package org.example.petcarebe.controller;

import jakarta.validation.Valid;
import org.example.petcarebe.model.Voucher;
import org.example.petcarebe.service.VoucherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vouchers")
public class VoucherController {

    @Autowired
    VoucherService voucherService;

    // Thêm voucher
    @PostMapping("/createVoucher")
    public ResponseEntity<?> createVoucher(@RequestBody @Valid Voucher voucher) {
        Voucher createdVoucher = voucherService.saveVoucher(voucher);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdVoucher);
    }

    @GetMapping("/getAllVouchers")
    public List<Voucher> getAllVouchers() {
        return voucherService.getAllVouchers();
    }

    @PutMapping("/updateVouchers/{voucherId}")
    public ResponseEntity<?> updateVoucher(@PathVariable Long voucherId, @RequestBody @Valid Voucher voucherDetails) {
        Voucher updatedVoucher = voucherService.updateVoucher(voucherId, voucherDetails);
        return ResponseEntity.ok(updatedVoucher);
    }


    @DeleteMapping("/deleteVouchers/{voucherId}")
    public ResponseEntity<Void> deleteVoucher(@PathVariable Long voucherId) {
        System.out.println("Deleting Voucher ID: " + voucherId); // Debug
        voucherService.deleteVoucher(voucherId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{voucherId}/decrement")
    public ResponseEntity<String> decrementVoucherQuantity(@PathVariable Long voucherId) {
        try {
            voucherService.decrementVoucherQuantity(voucherId);
            return ResponseEntity.ok("Số lượng voucher đã được giảm");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}
