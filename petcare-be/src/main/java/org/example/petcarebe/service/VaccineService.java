package org.example.petcarebe.service;

import org.example.petcarebe.model.Vaccine;
import org.example.petcarebe.repository.VaccineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class VaccineService {

    @Autowired
    private VaccineRepository vaccineRepository;

    // Create
    public Vaccine createVaccine(Vaccine vaccine) {
        validateVaccine(vaccine, null);
        return vaccineRepository.save(vaccine);
    }

    // Read (Get all)
    public List<Vaccine> getAllVaccines() {
        List<Vaccine> vaccines = vaccineRepository.findAll();
        LocalDate today = LocalDate.now();

        // Check and update status for expired vaccines
        return vaccines.stream()
                .map(vaccine -> {
                    if (vaccine.getExpiryDate() != null && vaccine.getExpiryDate().isBefore(today) && vaccine.getStatus()) {
                        vaccine.setStatus(false);
                        return vaccineRepository.save(vaccine); // Save updated status
                    }
                    return vaccine;
                })
                .collect(Collectors.toList());
    }

    // Read (Get by ID)
    public Optional<Vaccine> getVaccineById(Long id) {
        Optional<Vaccine> vaccineOpt = vaccineRepository.findById(id);
        if (vaccineOpt.isPresent()) {
            Vaccine vaccine = vaccineOpt.get();
            LocalDate today = LocalDate.now();

            // Check and update status if expired
            if (vaccine.getExpiryDate() != null && vaccine.getExpiryDate().isBefore(today) && vaccine.getStatus()) {
                vaccine.setStatus(false);
                return Optional.of(vaccineRepository.save(vaccine)); // Save updated status
            }
        }
        return vaccineOpt;
    }

    // Update
    public Vaccine updateVaccine(Long id, Vaccine updatedVaccine) {
        Optional<Vaccine> existingVaccine = vaccineRepository.findById(id);
        if (existingVaccine.isPresent()) {
            validateVaccine(updatedVaccine, id);
            Vaccine vaccine = existingVaccine.get();
            vaccine.setName(updatedVaccine.getName());
            vaccine.setOrigin(updatedVaccine.getOrigin());
            vaccine.setManufacturingDate(updatedVaccine.getManufacturingDate());
            vaccine.setExpiryDate(updatedVaccine.getExpiryDate());
            vaccine.setEntryDate(updatedVaccine.getEntryDate());
            vaccine.setType(updatedVaccine.getType());
            vaccine.setStatus(updatedVaccine.getStatus());
            vaccine.setSellingPrice(updatedVaccine.getSellingPrice());
            vaccine.setImportPrice(updatedVaccine.getImportPrice());
            vaccine.setQuantity(updatedVaccine.getQuantity());
            vaccine.setNote(updatedVaccine.getNote());
            return vaccineRepository.save(vaccine);
        } else {
            throw new RuntimeException("Vaccine not found with id: " + id);
        }
    }

    // Toggle status
    public void deleteVaccine(Long id) {
        Optional<Vaccine> existingVaccine = vaccineRepository.findById(id);
        if (existingVaccine.isPresent()) {
            Vaccine vaccine = existingVaccine.get();
            vaccine.setStatus(!vaccine.getStatus()); // Toggle status (true -> false, false -> true)
            vaccineRepository.save(vaccine); // Lưu lại thay đổi
        } else {
            throw new RuntimeException("Vaccine not found with id: " + id);
        }
    }

    private void validateVaccine(Vaccine vaccine, Long id) {
        LocalDate today = LocalDate.now();

        // Kiểm tra trùng tên
        Optional<Vaccine> existingVaccineByName = vaccineRepository.findByName(vaccine.getName());
        if (existingVaccineByName.isPresent() && (id == null || !existingVaccineByName.get().getId().equals(id))) {
            throw new IllegalArgumentException("Tên vaccine '" + vaccine.getName() + "' đã tồn tại!");
        }

        // Kiểm tra ngày sản xuất và ngày nhập kho
        if (vaccine.getManufacturingDate() != null && vaccine.getEntryDate() != null) {
            if (vaccine.getManufacturingDate().isAfter(vaccine.getEntryDate())) {
                throw new IllegalArgumentException("Ngày sản xuất không được lớn hơn ngày nhập kho!");
            }
        }

        // Kiểm tra ngày sản xuất không lớn hơn ngày hiện tại
        if (vaccine.getManufacturingDate() != null && vaccine.getManufacturingDate().isAfter(today)) {
            throw new IllegalArgumentException("Ngày sản xuất không được lớn hơn ngày hiện tại!");
        }

        // Kiểm tra ngày hết hạn
        if (vaccine.getExpiryDate() != null) {
            if (vaccine.getExpiryDate().isBefore(today)) {
                throw new IllegalArgumentException("Ngày hết hạn không được nhỏ hơn ngày hiện tại!");
            }
            if (vaccine.getManufacturingDate() != null && !vaccine.getExpiryDate().isAfter(vaccine.getManufacturingDate())) {
                throw new IllegalArgumentException("Ngày hết hạn phải lớn hơn ngày sản xuất!");
            }
        }

        // Kiểm tra giá và số lượng
        if (vaccine.getSellingPrice() == null || vaccine.getSellingPrice() < 0) {
            throw new IllegalArgumentException("Giá bán phải là số không âm!");
        }
        if (vaccine.getImportPrice() == null || vaccine.getImportPrice() < 0) {
            throw new IllegalArgumentException("Giá nhập phải là số không âm!");
        }
        if (vaccine.getImportPrice() != null && vaccine.getSellingPrice() != null && vaccine.getImportPrice() > vaccine.getSellingPrice()) {
            throw new IllegalArgumentException("Giá nhập không được lớn hơn giá bán!");
        }
        if (vaccine.getQuantity() == null || vaccine.getQuantity() < 0) {
            throw new IllegalArgumentException("Số lượng phải là số không âm!");
        }

        // Kiểm tra các trường bắt buộc
        if (vaccine.getName() == null || vaccine.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên vaccine không được để trống!");
        }
        if (vaccine.getType() == null || vaccine.getType().trim().isEmpty()) {
            throw new IllegalArgumentException("Loại vaccine không được để trống!");
        }
    }
}