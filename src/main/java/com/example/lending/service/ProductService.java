package com.example.lending.service;

import com.example.lending.dto.product.ProductCreateRequest;
import com.example.lending.dto.product.ProductResponse;

import java.util.List;

public interface ProductService {
    ProductResponse createProduct(ProductCreateRequest request);
    ProductResponse getProduct(Long id);
    List<ProductResponse> listProducts();
    ProductResponse deactivateProduct(Long id);
}
