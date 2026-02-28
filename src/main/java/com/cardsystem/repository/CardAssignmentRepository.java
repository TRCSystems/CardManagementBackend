package com.cardsystem.repository;


import com.cardsystem.models.Card;
import com.cardsystem.models.CardAssignment;
import com.cardsystem.models.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CardAssignmentRepository extends JpaRepository<CardAssignment, Long> {

    // Find active assignment for a card
    Optional<CardAssignment> findByCardAndUnassignedAtIsNull(Card card);

    // Find active assignment for a student
    Optional<CardAssignment> findByStudentAndUnassignedAtIsNull(Student student);

    // Check if student has any active card
    boolean existsByStudentAndUnassignedAtIsNull(Student student);

    // For re-issue: get most recent assignment (even if closed)
    Optional<CardAssignment> findTopByCardOrderByAssignedAtDesc(Long card);
}