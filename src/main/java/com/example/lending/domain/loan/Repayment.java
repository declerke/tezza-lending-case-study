package com.example.lending.domain.loan;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "repayments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Repayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @Column(name = "reference", nullable = false, unique = true, length = 60)
    private String reference;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "allocated_to_fees", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal allocatedToFees = BigDecimal.ZERO;

    @Column(name = "allocated_to_principal", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal allocatedToPrincipal = BigDecimal.ZERO;

    @Column(name = "paid_at", nullable = false)
    @Builder.Default
    private Instant paidAt = Instant.now();
}
