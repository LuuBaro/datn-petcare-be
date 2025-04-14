package org.example.petcarebe.controller;

import lombok.RequiredArgsConstructor;
import org.example.petcarebe.dto.VetPetDTO;
import org.example.petcarebe.service.VetPetService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pets")
@RequiredArgsConstructor
public class VetPetController {

    private final VetPetService vetPetService;

    @GetMapping
    public ResponseEntity<List<VetPetDTO>> getAllPets() {
        List<VetPetDTO> pets = vetPetService.getAllPets();
        return ResponseEntity.ok(pets);
    }

    @GetMapping("/{id}")
    public ResponseEntity<VetPetDTO> getPetById(@PathVariable Long id) {
        return vetPetService.getPetById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<VetPetDTO> createPet(@RequestBody VetPetDTO vetPetDTO) {
        VetPetDTO createdPet = vetPetService.createPet(vetPetDTO);
        return new ResponseEntity<>(createdPet, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<VetPetDTO> updatePet(@PathVariable Long id, @RequestBody VetPetDTO vetPetDTO) {
        return vetPetService.updatePet(id, vetPetDTO)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePet(@PathVariable Long id) {
        boolean deleted = vetPetService.deletePet(id);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}