// PetServiceService.java
package org.example.petcarebe.service;

import org.example.petcarebe.enums.PetType;
import org.example.petcarebe.enums.StatusType;
import org.example.petcarebe.model.PetService;
import org.example.petcarebe.repository.PetServiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PetServiceService {

    @Autowired
    private PetServiceRepository petServiceRepository;

    public List<PetService> getAllServices() {
        return petServiceRepository.findAll();
    }

    public PetService createService(PetService petService) {
        if (petService.getStatusType() == null) {
            petService.setStatusType(StatusType.ACTIVE);
        }
        if (petService.getServiceName() == null || petService.getServiceName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên dịch vụ không được để trống");
        }
        if (petService.getBasePrice() == null || petService.getBasePrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Giá cơ bản phải lớn hơn hoặc bằng 0");
        }
        return petServiceRepository.save(petService);
    }

    public PetService updateService(Long id, PetService petServiceDetails) {
        PetService petService = petServiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dịch vụ không tồn tại với ID: " + id));

        petService.setServiceName(petServiceDetails.getServiceName());
        petService.setDescription(petServiceDetails.getDescription());
        petService.setBasePrice(petServiceDetails.getBasePrice());
        petService.setPetType(petServiceDetails.getPetType());
        petService.setStatusType(petServiceDetails.getStatusType());

        return petServiceRepository.save(petService);
    }

    public void deactivateService(Long id) {
        PetService petService = petServiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dịch vụ không tồn tại với ID: " + id));
        petService.setStatusType(StatusType.INACTIVE);
        petServiceRepository.save(petService);
    }

    // Thêm phương thức mới để lấy dịch vụ theo petType
    public List<PetService> getServicesByPetType(PetType petType) {
        return petServiceRepository.findByPetType(petType);
    }
}