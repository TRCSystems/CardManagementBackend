package com.cardsystem.repository;

import com.cardsystem.models.Student;
import com.cardsystem.models.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    // Find wallet by student (most common lookup)
    Optional<Wallet> findByStudent(Student student);

    // Find wallet by student ID directly
    @Query("SELECT w FROM Wallet w WHERE w.student.id = :studentId")
    Optional<Wallet> findByStudentId(@Param("studentId") Long studentId);

    // Optional: exists check
    boolean existsByStudentId(Long studentId);

    // Optional: if you ever need to compute real balance from ledger (fallback when cache is suspect)
    @Query("""
        SELECT COALESCE(SUM(t.amount), 0)
        FROM LedgerTransaction t
        WHERE t.wallet.id = :walletId
          AND t.status = 'CONFIRMED'
    """)
    BigDecimal calculateRealBalance(@Param("walletId") Long walletId);
}