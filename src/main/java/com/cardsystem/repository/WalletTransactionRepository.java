package com.cardsystem.repository;

import com.cardsystem.models.WalletTransaction;
import com.cardsystem.models.constants.TransactionSource;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    boolean existsBySourceAndExternalReference(TransactionSource source, String externalReference);

    boolean existsBySourceAndProviderTransactionId(TransactionSource source, String providerTransactionId);
}
