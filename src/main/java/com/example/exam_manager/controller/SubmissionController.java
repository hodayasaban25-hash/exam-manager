package com.example.exam_manager.controller;

import com.example.exam_manager.model.Student;
import com.example.exam_manager.model.Submission;
import com.example.exam_manager.repository.StudentRepository;
import com.example.exam_manager.repository.SubmissionRepository;
import com.example.exam_manager.repository.TestRepository;
import com.example.exam_manager.service.AiService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tests/{testId}/submissions")
public class SubmissionController {

    private final SubmissionRepository submissionRepository;
    private final TestRepository testRepository;
    private final StudentRepository studentRepository;
    private final AiService aiService;

    public SubmissionController(SubmissionRepository submissionRepository,
                                TestRepository testRepository,
                                StudentRepository studentRepository,
                                AiService aiService) {
        this.submissionRepository = submissionRepository;
        this.testRepository = testRepository;
        this.studentRepository = studentRepository;
        this.aiService = aiService;
    }

    @GetMapping
    public ResponseEntity<List<Submission>> getSubmissions(@PathVariable Long testId) {
        if (!testRepository.existsById(testId)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(submissionRepository.findByTestId(testId));
    }

    @PostMapping
    public ResponseEntity<Submission> submitExam(@PathVariable Long testId,
                                                 @Valid @RequestBody Submission submission) {
        Student student = studentRepository.findById(submission.getStudentId()).orElse(null);
        if (student == null) {
            return ResponseEntity.badRequest().build();
        }

        return testRepository.findById(testId).map(test -> {
            submission.setTest(test);
            submission.setStudent(student);
            Submission saved = submissionRepository.save(submission);

            aiService.evaluateFullExam(saved, test.getQuestionPaperFile(), test.getMasterSolutionFile());
            Submission evaluated = submissionRepository.save(saved);

            return ResponseEntity.ok(evaluated);
        }).orElse(ResponseEntity.notFound().build());
    }
}
