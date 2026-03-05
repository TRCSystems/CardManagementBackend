package com.cardsystem.services;

// WalletService.java

import com.cardsystem.models.LedgerTransaction;
import com.cardsystem.models.Student;
import com.cardsystem.models.constants.TransactionStatus;
import com.cardsystem.models.constants.TransactionType;
import com.cardsystem.models.Wallet;
import com.cardsystem.models.constants.WalletStatus;
import com.cardsystem.repository.LedgerTransactionRepository;
import com.cardsystem.repository.StudentRepository;
import com.cardsystem.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
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


    // C2B Callback Handler (from M-Pesa)
    @Transactional
    public void handleC2BCallback(Map<String, Object> payload) {
        // Extract from payload
        String shortCode = (String) payload.get("BusinessShortCode");
        String amountStr = (String) payload.get("TransAmount");
        String reference = (String) payload.get("BillRefNumber");  // student ID
        String mpesaReceipt = (String) payload.get("TransID");
        String phone = (String) payload.get("MSISDN");

        // Validate shortCode is yours
        if (!shortCode.equals("YOUR_PAYBILL_SHORTCODE")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid shortcode");
        }

        // Find student by reference (studentID)
        Student student = (Student) studentRepository.findByStudentNumber(reference)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

        Wallet wallet = student.getWallet();

        BigDecimal amount = new BigDecimal(amountStr);

        // Check if already processed (idempotency)
        if (transactionRepository.existsByReference(mpesaReceipt)) {
            log.info("Duplicate transaction: " + mpesaReceipt);
            return;
        }

        // Create ledger entry
        LedgerTransaction txn = new LedgerTransaction();
        txn.setWallet(wallet);
        txn.setType(TransactionType.CREDIT);
        txn.setAmount(amount);
        txn.setSource("MPESA");
        txn.setReference(mpesaReceipt);
        txn.setStatus(TransactionStatus.CONFIRMED);  // assume callback is confirmation
        txn.setDetails("Phone: " + phone);

        transactionRepository.save(txn);

        // Update cached balance if using
        if (wallet.getCachedBalance() != null) {
            wallet.setCachedBalance(wallet.getCachedBalance().add(amount));
            walletRepository.save(wallet);
        }
    }

