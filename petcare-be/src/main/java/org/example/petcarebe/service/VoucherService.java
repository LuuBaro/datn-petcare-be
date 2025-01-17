package org.example.petcarebe.service;


import org.example.petcarebe.model.Voucher;
import org.example.petcarebe.repository.VoucherRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VoucherService {

    @Autowired
    private VoucherRepository voucherRepository;

    public Voucher saveVoucher(Voucher voucher) {return voucherRepository.save(voucher);}

    public List<Voucher> getAllVouchers() {return  voucherRepository.findAll();}

    public Voucher updateVoucher(Long voucherId, Voucher voucherDetails) {
        Voucher voucher = voucherRepository.findById(voucherId)
                .orElseThrow(() -> new RuntimeException("Voucher not found with id " + voucherId));

        // Cập nhật thông tin voucher
        voucher.setName(voucherDetails.getName());
        voucher.setStartDate(voucherDetails.getStartDate());
        voucher.setEndDate(voucherDetails.getEndDate());
        voucher.setQuantity(voucherDetails.getQuantity());
        voucher.setPercents(voucherDetails.getPercents());
        voucher.setCondition(voucherDetails.getCondition());

        return voucherRepository.save(voucher);
    }

    public void deleteVoucher(Long voucherId) {
        Voucher voucher = voucherRepository.findById(voucherId)
                .orElseThrow(() -> new RuntimeException("Voucher not found with id " + voucherId));

        voucherRepository.delete(voucher);
    }

    // Giảm số lượng voucher
    public void decrementVoucherQuantity(Long voucherId) {
        Voucher voucher = voucherRepository.findById(voucherId)
                .orElseThrow(() -> new RuntimeException("Voucher not found with id " + voucherId));

        if (voucher.getQuantity()<=0){
            throw new RuntimeException("Voucher đã hết số lượng");
        }

        voucher.setQuantity(voucher.getQuantity()-1);
        voucherRepository.save(voucher);
    }
}
