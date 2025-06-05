package org.example.petcarebe.service;

import lombok.RequiredArgsConstructor;
import org.example.petcarebe.dto.*;
import org.example.petcarebe.model.*;
import org.example.petcarebe.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VetOrderService {

    private final OrderRepository ordersRepository;
    private final OrderVetDetailRepository orderVetDetailRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final UserRepository userRepository;
    private final VetServiceRepository vetServiceRepository;
    private final VetPetWeightRepository vetPetWeightRepository;
    private final VaccineRepository vaccineRepository;
    private final StatusOrderRepository statusOrderRepository;
    private final VetPetRepository petRepository;

    @Transactional
    public User getUserNameByUserId(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Transactional
    public Orders createVetOrder(Long userId, List<MedicalRecordDTO> medicalRecordDTOs, String paymentMethod) {
        // Tìm user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Tạo đối tượng Orders
        Orders order = new Orders();
        order.setUser(user);
        order.setOrderDate(new Date());
        order.setPaymentMethod(paymentMethod);
        order.setPaymentStatus("Đã thanh toán");
        order.setType("VET_SERVICE");
        order.setTotalAmount(0f); // Sẽ được tính sau

        // Lưu order trước để có orderId
        Orders savedOrder = ordersRepository.save(order);

        // Xử lý các MedicalRecord và OrderVetDetail
        float totalAmount = 0f;
        for (MedicalRecordDTO medicalRecordDTO : medicalRecordDTOs) {
            // Tìm hoặc tạo MedicalRecord
            MedicalRecord medicalRecord;
            if (medicalRecordDTO.getId() != null) {
                medicalRecord = medicalRecordRepository.findById(medicalRecordDTO.getId())
                        .orElseThrow(() -> new RuntimeException("MedicalRecord not found"));
            } else {
                medicalRecord = new MedicalRecord();

                // Chuyển đổi VetPetDTO thành Pet
                VetPetDTO vetPetDTO = medicalRecordDTO.getVetPetDTO();
                if (vetPetDTO == null) {
                    throw new RuntimeException("VetPetDTO cannot be null in MedicalRecordDTO");
                }

                Pet pet;
                if (vetPetDTO.getId() != null) {
                    pet = petRepository.findById(vetPetDTO.getId())
                            .orElseThrow(() -> new RuntimeException("Pet not found with ID: " + vetPetDTO.getId()));
                } else {
                    pet = new Pet();
                    pet.setNamePet(vetPetDTO.getNamePet());
                    pet.setAge(vetPetDTO.getAge());
                    pet.setNote(vetPetDTO.getNote());
                    pet.setPhoneBoss(vetPetDTO.getPhoneBoss());
                    pet.setNameBoss(vetPetDTO.getNameBoss());
                    pet.setPetType(vetPetDTO.getPetType());
                    pet.setDeleted(vetPetDTO.getDeleted());

                    // Chuyển đổi VetPetWeightDTO thành PetWeight
                    if (vetPetDTO.getPetWeight() != null) {
                        PetWeight petWeight;
                        if (vetPetDTO.getPetWeight().getPetWeightId() != null) {
                            petWeight = vetPetWeightRepository.findById(vetPetDTO.getPetWeight().getPetWeightId())
                                    .orElseThrow(() -> new RuntimeException("PetWeight not found with ID: " + vetPetDTO.getPetWeight().getPetWeightId()));
                        } else {
                            petWeight = new PetWeight();
                            petWeight.setPetType(vetPetDTO.getPetWeight().getPetType());
                            petWeight.setWeightRange(vetPetDTO.getPetWeight().getWeightRange());
                            petWeight.setPriceMultiplier(vetPetDTO.getPetWeight().getPriceMultiplier());
                            petWeight.setStatusType(vetPetDTO.getPetWeight().getStatusType());
                            petWeight = vetPetWeightRepository.save(petWeight);
                        }
                        pet.setPetWeight(petWeight);
                    }

                    // Lưu Pet vào DB
                    pet = petRepository.save(pet);
                }

                medicalRecord.setPet(pet);
                medicalRecord.setExamDate(medicalRecordDTO.getExamDate());
                medicalRecord.setSymptoms(medicalRecordDTO.getSymptoms());
                medicalRecord.setDiagnosis(medicalRecordDTO.getDiagnosis());
                medicalRecord.setTreatment(medicalRecordDTO.getTreatment());
                medicalRecord.setNote(medicalRecordDTO.getNote());
                medicalRecord.setCreatedAt(medicalRecordDTO.getCreatedAt());
                medicalRecord.setUpdatedAt(medicalRecordDTO.getUpdatedAt());

                // Gán VetService và Vaccine nếu có
                if (medicalRecordDTO.getVetServiceId() != null) {
                    VetService vetService = vetServiceRepository.findById(medicalRecordDTO.getVetServiceId())
                            .orElseThrow(() -> new RuntimeException("VetService not found with ID: " + medicalRecordDTO.getVetServiceId()));
                    medicalRecord.setVetService(vetService);
                }
                if (medicalRecordDTO.getVaccineId() != null) {
                    Vaccine vaccine = vaccineRepository.findById(medicalRecordDTO.getVaccineId())
                            .orElseThrow(() -> new RuntimeException("Vaccine not found with ID: " + medicalRecordDTO.getVaccineId()));
                    medicalRecord.setVaccine(vaccine);
                }

                medicalRecord = medicalRecordRepository.save(medicalRecord);
            }

            // Kiểm tra số lượng vaccine trước khi tạo OrderVetDetail
            if (medicalRecord.getVaccine() != null) {
                Vaccine vaccine = medicalRecord.getVaccine();
                int requiredQuantity = 1; // Mặc định quantity là 1
                if (vaccine.getQuantity() < requiredQuantity) {
                    throw new RuntimeException("Vaccine " + vaccine.getName() + " is out of stock or insufficient quantity. Available: " + vaccine.getQuantity());
                }
            }

            // Tạo OrderVetDetail
            OrderVetDetail orderVetDetail = new OrderVetDetail();
            orderVetDetail.setOrderId(savedOrder);
            orderVetDetail.setMedicalRecord(medicalRecord);
            orderVetDetail.setQuantity(1); // Mặc định là 1, có thể tùy chỉnh
            orderVetDetail.setPrice(calculatePrice(medicalRecord)); // Tính giá
            orderVetDetailRepository.save(orderVetDetail);

            totalAmount += orderVetDetail.getPrice() * orderVetDetail.getQuantity();
        }

        // Cập nhật totalAmount cho order
        savedOrder.setTotalAmount(totalAmount);
        ordersRepository.save(savedOrder);

        return savedOrder;
    }

    @Transactional
    public Orders processPayment(Long orderId, String paymentStatus) {
        Orders order = ordersRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        order.setPaymentStatus(paymentStatus);
        if ("COMPLETED".equals(paymentStatus)) {
            // Tìm StatusOrder từ DB
            StatusOrder completedStatus = statusOrderRepository.findByStatusName("COMPLETED");
            if (completedStatus == null) {
                throw new RuntimeException("StatusOrder 'COMPLETED' not found in database");
            }
            order.setStatusOrder(completedStatus);

            // Lấy danh sách OrderVetDetail liên quan đến order
            List<OrderVetDetail> orderVetDetails = orderVetDetailRepository.findByOrderId_OrderId(orderId);
            for (OrderVetDetail orderVetDetail : orderVetDetails) {
                MedicalRecord medicalRecord = orderVetDetail.getMedicalRecord();
                if (medicalRecord.getVaccine() != null) {
                    // Giảm số lượng vaccine
                    Vaccine vaccine = medicalRecord.getVaccine();
                    if (vaccine.getQuantity() <= 0) {
                        throw new RuntimeException("Vaccine " + vaccine.getName() + " is out of stock");
                    }
                    vaccine.setQuantity(vaccine.getQuantity() - orderVetDetail.getQuantity());
                    vaccineRepository.save(vaccine);
                }
            }
        }

        return ordersRepository.save(order);
    }

    // Hàm tính giá cho MedicalRecord
    private Float calculatePrice(MedicalRecord medicalRecord) {
        Float totalPrice = 0f;

        // Lấy Pet từ MedicalRecord để lấy priceMultiplier
        Pet pet = medicalRecord.getPet();
        Float priceMultiplier = 1.0f; // Default multiplier nếu không có petWeight
        if (pet != null && pet.getPetWeight() != null) {
            priceMultiplier = pet.getPetWeight().getPriceMultiplier();
        }

        // Tính giá từ VetService (nếu có)
        if (medicalRecord.getVetService() != null) {
            VetService vetService = medicalRecord.getVetService();
            Float basePrice = vetService.getPriceBase();
            totalPrice += basePrice * priceMultiplier;
        }

        // Tính giá từ Vaccine (nếu có)
        if (medicalRecord.getVaccine() != null) {
            Vaccine vaccine = medicalRecord.getVaccine();
            totalPrice += vaccine.getSellingPrice() * priceMultiplier;
        }

        return totalPrice;
    }

    // Lấy thông tin order theo orderId
    public Optional<Orders> getOrderById(Long orderId) {
        return ordersRepository.findById(orderId);
    }

    // Lấy danh sách orders theo userId
    public List<Orders> getOrdersByUserId(Long userId) {
        return ordersRepository.findByUserUserId(userId);
    }

    // Lấy danh sách VetOrderDTO theo userId, chỉ lấy orders có type = "VET_SERVICE"
    public List<VetOrderDTO> getVetOrderDTOsByUserId(Long userId) {
        List<Orders> orders = ordersRepository.findByUserUserIdAndType(userId, "VET_SERVICE");
        return orders.stream().map(this::convertToVetOrderDTO).collect(Collectors.toList());
    }

    // Lấy tất cả danh sách VetOrderDTO với type = "VET_SERVICE"
    public List<VetOrderDTO> getAllVetOrdersByTypeVetService() {
        List<Orders> orders = ordersRepository.findByType("VET_SERVICE");
        return orders.stream()
                .map(this::convertToVetOrderDTO)
                .collect(Collectors.toList());
    }

    // Chuyển đổi Orders thành VetOrderDTO
    private VetOrderDTO convertToVetOrderDTO(Orders order) {
        VetOrderDTO vetOrderDTO = new VetOrderDTO();
        vetOrderDTO.setId(order.getOrderId());
        vetOrderDTO.setUserId(order.getUser().getUserId());
        vetOrderDTO.setOrderDate(order.getOrderDate());
        vetOrderDTO.setPaymentMethod(order.getPaymentMethod());
        vetOrderDTO.setPaymentStatus(order.getPaymentStatus());
        vetOrderDTO.setType(order.getType());
        vetOrderDTO.setTotalAmount(order.getTotalAmount());

        // Lấy danh sách OrderVetDetail và chuyển đổi thành OrderVetDetailDTO
        List<OrderVetDetail> orderVetDetails = orderVetDetailRepository.findByOrderId_OrderId(order.getOrderId());
        List<OrderVetDetailDTO> orderVetDetailDTOs = orderVetDetails.stream()
                .map(this::convertToOrderVetDetailDTO)
                .collect(Collectors.toList());
        vetOrderDTO.setOrderVetDetails(orderVetDetailDTOs);

        return vetOrderDTO;
    }

    // Chuyển đổi OrderVetDetail thành OrderVetDetailDTO
    private OrderVetDetailDTO convertToOrderVetDetailDTO(OrderVetDetail orderVetDetail) {
        OrderVetDetailDTO orderVetDetailDTO = new OrderVetDetailDTO();
        orderVetDetailDTO.setId(orderVetDetail.getId());
        orderVetDetailDTO.setOrderId(orderVetDetail.getOrderId().getOrderId());
        orderVetDetailDTO.setQuantity(orderVetDetail.getQuantity());
        orderVetDetailDTO.setPrice(orderVetDetail.getPrice());

        // Chuyển đổi MedicalRecord thành MedicalRecordDTO
        MedicalRecord medicalRecord = orderVetDetail.getMedicalRecord();
        MedicalRecordDTO medicalRecordDTO = convertToMedicalRecordDTO(medicalRecord);
        orderVetDetailDTO.setMedicalRecord(medicalRecordDTO);

        return orderVetDetailDTO;
    }

    // Chuyển đổi MedicalRecord thành MedicalRecordDTO
    private MedicalRecordDTO convertToMedicalRecordDTO(MedicalRecord medicalRecord) {
        MedicalRecordDTO medicalRecordDTO = new MedicalRecordDTO();
        medicalRecordDTO.setId(medicalRecord.getId());
        medicalRecordDTO.setExamDate(medicalRecord.getExamDate());
        medicalRecordDTO.setSymptoms(medicalRecord.getSymptoms());
        medicalRecordDTO.setDiagnosis(medicalRecord.getDiagnosis());
        medicalRecordDTO.setTreatment(medicalRecord.getTreatment());
        medicalRecordDTO.setNote(medicalRecord.getNote());
        medicalRecordDTO.setCreatedAt(medicalRecord.getCreatedAt());
        medicalRecordDTO.setUpdatedAt(medicalRecord.getUpdatedAt());

        // Gán VetServiceId nếu có
        if (medicalRecord.getVetService() != null) {
            medicalRecordDTO.setVetServiceId(medicalRecord.getVetService().getId());
        }

        // Gán VaccineId nếu có
        if (medicalRecord.getVaccine() != null) {
            medicalRecordDTO.setVaccineId(medicalRecord.getVaccine().getId());
        }

        // Chuyển đổi Pet thành VetPetDTO
        Pet pet = medicalRecord.getPet();
        if (pet != null) {
            VetPetDTO vetPetDTO = new VetPetDTO();
            vetPetDTO.setId(pet.getId());
            vetPetDTO.setNamePet(pet.getNamePet());
            vetPetDTO.setAge(pet.getAge());
            vetPetDTO.setNote(pet.getNote());
            vetPetDTO.setPhoneBoss(pet.getPhoneBoss());
            vetPetDTO.setNameBoss(pet.getNameBoss());
            vetPetDTO.setPetType(pet.getPetType());
            vetPetDTO.setDeleted(pet.getDeleted());

            // Chuyển đổi PetWeight nếu có
            if (pet.getPetWeight() != null) {
                VetPetWeightDTO vetPetWeightDTO = new VetPetWeightDTO();
                vetPetWeightDTO.setPetWeightId(pet.getPetWeight().getPetWeightId());
                vetPetWeightDTO.setPetType(pet.getPetWeight().getPetType());
                vetPetWeightDTO.setWeightRange(pet.getPetWeight().getWeightRange());
                vetPetWeightDTO.setPriceMultiplier(pet.getPetWeight().getPriceMultiplier());
                vetPetWeightDTO.setStatusType(pet.getPetWeight().getStatusType());
                vetPetDTO.setPetWeight(vetPetWeightDTO);
            }

            medicalRecordDTO.setVetPetDTO(vetPetDTO);
        }

        return medicalRecordDTO;
    }
}