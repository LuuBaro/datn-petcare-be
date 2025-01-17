package org.example.petcarebe.service;


import org.example.petcarebe.model.ProductImages;
import org.example.petcarebe.repository.ProductImagesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ProductImagesService {

    @Autowired
    private ProductImagesRepository productImagesRepository;

    public List<ProductImages> getAllProductImages() {
        return productImagesRepository.findAll();
    }

    public ProductImages getProductImagesById(Long productImagesId) {
        return productImagesRepository.findById(productImagesId).orElse(null);
    }

    public ProductImages saveProductImages(ProductImages productImages) {
        return productImagesRepository.save(productImages);
    }

    public void deleteProductImages(Long productImagesId) {
        productImagesRepository.deleteById(productImagesId);
    }

    public ProductImages updateProductImages(Long productImagesId, ProductImages productImages) {
        return productImagesRepository.findById(productImagesId).map(productImages1 -> {
            productImages1.setProductDetails(productImages.getProductDetails());
            productImages1.setImageUrl(productImages.getImageUrl());
            return productImagesRepository.save(productImages1);
        }).orElse(null);
    }


    // Lấy ảnh đầu tiên của sản phẩm theo ProductDetailsId
    public ProductImages getFirstImageByProductDetails(Long productDetailId) {
        // Lấy tất cả các ảnh cho sản phẩm theo ProductDetailsId
        List<ProductImages> productImagesList = productImagesRepository.findByProductDetails_ProductDetailId(productDetailId);

        // Kiểm tra nếu có ảnh và trả về ảnh đầu tiên (nếu có)
        if (productImagesList != null && !productImagesList.isEmpty()) {
            return productImagesList.get(0); // Lấy ảnh đầu tiên
        }
        return null; // Nếu không có ảnh, trả về null
    }

    public List<String> getImageUrlsByProductDetailId(Long productDetailId) {
        List<ProductImages> productImagesList = productImagesRepository.findByProductDetails_ProductDetailId(productDetailId);
        List<String> imageUrls = null;
        if (productImagesList != null && !productImagesList.isEmpty()) {
            imageUrls = new ArrayList<>();
            for (ProductImages productImages : productImagesList) {
                imageUrls.add(productImages.getImageUrl());
            }
        }
        return imageUrls;
    }
}
