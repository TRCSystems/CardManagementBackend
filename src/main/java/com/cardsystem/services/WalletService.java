package com.cardsystem.services;

// WalletService.java

import com.cardsystem.dto.MpesaC2BCallbackRequest;
import com.cardsystem.models.LedgerTransaction;
import com.cardsystem.models.Student;
import com.cardsystem.models.WalletTransaction;
import com.cardsystem.models.constants.TransactionStatus;
import com.cardsystem.models.constants.TransactionType;
import com.cardsystem.models.constants.TransactionSource;
import com.cardsystem.models.Wallet;
import com.cardsystem.models.constants.WalletStatus;
import com.cardsystem.repository.LedgerTransactionRepository;
import com.cardsystem.repository.StudentRepository;
import com.cardsystem.repository.WalletRepository;
import com.cardsystem.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final LedgerTransactionRepository transactionRepository;
    private final StudentRepository studentRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final AuditService auditService;

    // ==================== C2B Callback Handler (from M-Pesa) ====================
    @Transactional
    public void handleC2BCallback(MpesaC2BCallbackRequest payload) {
        // Extract from payload
        String shortCode = payload.getBusinessShortCode();
        String amountStr = payload.getTransAmount();
        String billRefNumber = payload.getBillRefNumber();  // student ID
        String transId = payload.getTransID();
        String phone = payload.getMsisdn();

        // Validate shortCode is yours
        if (!shortCode.equals("YOUR_PAYBILL_SHORTCODE")) {
            auditService.logFailure(AuditService.ACTION_WALLET_MPESA_C2B, AuditService.CATEGORY_WALLET, 
                "Invalid shortcode: " + shortCode, "Invalid shortcode");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid shortcode");
        }

        BigDecimal amount = new BigDecimal(amountStr);

        // Idempotency: skip if we already processed this provider transaction id
        if (walletTransactionRepository.existsBySourceAndProviderTransactionId(TransactionSource.MPESA, transId)) {
            log.info("Duplicate transaction: {}", transId);
            return;
        }

        // Prepare wallet transaction record first (even if student not found)
        WalletTransaction walletTxn = new WalletTransaction();
        walletTxn.setExternalReference(transId);
        walletTxn.setProviderTransactionId(transId);
        walletTxn.setBillRefNumber(billRefNumber);
        walletTxn.setSource(TransactionSource.MPESA);
        walletTxn.setType(TransactionType.CREDIT);
        walletTxn.setAmount(amount);
        walletTxn.setStatus(TransactionStatus.PENDING); // will be updated below

        // Find student by reference (studentID)
        studentRepository.findByStudentNumber(billRefNumber).ifPresentOrElse(student -> {
            Wallet wallet = student.getWallet();
            walletTxn.setWallet(wallet);
            walletTxn.confirm(); // sets status CONFIRMED + confirmedAt

            // Also record in ledger for balance computation
            LedgerTransaction txn = new LedgerTransaction();
            txn.setWallet(wallet);
            txn.setType(TransactionType.CREDIT);
            txn.setAmount(amount);
            txn.setSource("MPESA");
            txn.setReference(transId);
            txn.setStatus(TransactionStatus.CONFIRMED);  // assume callback is confirmation
            txn.setDetails("Phone: " + phone);
            transactionRepository.save(txn);

            // Update cached balance using domain method
            wallet.applyCredit(amount);
            walletRepository.save(wallet);

            // Audit
            auditService.logAction(AuditService.ACTION_WALLET_MPESA_C2B, AuditService.CATEGORY_WALLET, 
                AuditService.ENTITY_WALLET, wallet.getId(),
                "M-Pesa C2B: Credited " + amount + " from " + phone + " (Receipt: " + transId + ")",
                null, "{\"cachedBalance\":" + wallet.getCachedBalance() + "}");

        }, () -> {
            // Student not found; leave wallet null and mark pending confirmation for manual reconciliation
            walletTxn.markPendingConfirmation();
            log.warn("C2B received for unknown BillRefNumber {} (TransID {})", billRefNumber, transId);
            auditService.logFailure(AuditService.ACTION_WALLET_MPESA_C2B, AuditService.CATEGORY_WALLET, 
                "Student not found for BillRefNumber: " + billRefNumber + " (TransID " + transId + ")", "Student not found");
        });

        walletTransactionRepository.save(walletTxn);
    }

    // ==================== Get balance (compute from ledger if no cache) ====================
    @Transactional(readOnly = true)
    public BigDecimal getBalance(Long walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));

        // If no cached, compute
        return transactionRepository.calculateBalance(wallet);
    }

    // ==================== Manual adjust (admin only) ====================
    @Transactional
    public void manualAdjust(Long walletId, BigDecimal amount, String reason) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> {
                    auditService.logFailure(AuditService.ACTION_WALLET_ADJUSTED, AuditService.CATEGORY_WALLET, 
                        "Wallet not found: " + walletId, "Wallet not found");
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found");
                });

        BigDecimal previousBalance = wallet.getCachedBalance();
        
        LedgerTransaction txn = new LedgerTransaction();
        txn.setWallet(wallet);
        txn.setType(amount.compareTo(BigDecimal.ZERO) > 0 ? TransactionType.CREDIT : TransactionType.DEBIT);
        txn.setAmount(amount);
        txn.setSource("MANUAL");
        txn.setReference("MANUAL-" + System.currentTimeMillis());
        txn.setStatus(TransactionStatus.CONFIRMED);
        txn.setDetails(reason);

        transactionRepository.save(txn);

        // Update cached with non-negative guard
        if (amount.compareTo(BigDecimal.ZERO) >= 0) wallet.applyCredit(amount);
        else wallet.applyDebit(amount.abs());

        walletRepository.save(wallet);
        
        String actionType = amount.compareTo(BigDecimal.ZERO) > 0 ? 
            AuditService.ACTION_WALLET_CREDIT : AuditService.ACTION_WALLET_DEBIT;
        auditService.logAction(actionType, AuditService.CATEGORY_WALLET, 
            AuditService.ENTITY_WALLET, wallet.getId(),
            "Manual wallet adjustment: " + amount + " | Reason: " + reason,
            "{\"cachedBalance\":" + previousBalance + "}",
            "{\"cachedBalance\":" + wallet.getCachedBalance() + "}");
    }

    // Manual adjust that returns updated wallet (used by controller)
    @Transactional
    public Wallet manualAdjustAndReturn(Long walletId, BigDecimal amount, String reason) {
        manualAdjust(walletId, amount, reason);
        return walletRepository.findById(walletId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));
    }

    // Freeze/unfreeze
    @Transactional
    public Wallet setFreeze(Long walletId, boolean freeze) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> {
                    auditService.logFailure(freeze ? AuditService.ACTION_WALLET_FROZEN : AuditService.ACTION_WALLET_UNFROZEN, 
                        AuditService.CATEGORY_WALLET, "Wallet not found: " + walletId, "Wallet not found");
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found");
                });

        String previousStatus = wallet.getStatus().name();
        wallet.setStatus(freeze ? WalletStatus.FROZEN : WalletStatus.ACTIVE);
        walletRepository.saveAndFlush(wallet); // ensure DB is updated before returning

        Wallet savedWallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));
        
        auditService.logAction(freeze ? AuditService.ACTION_WALLET_FROZEN : AuditService.ACTION_WALLET_UNFROZEN, 
            AuditService.CATEGORY_WALLET, AuditService.ENTITY_WALLET, wallet.getId(),
            "Wallet " + (freeze ? "frozen" : "unfrozen"),
            "{\"status\":\"" + previousStatus + "\"}",
            "{\"status\":\"" + savedWallet.getStatus() + "\"}");
        
        return savedWallet;
    }

    // ==================== List wallets, optionally by school code ====================
    @Transactional(readOnly = true)
    public List<Wallet> listWallets(String schoolCode) {
        if (schoolCode == null || schoolCode.isBlank()) {
            return walletRepository.findAll();
        }
        return studentRepository.findBySchool_Code(schoolCode).stream()
                .map(Student::getWallet)
                .filter(Objects::nonNull)
                .toList();
    }

    // ==================== Commented-out B2C logic ====================
    // Initiate B2C payout (for canteen withdrawal)
    /*
    @Transactional
    public String initiateB2C(
            String initiatorName,
            String securityCredential,
            BigDecimal amount,
            String partyA,
            String partyB,
            String remarks,
            String queueTimeoutURL,
            String resultURL,
            Long walletId,
            String occasion
    ) {
        // Original commented code preserved here...
    }
    */

    // B2C Callback Handler
    /*
    @Transactional
    public void handleB2CCallback(Map<String, Object> payload) {
        // Original commented code preserved here...
    }
    */

    // private String getDarajaAccessToken() { ... } // Preserved commented method
}