//    // Initiate B2C payout (for canteen withdrawal)
//    @Transactional
//    public String initiateB2C(
//            String initiatorName,
//            String securityCredential,      // Base64 encoded password from Daraja portal
//            BigDecimal amount,
//            String partyA,                  // Your Paybill number (shortcode)
//            String partyB,                  // Recipient phone (2547xxxxxxxx)
//            String remarks,
//            String queueTimeoutURL,         // Your callback URL for timeout
//            String resultURL,               // Your callback URL for final result
//            Long walletId,                  // Which wallet is funding this payout (school/canteen wallet)
//            String occasion                 // e.g. "Canteen Staff Withdrawal"
//    ) {
//        log.info("Initiating B2C payout: amount={}, to={}, from wallet={}", amount, partyB, walletId);
//
//        // 1. Get fresh OAuth access token from Daraja
//        String accessToken = getDarajaAccessToken();
//
//        // 2. Prepare B2C request body
//        Map<String, Object> requestBody = Map.ofEntries(
//                Map.entry("InitiatorName", initiatorName),
//                Map.entry("SecurityCredential", securityCredential),
//                Map.entry("CommandID", "BusinessPayment"),  // or "SalaryPayment", "PromotionPayment"
//                Map.entry("Amount", amount.longValue()),    // M-Pesa expects whole number
//                Map.entry("PartyA", partyA),
//                Map.entry("PartyB", partyB),
//                Map.entry("Remarks", remarks),
//                Map.entry("QueueTimeOutURL", queueTimeoutURL),
//                Map.entry("ResultURL", resultURL),
//                Map.entry("Occasion", occasion != null ? occasion : "Canteen Withdrawal")
//        );
//
//        // 3. Prepare HTTP request
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//        headers.setBearerAuth(accessToken);
//
//        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
//
//        String endpoint = isProduction()
//                ? "https://api.safaricom.co.ke/mpesa/b2c/v1"
//                : "https://sandbox.safaricom.co.ke/mpesa/b2c/v1";
//
//        try {
//            ResponseEntity<Map> response = restTemplate.postForEntity(endpoint, entity, Map.class);
//
//            Map<String, Object> body = response.getBody();
//
//            if (response.getStatusCode().is2xxSuccessful() && "0".equals(body.get("ResponseCode"))) {
//                String conversationID = (String) body.get("ConversationID");
//                String originatorConversationID = (String) body.get("OriginatorConversationID");
//
//                log.info("B2C initiated successfully. OriginatorConversationID: {}", originatorConversationID);
//
//                // 4. Create pending DEBIT transaction
//                Wallet wallet = walletRepository.findById(walletId)
//                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found: " + walletId));
//
//                if (wallet.getCachedBalance().compareTo(amount) < 0) {
//                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient balance in wallet");
//                }
//
//                LedgerTransaction txn = new LedgerTransaction();
//                txn.setWallet(wallet);
//                txn.setType(TransactionType.DEBIT);
//                txn.setAmount(amount.negate());
//                txn.setSource("B2C");
//                txn.setReference(originatorConversationID);  // use this to match callback
//                txn.setStatus(TransactionStatus.PENDING);
//                txn.setDetails("B2C to " + partyB + " | Remarks: " + remarks);
//
//                transactionRepository.save(txn);
//
//                // Optionally reduce cached balance immediately (optimistic)
//                wallet.setCachedBalance(wallet.getCachedBalance().subtract(amount));
//                walletRepository.save(wallet);
//
//                return originatorConversationID;  // return for tracking
//
//            } else {
//                String errorMessage = (String) body.getOrDefault("ErrorMessage", "Unknown error");
//                log.error("B2C initiation failed: {}", errorMessage);
//                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "B2C request failed: " + errorMessage);
//            }
//
//        } catch (HttpClientErrorException | HttpServerErrorException e) {
//            log.error("B2C HTTP error: status={}, response={}", e.getStatusCode(), e.getResponseBodyAsString());
//            throw new ResponseStatusException(e.getStatusCode(), "M-Pesa B2C communication error");
//        } catch (Exception e) {
//            log.error("Unexpected error during B2C initiation", e);
//            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to initiate B2C");
//        }
//    }
//    // B2C Callback Handler
//    @Transactional
//    public void handleB2CCallback(Map<String, Object> payload) {
//        String resultCode = (String) payload.get("Result").get("ResultCode");
//        String txnId = (String) payload.get("Result").get("TransactionID");
//
//        if ("0".equals(resultCode)) {
//            // Success – update txn to CONFIRMED
//            LedgerTransaction txn = transactionRepository.findByReference(txnId)
//                    .orElseThrow();
//            txn.setStatus(TransactionStatus.CONFIRMED);
//            transactionRepository.save(txn);
//
//            // Update cached balance
//            Wallet wallet = txn.getWallet();
//            wallet.setCachedBalance(wallet.getCachedBalance().add(txn.getAmount()));
//            walletRepository.save(wallet);
//        } else {
//            // Failure – mark FAILED
//        }
//    }

    // Get balance (compute from ledger if no cache)
    @Transactional(readOnly = true)
    public BigDecimal getBalance(Long walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));

        // If no cached, compute
        return transactionRepository.calculateBalance(wallet);
    }

    // Manual adjust (admin only)
    @Transactional
    public void manualAdjust(Long walletId, BigDecimal amount, String reason) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow();

        LedgerTransaction txn = new LedgerTransaction();
        txn.setWallet(wallet);
        txn.setType(amount.compareTo(BigDecimal.ZERO) > 0 ? TransactionType.CREDIT : TransactionType.DEBIT);
        txn.setAmount(amount);
        txn.setSource("MANUAL");
        txn.setReference("MANUAL-" + System.currentTimeMillis());
        txn.setStatus(TransactionStatus.CONFIRMED);
        txn.setDetails(reason);

        transactionRepository.save(txn);

        // Update cached
        wallet.setCachedBalance(wallet.getCachedBalance().add(amount));
        walletRepository.save(wallet);
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));

        wallet.setStatus(freeze ? WalletStatus.FROZEN : WalletStatus.ACTIVE);
        walletRepository.saveAndFlush(wallet); // ensure DB is updated before returning
        return walletRepository.findById(walletId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));
    }

    // List wallets, optionally by school code
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
//
//    private String getDarajaAccessToken() {
//        String consumerKey = "YOUR_CONSUMER_KEY";
//        String consumerSecret = "YOUR_CONSUMER_SECRET";
//
//        String credentials = consumerKey + ":" + consumerSecret;
//        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.set("Authorization", "Basic " + encodedCredentials);
//
//        HttpEntity<String> entity = new HttpEntity<>(headers);
//
//        String url = isProduction()
//                ? "https://api.safaricom.co.ke/oauth/v1/generate?grant_type=client_credentials"
//                : "https://sandbox.safaricom.co.ke/oauth/v1/generate?grant_type=client_credentials";
//
//        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
//
//        if (response.getStatusCode().is2xxSuccessful()) {
//            return (String) response.getBody().get("access_token");
//        }
//
//        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to get Daraja access token");
//    }
}
