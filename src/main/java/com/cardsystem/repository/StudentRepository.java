package com.cardsystem.repository;


import com.cardsystem.models.School;
import com.cardsystem.models.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    boolean existsBySchoolAndStudentNumber(School school, String studentNumber);

    Optional<Object> findByStudentNumber(String reference);

    List<Student> findBySchool_Code(String schoolCode);
    // Optional: find by student number within school
    // Optional<Student> findBySchoolAndStudentNumber(School school, String studentNumber);
}