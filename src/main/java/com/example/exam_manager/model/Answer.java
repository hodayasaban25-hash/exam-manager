package com.example.exam_manager.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "answers")
public class Answer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "שם התלמיד לא יכול להיות ריק")
    private String studentName;

    @NotBlank(message = "תמונת כתב היד לא יכולה להיות ריקה")
    @Column(columnDefinition = "TEXT")
    private String handwrittenImageBase64;

    private Integer score;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    @ManyToOne
    @JoinColumn(name = "question_id")
    @JsonIgnore
    private Question question;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getHandwrittenImageBase64() { return handwrittenImageBase64; }
    public void setHandwrittenImageBase64(String handwrittenImageBase64) { this.handwrittenImageBase64 = handwrittenImageBase64; }

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }

    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }

    public Question getQuestion() { return question; }
    public void setQuestion(Question question) { this.question = question; }
}
