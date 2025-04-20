package org.example.petcarebe.service;

import lombok.RequiredArgsConstructor;
import org.example.petcarebe.enums.PetType;
import org.example.petcarebe.model.VetService;
import org.example.petcarebe.repository.VetServiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class VetServiceService {

    private final VetServiceRepository vetServiceRepository;


    // Create
    @Transactional
    public VetService createVetService(VetService vetService) {
        return vetServiceRepository.save(vetService);
    }

    // Read - Get all active services
    public List<VetService> getAllActiveVetServices() {
        return vetServiceRepository.findByActiveTrue();
    }

    // Read - Get all services
    public List<VetService> getAllVetServices() {
        return vetServiceRepository.findAll();
    }

    // Read - Get by ID
    public VetService getVetServiceById(Long id) {
        return vetServiceRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("VetService not found with id: " + id));
    }

    // Update
    @Transactional
    public VetService updateVetService(Long id, VetService updatedService) {
        VetService existingService = getVetServiceById(id);

        existingService.setName(updatedService.getName());
        existingService.setDescription(updatedService.getDescription());
        existingService.setPriceBase(updatedService.getPriceBase());
        existingService.setPetType(updatedService.getPetType());
        existingService.setActive(updatedService.getActive());

        return vetServiceRepository.save(existingService);
    }

    // Delete (soft delete - set active to false)
    @Transactional
    public void toggleVetServiceActive(Long id) {
        VetService vetService = getVetServiceById(id);
        vetService.setActive(!vetService.getActive()); // Đảo trạng thái active
        vetServiceRepository.save(vetService);
    }

    public List<VetService> getVetServicesByPetType(PetType petType) {
        return vetServiceRepository.findByPetType(petType);
    }


}
