package org.example.petcarebe.controller;


import lombok.RequiredArgsConstructor;
import org.example.petcarebe.dto.ProductDetailsDTO;
import org.example.petcarebe.model.ProductDetails;
import org.example.petcarebe.service.ProductDetailsService;
import org.example.petcarebe.service.ProductImagesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/productDetails")
public class ProductDetailsController {

    @Autowired
    private final ProductDetailsService productDetailsService;

    @Autowired
    private final ProductImagesService productImagesService;

    @GetMapping("/getAll")
    public List<ProductDetailsDTO> getAllProductDetails() {
        List<ProductDetailsDTO> productDetailsList = productDetailsService.findAllProductDetails();
        for (ProductDetailsDTO productDetails : productDetailsList) {
            List<String> imageUrls = productImagesService.getImageUrlsByProductDetailId(productDetails.getProductDetailId());

            if (imageUrls != null && !imageUrls.isEmpty()) {
                productDetails.setImageUrls(imageUrls);
            }
        }
        return productDetailsList;
    }


    @GetMapping("/getById/{productDetailId}")
    public ProductDetailsDTO getProductDetails(@PathVariable Long productDetailId) {
        ProductDetailsDTO productDetails = productDetailsService.getProductDetailsById(productDetailId);
        List<String> imageUrls = productImagesService.getImageUrlsByProductDetailId(productDetailId);
        productDetails.setImageUrls(imageUrls);

        return productDetails;
    }


    @GetMapping("/find")
    public Long findProductDetailId(
            @RequestParam String colorValue,
            @RequestParam String sizeValue,
            @RequestParam String weightValue) {

        return productDetailsService.getProductDetailIdByVariants(colorValue, sizeValue, weightValue);
    }




@GetMapping("/getProductDetails")
public ProductDetailsDTO getProductDetails(
        @RequestParam(required = false) Long productDetailId,
        @RequestParam(required = false) String color,
        @RequestParam(required = false) String size,
        @RequestParam(required = false) String weight) {

    if (productDetailId != null) {
        ProductDetailsDTO productDetails = productDetailsService.getProductDetailsById(productDetailId);
        List<String> imageUrls = productImagesService.getImageUrlsByProductDetailId(productDetailId);
        if (imageUrls != null && !imageUrls.isEmpty()) {
            productDetails.setImageUrls(imageUrls);
        }
        return productDetails;
    } else if (color != null && size != null && weight != null) {
        Long foundProductDetailId = productDetailsService.getProductDetailIdByVariants(color, size, weight);
        if (foundProductDetailId != null) {
            ProductDetailsDTO productDetails = productDetailsService.getProductDetailsById(foundProductDetailId);

            List<String> imageUrls = productImagesService.getImageUrlsByProductDetailId(foundProductDetailId);

            if (imageUrls != null && !imageUrls.isEmpty()) {
                productDetails.setImageUrls(imageUrls);
            }

            return productDetails;
        } else {
            throw new RuntimeException("Không tìm thấy sản phẩm với các biến thể này.");
        }
    } else {
        throw new RuntimeException("Thiếu tham số cần thiết để tìm sản phẩm.");
    }
}



    /**
     * Lấy danh sách ProductDetails theo productId.
     *
     * @param productId ID của sản phẩm.
     * @return Danh sách ProductDetails.
     */
    @GetMapping("/by-product/{productId}")
    public ResponseEntity<List<ProductDetails>> getProductDetailsByProductId(@PathVariable Long productId) {
        List<ProductDetails> productDetailsList = productDetailsService.getProductDetailsByProductId(productId);
        return ResponseEntity.ok(productDetailsList);
    }

    /**
     * Lấy danh sách ProductDetailsDTO theo productId (chỉ trả lại thông tin cần thiết).
     *
     * @param productId ID của sản phẩm.
     * @return Danh sách ProductDetailsDTO.
     */
    @GetMapping("/dto/by-product/{productId}")
    public ResponseEntity<List<ProductDetailsDTO>> getProductDetailsDTOByProductId(@PathVariable Long productId) {
        // Lấy danh sách ProductDetailsDTO theo productId
        List<ProductDetailsDTO> productDetailsDTOList = productDetailsService.getProductDetailsDTOByProductId(productId);

        // Duyệt qua danh sách và thêm imageUrls từ ProductImagesService
        for (ProductDetailsDTO productDetails : productDetailsDTOList) {
            List<String> imageUrls = productImagesService.getImageUrlsByProductDetailId(productDetails.getProductDetailId());

            if (imageUrls != null && !imageUrls.isEmpty()) {
                productDetails.setImageUrls(imageUrls); // Gán danh sách URL hình ảnh
            }
        }

        return ResponseEntity.ok(productDetailsDTOList); // Trả về danh sách DTO
    }

    @PostMapping("/add")
    public ResponseEntity<ProductDetails> addProductDetail(@RequestBody ProductDetails productDetails) {
        ProductDetails savedProductDetail = productDetailsService.addProductDetail(productDetails);
        return ResponseEntity.ok(savedProductDetail);
    }



    @PutMapping("/update/{id}")
    public ResponseEntity<ProductDetails> updateProductDetail(@PathVariable Long id, @RequestBody ProductDetails productDetails) {
        ProductDetails updatedProductDetail = productDetailsService.updateProductDetail(id, productDetails);
        return ResponseEntity.ok(updatedProductDetail);
    }

    @DeleteMapping("/deleteProductDetail/{id}")
    public ResponseEntity<?> deleteProductDetail(@PathVariable Long id) {
        try {
            productDetailsService.deleteProductDetail(id);
            return ResponseEntity.ok("Xóa chi tiết sản phẩm thành công!");
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }





}
