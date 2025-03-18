package org.example.petcarebe.controller;

import org.example.petcarebe.model.PetWeight;
import org.example.petcarebe.service.PetWeightService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pet-weights")
public class PetWeightController {

    @Autowired
    private PetWeightService petWeightService;

    @GetMapping
    public List<PetWeight> getAllPetWeights() {
        return petWeightService.getAllPetWeights();
    }

    @PostMapping
    public ResponseEntity<PetWeight> createPetWeight(@RequestBody PetWeight petWeight) {
        PetWeight createdPetWeight = petWeightService.createPetWeight(petWeight);
        return ResponseEntity.ok(createdPetWeight);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PetWeight> updatePetWeight(@PathVariable Long id, @RequestBody PetWeight petWeightDetails) {
        PetWeight updatedPetWeight = petWeightService.updatePetWeight(id, petWeightDetails);
        return ResponseEntity.ok(updatedPetWeight);
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivatePetWeight(@PathVariable Long id) {
        petWeightService.deactivatePetWeight(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<Void> activatePetWeight(@PathVariable Long id) {
        petWeightService.activatePetWeight(id);
        return ResponseEntity.ok().build();
    }
}