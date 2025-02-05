package org.example.petcarebe.service;


import org.example.petcarebe.dto.ProductsDTO;
import org.example.petcarebe.model.Brand;
import org.example.petcarebe.model.Categories;
import org.example.petcarebe.model.Products;
import org.example.petcarebe.repository.BrandRepository;
import org.example.petcarebe.repository.CategoriesRepository;
import org.example.petcarebe.repository.ProductDetailsRepository;
import org.example.petcarebe.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductsService {

    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private BrandRepository brandRepository;
    @Autowired
    private CategoriesRepository categoriesRepository;

    @Autowired
    private ProductDetailsRepository productDetailsRepository;

    public List<Products> getAllProducts() {
        return productRepository.findAll();
    }

    public Products getProductsById(Long productsId) {
        return productRepository.findById(productsId).orElse(null);
    }

    public Products createProduct(Products product) {
        // Kiểm tra null cho brand ID
        if (product.getBrand() == null || product.getBrand().getBrandId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Brand ID không được để trống");
        }

        // Kiểm tra null cho categories ID
        if (product.getCategories() == null || product.getCategories().getCategoryId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Categories ID không được để trống");
        }

        // Kiểm tra và lấy dữ liệu từ database
        Brand existingBrand = brandRepository.findById(product.getBrand().getBrandId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Thương hiệu không tồn tại"));
        Categories existingCategory = categoriesRepository.findById(product.getCategories().getCategoryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Danh mục không tồn tại"));

        // Gán giá trị chính xác cho product
        product.setBrand(existingBrand);
        product.setCategories(existingCategory);

        return productRepository.save(product);
    }


    public Products update(Long productId, Products updatedProduct) {
        return productRepository.findById(productId).map(existingProduct -> {
            // Cập nhật tên sản phẩm
            existingProduct.setProductName(updatedProduct.getProductName());

            // Cập nhật mô tả
            existingProduct.setDescription(updatedProduct.getDescription());

            // Cập nhật URL hình ảnh
            existingProduct.setImage(updatedProduct.getImage());

            // Lấy Brand mới và gán
            Brand brand = brandRepository.findById(updatedProduct.getBrand().getBrandId())
                    .orElseThrow(() -> new IllegalArgumentException("Thương hiệu không tồn tại với ID: " + updatedProduct.getBrand().getBrandId()));
            existingProduct.setBrand(brand);

            // Lấy Category mới và gán
            Categories category = categoriesRepository.findById(updatedProduct.getCategories().getCategoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Danh mục không tồn tại với ID: " + updatedProduct.getCategories().getCategoryId()));
            existingProduct.setCategories(category);

            // Lưu sản phẩm đã cập nhật
            return productRepository.save(existingProduct);
        }).orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại với ID: " + productId));
    }


    public void delete(Long productsId) {
        if (productRepository.existsById(productsId)) {
            productRepository.deleteById(productsId);
        } else {
            throw new IllegalArgumentException("Sản phẩm không tồn tại với ID: " + productsId);
        }
    }


    @Autowired
    public ProductsService(ProductRepository productRepository, ProductDetailsRepository productDetailsRepository) {
        this.productRepository = productRepository;
        this.productDetailsRepository = productDetailsRepository;
    }

//    public List<ProductsDTO> getAllProductss() {
//        // Lấy tất cả các sản phẩm từ ProductRepository
//        List<ProductsDTO> productsDTOList = productRepository.findAllProductsWithMinPrice();
//
//        // Lấy danh sách giá thấp nhất của từng sản phẩm từ ProductDetailsRepository
//        productsDTOList.forEach(dto -> {
//            // Lấy giá thấp nhất của sản phẩm từ ProductDetailsRepository
//            Float minPrice = productDetailsRepository.findMinPriceByProductId(dto.getProductId());
//
//            // Nếu có giá thấp nhất, cập nhật lại thông tin price của DTO
//            if (minPrice != null) {
//                dto.setPrice(minPrice);  // Cập nhật giá vào DTO
//            }
//        });
//
//        return productsDTOList;
//    }

    public List<ProductsDTO> getAllProductss() {
        // Lấy tất cả các sản phẩm từ ProductRepository
        List<Products> products = productRepository.findAll();

        // Lọc và map các sản phẩm có ít nhất một ProductDetails
        return products.stream()
                .filter(product -> !productDetailsRepository.findByProductId(product.getProductId()).isEmpty()) // Chỉ lấy sản phẩm có ProductDetails
                .map(product -> {
                    // Lấy giá thấp nhất của sản phẩm
                    Float minPrice = productDetailsRepository.findMinPriceByProductId(product.getProductId());
                    // Tạo ProductsDTO và set thông tin
                    ProductsDTO productDTO = new ProductsDTO(
                            product.getProductId(),
                            product.getProductName(),
                            product.getImage()
                    );

                    // Set giá trị price
                    productDTO.setPrice(minPrice);  // Gọi setter, sẽ ép kiểu float và xử lý null nếu cần

                    return productDTO;
                })
                .collect(Collectors.toList());
    }






}
