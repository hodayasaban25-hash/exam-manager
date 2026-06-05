package com.example.exam_manager.controller;

import com.example.exam_manager.model.Answer;
import com.example.exam_manager.repository.AnswerRepository;
import com.example.exam_manager.repository.QuestionRepository;
import com.example.exam_manager.service.AiService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/questions/{questionId}/answers")
public class AnswerController {

    private final AnswerRepository answerRepository;
    private final QuestionRepository questionRepository;
    private final AiService aiService;

    public AnswerController(AnswerRepository answerRepository,
                            QuestionRepository questionRepository,
                            AiService aiService) {
        this.answerRepository = answerRepository;
        this.questionRepository = questionRepository;
        this.aiService = aiService;
    }

    @GetMapping
    public ResponseEntity<List<Answer>> getAnswersByQuestion(@PathVariable Long questionId) {
        if (!questionRepository.existsById(questionId)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(answerRepository.findByQuestionId(questionId));
    }

    @PostMapping
    public ResponseEntity<Answer> addAnswer(@PathVariable Long questionId,
                                            @Valid @RequestBody Answer answer) {
        return questionRepository.findById(questionId).map(question -> {
            answer.setQuestion(question);
            Answer saved = answerRepository.save(answer);

            // הפעלת ה-AI לבדיקת התשובה ועדכון הציון והמשוב
            aiService.evaluateAnswer(question, saved);
            Answer evaluated = answerRepository.save(saved);

            return ResponseEntity.ok(evaluated);
        }).orElse(ResponseEntity.notFound().build());
    }
}
