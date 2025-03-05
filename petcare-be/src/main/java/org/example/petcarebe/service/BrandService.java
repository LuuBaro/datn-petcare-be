package org.example.petcarebe.service;


import org.example.petcarebe.model.Brand;
import org.example.petcarebe.repository.BrandRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
@Service
public class BrandService {
    @Autowired
    private BrandRepository brandRepository;

    public List<Brand> getActiveBrand() {
        return brandRepository.findAll().stream()
                .filter(Brand::getStatus)
                .toList();
    }

    public List<Brand> getAllBrands() {
        return brandRepository.findAll();
    }

    public Optional<Brand> getBrandById(Long id) {
        return brandRepository.findById(id);
    }

    public Brand saveBrand(Brand brand) {
        return brandRepository.save(brand);
    }

    public Optional<Brand> updateBrand(Long brandId, Brand brand) {
        Optional<Brand> existingBrandOpt = brandRepository.findById(brandId);
        if (existingBrandOpt.isPresent()) {
            Brand existingBrand = existingBrandOpt.get();

            // Cập nhật các thuộc tính của thương hiệu
            existingBrand.setBrandName(brand.getBrandName());
            if (brand.getStatus() != null) { // Kiểm tra nếu `status` được cung cấp
                existingBrand.setStatus(brand.getStatus());
            }

            // Lưu lại thương hiệu đã cập nhật
            return Optional.of(brandRepository.save(existingBrand));
        }
        return Optional.empty(); // Trả về Optional.empty() nếu không tìm thấy thương hiệu
    }


    public void deleteBrand(Long BrandId) {
        brandRepository.deleteById(BrandId);
    }
}
