package com.example.lending.domain.product;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "fees")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Fee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(name = "fee_category", nullable = false)
    private FeeCategory feeCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "calculation_type", nullable = false)
    private FeeCalculationType calculationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "application_timing", nullable = false)
    @Builder.Default
    private FeeApplicationTiming applicationTiming = FeeApplicationTiming.ORIGINATION;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "days_after_due")
    private Integer daysAfterDue;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;
}
