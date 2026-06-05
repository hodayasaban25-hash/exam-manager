package com.example.exam_manager.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "submissions")
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_id")
    private Student student;

    // שדה עזר לקבלת studentId מה-JSON (לא נשמר בבסיס הנתונים)
    @Transient
    @NotNull(message = "יש לציין מזהה תלמיד")
    private Long studentId;

    @NotBlank(message = "קובץ המבחן לא יכול להיות ריק")
    @Column(columnDefinition = "TEXT")
    private String examFileBase64;

    private Integer totalGrade;

    @Column(columnDefinition = "TEXT")
    private String generalFeedback;

    @ManyToOne
    @JoinColumn(name = "test_id")
    @JsonIgnore
    private Test test;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public String getExamFileBase64() { return examFileBase64; }
    public void setExamFileBase64(String examFileBase64) { this.examFileBase64 = examFileBase64; }

    public Integer getTotalGrade() { return totalGrade; }
    public void setTotalGrade(Integer totalGrade) { this.totalGrade = totalGrade; }

    public String getGeneralFeedback() { return generalFeedback; }
    public void setGeneralFeedback(String generalFeedback) { this.generalFeedback = generalFeedback; }

    public Test getTest() { return test; }
    public void setTest(Test test) { this.test = test; }
}
