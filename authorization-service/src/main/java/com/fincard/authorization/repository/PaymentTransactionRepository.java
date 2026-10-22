package com.fincard.authorization.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fincard.authorization.model.PaymentTransaction;

public interface PaymentTransactionRepository
        extends JpaRepository<PaymentTransaction, Long> {

    public Optional<PaymentTransaction> findByTransactionId(String transactionId);
}