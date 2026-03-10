package com.cardsystem.services;

import com.cardsystem.dto.PaymentNotificationDto;
import com.cardsystem.models.*;
import com.cardsystem.models.constants.*;
import com.cardsystem.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
@Slf4j

@Service
@RequiredArgsConstructor
public class LedgerService {
    private final StudentRepository studentRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final UnallocatedPaymentRepository unallocatedPaymentRepository;

    @Transactional
    public void handlePayment(PaymentNotificationDto dto) {

        // 1. Idempotency
        if (transactionRepository.existsBySourceAndExternalReference(
                TransactionSource.MPESA, dto.getTransId())) {
            log.warn("Duplicate payment notification. transId={}", dto.getTransId());
            return;
        }

        // 2. Resolve student
        Student student = (Student) studentRepository
                .findByStudentNumber(dto.getBillRefNumber())
                .orElse(null);

        if (student == null) {
            log.error("Payment received for unknown billRefNumber={} transId={} amount={} msisdn={}",
                    dto.getBillRefNumber(), dto.getTransId(),
                    dto.getAmount(), dto.getMsisdn());

            unallocatedPaymentRepository.save(
                    UnallocatedPayment.of(
                            dto.getTransId(),
                            dto.getBillRefNumber(),
                            dto.getAmount(),
                            dto.getMsisdn(),
                            "Student not found for billRefNumber: " + dto.getBillRefNumber()
                    )
            );
            return;
        }

        // 3. Resolve wallet
        Wallet wallet = student.getWallet();
        if (wallet == null) {
            log.error("Student has no wallet. studentId={} transId={}",
                    student.getId(), dto.getTransId());

            unallocatedPaymentRepository.save(
                    UnallocatedPayment.of(
                            dto.getTransId(),
                            dto.getBillRefNumber(),
                            dto.getAmount(),
                            dto.getMsisdn(),
                            "Wallet not found for studentId: " + student.getId()
                    )
            );
            return;
        }

        // 4. Credit wallet
        wallet.credit(dto.getAmount());

        // 5. Record transaction
        WalletTransaction tx = WalletTransaction.create(
                wallet,
                TransactionType.CREDIT,
                TransactionSource.MPESA,
                dto.getAmount(),
                dto.getTransId()
        );
        tx.confirm();

        // 6. Persist
        transactionRepository.save(tx);
        walletRepository.save(wallet);

        log.info("Wallet credited. studentNumber={} amount={} transId={}",
                dto.getBillRefNumber(), dto.getAmount(), dto.getTransId());
    }
}