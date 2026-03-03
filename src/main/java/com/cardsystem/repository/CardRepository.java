package com.cardsystem.repository;


import com.cardsystem.models.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {

    // Custom query methods (add as needed)
    boolean existsByUid(String uid);

    Optional<Card> findByUid(String uid);

    // Example: find all cards for a school
    List<Card> findBySchool_Code(String schoolCode);
}