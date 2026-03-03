package com.cardsystem.services;

import com.cardsystem.dto.BulkUploadResponse;
import com.cardsystem.dto.StudentCreateRequest;
import com.cardsystem.dto.StudentUpdateRequest;
import com.cardsystem.models.School;
import com.cardsystem.models.Student;
import com.cardsystem.models.Wallet;
import com.cardsystem.repository.SchoolRepository;
import com.cardsystem.repository.StudentRepository;
import com.cardsystem.repository.CardAssignmentRepository;
import com.cardsystem.models.Card;
import com.cardsystem.models.CardAssignment;

//import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentService {

    private final StudentRepository studentRepository;
    private final SchoolRepository schoolRepository;
    private final CardAssignmentRepository assignmentRepository;

    @Transactional
    public Student createStudent(String schoolId, StudentCreateRequest request) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "School not found"));

        if (studentRepository.existsBySchoolAndStudentNumber(school, request.getStudentNumber())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Student number " + request.getStudentNumber() + " already exists in this school");
        }

        Student student = new Student();
        student.setSchool(school);
        student.setStudentNumber(request.getStudentNumber());
        student.setName(request.getName());
        student.setClassGrade(request.getClassGrade());

        Wallet wallet = new Wallet();
        wallet.setStudent(student);

        student.setWallet(wallet);

        return studentRepository.save(student);
    }

    @Transactional(readOnly = true)
    public Student getStudentById(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
    }
    @Transactional(readOnly = true)
    public List<com.cardsystem.dto.StudentResponse> listStudentsBySchoolWithCards(String schoolId) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "School not found"));
        List<Student> students = studentRepository.findBySchool_Code(schoolId);
        List<com.cardsystem.dto.StudentResponse> out = new ArrayList<>();

        for (Student student : students) {
            com.cardsystem.dto.StudentResponse.StudentResponseBuilder b = com.cardsystem.dto.StudentResponse.builder()
                    .id(student.getId())
                    .studentNumber(student.getStudentNumber())
                    .name(student.getName())
                    .classGrade(student.getClassGrade())
                    .schoolCode(student.getSchool().getCode())
                    .createdAt(student.getCreatedAt());

            assignmentRepository.findByStudentAndUnassignedAtIsNull(student)
                    .ifPresent(assign -> {
                        Card card = assign.getCard();
                        b.cardId(card.getId());
                        b.cardUid(card.getUid());
                    });

            out.add(b.build());
        }

        return out;
    }

    @Transactional
    public Student updateStudent(Long id, StudentUpdateRequest request) {
        Student student = getStudentById(id);

        // Only update provided fields (partial update)
        if (request.getName() != null) {
            student.setName(request.getName());
        }
        if (request.getClassGrade() != null) {
            student.setClassGrade(request.getClassGrade());
        }

        return studentRepository.save(student);
    }

    @Transactional
    public BulkUploadResponse bulkCreateFromCsv(MultipartFile file, String schoolId) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "School not found"));

        List<BulkUploadResponse.FailedRowDetail> failures = new ArrayList<>();
        int successCount = 0;
        int rowNum = 0;

        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream()));
             CSVParser parser = new CSVParser(br, CSVFormat.DEFAULT
                     .withFirstRecordAsHeader()
                     .withIgnoreHeaderCase()
                     .withTrim())) {

            for (CSVRecord record : parser) {
                rowNum++;

                try {
                    String studentNumber = record.get("studentNumber").trim();
                    String name = record.get("name").trim();
                    String classGrade = record.isSet("classGrade") ? record.get("classGrade").trim() : null;

                    if (studentNumber.isBlank() || name.isBlank()) {
                        failures.add(failure(rowNum, studentNumber, "Missing studentNumber or name"));
                        continue;
                    }

                    if (studentRepository.existsBySchoolAndStudentNumber(school, studentNumber)) {
                        failures.add(failure(rowNum, studentNumber, "Duplicate student number"));
                        continue;
                    }

                    Student student = new Student();
                    student.setSchool(school);
                    student.setStudentNumber(studentNumber);
                    student.setName(name);
                    student.setClassGrade(classGrade);

                    Wallet wallet = new Wallet();
                    wallet.setStudent(student);
                    student.setWallet(wallet);

                    studentRepository.save(student);
                    successCount++;

                } catch (Exception e) {
                    failures.add(failure(rowNum, record.get("studentNumber"), e.getMessage()));
                }
            }

        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to read CSV: " + e.getMessage());
        }

        return BulkUploadResponse.builder()
                .totalRows(rowNum)
                .successCount(successCount)
                .failedCount(failures.size())
                .failures(failures)
                .build();
    }

    private BulkUploadResponse.FailedRowDetail failure(int row, String num, String msg) {
        return BulkUploadResponse.FailedRowDetail.builder()
                .rowNumber(row)
                .studentNumber(num)
                .errorMessage(msg)
                .build();
    }
}