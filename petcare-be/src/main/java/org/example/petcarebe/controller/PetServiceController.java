package org.example.petcarebe.controller;

import org.example.petcarebe.enums.PetType;
import org.example.petcarebe.model.PetService;
import org.example.petcarebe.service.PetServiceService; // Lưu ý: Sử dụng PetServiceService, không phải PetManagementService
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pet-services")
public class PetServiceController {

    @Autowired
    private PetServiceService petServiceService; // Đổi sang PetServiceService

    @GetMapping("/{petServiceId}/price")
    public ResponseEntity<Double> getServicePrice(
            @PathVariable Long petServiceId,
            @RequestParam Long petWeightId) {
        try {
            System.out.println("PetServiceController - Nhận yêu cầu lấy giá dịch vụ:");
            System.out.println("  ID Dịch vụ: " + petServiceId);
            System.out.println("  ID Cân nặng: " + petWeightId);

            if (petServiceId == null || petWeightId == null) {
                return ResponseEntity.badRequest().body(null);
            }

            double price = petServiceService.getServicePrice(petServiceId, petWeightId); // Gọi qua PetServiceService

            System.out.println("PetServiceController - Tính giá dịch vụ thành công:");
            System.out.println("  Giá: " + price);

            return ResponseEntity.ok(price);
        } catch (RuntimeException e) {
            System.err.println("PetServiceController - Lỗi khi tính giá dịch vụ: " + e.getMessage());
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            System.err.println("PetServiceController - Lỗi không mong muốn: " + e.getMessage());
            return ResponseEntity.status(500).body(null);
        }
    }

    // Thêm endpoint mới cho /api/pet-services/by-pet-type
    @GetMapping("/by-pet-type")
    public ResponseEntity<List<PetService>> getServicesByPetType(@RequestParam("petType") String petType) {
        try {
            System.out.println("PetServiceController - Nhận yêu cầu lấy dịch vụ theo loại thú cưng: " + petType);
            PetType type = PetType.valueOf(petType.toUpperCase());
            List<PetService> services = petServiceService.getServicesByPetType(type);
            System.out.println("PetServiceController - Lấy dịch vụ thành công: " + services.size() + " dịch vụ");
            return ResponseEntity.ok(services);
        } catch (IllegalArgumentException e) {
            System.err.println("PetServiceController - Loại thú cưng không hợp lệ: " + petType);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            System.err.println("PetServiceController - Lỗi khi lấy dịch vụ: " + e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }
}