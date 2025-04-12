package org.example.petcarebe.controller;

import lombok.RequiredArgsConstructor;
import org.example.petcarebe.model.VetService;
import org.example.petcarebe.service.VetServiceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vet-services")
@RequiredArgsConstructor
public class VetServiceController {

    private final VetServiceService vetServiceService;

    // Create
    @PostMapping("/createVetService")
    public ResponseEntity<VetService> createVetService(@RequestBody VetService vetService) {
        VetService createdService = vetServiceService.createVetService(vetService);
        return new ResponseEntity<>(createdService, HttpStatus.CREATED);
    }

    // Read - Get all active services
    @GetMapping("/active")
    public ResponseEntity<List<VetService>> getAllActiveVetServices() {
        List<VetService> services = vetServiceService.getAllActiveVetServices();
        return new ResponseEntity<>(services, HttpStatus.OK);
    }

    // Read - Get all services
    @GetMapping("getAllVetServices")
    public ResponseEntity<List<VetService>> getAllVetServices() {
        List<VetService> services = vetServiceService.getAllVetServices();
        return new ResponseEntity<>(services, HttpStatus.OK);
    }

    // Read - Get by ID
    @GetMapping("/getVetServiceById/{id}")
    public ResponseEntity<VetService> getVetServiceById(@PathVariable Long id) {
        VetService service = vetServiceService.getVetServiceById(id);
        return new ResponseEntity<>(service, HttpStatus.OK);
    }

    // Update
    @PutMapping("/updateVetService/{id}")
    public ResponseEntity<VetService> updateVetService(@PathVariable Long id, @RequestBody VetService vetService) {
        VetService updatedService = vetServiceService.updateVetService(id, vetService);
        return new ResponseEntity<>(updatedService, HttpStatus.OK);
    }

    // Delete
    @DeleteMapping("/deleteVetService/{id}")
    public ResponseEntity<Void> deleteVetService(@PathVariable Long id) {
        vetServiceService.deleteVetService(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}