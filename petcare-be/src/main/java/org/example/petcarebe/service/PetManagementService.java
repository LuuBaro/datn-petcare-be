package org.example.petcarebe.service;

import org.example.petcarebe.model.Pet;
import org.example.petcarebe.model.PetService;
import org.example.petcarebe.model.PetWeight;
import org.example.petcarebe.repository.PetRepository;
import org.example.petcarebe.repository.PetServiceRepository;
import org.example.petcarebe.repository.PetWeightRepository;
import org.example.petcarebe.enums.PetType;
import org.example.petcarebe.enums.StatusType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.lang.Float;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class PetManagementService {

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private PetWeightRepository petWeightRepository;

    @Autowired
    private PetServiceRepository petServiceRepository;

    @Autowired
    private WebSocketService webSocketService;

    @Transactional
    public Pet updatePetWeight(Long petId, Long petWeightId, double price, Long appointmentId, Float actualWeight) {
        // Tìm pet theo ID
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new RuntimeException("Thú cưng không tồn tại với ID: " + petId));

        // Kiểm tra xem pet có thuộc về lịch hẹn này không
        if (pet.getAppointment() == null || !pet.getAppointment().getAppointmentId().equals(appointmentId)) {
            throw new RuntimeException("Thú cưng không thuộc về lịch hẹn này");
        }

        // Kiểm tra xem cân nặng đã được cập nhật trước đó chưa
        // Giả sử có một cờ trong Pet để theo dõi (cần thêm vào entity nếu chưa có)
        // Ví dụ: pet.isWeightUpdated() (cần thêm vào entity Pet)
        // Đây là giả định, bạn có thể thay bằng kiểm tra lịch sử hành động nếu cần
        if (pet.getWeightUpdateCount() != null && pet.getWeightUpdateCount() > 0) {
            throw new RuntimeException("Cân nặng của thú cưng này đã được cập nhật trước đó. Chỉ được phép cập nhật một lần.");
        }

        // Tìm cân nặng mới theo ID
        PetWeight petWeight = petWeightRepository.findById(petWeightId)
                .orElseThrow(() -> new RuntimeException("Khoảng cân nặng không tồn tại với ID: " + petWeightId));

        // Kiểm tra petType khớp nhau
        if (!pet.getPetType().equals(petWeight.getPetType())) {
            throw new RuntimeException("Loại thú cưng của Pet (" + pet.getPetType() +
                    ") không khớp với loại thú cưng của khoảng cân nặng (" + petWeight.getPetType() + ")");
        }
        if (!pet.getPetType().equals(pet.getPetService().getPetType())) {
            throw new RuntimeException("Loại thú cưng của Pet (" + pet.getPetType() +
                    ") không khớp với loại thú cưng của dịch vụ (" + pet.getPetService().getPetType() + ")");
        }

        // Log thông tin trước khi cập nhật
        System.out.println("Current pet weight: " + (pet.getPetWeight() != null ? pet.getPetWeight().getWeightRange() : "null"));
        System.out.println("Current pet price: " + pet.getPrice());
        System.out.println("New pet weight: " + petWeight.getWeightRange());
        System.out.println("New price: " + price);
        System.out.println("Actual weight: " + actualWeight);

        // Cập nhật cân nặng thực tế, khoảng cân nặng và giá
        pet.setActualWeight(actualWeight);
        pet.setPetWeight(petWeight);
        pet.setPrice(price);

        // Cập nhật số lần chỉnh sửa cân nặng (giả định có cờ)
        pet.setWeightUpdateCount((pet.getWeightUpdateCount() == null ? 0 : pet.getWeightUpdateCount()) + 1);

        // Lưu pet và trả về kết quả
        Pet savedPet = petRepository.save(pet);
        System.out.println("Pet updated successfully - ID: " + savedPet.getId() +
                ", New weight: " + savedPet.getPetWeight().getWeightRange() +
                ", New price: " + savedPet.getPrice() +
                ", Actual weight: " + savedPet.getActualWeight());

        // Gửi thông báo WebSocket
        Map<String, Object> updateMessage = new HashMap<>();
        updateMessage.put("type", "PET_WEIGHT_UPDATED");
        updateMessage.put("appointmentId", appointmentId);
        updateMessage.put("petId", petId);
        updateMessage.put("newWeightRange", petWeight.getWeightRange());
        updateMessage.put("newPrice", price);
        webSocketService.sendToTopic("/topic/appointments", updateMessage.toString());
        System.out.println("WebSocket message sent: " + updateMessage);

        return savedPet;
    }

    public double getServicePrice(Long petServiceId, Long petWeightId) {
        // Tìm PetService theo ID
        PetService petService = petServiceRepository.findById(petServiceId)
                .orElseThrow(() -> new RuntimeException("Dịch vụ không tồn tại với ID: " + petServiceId));

        // Tìm PetWeight theo ID
        PetWeight petWeight = petWeightRepository.findById(petWeightId)
                .orElseThrow(() -> new RuntimeException("Khoảng cân nặng không tồn tại với ID: " + petWeightId));

        // Kiểm tra petType khớp nhau
        if (!petService.getPetType().equals(petWeight.getPetType())) {
            throw new RuntimeException("Loại thú cưng của dịch vụ (" + petService.getPetType() +
                    ") không khớp với loại thú cưng của khoảng cân nặng (" + petWeight.getPetType() + ")");
        }

        // Kiểm tra null cho basePrice
        if (petService.getBasePrice() == null) {
            throw new RuntimeException("Giá cơ bản (basePrice) của dịch vụ không được để trống");
        }

        // Tính giá: basePrice * priceMultiplier và làm tròn về 0 chữ số thập phân
        double price = petService.getBasePrice().doubleValue() * petWeight.getPriceMultiplier();
        BigDecimal roundedPrice = BigDecimal.valueOf(price).setScale(0, RoundingMode.HALF_UP);

        System.out.println("Calculated service price - petServiceId: " + petServiceId +
                ", petWeightId: " + petWeightId +
                ", basePrice: " + petService.getBasePrice() +
                ", priceMultiplier: " + petWeight.getPriceMultiplier() +
                ", final price: " + roundedPrice);

        return roundedPrice.doubleValue();
    }

    public List<PetWeight> getPetWeightsByType(String petTypeStr) {
        // Chuẩn hóa petType từ frontend (CHÓ/MÈO hoặc DOG/CAT)
        String normalizedPetType = petTypeStr.toUpperCase();
        if (normalizedPetType.equals("CHÓ") || normalizedPetType.equals("CHO")) {
            normalizedPetType = "DOG";
        } else if (normalizedPetType.equals("MÈO") || normalizedPetType.equals("MEO")) {
            normalizedPetType = "CAT";
        }

        // Kiểm tra petType hợp lệ
        PetType petType;
        try {
            petType = PetType.valueOf(normalizedPetType);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Loại thú cưng không hợp lệ: " + petTypeStr);
        }

        // Lấy danh sách cân nặng theo petType và statusType = ACTIVE
        List<PetWeight> weights = petWeightRepository.findByPetTypeAndStatusType(petType, StatusType.ACTIVE);
        System.out.println("Fetched pet weights for petType: " + petType + ", count: " + weights.size());

        return weights;
    }
}