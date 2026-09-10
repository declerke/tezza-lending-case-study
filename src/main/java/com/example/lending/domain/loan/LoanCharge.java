package com.example.lending.domain.loan;

import com.example.lending.domain.product.FeeCategory;
import com.example.lending.domain.product.Fee;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "loan_charges")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanCharge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "installment_id")
    private Installment installment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fee_configuration_id")
    private Fee feeConfiguration;

    @Enumerated(EnumType.STRING)
    @Column(name = "fee_category", nullable = false)
    private FeeCategory feeCategory;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "applied_date", nullable = false)
    private LocalDate appliedDate;

    @Column(name = "description", length = 250)
    private String description;

    @Column(name = "deduplication_key", nullable = false, unique = true, length = 180)
    private String deduplicationKey;
}
