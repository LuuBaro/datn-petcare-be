package org.example.petcarebe.service;

import lombok.RequiredArgsConstructor;
import org.example.petcarebe.dto.VetPetDTO;
import org.example.petcarebe.dto.VetPetWeightDTO;
import org.example.petcarebe.model.Pet;
import org.example.petcarebe.model.PetWeight;
import org.example.petcarebe.repository.VetPetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VetPetService {

    private final VetPetRepository vetPetRepository;

    @Transactional(readOnly = true)
    public List<VetPetDTO> getAllPets() {
        return vetPetRepository.findAll().stream()
                .filter(pet -> !pet.isDeleted())
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<VetPetDTO> getPetById(Long id) {
        return vetPetRepository.findById(id)
                .filter(pet -> !pet.isDeleted())
                .map(this::convertToDTO);
    }

    @Transactional
    public VetPetDTO createPet(VetPetDTO vetPetDTO) {
        Pet pet = new Pet();
        mapDtoToEntity(vetPetDTO, pet);
        Pet savedPet = vetPetRepository.save(pet);
        return convertToDTO(savedPet);
    }

    @Transactional
    public Optional<VetPetDTO> updatePet(Long id, VetPetDTO vetPetDTO) {
        return vetPetRepository.findById(id)
                .map(pet -> {
                    mapDtoToEntity(vetPetDTO, pet);
                    Pet updatedPet = vetPetRepository.save(pet);
                    return convertToDTO(updatedPet);
                });
    }

    @Transactional
    public boolean deletePet(Long id) {
        return vetPetRepository.findById(id)
                .map(pet -> {
                    pet.setDeleted(true);
                    vetPetRepository.save(pet);
                    return true;
                })
                .orElse(false);
    }

    private VetPetDTO convertToDTO(Pet pet) {
        VetPetDTO dto = new VetPetDTO();
        // Pet info
        dto.setId(pet.getId());
        dto.setNamePet(pet.getNamePet());
        dto.setAge(pet.getAge());
        dto.setPetType(pet.getPetType());
        dto.setNote(pet.getNote());
        dto.setPrice(pet.getPrice());
        dto.setPhoneBoss(pet.getPhoneBoss());
        dto.setNameBoss(pet.getNameBoss());
        dto.setDepositAmount(pet.getDepositAmount());
        dto.setPaidAmount(pet.getPaidAmount());
        dto.setDeleted(pet.isDeleted());

        // PetWeight info
        if (pet.getPetWeight() != null) {
            VetPetWeightDTO weightDTO = VetPetWeightDTO.builder()
                    .petWeightId(pet.getPetWeight().getPetWeightId())
                    .weightRange(pet.getPetWeight().getWeightRange())
                    .priceMultiplier(pet.getPetWeight().getPriceMultiplier())
                    .statusType(pet.getPetWeight().getStatusType())
                    .petType(pet.getPetWeight().getPetType())
                    .build();
            dto.setPetWeight(weightDTO);
        }

        return dto;
    }

    private void mapDtoToEntity(VetPetDTO dto, Pet pet) {
        // Pet info
        pet.setNamePet(dto.getNamePet());
        pet.setAge(dto.getAge());
        pet.setPetType(dto.getPetType());
        pet.setNote(dto.getNote());
        pet.setPrice(dto.getPrice());
        pet.setPhoneBoss(dto.getPhoneBoss());
        pet.setNameBoss(dto.getNameBoss());
        pet.setDepositAmount(dto.getDepositAmount() != null ? dto.getDepositAmount() : 0f);
        pet.setPaidAmount(dto.getPaidAmount() != null ? dto.getPaidAmount() : 0f);

        // PetWeight info
        if (dto.getPetWeight() != null) {
            PetWeight petWeight = new PetWeight();
            petWeight.setPetWeightId(dto.getPetWeight().getPetWeightId());
            petWeight.setWeightRange(dto.getPetWeight().getWeightRange());
            petWeight.setPriceMultiplier(dto.getPetWeight().getPriceMultiplier());
            petWeight.setStatusType(dto.getPetWeight().getStatusType());
            petWeight.setPetType(dto.getPetWeight().getPetType());
            pet.setPetWeight(petWeight);
        }
    }
}