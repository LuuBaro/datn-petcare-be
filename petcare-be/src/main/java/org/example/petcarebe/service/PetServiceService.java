package org.example.petcarebe.service;

import org.example.petcarebe.enums.PetType;
import org.example.petcarebe.enums.StatusType;
import org.example.petcarebe.model.PetService;
import org.example.petcarebe.model.PetWeight;
import org.example.petcarebe.repository.PetServiceRepository;
import org.example.petcarebe.repository.PetWeightRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PetServiceService {

    @Autowired
    private PetServiceRepository petServiceRepository;
    
    @Autowired
    private PetWeightRepository petWeightRepository;

    public List<PetService> getAllServices() {
        try {
            List<PetService> services = petServiceRepository.findAll();
            System.out.println("PetServiceService - Lấy tất cả dịch vụ: " + services.size());
            return services;
        } catch (Exception e) {
            System.err.println("PetServiceService - Lỗi khi lấy tất cả dịch vụ: " + e.getMessage());
            throw new RuntimeException("Lỗi khi lấy tất cả dịch vụ: " + e.getMessage());
        }
    }

    public PetService createService(PetService petService) {
        try {
            if (petService.getStatusType() == null) {
                petService.setStatusType(StatusType.ACTIVE);
            }
            if (petService.getServiceName() == null || petService.getServiceName().trim().isEmpty()) {
                throw new IllegalArgumentException("Tên dịch vụ không được để trống");
            }
            if (petService.getBasePrice() == null || petService.getBasePrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Giá cơ bản phải lớn hơn hoặc bằng 0");
            }
            PetService savedService = petServiceRepository.save(petService);
            System.out.println("PetServiceService - Tạo dịch vụ: " + savedService.getId());
            return savedService;
        } catch (Exception e) {
            System.err.println("PetServiceService - Lỗi khi tạo dịch vụ: " + e.getMessage());
            throw new RuntimeException("Lỗi khi tạo dịch vụ: " + e.getMessage());
        }
    }

    public PetService updateService(Long id, PetService petServiceDetails) {
        try {
            PetService petService = petServiceRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Dịch vụ không tồn tại với ID: " + id));

            petService.setServiceName(petServiceDetails.getServiceName());
            petService.setDescription(petServiceDetails.getDescription());
            petService.setBasePrice(petServiceDetails.getBasePrice());
            petService.setPetType(petServiceDetails.getPetType());
            petService.setStatusType(petServiceDetails.getStatusType());

            PetService updatedService = petServiceRepository.save(petService);
            System.out.println("PetServiceService - Cập nhật dịch vụ: " + updatedService.getId());
            return updatedService;
        } catch (Exception e) {
            System.err.println("PetServiceService - Lỗi khi cập nhật dịch vụ: " + e.getMessage());
            throw new RuntimeException("Lỗi khi cập nhật dịch vụ: " + e.getMessage());
        }
    }

    public void deactivateService(Long id) {
        try {
            PetService petService = petServiceRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Dịch vụ không tồn tại với ID: " + id));
            petService.setStatusType(StatusType.INACTIVE);
            petServiceRepository.save(petService);
            System.out.println("PetServiceService - Vô hiệu hóa dịch vụ: " + id);
        } catch (Exception e) {
            System.err.println("PetServiceService - Lỗi khi vô hiệu hóa dịch vụ: " + e.getMessage());
            throw new RuntimeException("Lỗi khi vô hiệu hóa dịch vụ: " + e.getMessage());
        }
    }

    public double getServicePrice(Long petServiceId, Long petWeightId) {
        try {
            PetService petService = petServiceRepository.findById(petServiceId)
                    .orElseThrow(() -> new RuntimeException("Dịch vụ không tồn tại với ID: " + petServiceId));
            
            PetWeight petWeight = petWeightRepository.findById(petWeightId)
                    .orElseThrow(() -> new RuntimeException("Khoảng cân nặng không tồn tại với ID: " + petWeightId));
            
            BigDecimal basePrice = petService.getBasePrice();
            float priceMultiplier = petWeight.getPriceMultiplier();
            
            double finalPrice = basePrice.doubleValue() * priceMultiplier;
            System.out.println("PetServiceService - Tính giá dịch vụ: " + petServiceId + 
                    ", Cân nặng: " + petWeightId + 
                    ", Giá cơ bản: " + basePrice + 
                    ", Hệ số: " + priceMultiplier + 
                    ", Giá cuối: " + finalPrice);
            
            return finalPrice;
        } catch (Exception e) {
            System.err.println("PetServiceService - Lỗi khi tính giá dịch vụ: " + e.getMessage());
            throw new RuntimeException("Lỗi khi tính giá dịch vụ: " + e.getMessage());
        }
    }

    public List<PetService> getServicesByPetType(PetType petType) {
        try {
            System.out.println("PetServiceService - Lấy dịch vụ cho loại thú cưng: " + petType);
            List<PetService> services = petServiceRepository.findByPetTypeAndStatusType(petType, StatusType.ACTIVE);
            System.out.println("PetServiceService - Lấy được: " + services.size() + " dịch vụ cho loại: " + petType);
            return services;
        } catch (Exception e) {
            System.err.println("PetServiceService - Lỗi khi lấy dịch vụ theo loại thú cưng: " + petType + ", lỗi: " + e.getMessage());
            throw new RuntimeException("Lỗi khi lấy dịch vụ theo loại thú cưng: " + e.getMessage());
        }
    }
}