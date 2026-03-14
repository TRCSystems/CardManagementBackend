package com.cardsystem.services;

import com.cardsystem.dto.PosAuthorizationRequest;
import com.cardsystem.dto.PosAuthorizationResponse;
import com.cardsystem.models.Card;
import com.cardsystem.models.CardAssignment;
import com.cardsystem.models.Student;
import com.cardsystem.models.Wallet;
import com.cardsystem.models.constants.CardStatus;
import com.cardsystem.models.constants.WalletStatus;
import com.cardsystem.repository.CardAssignmentRepository;
import com.cardsystem.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PosAuthorizationService {

    private static final int MAX_PIN_RETRIES = 3;
    private static final int PIN_LOCK_MINUTES = 15;

    private final CardRepository cardRepository;
    private final CardAssignmentRepository assignmentRepository;
    private final AuditService auditService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public PosAuthorizationResponse authorize(PosAuthorizationRequest req) {
        String uid = req.getCardUid().trim().toUpperCase();

        Card card = cardRepository.findByUid(uid)
                .orElse(null);
        if (card == null) {
            return decline("CARD_NOT_FOUND", "Card not found");
        }

        if (card.getStatus() == CardStatus.BLOCKED) {
            return decline(card, "CARD_BLOCKED", "Card is blocked");
        }
        if (card.getStatus() == CardStatus.RETIRED || card.getStatus() == CardStatus.UNASSIGNED) {
            return decline(card, "CARD_INACTIVE", "Card is inactive");
        }

        CardAssignment assignment = assignmentRepository.findByCardAndUnassignedAtIsNull(card)
                .orElse(null);
        if (assignment == null) {
            return decline(card, "CARD_UNASSIGNED", "Card not assigned");
        }

        Student student = assignment.getStudent();
        Wallet wallet = student.getWallet();
        if (wallet == null) {
            return decline(card, "WALLET_NOT_FOUND", "Wallet not found");
        }
        if (wallet.getStatus() == WalletStatus.FROZEN) {
            return decline(card, wallet, "WALLET_FROZEN", "Wallet is frozen");
        }

        // PIN checks
        if (card.getPinHash() == null) {
            return decline(card, wallet, "PIN_NOT_SET", "PIN not set");
        }
        if (card.isPinLocked()) {
            return decline(card, wallet, "PIN_LOCKED", "PIN locked, try later");
        }
        if (!passwordEncoder.matches(req.getPin(), card.getPinHash())) {
            int nextRetries = card.getPinRetryCount() + 1;
            card.setPinRetryCount(nextRetries);
            if (nextRetries >= MAX_PIN_RETRIES) {
                card.setPinLockedUntil(LocalDateTime.now().plusMinutes(PIN_LOCK_MINUTES));
                auditService.logFailure(AuditService.ACTION_CARD_PIN_LOCKED, AuditService.CATEGORY_CARD,
                        "PIN locked for card " + card.getId(), "PIN locked");
                cardRepository.save(card);
                return decline(card, wallet, "PIN_LOCKED", "Too many attempts; try later");
            }
            auditService.logFailure(AuditService.ACTION_CARD_PIN_FAILED, AuditService.CATEGORY_CARD,
                    "Bad PIN for card " + card.getId(), "PIN invalid");
            cardRepository.save(card);
            return decline(card, wallet, "PIN_INVALID", "Invalid PIN");
        }

        // PIN correct: reset counters
        card.setPinRetryCount(0);
        card.setPinLockedUntil(null);
        cardRepository.save(card);

        return PosAuthorizationResponse.builder()
                .approved(true)
                .code("APPROVED")
                .message("Approved")
                .cardId(card.getId())
                .cardStatus(card.getStatus())
                .walletId(wallet.getId())
                .walletStatus(wallet.getStatus())
                .build();
    }

    private PosAuthorizationResponse decline(String code, String msg) {
        return PosAuthorizationResponse.builder()
                .approved(false)
                .code(code)
                .message(msg)
                .build();
    }

    private PosAuthorizationResponse decline(Card card, String code, String msg) {
        return PosAuthorizationResponse.builder()
                .approved(false)
                .code(code)
                .message(msg)
                .cardId(card.getId())
                .cardStatus(card.getStatus())
                .build();
    }

    private PosAuthorizationResponse decline(Card card, Wallet wallet, String code, String msg) {
        return PosAuthorizationResponse.builder()
                .approved(false)
                .code(code)
                .message(msg)
                .cardId(card.getId())
                .cardStatus(card.getStatus())
                .walletId(wallet != null ? wallet.getId() : null)
                .walletStatus(wallet != null ? wallet.getStatus() : null)
                .build();
    }
}
