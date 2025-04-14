package org.example.petcarebe.controller;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.petcarebe.enums.PetType;
import org.example.petcarebe.model.VetService;
import org.example.petcarebe.service.VetServiceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vet-services")
@RequiredArgsConstructor
public class VetServiceController {

    private final VetServiceService vetServiceService;

    // Create
    @PostMapping
    public ResponseEntity<ApiResponse<VetService>> createVetService(@RequestBody VetService vetService) {
        VetService created = vetServiceService.createVetService(vetService);
        return ResponseEntity.ok(new ApiResponse<>(true, "Tạo dịch vụ thú y thành công", created));
    }

    // Get all active vet services
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<VetService>>> getAllActiveVetServices() {
        List<VetService> list = vetServiceService.getAllActiveVetServices();
        return ResponseEntity.ok(new ApiResponse<>(true, "Lấy danh sách dịch vụ đang hoạt động", list));
    }

    // Get all vet services
    @GetMapping
    public ResponseEntity<ApiResponse<List<VetService>>> getAllVetServices() {
        List<VetService> list = vetServiceService.getAllVetServices();
        return ResponseEntity.ok(new ApiResponse<>(true, "Lấy tất cả dịch vụ thú y", list));
    }

    // Get vet service by ID
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VetService>> getVetServiceById(@PathVariable Long id) {
        VetService vetService = vetServiceService.getVetServiceById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Lấy thông tin dịch vụ thành công", vetService));
    }

    // Update vet service
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VetService>> updateVetService(@PathVariable Long id, @RequestBody VetService updatedService) {
        VetService updated = vetServiceService.updateVetService(id, updatedService);
        return ResponseEntity.ok(new ApiResponse<>(true, "Cập nhật dịch vụ thành công", updated));
    }

    // Toggle active status (soft delete / restore)
    @PatchMapping("/{id}/toggle-active") // Changed from @DeleteMapping to @PatchMapping
    public ResponseEntity<ApiResponse<Void>> toggleVetServiceActive(@PathVariable Long id) {
        vetServiceService.toggleVetServiceActive(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Thay đổi trạng thái hoạt động thành công", null));
    }

    // Get vet services by PetType
    @GetMapping("/by-pet-type")
    public ResponseEntity<ApiResponse<List<VetService>>> getByPetType(@RequestParam PetType petType) {
        List<VetService> services = vetServiceService.getVetServicesByPetType(petType);
        return ResponseEntity.ok(new ApiResponse<>(true, "Lọc dịch vụ theo loại thú cưng thành công", services));
    }

    // Inner static class dùng chung cho response
    @Data
    @AllArgsConstructor
    static class ApiResponse<T> {
        private boolean success;
        private String message;
        private T data;
    }
}