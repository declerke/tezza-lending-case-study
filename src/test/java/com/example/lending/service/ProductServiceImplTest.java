package com.example.lending.service;

import com.example.lending.domain.product.BillingCycleType;
import com.example.lending.domain.product.LoanStructureType;
import com.example.lending.domain.product.Product;
import com.example.lending.domain.product.TenureType;
import com.example.lending.dto.product.ProductCreateRequest;
import com.example.lending.dto.product.ProductResponse;
import com.example.lending.exception.BusinessException;
import com.example.lending.exception.ResourceNotFoundException;
import com.example.lending.repository.ProductRepository;
import com.example.lending.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    private ProductServiceImpl productService;

    @BeforeEach
    void setUp() {
        productService = new ProductServiceImpl(productRepository);
    }

    @Test
    void createsProductWithFees() {
        ProductCreateRequest request = new ProductCreateRequest(
                "QUICK-30", "Quick 30", "desc", TenureType.DAYS, 7, 30,
                new BigDecimal("5.0"), LoanStructureType.LUMP_SUM, null,
                BillingCycleType.INDIVIDUAL_DUE_DATE, java.util.List.of());

        when(productRepository.existsByCode("QUICK-30")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        ProductResponse response = productService.createProduct(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.code()).isEqualTo("QUICK-30");

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        assertThat(captor.getValue().getMinTenureValue()).isEqualTo(7);
    }

    @Test
    void rejectsDuplicateProductCode() {
        ProductCreateRequest request = new ProductCreateRequest(
                "QUICK-30", "Quick 30", "desc", TenureType.DAYS, 7, 30,
                new BigDecimal("5.0"), LoanStructureType.LUMP_SUM, null,
                BillingCycleType.INDIVIDUAL_DUE_DATE, java.util.List.of());

        when(productRepository.existsByCode("QUICK-30")).thenReturn(true);

        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(BusinessException.class);

        verify(productRepository, never()).save(any());
    }

    @Test
    void rejectsInvalidTenureRange() {
        ProductCreateRequest request = new ProductCreateRequest(
                "QUICK-30", "Quick 30", "desc", TenureType.DAYS, 30, 7,
                new BigDecimal("5.0"), LoanStructureType.LUMP_SUM, null,
                BillingCycleType.INDIVIDUAL_DUE_DATE, java.util.List.of());

        when(productRepository.existsByCode("QUICK-30")).thenReturn(false);

        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void rejectsInstallmentProductWithoutInstallmentCount() {
        ProductCreateRequest request = new ProductCreateRequest(
                "INSTALLMENT-MISSING-COUNT", "Invalid Installment", "desc", TenureType.MONTHS, 1, 12,
                new BigDecimal("0.0"), LoanStructureType.INSTALLMENTS, null,
                BillingCycleType.INDIVIDUAL_DUE_DATE, java.util.List.of());

        when(productRepository.existsByCode("INSTALLMENT-MISSING-COUNT")).thenReturn(false);

        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("installmentCount");

        verify(productRepository, never()).save(any());
    }

    @Test
    void rejectsInstallmentProductWithNonPositiveInstallmentCount() {
        ProductCreateRequest request = new ProductCreateRequest(
                "INSTALLMENT-ZERO-COUNT", "Invalid Installment", "desc", TenureType.MONTHS, 1, 12,
                new BigDecimal("0.0"), LoanStructureType.INSTALLMENTS, 0,
                BillingCycleType.INDIVIDUAL_DUE_DATE, java.util.List.of());

        when(productRepository.existsByCode("INSTALLMENT-ZERO-COUNT")).thenReturn(false);

        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("installmentCount");

        verify(productRepository, never()).save(any());
    }

    @Test
    void throwsWhenProductNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProduct(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
