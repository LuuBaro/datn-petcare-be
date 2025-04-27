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
        return petWeightRepository.findAll();
    }

    public PetWeight createPetWeight(PetWeight petWeight) {
        if (petWeight.getStatusType() == null) {
            petWeight.setStatusType(StatusType.ACTIVE);
        }
        if (petWeight.getWeightRange() == null || petWeight.getWeightRange().trim().isEmpty()) {
            throw new IllegalArgumentException("Khoảng cân nặng không được để trống");
        }
        if (petWeight.getPriceMultiplier() <= 0) {
            throw new IllegalArgumentException("Hệ số giá phải lớn hơn 0");
        }
        return petWeightRepository.save(petWeight);
    }

    public PetWeight updatePetWeight(Long id, PetWeight petWeightDetails) {
        PetWeight petWeight = petWeightRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Khoảng cân nặng không tồn tại với ID: " + id));

        petWeight.setPetType(petWeightDetails.getPetType());
        petWeight.setWeightRange(petWeightDetails.getWeightRange());
        petWeight.setPriceMultiplier(petWeightDetails.getPriceMultiplier());
        petWeight.setStatusType(petWeightDetails.getStatusType());

        return petWeightRepository.save(petWeight);
    }

    public void deactivatePetWeight(Long id) {
        PetWeight petWeight = petWeightRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Khoảng cân nặng không tồn tại với ID: " + id));
        petWeight.setStatusType(StatusType.INACTIVE);
        petWeightRepository.save(petWeight);
    }

    public void activatePetWeight(Long id) {
        PetWeight petWeight = petWeightRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Khoảng cân nặng không tồn tại với ID: " + id));
        petWeight.setStatusType(StatusType.ACTIVE);
        petWeightRepository.save(petWeight);
    }

    public List<PetWeight> getWeightsByPetType(PetType petType) {
        return petWeightRepository.findByPetTypeAndStatusType(petType, StatusType.ACTIVE);
    }
}