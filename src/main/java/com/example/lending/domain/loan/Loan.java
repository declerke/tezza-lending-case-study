package com.example.lending.domain.loan;

import com.example.lending.domain.customer.Customer;
import com.example.lending.domain.product.BillingCycleType;
import com.example.lending.domain.product.LoanStructureType;
import com.example.lending.domain.product.Product;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "loans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "loan_number", nullable = false, unique = true, length = 40)
    private String loanNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "principal_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal principalAmount;

    @Column(name = "interest_rate", nullable = false, precision = 9, scale = 4)
    private BigDecimal interestRate;

    @Column(name = "tenure_value", nullable = false)
    private Integer tenureValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "loan_structure_type", nullable = false)
    private LoanStructureType loanStructureType;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle_type", nullable = false)
    private BillingCycleType billingCycleType;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private LoanState state;

    @Column(name = "disbursement_date")
    private LocalDate disbursementDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "outstanding_principal", nullable = false, precision = 19, scale = 4)
    private BigDecimal outstandingPrincipal;

    @Column(name = "outstanding_fees", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal outstandingFees = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "closed_at")
    private Instant closedAt;

    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Installment> installments = new ArrayList<>();

    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<LoanCharge> charges = new ArrayList<>();

    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Repayment> repayments = new ArrayList<>();

    public void addInstallment(Installment installment) {
        installment.setLoan(this);
        this.installments.add(installment);
    }

    public void addCharge(LoanCharge charge) {
        charge.setLoan(this);
        this.charges.add(charge);
    }

    public void applyCharge(LoanCharge charge) {
        outstandingFees = outstandingFees.add(charge.getAmount());
        Installment installment = charge.getInstallment();
        if (installment != null) {
            installment.setFeesDue(installment.getFeesDue().add(charge.getAmount()));
        }
        addCharge(charge);
    }

    public boolean hasActiveExposure() {
        return state == LoanState.OPEN || state == LoanState.OVERDUE;
    }

    public void addRepayment(Repayment repayment) {
        repayment.setLoan(this);
        this.repayments.add(repayment);
    }

    public BigDecimal totalOutstanding() {
        return outstandingPrincipal.add(outstandingFees);
    }
}
