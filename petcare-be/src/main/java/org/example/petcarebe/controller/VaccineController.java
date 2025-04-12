package org.example.petcarebe.controller;

import jakarta.validation.Valid;
import org.example.petcarebe.model.Vaccine;
import org.example.petcarebe.service.VaccineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/vaccines")
public class VaccineController {

    @Autowired
    private VaccineService vaccineService;

    // Create
    @PostMapping("/createVaccine")
    public ResponseEntity<Vaccine> createVaccine(@Valid @RequestBody Vaccine vaccine) {
        Vaccine createdVaccine = vaccineService.createVaccine(vaccine);
        return ResponseEntity.ok(createdVaccine);
    }

    // Read (Get all)
    @GetMapping("/getAllVaccine")
    public ResponseEntity<List<Vaccine>> getAllVaccines() {
        List<Vaccine> vaccines = vaccineService.getAllVaccines();
        return ResponseEntity.ok(vaccines);
    }

    // Read (Get by ID)
    @GetMapping("/getVaccineId/{id}")
    public ResponseEntity<Vaccine> getVaccineById(@PathVariable Long id) {
        Optional<Vaccine> vaccine = vaccineService.getVaccineById(id);
        return vaccine.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Update
    @PutMapping("/updateVaccine/{id}")
    public ResponseEntity<Vaccine> updateVaccine(@PathVariable Long id, @Valid @RequestBody Vaccine vaccine) {
        Vaccine updatedVaccine = vaccineService.updateVaccine(id, vaccine);
        return ResponseEntity.ok(updatedVaccine);
    }

    // Delete (Soft delete - đổi status thành false)
    @DeleteMapping("/deleteVaccine/{id}")
    public ResponseEntity<Vaccine> deleteVaccine(@PathVariable Long id) {
        vaccineService.deleteVaccine(id);
        Optional<Vaccine> vaccine = vaccineService.getVaccineById(id); // Lấy lại vaccine để trả về
        return vaccine.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
