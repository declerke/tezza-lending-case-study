package com.example.lending.repository;

import com.example.lending.domain.customer.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByEmail(String email);
    boolean existsByEmailOrPhoneOrNationalId(String email, String phone, String nationalId);
}
