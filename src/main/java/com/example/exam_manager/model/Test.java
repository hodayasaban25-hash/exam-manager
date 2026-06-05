package com.example.exam_manager.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

@Entity
@Table(name = "tests")
public class Test {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "כותרת המבחן לא יכולה להיות ריקה")
    private String title;

    @Column(columnDefinition = "TEXT")
    private String questionPaperFile;

    @Column(columnDefinition = "TEXT")
    private String masterSolutionFile;

    @ManyToOne
    @JoinColumn(name = "teacher_id")
    private Teacher teacher;

    @ManyToOne
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @ManyToOne
    @JoinColumn(name = "classroom_id")
    private Classroom classroom;

    // שדות עזר לקבלת IDs מה-JSON (לא נשמרים בבסיס הנתונים)
    @Transient
    private Long subjectId;

    @Transient
    private Long classroomId;

    @OneToMany(mappedBy = "test", cascade = CascadeType.ALL)
    private List<Question> questions;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getQuestionPaperFile() { return questionPaperFile; }
    public void setQuestionPaperFile(String questionPaperFile) { this.questionPaperFile = questionPaperFile; }

    public String getMasterSolutionFile() { return masterSolutionFile; }
    public void setMasterSolutionFile(String masterSolutionFile) { this.masterSolutionFile = masterSolutionFile; }

    public Teacher getTeacher() { return teacher; }
    public void setTeacher(Teacher teacher) { this.teacher = teacher; }

    public Subject getSubject() { return subject; }
    public void setSubject(Subject subject) { this.subject = subject; }

    public Classroom getClassroom() { return classroom; }
    public void setClassroom(Classroom classroom) { this.classroom = classroom; }

    public Long getSubjectId() { return subjectId; }
    public void setSubjectId(Long subjectId) { this.subjectId = subjectId; }

    public Long getClassroomId() { return classroomId; }
    public void setClassroomId(Long classroomId) { this.classroomId = classroomId; }

    public List<Question> getQuestions() { return questions; }
    public void setQuestions(List<Question> questions) { this.questions = questions; }

    @JsonProperty("totalScore")
    public int getTotalScore() {
        if (questions == null) return 0;
        return questions.stream().mapToInt(Question::getMaxScore).sum();
    }
}
