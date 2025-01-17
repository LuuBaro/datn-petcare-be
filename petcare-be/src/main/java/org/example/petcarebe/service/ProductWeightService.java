package org.example.petcarebe.service;


import org.example.petcarebe.model.Weights;
import org.example.petcarebe.repository.ProductWeightRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductWeightService {

    @Autowired
    private ProductWeightRepository productWeightRepository;

    public List<Weights> getAllWeights() {
        return productWeightRepository.findAll();
    }

    public List<Weights> getActiveWeights() {
        return productWeightRepository.findAll().stream()
                .filter(Weights::getStatus)
                .toList();
    }

    public Weights createWeight(Weights weights) {
        return productWeightRepository.save(weights);
    }

    public Optional<Weights> getProductWeightById(Long id) {
        return productWeightRepository.findById(id);
    }

    public Weights updateProductWeight(Long id, Weights weightsDetails) {
        Weights weights = productWeightRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Cân nặng sản phẩm cho id này :: " + id));

        // Cập nhật giá trị weightValue
        weights.setWeightValue(weightsDetails.getWeightValue());

        // Cập nhật trạng thái status nếu có
        if (weightsDetails.getStatus() != null) {
            weights.setStatus(weightsDetails.getStatus());
        }

        // Lưu đối tượng đã cập nhật vào cơ sở dữ liệu
        return productWeightRepository.save(weights);
    }


    // Delete a product weight by its ID
    public boolean deleteProductWeight(Long id) {
        if (productWeightRepository.existsById(id)) {
            productWeightRepository.deleteById(id);
            return true;
        }
        return false; // Or throw an exception if you want
    }
}
