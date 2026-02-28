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

@Service
@RequiredArgsConstructor
@Slf4j
public class CardService {

    private final CardRepository cardRepository;
    private final CardAssignmentRepository assignmentRepository;
    private final StudentRepository studentRepository;
    private final SchoolRepository schoolRepository;
    // private final AuditService auditService;  // add later

    @Transactional
    public Card issueCard(String uidRaw, String schoolId) throws ChangeSetPersister.NotFoundException {
        String uid = uidRaw.trim().toUpperCase();

        if (cardRepository.existsByUid(uid)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Card UID already exists: " + uid);
        }

        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ChangeSetPersister.NotFoundException());

        Card card = new Card();
        card.setUid(uid);
        card.setSchool(school);
        card.setStatus(CardStatus.UNASSIGNED);
        card.setIssuedAt(LocalDateTime.now());

        return cardRepository.save(card);
    }

    @Transactional
    public Card bindCard(Long cardId, Long studentId) throws ChangeSetPersister.NotFoundException {
        Card card = getCardForUpdate(cardId);  // with lock

        if (card.getStatus() != CardStatus.UNASSIGNED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Card is not available for binding (status: " + card.getStatus() + ")");
        }

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ChangeSetPersister.NotFoundException());

        // Check student doesn't already have active card
        Optional<CardAssignment> existing = assignmentRepository.findByStudentAndUnassignedAtIsNull(student);
        if (existing.isPresent()) {
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

        return cardRepository.save(card);
    }

    @Transactional
    public Card blockCard(Long cardId) {
        Card card = getCardForUpdate(cardId);
        if (card.getStatus() == CardStatus.BLOCKED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Card is already blocked");
        }
        if (card.getStatus() == CardStatus.RETIRED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Cannot block retired card");
        }

        card.setStatus(CardStatus.BLOCKED);
        // audit later

        return cardRepository.save(card);
    }

    @Transactional
    public Card retireCard(Long cardId) {
        Card card = getCardForUpdate(cardId);
        if (card.getStatus() == CardStatus.RETIRED) {
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

        return cardRepository.save(card);
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
}
