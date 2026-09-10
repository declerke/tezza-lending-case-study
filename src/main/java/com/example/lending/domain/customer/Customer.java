package com.example.lending.domain.customer;

import com.example.lending.domain.notification.NotificationChannel;
import com.example.lending.domain.product.BillingCycleType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false, length = 80)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 80)
    private String lastName;

    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "phone", nullable = false, unique = true, length = 30)
    private String phone;

    @Column(name = "national_id", nullable = false, unique = true, length = 40)
    private String nationalId;

    @Column(name = "address", length = 250)
    private String address;

    @Column(name = "customer_segment", length = 40)
    private String customerSegment;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_channel", nullable = false)
    @Builder.Default
    private NotificationChannel preferredChannel = NotificationChannel.EMAIL;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle_type", nullable = false)
    @Builder.Default
    private BillingCycleType billingCycleType = BillingCycleType.INDIVIDUAL_DUE_DATE;

    @Column(name = "billing_day")
    private Integer billingDay;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
