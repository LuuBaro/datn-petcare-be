package org.example.petcarebe.controller;



import jakarta.validation.Valid;
import org.example.petcarebe.model.Brand;
import org.example.petcarebe.service.BrandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("api/brands")

public class BrandController {

    @Autowired
    private BrandService brandService;

    @GetMapping("/activeBrand")
    public List<Brand> getActiveBrands() {
        return brandService.getActiveBrand();
    }


    @GetMapping("/getAllBrand")
    public List<Brand>getAllBrands() {return brandService.getAllBrands();}

    @GetMapping("/getByBrandId/{BrandId}")
    public Optional<Brand> getBrandById(@PathVariable Long BrandId) {return brandService.getBrandById(BrandId);}

    @PostMapping("/create")
    public ResponseEntity<Brand> createBrand(@RequestBody @Valid Brand brand) {
        Brand createdBrand = brandService.saveBrand(brand);
        return ResponseEntity.status(201).body(createdBrand);
    }

    // Phương thức update
    @PutMapping("/update/{BrandId}")
    public ResponseEntity<Brand> updateBrand(@PathVariable Long BrandId, @RequestBody @Valid Brand brand) {
        Optional<Brand> updatedBrandOpt = brandService.updateBrand(BrandId, brand);

        if (updatedBrandOpt.isPresent()) {
            return ResponseEntity.ok(updatedBrandOpt.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/delete/{BrandId}")
    public void deleteBrand(@PathVariable Long BrandId) {
    brandService.deleteBrand(BrandId);
}
}
