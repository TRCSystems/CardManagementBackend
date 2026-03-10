package com.cardsystem.repository;

import com.cardsystem.models.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    Optional<WalletTransaction> findBySourceAndExternalReference(
            com.cardsystem.models.constants.TransactionSource source,
            String externalReference
    );

    boolean existsBySourceAndExternalReference(
            com.cardsystem.models.constants.TransactionSource source,
            String externalReference
    );
}