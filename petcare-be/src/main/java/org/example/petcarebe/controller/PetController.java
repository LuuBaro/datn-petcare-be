package org.example.petcarebe.controller;

import org.example.petcarebe.dto.UpdatePetWeightRequest;
import org.example.petcarebe.model.Pet;
import org.example.petcarebe.service.PetService;
import org.example.petcarebe.service.AppointmentHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pets")
public class PetController {

    @Autowired
    private PetService petService;

    @Autowired
    private AppointmentHistoryService appointmentHistoryService;

    @PutMapping("/{petId}/weight")
    public ResponseEntity<Pet> updatePetWeight(
            @PathVariable Long petId,
            @RequestBody UpdatePetWeightRequest request) {
        try {
            System.out.println("PetController - Received request to update pet weight:");
            System.out.println("  Pet ID: " + petId);
            System.out.println("  New Weight ID: " + request.getPetWeightId());
            System.out.println("  New Price: " + request.getPrice());
            System.out.println("  Appointment ID: " + request.getAppointmentId());
            System.out.println("  User ID: " + request.getUserId());
            System.out.println("  Reason: " + request.getReason());
            

            Pet updatedPet = petService.updatePetWeight(
                    petId,
                    request.getPetWeightId(),
                    request.getPrice(),
                    request.getAppointmentId()
            );

            System.out.println("PetController - Pet weight updated successfully:");
            System.out.println("  Pet ID: " + updatedPet.getId());
            System.out.println("  New Weight Range: " + updatedPet.getPetWeight().getWeightRange());
            System.out.println("  New Price: " + updatedPet.getPrice());
            

            appointmentHistoryService.logAction(
                    request.getAppointmentId(),
                    request.getUserId(),
                    "UPDATE_WEIGHT",
                    null,
                    null,
                    request.getReason()
            );
            
            System.out.println("PetController - History logged for UPDATE_WEIGHT action");

            return ResponseEntity.ok(updatedPet);
        } catch (Exception e) {
            System.err.println("PetController - Error updating pet weight: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(null);
        }
    }
}