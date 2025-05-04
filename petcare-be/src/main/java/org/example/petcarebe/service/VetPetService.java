package org.example.petcarebe.service;

import lombok.RequiredArgsConstructor;
import org.example.petcarebe.dto.VetPetDTO;
import org.example.petcarebe.dto.VetPetWeightDTO;
import org.example.petcarebe.model.Pet;
import org.example.petcarebe.model.PetWeight;
import org.example.petcarebe.repository.VetPetRepository;
import org.example.petcarebe.repository.VetPetWeightRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VetPetService {

    private final VetPetRepository vetPetRepository;
    private final VetPetWeightRepository petWeightRepository;

    @Transactional(readOnly = true)
    public Page<VetPetDTO> getAllPets(Pageable pageable) {
        return vetPetRepository.findAllByDeletedFalse(pageable)
                .map(this::convertToDTO);
    }

    @Transactional(readOnly = true)
    public Optional<VetPetDTO> getPetById(Long id) {
        return vetPetRepository.findById(id)
                .filter(pet -> !pet.getDeleted())
                .map(this::convertToDTO);
    }

    @Transactional
    public VetPetDTO createPet(VetPetDTO vetPetDTO) {
        Pet pet = new Pet();
        pet.setNamePet(vetPetDTO.getNamePet());
        pet.setNameBoss(vetPetDTO.getNameBoss());
        pet.setPhoneBoss(vetPetDTO.getPhoneBoss());
        pet.setAge(vetPetDTO.getAge());
        pet.setNote(vetPetDTO.getNote());
        pet.setDeleted(false);
        pet.setPetType(vetPetDTO.getPetType());

        if (vetPetDTO.getPetWeight() != null && vetPetDTO.getPetWeight().getPetWeightId() != null) {
            Optional<PetWeight> petWeightOpt = petWeightRepository.findById(vetPetDTO.getPetWeight().getPetWeightId());
            if (petWeightOpt.isPresent()) {
                pet.setPetWeight(petWeightOpt.get());
            } else {
                throw new IllegalArgumentException("PetWeight with ID " + vetPetDTO.getPetWeight().getPetWeightId() + " not found");
            }
        }

        Pet savedPet = vetPetRepository.save(pet);
        return convertToDTO(savedPet);
    }

    @Transactional
    public Optional<VetPetDTO> updatePet(Long id, VetPetDTO vetPetDTO) {
        return vetPetRepository.findById(id)
                .map(pet -> {
                    pet.setNamePet(vetPetDTO.getNamePet());
                    pet.setNameBoss(vetPetDTO.getNameBoss());
                    pet.setPhoneBoss(vetPetDTO.getPhoneBoss());
                    pet.setAge(vetPetDTO.getAge());
                    pet.setNote(vetPetDTO.getNote());

                    if (vetPetDTO.getPetWeight() != null && vetPetDTO.getPetWeight().getPetWeightId() != null) {
                        Optional<PetWeight> petWeightOpt = petWeightRepository.findById(vetPetDTO.getPetWeight().getPetWeightId());
                        if (petWeightOpt.isPresent()) {
                            pet.setPetWeight(petWeightOpt.get());
                        } else {
                            throw new IllegalArgumentException("PetWeight with ID " + vetPetDTO.getPetWeight().getPetWeightId() + " not found");
                        }
                    }

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
        dto.setId(pet.getId());
        dto.setNamePet(pet.getNamePet());
        dto.setAge(pet.getAge());
        dto.setNote(pet.getNote());
        dto.setPhoneBoss(pet.getPhoneBoss());
        dto.setNameBoss(pet.getNameBoss());
        dto.setDeleted(pet.getDeleted());
        dto.setPetType(pet.getPetType());

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
}