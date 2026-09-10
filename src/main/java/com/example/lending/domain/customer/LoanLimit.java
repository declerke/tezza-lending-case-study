package com.example.lending.domain.customer;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "loan_limits")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false, unique = true)
    private Customer customer;

    @Column(name = "max_limit", nullable = false, precision = 19, scale = 4)
    private BigDecimal maxLimit;

    @Column(name = "available_limit", nullable = false, precision = 19, scale = 4)
    private BigDecimal availableLimit;

    @Column(name = "risk_tier", length = 20)
    private String riskTier;

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;

    public void reserve(BigDecimal amount, Instant reservedAt) {
        this.availableLimit = this.availableLimit.subtract(amount);
        this.updatedAt = reservedAt;
    }

    public void release(BigDecimal amount, Instant releasedAt) {
        this.availableLimit = this.availableLimit.add(amount);
        if (this.availableLimit.compareTo(this.maxLimit) > 0) {
            this.availableLimit = this.maxLimit;
        }
        this.updatedAt = releasedAt;
    }
}
