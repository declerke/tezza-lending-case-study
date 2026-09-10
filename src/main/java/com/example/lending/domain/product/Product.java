package com.example.lending.domain.product;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 40)
    private String code;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "tenure_type", nullable = false)
    private TenureType tenureType;

    @Column(name = "min_tenure_value", nullable = false)
    private Integer minTenureValue;

    @Column(name = "max_tenure_value", nullable = false)
    private Integer maxTenureValue;

    @Column(name = "interest_rate", nullable = false, precision = 9, scale = 4)
    private BigDecimal interestRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "loan_structure_type", nullable = false)
    private LoanStructureType loanStructureType;

    @Column(name = "installment_count")
    private Integer installmentCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle_type", nullable = false)
    private BillingCycleType billingCycleType;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Fee> fees = new ArrayList<>();

    public void addFee(Fee fee) {
        fee.setProduct(this);
        this.fees.add(fee);
    }
}
