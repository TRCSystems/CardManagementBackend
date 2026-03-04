package com.cardsystem.controller;


import com.cardsystem.dto.*;
import com.cardsystem.models.Student;
import com.cardsystem.services.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    // ────────────────────────────────────────────────
    // 1. Create single student
    // ────────────────────────────────────────────────
    @PostMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<StudentResponse> createStudent(
            @RequestParam("schoolId") String schoolId,
            @Valid @RequestBody StudentCreateRequest request) {

        Student student = studentService.createStudent(schoolId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toStudentResponse(student));
    }

    // ────────────────────────────────────────────────
    // 2. Bulk upload students from CSV
    // ────────────────────────────────────────────────
    @PostMapping(value = "/bulk-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<BulkUploadResponse> bulkUploadStudents(
            @RequestParam("file") MultipartFile file,
            @RequestParam("schoolId") String schoolId) {

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No file uploaded");
        }

        if (!file.getOriginalFilename().toLowerCase().endsWith(".xlsx")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only .xlsx files are allowed");
        }

        BulkUploadResponse response = studentService.bulkCreateFromCsv(file, schoolId);

        return ResponseEntity.ok(response);
    }

    // ────────────────────────────────────────────────
    // 3. Get single student by ID
    // ────────────────────────────────────────────────
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'SUPER_ADMIN', 'FINANCE_ADMIN', 'READ_ONLY')")
    public ResponseEntity<StudentResponse> getStudent(@PathVariable Long id) {
        Student student = studentService.getStudentById(id);
        return ResponseEntity.ok(toStudentResponse(student));
    }

    // ────────────────────────────────────────────────
    // 4. List all students for a school (include card assignment info)
    // ────────────────────────────────────────────────
    @GetMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'SUPER_ADMIN', 'FINANCE_ADMIN', 'READ_ONLY')")
    public ResponseEntity<java.util.List<StudentResponse>> listStudents(@RequestParam("schoolId") String schoolId) {
        java.util.List<StudentResponse> list = studentService.listStudentsBySchoolWithCards(schoolId);
        return ResponseEntity.ok(list);
    }

    // Fee status per student
    @GetMapping("/{id}/fee-status")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'SUPER_ADMIN', 'FINANCE_ADMIN', 'READ_ONLY')")
    public ResponseEntity<com.cardsystem.dto.FeeStatusResponse> getFeeStatus(@PathVariable Long id) {
        return ResponseEntity.ok(studentService.getFeeStatus(id));
    }

    // ────────────────────────────────────────────────
    // 5. Update student (partial)
    // ────────────────────────────────────────────────
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<StudentResponse> updateStudent(
            @PathVariable Long id,
            @Valid @RequestBody StudentUpdateRequest request) {

        Student updated = studentService.updateStudent(id, request);
        return ResponseEntity.ok(toStudentResponse(updated));
    }

    // Helper: convert entity → DTO
    private StudentResponse toStudentResponse(Student student) {
        StudentResponse.StudentResponseBuilder b = StudentResponse.builder()
                .id(student.getId())
                .studentNumber(student.getStudentNumber())
                .name(student.getName())
                .classGrade(student.getClassGrade())
                .schoolCode(student.getSchool().getCode())
                .createdAt(student.getCreatedAt());

        // card info may be added later if loaded by service; default null
        return b.build();
    }
}
