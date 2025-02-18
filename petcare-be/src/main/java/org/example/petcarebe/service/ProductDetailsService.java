package org.example.petcarebe.service;


import lombok.RequiredArgsConstructor;
import org.example.petcarebe.dto.ProductDetailsDTO;
import org.example.petcarebe.model.ProductDetails;
import org.example.petcarebe.repository.ProductDetailsRepository;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductDetailsService {

    private final ProductDetailsRepository productDetailsRepository;

    // Phương thức lấy tất cả chi tiết sản phẩm dưới dạng ProductDetailsDTO
    public List<ProductDetailsDTO> findAllProductDetails() {
        List<ProductDetailsDTO> productDetailsList = productDetailsRepository.findAllProductDetails();
        if (productDetailsList == null || productDetailsList.isEmpty()) {
            throw new RuntimeException("Không có chi tiết sản phẩm nào trong cơ sở dữ liệu.");
        }
        return productDetailsList;
    }


    // Phương thức lấy chi tiết sản phẩm theo ID
    public ProductDetailsDTO getProductDetailsById(Long productDetailId) {
        return productDetailsRepository.findByProductDetailId(productDetailId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chi tiết sản phẩm với id: " + productDetailId));
    }

    public Long getProductDetailIdByVariants(String colorValue, String sizeValue, String weightValue) {
        // Tìm sản phẩm dựa trên biến thể color, size, weight
        ProductDetails productDetails = productDetailsRepository.findByColorSizeWeight(colorValue, sizeValue, weightValue)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với các biến thể đã chọn"));

        return productDetails.getProductDetailId(); // Trả về productDetailId
    }

    public List<ProductDetails> getProductDetailsByProductId(Long productId) {
        return productDetailsRepository.findByProductId(productId);
    }

    public List<ProductDetailsDTO> getProductDetailsDTOByProductId(Long productId) {
        // Lấy danh sách ProductDetails theo productId
        List<ProductDetails> productDetailsList = productDetailsRepository.findByProductId(productId);

        // Chuyển đổi danh sách ProductDetails thành danh sách ProductDetailsDTO
        return productDetailsList.stream().map(productDetails -> new ProductDetailsDTO(
                productDetails.getProductDetailId(),
                productDetails.getProducts().getProductName(),
                productDetails.getProducts().getImage(),
                productDetails.getPrice(),
                productDetails.getProductColors().getColorValue(),
                productDetails.getProductSizes().getSizeValue(),
                productDetails.getWeights().getWeightValue(),
                productDetails.getQuantity(),
                productDetails.getProducts().getDescription()
        )).collect(Collectors.toList());
    }

    public ProductDetails addProductDetail(ProductDetails productDetails) {
        // Kiểm tra dữ liệu đầu vào nếu cần
        return productDetailsRepository.save(productDetails);
    }


    public ProductDetails updateProductDetail(Long id, ProductDetails newProductDetails) {
        Optional<ProductDetails> existingProductDetail = productDetailsRepository.findById(id);

        if (existingProductDetail.isPresent()) {
            ProductDetails productDetail = existingProductDetail.get();
            productDetail.setQuantity(newProductDetails.getQuantity());
            productDetail.setPrice(newProductDetails.getPrice());
            productDetail.setProducts(newProductDetails.getProducts());
            productDetail.setWeights(newProductDetails.getWeights());
            productDetail.setProductSizes(newProductDetails.getProductSizes());
            productDetail.setProductColors(newProductDetails.getProductColors());

            return productDetailsRepository.save(productDetail);
        } else {
            throw new RuntimeException("Không tìm thấy chi tiết sản phẩm với ID: " + id);
        }
    }

    public void deleteProductDetail(Long id) {
        try {
            productDetailsRepository.deleteById(id);
        } catch (EmptyResultDataAccessException e) {
            throw new RuntimeException("Không tìm thấy chi tiết sản phẩm với ID: " + id);
        }
    }






}


