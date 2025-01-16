package org.example.petcarebe.controller;


import jakarta.validation.Valid;
import org.example.petcarebe.model.Categories;
import org.example.petcarebe.service.CategoriesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoriesController {

    @Autowired
    private CategoriesService categoriesService;

    // Lấy danh sách tất cả Categories
    @GetMapping("/getAllCategories")
    public List<Categories> getAllCategories() {
        return categoriesService.getAllCategories();
    }

    @GetMapping("/activeProductCategories")
    public List<Categories> getActiveProductCategories() {
        return categoriesService.getActiveCategories();
    }

    // Lấy một Category theo ID
    @GetMapping("/getByIdCategories/{categoryId}")
    public Categories getCategoriesById(@PathVariable Long categoryId) {
        return categoriesService.getCategoriesById(categoryId);
    }

    // Tạo mới một Category
    @PostMapping("/createCategories")
    public ResponseEntity<Categories> createCategories(@Valid @RequestBody Categories category) {
        Categories createdCategory = categoriesService.create(category);
        return ResponseEntity.status(201).body(createdCategory);  // Trả về HTTP 201 (Created)
    }

    // Cập nhật Category theo ID
    @PutMapping("/updateProductCategories/{categoryId}")
    public ResponseEntity<Categories> updateProductCategories(@PathVariable Long categoryId, @Valid @RequestBody Categories categories) {
        Categories updatedCategories = categoriesService.updateCategories(categoryId, categories);
        return updatedCategories != null
                ? ResponseEntity.ok(updatedCategories)
                : ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }


    // Xóa một Category theo ID
    @DeleteMapping("/deleteCategories/{categoryId}")
    public ResponseEntity<String> deleteCategory(@PathVariable Long categoryId) {
        categoriesService.delete(categoryId);
        return ResponseEntity.ok("Danh mục với ID " + categoryId + " đã được xóa thành công.");
    }
}
