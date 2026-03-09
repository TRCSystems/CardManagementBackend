package com.cardsystem.controller;



import com.cardsystem.dto.*;
import com.cardsystem.models.Card;
import com.cardsystem.models.constants.CardStatus;
import com.cardsystem.services.CardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cards")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;

    // ────────────────────────────────────────────────
    // 1. Issue new card (register blank card)
    // ────────────────────────────────────────────────
    @PostMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<CardActionResponse> issueCard(
            @Valid @RequestBody IssueCardRequest request) {

        try {
            Card card = cardService.issueCard(request.getUid(), request.getSchoolId());

            return ResponseEntity.ok(CardActionResponse.builder()
                    .cardId(card.getId())
                    .uid(card.getUid())
                    .status(card.getStatus())
                    .message("Card issued successfully (unassigned)")
                    .build());

        } catch (ResponseStatusException e) {
            // Let Spring handle 409, 404, etc. directly
            throw e;
        } catch (ChangeSetPersister.NotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    // ────────────────────────────────────────────────
    // 2. Bind / Assign card to student
    // ────────────────────────────────────────────────
    @PostMapping("/{cardId}/bind")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<CardActionResponse> bindCard(
            @PathVariable Long cardId,
            @Valid @RequestBody BindCardRequest request) throws ChangeSetPersister.NotFoundException {

        Card updated = cardService.bindCard(cardId, request.getStudentId());

        return ResponseEntity.ok(CardActionResponse.builder()
                .cardId(updated.getId())
                .uid(updated.getUid())
                .status(updated.getStatus())
                .studentId(request.getStudentId())
                .message("Card successfully bound to student ID " + request.getStudentId())
                .build());
    }

    // ────────────────────────────────────────────────
    // 3. Block card
    // ────────────────────────────────────────────────
    @PostMapping("/{cardId}/block")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'SUPER_ADMIN', 'FINANCE_ADMIN')")
    public ResponseEntity<CardActionResponse> blockCard(@PathVariable Long cardId) {

        Card updated = cardService.blockCard(cardId);

        return ResponseEntity.ok(CardActionResponse.builder()
                .cardId(updated.getId())
                .uid(updated.getUid())
                .status(updated.getStatus())
                .message("Card blocked successfully")
                .build());
    }

    // ────────────────────────────────────────────────
    // 4. Retire card (mark as unusable – usually before re-issue)
    // ────────────────────────────────────────────────
    @PostMapping("/{cardId}/retire")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<CardActionResponse> retireCard(@PathVariable Long cardId) {

        Card retired = cardService.retireCard(cardId);

        return ResponseEntity.ok(CardActionResponse.builder()
                .cardId(retired.getId())
                .uid(retired.getUid())
                .status(retired.getStatus())
                .message("Card retired successfully")
                .build());
    }

    // ────────────────────────────────────────────────
    // Optional: Quick lookup by UID (very useful for testing & future POS/NFC)
    // ────────────────────────────────────────────────
//    @GetMapping("/resolve")
//    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'SUPER_ADMIN', 'OPERATOR')")
//    public ResponseEntity<CardResolutionResponse> resolveCard(
//            @RequestParam String uid) {
//
//        // You will need to add this method to CardService later
//        // CardResolution resolution = cardService.resolveByUid(uid.trim().toUpperCase());
//
//        // For now placeholder – implement in next step if needed
//        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Resolve endpoint not yet implemented");
//    }

    // ────────────────────────────────────────────────
    // 5. Get all cards for a school (assigned + unassigned)
    // ────────────────────────────────────────────────
    @GetMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'SUPER_ADMIN', 'FINANCE_ADMIN', 'READ_ONLY')")
    public ResponseEntity<java.util.List<com.cardsystem.dto.CardActionResponse>> getCardsBySchool(@RequestParam("schoolId") String schoolId) {

        java.util.List<com.cardsystem.dto.CardActionResponse> list = cardService.listCardsBySchoolWithAssignment(schoolId);

        return ResponseEntity.ok(list);
    }

    // ───────────────────────────────────────────────-
    // 6. Get full card details for ALL cards (card + student + wallet)
    // ───────────────────────────────────────────────-
    @GetMapping("/full-details")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'SUPER_ADMIN', 'FINANCE_ADMIN', 'READ_ONLY')")
    public ResponseEntity<List<CardFullDetailsResponse>> getAllCardsFullDetails(
            @RequestParam(required = false) String schoolId) {
        List<CardFullDetailsResponse> details;
        if (schoolId != null && !schoolId.isBlank()) {
            details = cardService.getFullCardDetailsBySchool(schoolId);
        } else {
            details = cardService.getAllFullCardDetails();
        }
        return ResponseEntity.ok(details);
    }

}