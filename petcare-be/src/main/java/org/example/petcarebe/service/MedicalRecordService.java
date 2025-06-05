package org.example.petcarebe.service;

import org.example.petcarebe.dto.MedicalRecordDTO;
import org.example.petcarebe.dto.VetPetDTO;
import org.example.petcarebe.dto.VetPetWeightDTO;
import org.example.petcarebe.dto.request.MedicalRecordRequestDTO;
import org.example.petcarebe.dto.response.MedicalRecordResponseDTO;
import org.example.petcarebe.model.MedicalRecord;
import org.example.petcarebe.model.Pet;
import org.example.petcarebe.model.Vaccine;
import org.example.petcarebe.model.VetService;
import org.example.petcarebe.repository.MedicalRecordRepository;
import org.example.petcarebe.repository.VaccineRepository;
import org.example.petcarebe.repository.VetPetRepository;
import org.example.petcarebe.repository.VetServiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class MedicalRecordService {

    @Autowired
    private MedicalRecordRepository medicalRecordRepository;

    @Autowired
    private VetPetRepository petRepository;

    @Autowired
    private VaccineRepository vaccineRepository;

    @Autowired
    private VetServiceRepository vetServiceRepository;

    // Helper method to map Pet to VetPetDTO (without mapping medicalRecords)
    private VetPetDTO mapPetToDTO(Pet pet) {
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

        // Note: We don't map medicalRecords here to avoid circular reference
        return dto;
    }

    // Helper method to map MedicalRecord to DTO
    private MedicalRecordDTO mapToDTO(MedicalRecord medicalRecord) {
        MedicalRecordDTO dto = new MedicalRecordDTO();
        dto.setId(medicalRecord.getId());
        dto.setExamDate(medicalRecord.getExamDate());
        dto.setSymptoms(medicalRecord.getSymptoms());
        dto.setDiagnosis(medicalRecord.getDiagnosis());
        dto.setTreatment(medicalRecord.getTreatment());
        dto.setNote(medicalRecord.getNote());
        dto.setVaccineId(medicalRecord.getVaccine() != null ? medicalRecord.getVaccine().getId() : null);
        dto.setVetServiceId(medicalRecord.getVetService() != null ? medicalRecord.getVetService().getId() : null);
        dto.setCreatedAt(medicalRecord.getCreatedAt());
        dto.setUpdatedAt(medicalRecord.getUpdatedAt());
        dto.setVetPetDTO(medicalRecord.getPet() != null ? mapPetToDTO(medicalRecord.getPet()) : null);
        return dto;
    }

    // Create a new MedicalRecord
    public MedicalRecordDTO createMedicalRecord(MedicalRecordRequestDTO requestDTO) {
        MedicalRecord medicalRecord = new MedicalRecord();

        // Validate and set Pet
        Pet pet = petRepository.findById(requestDTO.getPetId())
                .orElseThrow(() -> new IllegalArgumentException("Pet not found with ID: " + requestDTO.getPetId()));
        medicalRecord.setPet(pet);

        // Validate and set Vaccine if provided
        if (requestDTO.getVaccineId() != null) {
            Vaccine vaccine = vaccineRepository.findById(requestDTO.getVaccineId())
                    .orElseThrow(() -> new IllegalArgumentException("Vaccine not found with ID: " + requestDTO.getVaccineId()));
            medicalRecord.setVaccine(vaccine);
        }

        // Validate and set VetService if provided
        if (requestDTO.getVetServiceId() != null) {
            VetService vetService = vetServiceRepository.findById(requestDTO.getVetServiceId())
                    .orElseThrow(() -> new IllegalArgumentException("VetService not found with ID: " + requestDTO.getVetServiceId()));
            medicalRecord.setVetService(vetService);
        }

        // Set MedicalRecord fields
        medicalRecord.setExamDate(requestDTO.getExamDate());
        medicalRecord.setSymptoms(requestDTO.getSymptoms());
        medicalRecord.setDiagnosis(requestDTO.getDiagnosis());
        medicalRecord.setTreatment(requestDTO.getTreatment());
        medicalRecord.setNote(requestDTO.getNote());
        medicalRecord.setCreatedAt(LocalDateTime.now());
        medicalRecord.setUpdatedAt(LocalDateTime.now());

        MedicalRecord savedMedicalRecord = medicalRecordRepository.save(medicalRecord);
        return mapToDTO(savedMedicalRecord);
    }

    // Get all MedicalRecords
    public List<MedicalRecordDTO> getAllMedicalRecords() {
        return medicalRecordRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    // Get MedicalRecords by Pet ID
    public List<MedicalRecordDTO> getMedicalRecordsByPetId(Long petId) {
        return medicalRecordRepository.findByPetId(petId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    // Get a MedicalRecord by ID
    public MedicalRecordDTO getMedicalRecordById(Long id) {
        MedicalRecord medicalRecord = medicalRecordRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("MedicalRecord not found with ID: " + id));
        return mapToDTO(medicalRecord);
    }

    // Update a MedicalRecord
    public MedicalRecordDTO updateMedicalRecord(Long id, MedicalRecordRequestDTO requestDTO) {
        MedicalRecord existingMedicalRecord = medicalRecordRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("MedicalRecord not found with ID: " + id));

        // Update fields
        existingMedicalRecord.setExamDate(requestDTO.getExamDate());
        existingMedicalRecord.setSymptoms(requestDTO.getSymptoms());
        existingMedicalRecord.setDiagnosis(requestDTO.getDiagnosis());
        existingMedicalRecord.setTreatment(requestDTO.getTreatment());
        existingMedicalRecord.setNote(requestDTO.getNote());

        // Update Vaccine if provided
        if (requestDTO.getVaccineId() != null) {
            Vaccine vaccine = vaccineRepository.findById(requestDTO.getVaccineId())
                    .orElseThrow(() -> new IllegalArgumentException("Vaccine not found with ID: " + requestDTO.getVaccineId()));
            existingMedicalRecord.setVaccine(vaccine);
        } else {
            existingMedicalRecord.setVaccine(null);
        }

        // Update VetService if provided
        if (requestDTO.getVetServiceId() != null) {
            VetService vetService = vetServiceRepository.findById(requestDTO.getVetServiceId())
                    .orElseThrow(() -> new IllegalArgumentException("VetService not found with ID: " + requestDTO.getVetServiceId()));
            existingMedicalRecord.setVetService(vetService);
        } else {
            existingMedicalRecord.setVetService(null);
        }

        existingMedicalRecord.setUpdatedAt(LocalDateTime.now());
        MedicalRecord updatedMedicalRecord = medicalRecordRepository.save(existingMedicalRecord);
        return mapToDTO(updatedMedicalRecord);
    }

    // Delete a MedicalRecord
    public void deleteMedicalRecord(Long id) {
        MedicalRecord medicalRecord = medicalRecordRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("MedicalRecord not found with ID: " + id));
        medicalRecordRepository.delete(medicalRecord);
    }
}