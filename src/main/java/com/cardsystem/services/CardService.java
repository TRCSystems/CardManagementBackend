package com.cardsystem.services;
import com.cardsystem.models.Card;
import com.cardsystem.models.CardAssignment;
import com.cardsystem.models.School;
import com.cardsystem.models.Student;
import com.cardsystem.models.constants.CardStatus;
import com.cardsystem.repository.CardAssignmentRepository;
import com.cardsystem.repository.CardRepository;
import com.cardsystem.repository.SchoolRepository;
import com.cardsystem.repository.StudentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardService {

    private final CardRepository cardRepository;
    private final CardAssignmentRepository assignmentRepository;
    private final StudentRepository studentRepository;
    private final SchoolRepository schoolRepository;
    private final AuditService auditService;

    @Transactional
    public Card issueCard(String uidRaw, String schoolId) throws ChangeSetPersister.NotFoundException {
        String uid = uidRaw.trim().toUpperCase();

        if (cardRepository.existsByUid(uid)) {
            auditService.logFailure(AuditService.ACTION_CARD_ISSUED, AuditService.CATEGORY_CARD, 
                "Card UID already exists: " + uid, "Card UID already exists");
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Card UID already exists: " + uid);
        }

        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> {
                    auditService.logFailure(AuditService.ACTION_CARD_ISSUED, AuditService.CATEGORY_CARD, 
                        "School not found: " + schoolId, "School not found");
                    return new ChangeSetPersister.NotFoundException();
                });

        Card card = new Card();
        card.setUid(uid);
        card.setSchool(school);
        card.setStatus(CardStatus.UNASSIGNED);
        card.setIssuedAt(LocalDateTime.now());

        Card savedCard = cardRepository.save(card);
        
        auditService.logAction(AuditService.ACTION_CARD_ISSUED, AuditService.CATEGORY_CARD, 
            AuditService.ENTITY_CARD, savedCard.getId(),
            "Card issued with UID: " + uid + " for school: " + school.getCode());
        
        return savedCard;
    }

    @Transactional
    public Card bindCard(Long cardId, Long studentId) throws ChangeSetPersister.NotFoundException {
        Card card = getCardForUpdate(cardId);  // with lock

        if (card.getStatus() != CardStatus.UNASSIGNED) {
            auditService.logFailure(AuditService.ACTION_CARD_BOUND, AuditService.CATEGORY_CARD, 
                "Card " + cardId + " is not available for binding (status: " + card.getStatus() + ")",
                "Card not available for binding");
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Card is not available for binding (status: " + card.getStatus() + ")");
        }

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> {
                    auditService.logFailure(AuditService.ACTION_CARD_BOUND, AuditService.CATEGORY_CARD, 
                        "Student not found: " + studentId, "Student not found");
                    return new ChangeSetPersister.NotFoundException();
                });

        // Check student doesn't already have active card
        Optional<CardAssignment> existing = assignmentRepository.findByStudentAndUnassignedAtIsNull(student);
        if (existing.isPresent()) {
            auditService.logFailure(AuditService.ACTION_CARD_BOUND, AuditService.CATEGORY_CARD, 
                "Student " + studentId + " already has active card", "Student already has active card");
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Student already has an active card (ID: " + existing.get().getCard().getId() + ")");
        }

        CardAssignment assignment = new CardAssignment();
        assignment.setCard(card);
        assignment.setStudent(student);
        assignment.setAssignedAt(LocalDateTime.now());
        // assignment.setAssignedBy(currentUser); // later

        assignmentRepository.save(assignment);

        card.setStatus(CardStatus.ASSIGNED);
        card.setCurrentAssignmentId(assignment.getId());

        Card savedCard = cardRepository.save(card);
        
        auditService.logAction(AuditService.ACTION_CARD_BOUND, AuditService.CATEGORY_CARD, 
            AuditService.ENTITY_CARD, savedCard.getId(),
            "Card " + card.getUid() + " bound to student: " + student.getStudentNumber() + " (" + student.getName() + ")",
            null, null);
        
        return savedCard;
    }

    @Transactional
    public Card blockCard(Long cardId) {
        Card card = getCardForUpdate(cardId);
        if (card.getStatus() == CardStatus.BLOCKED) {
            auditService.logFailure(AuditService.ACTION_CARD_BLOCKED, AuditService.CATEGORY_CARD, 
                "Card " + cardId + " is already blocked", "Card already blocked");
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Card is already blocked");
        }
        if (card.getStatus() == CardStatus.RETIRED) {
            auditService.logFailure(AuditService.ACTION_CARD_BLOCKED, AuditService.CATEGORY_CARD, 
                "Cannot block retired card " + cardId, "Cannot block retired card");
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Cannot block retired card");
        }

        Card savedCard = cardRepository.save(card);
        
        auditService.logAction(AuditService.ACTION_CARD_BLOCKED, AuditService.CATEGORY_CARD, 
            AuditService.ENTITY_CARD, savedCard.getId(),
            "Card " + card.getUid() + " blocked",
            "{\"status\":\"" + card.getStatus() + "\"}",
            "{\"status\":\"BLOCKED\"}");

        return savedCard;
    }

    @Transactional
    public Card retireCard(Long cardId) {
        Card card = getCardForUpdate(cardId);
        if (card.getStatus() == CardStatus.RETIRED) {
            auditService.logFailure(AuditService.ACTION_CARD_RETIRED, AuditService.CATEGORY_CARD, 
                "Card " + cardId + " is already retired", "Card already retired");
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Card is already retired");
        }

        card.setStatus(CardStatus.RETIRED);
        card.setRetiredAt(LocalDateTime.now());

        // Close any active assignment
        assignmentRepository.findByCardAndUnassignedAtIsNull(card)
                .ifPresent(assignment -> {
                    assignment.setUnassignedAt(LocalDateTime.now());
                    // assignment.setUnassignedBy(currentUser);
                    assignmentRepository.save(assignment);
                });

        Card savedCard = cardRepository.save(card);
        
        auditService.logAction(AuditService.ACTION_CARD_RETIRED, AuditService.CATEGORY_CARD, 
            AuditService.ENTITY_CARD, savedCard.getId(),
            "Card " + card.getUid() + " retired",
            "{\"status\":\"" + card.getStatus() + "\"}",
            "{\"status\":\"RETIRED\"}");

        return savedCard;
    }

    // Helper – get card with pessimistic lock if needed
    private Card getCardForUpdate(Long id) {
        return cardRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,"Card not found: " + id));
    }

    public Long getPreviousStudentIdIfAssigned(Long cardId) {
        return assignmentRepository.findTopByCardOrderByAssignedAtDesc(cardId)
                .map(CardAssignment::getStudent)
                .map(Student::getId)
                .orElse(null);
    }

    /**
     * List all cards belonging to a school, with current assignment info.
     */
    public List<com.cardsystem.dto.CardActionResponse> listCardsBySchoolWithAssignment(String schoolCode) {
        List<Card> cards = cardRepository.findBySchool_Code(schoolCode);
        List<com.cardsystem.dto.CardActionResponse> out = new ArrayList<>();

        for (Card card : cards) {
            Long studentId = assignmentRepository.findByCardAndUnassignedAtIsNull(card)
                    .map(CardAssignment::getStudent)
                    .map(Student::getId)
                    .orElse(null);

            com.cardsystem.dto.CardActionResponse item = com.cardsystem.dto.CardActionResponse.builder()
                    .cardId(card.getId())
                    .uid(card.getUid())
                    .status(card.getStatus())
                    .studentId(studentId)
                    .build();

            out.add(item);
        }

        return out;
    }
}
