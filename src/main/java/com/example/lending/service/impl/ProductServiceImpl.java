package com.example.lending.service.impl;

import com.example.lending.domain.product.Fee;
import com.example.lending.domain.product.LoanStructureType;
import com.example.lending.domain.product.Product;
import com.example.lending.dto.product.FeeRequest;
import com.example.lending.dto.product.ProductCreateRequest;
import com.example.lending.dto.product.ProductResponse;
import com.example.lending.exception.BusinessException;
import com.example.lending.exception.ResourceNotFoundException;
import com.example.lending.repository.ProductRepository;
import com.example.lending.service.ProductService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    public ProductServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public ProductResponse createProduct(ProductCreateRequest request) {
        if (productRepository.existsByCode(request.code())) {
            throw new BusinessException("A product with code '" + request.code() + "' already exists");
        }
        if (request.minTenureValue() > request.maxTenureValue()) {
            throw new BusinessException("minTenureValue cannot be greater than maxTenureValue");
        }
        validateInstallmentConfiguration(request);

        Product product = Product.builder()
                .code(request.code())
                .name(request.name())
                .description(request.description())
                .tenureType(request.tenureType())
                .minTenureValue(request.minTenureValue())
                .maxTenureValue(request.maxTenureValue())
                .interestRate(request.interestRate())
                .loanStructureType(request.loanStructureType())
                .installmentCount(request.installmentCount())
                .billingCycleType(request.billingCycleType())
                .build();

        if (request.fees() != null) {
            for (FeeRequest feeRequest : request.fees()) {
                Fee fee = Fee.builder()
                        .feeCategory(feeRequest.feeCategory())
                        .calculationType(feeRequest.calculationType())
                        .applicationTiming(feeRequest.applicationTiming())
                        .amount(feeRequest.amount())
                        .daysAfterDue(feeRequest.daysAfterDue())
                        .build();
                product.addFee(fee);
            }
        }

        Product saved = productRepository.save(product);
        return ProductResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long id) {
        return ProductResponse.from(findProductOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> listProducts() {
        return productRepository.findAll().stream().map(ProductResponse::from).toList();
    }

    @Override
    public ProductResponse deactivateProduct(Long id) {
        Product product = findProductOrThrow(id);
        product.setActive(false);
        return ProductResponse.from(productRepository.save(product));
    }

    private Product findProductOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id " + id));
    }

    private void validateInstallmentConfiguration(ProductCreateRequest request) {
        if (request.loanStructureType() == LoanStructureType.INSTALLMENTS
                && (request.installmentCount() == null || request.installmentCount() < 1)) {
            throw new BusinessException("Installment products must define a positive installmentCount");
        }
    }
}
