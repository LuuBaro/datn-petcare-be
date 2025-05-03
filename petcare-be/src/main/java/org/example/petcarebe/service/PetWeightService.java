package org.example.petcarebe.service;

import org.example.petcarebe.enums.PetType;
import org.example.petcarebe.model.PetWeight;
import org.example.petcarebe.repository.PetWeightRepository;
import org.example.petcarebe.enums.StatusType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PetWeightService {

    @Autowired
    private PetWeightRepository petWeightRepository;

    public List<PetWeight> getAllPetWeights() {
        try {
            List<PetWeight> weights = petWeightRepository.findAll();
            System.out.println("PetWeightService - Lấy tất cả khoảng cân nặng: " + weights.size());
            return weights;
        } catch (Exception e) {
            System.err.println("PetWeightService - Lỗi khi lấy tất cả khoảng cân nặng: " + e.getMessage());
            throw new RuntimeException("Lỗi khi lấy tất cả khoảng cân nặng: " + e.getMessage());
        }
    }

    public PetWeight createPetWeight(PetWeight petWeight) {
        try {
            if (petWeight.getStatusType() == null) {
                petWeight.setStatusType(StatusType.ACTIVE);
            }
            if (petWeight.getWeightRange() == null || petWeight.getWeightRange().trim().isEmpty()) {
                throw new IllegalArgumentException("Khoảng cân nặng không được để trống");
            }
            if (petWeight.getPriceMultiplier() <= 0) {
                throw new IllegalArgumentException("Hệ số giá phải lớn hơn 0");
            }
            PetWeight savedWeight = petWeightRepository.save(petWeight);
            System.out.println("PetWeightService - Tạo khoảng cân nặng: " + savedWeight.getPetWeightId());
            return savedWeight;
        } catch (Exception e) {
            System.err.println("PetWeightService - Lỗi khi tạo khoảng cân nặng: " + e.getMessage());
            throw new RuntimeException("Lỗi khi tạo khoảng cân nặng: " + e.getMessage());
        }
    }

    public PetWeight updatePetWeight(Long id, PetWeight petWeightDetails) {
        try {
            PetWeight petWeight = petWeightRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Khoảng cân nặng không tồn tại với ID: " + id));

            petWeight.setPetType(petWeightDetails.getPetType());
            petWeight.setWeightRange(petWeightDetails.getWeightRange());
            petWeight.setPriceMultiplier(petWeightDetails.getPriceMultiplier());
            petWeight.setStatusType(petWeightDetails.getStatusType());

            PetWeight updatedWeight = petWeightRepository.save(petWeight);
            System.out.println("PetWeightService - Cập nhật khoảng cân nặng: " + updatedWeight.getPetWeightId());
            return updatedWeight;
        } catch (Exception e) {
            System.err.println("PetWeightService - Lỗi khi cập nhật khoảng cân nặng: " + e.getMessage());
            throw new RuntimeException("Lỗi khi cập nhật khoảng cân nặng: " + e.getMessage());
        }
    }

    public void deactivatePetWeight(Long id) {
        try {
            PetWeight petWeight = petWeightRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Khoảng cân nặng không tồn tại với ID: " + id));
            petWeight.setStatusType(StatusType.INACTIVE);
            petWeightRepository.save(petWeight);
            System.out.println("PetWeightService - Vô hiệu hóa khoảng cân nặng: " + id);
        } catch (Exception e) {
            System.err.println("PetWeightService - Lỗi khi vô hiệu hóa khoảng cân nặng: " + e.getMessage());
            throw new RuntimeException("Lỗi khi vô hiệu hóa khoảng cân nặng: " + e.getMessage());
        }
    }

    public void activatePetWeight(Long id) {
        try {
            PetWeight petWeight = petWeightRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Khoảng cân nặng không tồn tại với ID: " + id));
            petWeight.setStatusType(StatusType.ACTIVE);
            petWeightRepository.save(petWeight);
            System.out.println("PetWeightService - Kích hoạt khoảng cân nặng: " + id);
        } catch (Exception e) {
            System.err.println("PetWeightService - Lỗi khi kích hoạt khoảng cân nặng: " + e.getMessage());
            throw new RuntimeException("Lỗi khi kích hoạt khoảng cân nặng: " + e.getMessage());
        }
    }

    public List<PetWeight> getWeightsByPetType(PetType petType) {
        try {
            System.out.println("PetWeightService - Lấy cân nặng cho loại thú cưng: " + petType);
            List<PetWeight> weights = petWeightRepository.findByPetTypeAndStatusType(petType, StatusType.ACTIVE);
            System.out.println("PetWeightService - Lấy được: " + weights.size() + " khoảng cân nặng cho loại: " + petType);
            return weights;
        } catch (Exception e) {
            System.err.println("PetWeightService - Lỗi khi lấy cân nặng theo loại thú cưng: " + petType + ", lỗi: " + e.getMessage());
            throw new RuntimeException("Lỗi khi lấy cân nặng theo loại thú cưng: " + e.getMessage());
        }
    }
}