package com.example.exam_manager.controller;

import com.example.exam_manager.model.Student;
import com.example.exam_manager.model.Submission;
import com.example.exam_manager.model.Test;
import com.example.exam_manager.repository.StudentRepository;
import com.example.exam_manager.repository.SubmissionRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentRepository studentRepository;
    private final SubmissionRepository submissionRepository;

    public StudentController(StudentRepository studentRepository,
                             SubmissionRepository submissionRepository) {
        this.studentRepository = studentRepository;
        this.submissionRepository = submissionRepository;
    }

    @GetMapping
    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Student> updateStudent(
            @PathVariable Long id,
            @Valid @RequestBody Student updated,
            @RequestHeader(value = "X-Teacher-Id", required = false) Long teacherId) {
        Optional<Student> opt = studentRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Student student = opt.get();
        if (teacherId != null && student.getClassroom() != null &&
                student.getClassroom().getTeacher() != null &&
                !teacherId.equals(student.getClassroom().getTeacher().getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        student.setFirstName(updated.getFirstName());
        student.setLastName(updated.getLastName());
        return ResponseEntity.ok(studentRepository.save(student));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStudent(
            @PathVariable Long id,
            @RequestHeader(value = "X-Teacher-Id", required = false) Long teacherId) {
        Optional<Student> opt = studentRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Student student = opt.get();
        if (teacherId != null && student.getClassroom() != null &&
                student.getClassroom().getTeacher() != null &&
                !teacherId.equals(student.getClassroom().getTeacher().getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        submissionRepository.deleteAll(submissionRepository.findByStudent_Id(id));
        studentRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    record SubmissionSummary(Long id, String testTitle, String subjectName,
                             Integer totalGrade, String generalFeedback) {}

    @GetMapping("/{studentId}/submissions")
    public ResponseEntity<List<SubmissionSummary>> getStudentSubmissions(@PathVariable Long studentId) {
        if (!studentRepository.existsById(studentId)) {
            return ResponseEntity.notFound().build();
        }
        List<Submission> subs = submissionRepository.findByStudent_Id(studentId);
        List<SubmissionSummary> result = subs.stream().map(s -> {
            Test test = s.getTest();
            String title   = test != null ? test.getTitle() : "—";
            String subject = (test != null && test.getSubject() != null) ? test.getSubject().getName() : "—";
            return new SubmissionSummary(s.getId(), title, subject, s.getTotalGrade(), s.getGeneralFeedback());
        }).toList();
        return ResponseEntity.ok(result);
    }
}
