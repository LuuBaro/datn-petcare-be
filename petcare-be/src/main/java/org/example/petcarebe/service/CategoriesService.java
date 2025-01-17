package org.example.petcarebe.service;

import org.example.petcarebe.model.Categories;
import org.example.petcarebe.repository.CategoriesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CategoriesService {

    @Autowired
    private CategoriesRepository categoriesRepository;

    // Lấy danh sách tất cả các danh mục
    public List<Categories> getAllCategories() {
        return categoriesRepository.findAll();
    }

    public List<Categories> getActiveCategories() {
        return categoriesRepository.findAll().stream()
                .filter(Categories::getStatus)
                .toList();
    }

    // Lấy một danh mục theo ID
    public Categories getCategoriesById(Long categoriesId) {
        return categoriesRepository.findById(categoriesId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy danh mục với ID: " + categoriesId));
    }

    // Tạo mới một danh mục
    public Categories create(Categories categories) {
        return categoriesRepository.save(categories);
    }

    // Cập nhật danh mục theo ID
    public Categories updateCategories(Long id, Categories categoriesDetails) {
        Categories categories = categoriesRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Loại sản phẩm cho id này :: " + id));

        // Cập nhật tên categoriesValue
        categories.setCategoryName(categoriesDetails.getCategoryName());

        // Cập nhật trạng thái status nếu có
        if (categoriesDetails.getStatus() != null) {
            categories.setStatus(categoriesDetails.getStatus());
        }

        // Lưu đối tượng đã cập nhật vào cơ sở dữ liệu
        return categoriesRepository.save(categories);
    }



    // Xóa danh mục theo ID
    public void delete(Long categorieId) {
        if (categoriesRepository.existsById(categorieId)) {
            categoriesRepository.deleteById(categorieId);
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy danh mục với ID: " + categorieId);
        }
    }
}
