package org.example.petcarebe.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.petcarebe.dto.VetPetDTO;
import org.example.petcarebe.dto.VetPetWeightDTO;
import org.example.petcarebe.service.VetPetService;
import org.example.petcarebe.service.VetPetWeightService; // Sửa thành VetPetWeightService
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pets")
@RequiredArgsConstructor
public class VetPetController {

    private final VetPetService vetPetService;
    private final VetPetWeightService vetPetWeightService; // Sử dụng VetPetWeightService

    @GetMapping
    public ResponseEntity<Page<VetPetDTO>> getAllPets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<VetPetDTO> pets = vetPetService.getAllPets(pageable);
        return ResponseEntity.ok(pets);
    }

    @GetMapping("/{id}")
    public ResponseEntity<VetPetDTO> getPetById(@PathVariable Long id) {
        return vetPetService.getPetById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<VetPetDTO> createPet(@Valid @RequestBody VetPetDTO vetPetDTO) {
        VetPetDTO createdPet = vetPetService.createPet(vetPetDTO);
        return new ResponseEntity<>(createdPet, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<VetPetDTO> updatePet(@PathVariable Long id, @Valid @RequestBody VetPetDTO vetPetDTO) {
        return vetPetService.updatePet(id, vetPetDTO)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePet(@PathVariable Long id) {
        boolean deleted = vetPetService.deletePet(id);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @GetMapping("/weights")
    public ResponseEntity<List<VetPetWeightDTO>> getAllPetWeights() {
        List<VetPetWeightDTO> weights = vetPetWeightService.getAllPetWeights();
        return ResponseEntity.ok(weights);
    }
}