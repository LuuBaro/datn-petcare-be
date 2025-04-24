// PetServiceController.java
package org.example.petcarebe.controller;

import org.example.petcarebe.enums.PetType;
import org.example.petcarebe.model.PetService;
import org.example.petcarebe.service.PetServiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pet-services")
public class PetServiceController {

    @Autowired
    private PetServiceService petServiceService;

    @GetMapping
    public List<PetService> getAllServices() {
        return petServiceService.getAllServices();
    }

    @PostMapping
    public ResponseEntity<PetService> createService(@RequestBody PetService petService) {
        PetService createdService = petServiceService.createService(petService);
        return ResponseEntity.ok(createdService);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PetService> updateService(@PathVariable Long id, @RequestBody PetService petServiceDetails) {
        PetService updatedService = petServiceService.updateService(id, petServiceDetails);
        return ResponseEntity.ok(updatedService);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateService(@PathVariable Long id) {
        petServiceService.deactivateService(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/by-pet-type")
    public ResponseEntity<List<PetService>> getServicesByPetType(@RequestParam("petType") String petType) {
        try {
            PetType type = PetType.valueOf(petType.toUpperCase());
            List<PetService> services = petServiceService.getServicesByPetType(type);
            return ResponseEntity.ok(services);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}