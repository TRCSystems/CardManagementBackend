package com.cardsystem.repository;

import com.cardsystem.models.LedgerTransaction;
import com.cardsystem.models.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

// LedgerTransactionRepository.java
public interface LedgerTransactionRepository extends JpaRepository<LedgerTransaction, Long> {

    boolean existsByReference(String reference);

    Optional<LedgerTransaction> findByReference(String reference);

    @Query("SELECT SUM(t.amount) FROM LedgerTransaction t WHERE t.wallet.id = :walletId AND t.status = 'CONFIRMED'")
    BigDecimal calculateBalance(@Param("walletId") Wallet walletId);
}
