package org.example.petcarebe.service;

import org.example.petcarebe.model.Pet;
import org.example.petcarebe.model.PetWeight;
import org.example.petcarebe.repository.PetRepository;
import org.example.petcarebe.repository.PetWeightRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PetService {

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private PetWeightRepository petWeightRepository;

    @Transactional
    public Pet updatePetWeight(Long petId, Long petWeightId, double price, Long appointmentId) {
        // Thêm logs để debug
        System.out.println("Updating pet weight - petId: " + petId + ", petWeightId: " + petWeightId + 
                           ", price: " + price + ", appointmentId: " + appointmentId);
        
        // Tìm pet theo ID
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new RuntimeException("Thú cưng không tồn tại với ID: " + petId));
        
        // Kiểm tra xem pet có thuộc về lịch hẹn này không
        if (pet.getAppointment() == null || !pet.getAppointment().getAppointmentId().equals(appointmentId)) {
            throw new RuntimeException("Thú cưng không thuộc về lịch hẹn này");
        }

        // Tìm cân nặng mới theo ID
        PetWeight petWeight = petWeightRepository.findById(petWeightId)
                .orElseThrow(() -> new RuntimeException("Khoảng cân nặng không tồn tại với ID: " + petWeightId));

        // Log thông tin trước khi cập nhật
        System.out.println("Current pet weight: " + (pet.getPetWeight() != null ? pet.getPetWeight().getWeightRange() : "null"));
        System.out.println("Current pet price: " + pet.getPrice());
        System.out.println("New pet weight: " + petWeight.getWeightRange());
        System.out.println("New price: " + price);

        // Cập nhật cân nặng và giá
        pet.setPetWeight(petWeight);
        pet.setPrice(price);

        // Lưu pet và trả về kết quả
        Pet savedPet = petRepository.save(pet);
        System.out.println("Pet updated successfully - ID: " + savedPet.getId() + 
                           ", New weight: " + savedPet.getPetWeight().getWeightRange() + 
                           ", New price: " + savedPet.getPrice());
        
        return savedPet;
    }
}