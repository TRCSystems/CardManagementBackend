package com.cardsystem.repository;


import com.cardsystem.models.School;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SchoolRepository extends JpaRepository<School, String> {
    // Since School uses String code as ID, JpaRepository<School, String>

    // Optional: find by name or partial match if needed later
}
