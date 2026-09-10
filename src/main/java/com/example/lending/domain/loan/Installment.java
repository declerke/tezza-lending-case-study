package com.example.lending.domain.loan;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "installments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Installment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @Column(name = "installment_number", nullable = false)
    private Integer installmentNumber;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "principal_due", nullable = false, precision = 19, scale = 4)
    private BigDecimal principalDue;

    @Column(name = "fees_due", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal feesDue = BigDecimal.ZERO;

    @Column(name = "amount_paid", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Column(name = "fees_paid", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal feesPaid = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    @Builder.Default
    private InstallmentState state = InstallmentState.PENDING;

    public BigDecimal totalDue() {
        return principalDue.add(feesDue);
    }

    public BigDecimal balance() {
        return principalDue.subtract(amountPaid).add(feesDue.subtract(feesPaid));
    }

    public boolean isOverdueOn(LocalDate date) {
        return state != InstallmentState.PAID && dueDate.isBefore(date);
    }

    public void markPaidIfSettled() {
        if (amountPaid.compareTo(principalDue) >= 0 && feesPaid.compareTo(feesDue) >= 0) {
            state = InstallmentState.PAID;
        }
    }
}
