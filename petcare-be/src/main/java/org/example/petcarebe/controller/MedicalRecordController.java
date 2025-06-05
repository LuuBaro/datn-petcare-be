package org.example.petcarebe.controller;

import org.example.petcarebe.dto.MedicalRecordDTO;
import org.example.petcarebe.dto.request.MedicalRecordRequestDTO;
import org.example.petcarebe.service.MedicalRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/medical-records")
public class MedicalRecordController {

    @Autowired
    private MedicalRecordService medicalRecordService;

    // Create a new MedicalRecord
    @PostMapping
    public ResponseEntity<MedicalRecordDTO> createMedicalRecord(@RequestBody MedicalRecordRequestDTO requestDTO) {
        try {
            MedicalRecordDTO createdMedicalRecord = medicalRecordService.createMedicalRecord(requestDTO);
            return ResponseEntity.ok(createdMedicalRecord);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    // Get all MedicalRecords
    @GetMapping
    public ResponseEntity<List<MedicalRecordDTO>> getAllMedicalRecords() {
        List<MedicalRecordDTO> medicalRecords = medicalRecordService.getAllMedicalRecords();
        return ResponseEntity.ok(medicalRecords);
    }

    // Get MedicalRecords by Pet ID
    @GetMapping("/pet/{petId}")
    public ResponseEntity<List<MedicalRecordDTO>> getMedicalRecordsByPetId(@PathVariable Long petId) {
        List<MedicalRecordDTO> medicalRecords = medicalRecordService.getMedicalRecordsByPetId(petId);
        return ResponseEntity.ok(medicalRecords);
    }

    // Get a MedicalRecord by ID
    @GetMapping("/{id}")
    public ResponseEntity<MedicalRecordDTO> getMedicalRecordById(@PathVariable Long id) {
        try {
            MedicalRecordDTO medicalRecord = medicalRecordService.getMedicalRecordById(id);
            return ResponseEntity.ok(medicalRecord);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Update a MedicalRecord
    @PutMapping("/{id}")
    public ResponseEntity<MedicalRecordDTO> updateMedicalRecord(
            @PathVariable Long id,
            @RequestBody MedicalRecordRequestDTO requestDTO) {
        try {
            MedicalRecordDTO updatedMedicalRecord = medicalRecordService.updateMedicalRecord(id, requestDTO);
            return ResponseEntity.ok(updatedMedicalRecord);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    // Delete a MedicalRecord
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMedicalRecord(@PathVariable Long id) {
        try {
            medicalRecordService.deleteMedicalRecord(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}