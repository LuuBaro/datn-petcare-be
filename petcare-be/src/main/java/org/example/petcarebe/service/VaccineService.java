package org.example.petcarebe.service;

import org.example.petcarebe.model.Vaccine;
import org.example.petcarebe.repository.VaccineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class VaccineService {

    @Autowired
    private VaccineRepository vaccineRepository;

    // Create
    public Vaccine createVaccine(Vaccine vaccine) {
        return vaccineRepository.save(vaccine);
    }

    // Read (Get all)
    public List<Vaccine> getAllVaccines() {
        return vaccineRepository.findAll();
    }

    // Read (Get by ID)
    public Optional<Vaccine> getVaccineById(Long id) {
        return vaccineRepository.findById(id);
    }

    // Update
    public Vaccine updateVaccine(Long id, Vaccine updatedVaccine) {
        Optional<Vaccine> existingVaccine = vaccineRepository.findById(id);
        if (existingVaccine.isPresent()) {
            Vaccine vaccine = existingVaccine.get();
            vaccine.setName(updatedVaccine.getName());
            vaccine.setOrigin(updatedVaccine.getOrigin());
            vaccine.setManufacturingDate(updatedVaccine.getManufacturingDate());
            vaccine.setExpiryDate(updatedVaccine.getExpiryDate());
            vaccine.setEntryDate(updatedVaccine.getEntryDate());
            vaccine.setType(updatedVaccine.getType());
            vaccine.setStatus(updatedVaccine.getStatus());
            vaccine.setSellingPrice(updatedVaccine.getSellingPrice());
            vaccine.setImportPrice(updatedVaccine.getImportPrice());
            vaccine.setQuantity(updatedVaccine.getQuantity());
            vaccine.setNote(updatedVaccine.getNote());
            return vaccineRepository.save(vaccine);
        } else {
            throw new RuntimeException("Vaccine not found with id: " + id);
        }
    }


    public void deleteVaccine(Long id) {
        Optional<Vaccine> existingVaccine = vaccineRepository.findById(id);
        if (existingVaccine.isPresent()) {
            Vaccine vaccine = existingVaccine.get();
            vaccine.setStatus(false); // Đổi status thành false
            vaccineRepository.save(vaccine); // Lưu lại thay đổi
        } else {
            throw new RuntimeException("Vaccine not found with id: " + id);
        }
    }
}
