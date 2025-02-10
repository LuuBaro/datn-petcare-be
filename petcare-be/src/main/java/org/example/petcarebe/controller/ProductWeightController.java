package org.example.petcarebe.controller;


import jakarta.validation.Valid;
import org.example.petcarebe.model.Weights;
import org.example.petcarebe.service.ProductWeightService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/product-weights")
public class ProductWeightController {

    @Autowired
    private ProductWeightService productWeightService;

    @GetMapping
    public List<Weights> getAllProductWeights() {return productWeightService.getAllWeights();}

    @GetMapping("/activeProductWeights")
    public List<Weights> getActiveProductWeights() {
        return productWeightService.getActiveWeights();
    }

    @GetMapping("/getProductWeight/{weightId}")
    public ResponseEntity<Weights> getProductWeightById(@PathVariable Long weightId) {
        Optional<Weights> productWeight = productWeightService.getProductWeightById(weightId);
        return productWeight.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PostMapping("/createProductWeight")
    public ResponseEntity<Weights> createProductWeight(@RequestBody @Valid Weights productWeight) {
        Weights createProductWeight = productWeightService.createWeight(productWeight);
        return ResponseEntity.status(201).body(createProductWeight);
    }

    @PutMapping("/updateProductWeight/{weightId}")
    public ResponseEntity<Weights> updateProductWeight(@PathVariable Long weightId, @Valid @RequestBody Weights productWeight) {
        Weights updatedProductWeight = productWeightService.updateProductWeight(weightId, productWeight);
        return updatedProductWeight != null
                ? ResponseEntity.ok(updatedProductWeight)
                : ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @DeleteMapping("deleteProductWeight/{weightId}")
    public ResponseEntity<Weights> deleteProductWeight(@PathVariable Long weightId) {
        boolean isDeleted = productWeightService.deleteProductWeight(weightId);
        return isDeleted ? ResponseEntity.noContent().build() : ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

}
