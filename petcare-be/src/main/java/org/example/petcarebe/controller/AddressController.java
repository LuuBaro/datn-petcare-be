package org.example.petcarebe.controller;

import jakarta.validation.Valid;
import org.example.petcarebe.dto.request.AddressRequest;
import org.example.petcarebe.model.Address;
import org.example.petcarebe.model.User;
import org.example.petcarebe.repository.AddressRepository;
import org.example.petcarebe.service.AddressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
public class AddressController {

    @Autowired
    private AddressService addressService;

    @Autowired
    private AddressRepository addressRepository;

    @GetMapping
    public ResponseEntity<List<Address>> getAddresses() {
        return ResponseEntity.ok(addressService.getAllAddresses());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserAddresses(@PathVariable Long userId) {
        System.out.println("📩 Nhận yêu cầu lấy danh sách địa chỉ của userId: " + userId);
        try {
            List<Address> addresses = addressService.getAddressesByUserId(userId);
            return ResponseEntity.ok(addresses);
        } catch (Exception e) {
            System.out.println("❌ Lỗi lấy danh sách địa chỉ: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi: " + e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> addAddress(@Valid @RequestBody AddressRequest addressRequest) {
        System.out.println("Dữ liệu nhận được: " + addressRequest);
        try {
            Address newAddress = addressService.createAddress(addressRequest.getUserId(), addressRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(newAddress);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Lỗi: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateAddress(@PathVariable Long id, @RequestBody AddressRequest request) {
        try {
            Address address = addressRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ với ID: " + id));

            address.setStreet(request.getStreet());
            address.setWard(request.getWard());
            address.setDistrict(request.getDistrict());
            address.setProvince(request.getProvince());

            Address updatedAddress = addressRepository.save(address);
            return ResponseEntity.ok(updatedAddress);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Lỗi: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAddress(@PathVariable Long id) {
        try {
            addressService.deleteAddress(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

}
