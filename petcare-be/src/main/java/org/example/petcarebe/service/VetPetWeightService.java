package org.example.petcarebe.service;

import lombok.RequiredArgsConstructor;
import org.example.petcarebe.dto.VetPetWeightDTO;
import org.example.petcarebe.model.PetWeight;
import org.example.petcarebe.repository.VetPetWeightRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VetPetWeightService {

    private final VetPetWeightRepository VetPetWeightRepository;

    @Transactional(readOnly = true)
    public List<VetPetWeightDTO> getAllPetWeights() {
        return VetPetWeightRepository.findAll().stream()
                .map(petWeight -> VetPetWeightDTO.builder()
                        .petWeightId(petWeight.getPetWeightId())
                        .weightRange(petWeight.getWeightRange())
                        .priceMultiplier(petWeight.getPriceMultiplier())
                        .statusType(petWeight.getStatusType())
                        .petType(petWeight.getPetType())
                        .build())
                .collect(Collectors.toList());
    }
}