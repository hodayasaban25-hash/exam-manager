package com.example.exam_manager.controller;

import com.example.exam_manager.model.Question;
import com.example.exam_manager.model.Test;
import com.example.exam_manager.repository.QuestionRepository;
import com.example.exam_manager.repository.TestRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tests/{testId}/questions")
public class QuestionController {

    private final QuestionRepository questionRepository;
    private final TestRepository testRepository;

    public QuestionController(QuestionRepository questionRepository, TestRepository testRepository) {
        this.questionRepository = questionRepository;
        this.testRepository = testRepository;
    }

    @GetMapping
    public ResponseEntity<List<Question>> getQuestionsByTest(@PathVariable Long testId) {
        if (!testRepository.existsById(testId)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(questionRepository.findByTestId(testId));
    }

    @PostMapping
    public ResponseEntity<Question> addQuestion(@PathVariable Long testId,
                                                @Valid @RequestBody Question question) {
        return testRepository.findById(testId).map(test -> {
            question.setTest(test);
            return ResponseEntity.ok(questionRepository.save(question));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{questionId}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable Long testId,
                                               @PathVariable Long questionId) {
        if (!testRepository.existsById(testId)) {
            return ResponseEntity.notFound().build();
        }
        if (!questionRepository.existsById(questionId)) {
            return ResponseEntity.notFound().build();
        }
        questionRepository.deleteById(questionId);
        return ResponseEntity.noContent().build();
    }
}
