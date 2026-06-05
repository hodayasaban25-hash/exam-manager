package com.example.exam_manager.controller;

import com.example.exam_manager.model.Teacher;
import com.example.exam_manager.model.TeacherRole;
import com.example.exam_manager.model.Test;
import com.example.exam_manager.repository.ClassroomRepository;
import com.example.exam_manager.repository.SubjectRepository;
import com.example.exam_manager.repository.SubmissionRepository;
import com.example.exam_manager.repository.TeacherRepository;
import com.example.exam_manager.repository.TestRepository;
import com.example.exam_manager.service.AiService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/tests")
public class TestController {

    private final TestRepository testRepository;
    private final SubjectRepository subjectRepository;
    private final ClassroomRepository classroomRepository;
    private final TeacherRepository teacherRepository;
    private final SubmissionRepository submissionRepository;
    private final AiService aiService;

    public TestController(TestRepository testRepository,
                          SubjectRepository subjectRepository,
                          ClassroomRepository classroomRepository,
                          TeacherRepository teacherRepository,
                          SubmissionRepository submissionRepository,
                          AiService aiService) {
        this.testRepository = testRepository;
        this.subjectRepository = subjectRepository;
        this.classroomRepository = classroomRepository;
        this.teacherRepository = teacherRepository;
        this.submissionRepository = submissionRepository;
        this.aiService = aiService;
    }

    @GetMapping
    public List<Test> getAllTests(
            @RequestHeader(value = "X-Teacher-Id", required = false) Long teacherId) {
        if (teacherId != null) {
            Teacher teacher = teacherRepository.findById(teacherId).orElse(null);
            if (teacher != null) {
                TeacherRole role = teacher.getRole() != null ? teacher.getRole() : TeacherRole.HOMEROOM;
                if (role == TeacherRole.SUBJECT) {
                    return testRepository.findBySubject_Name(teacher.getSpecializedSubject());
                }
                if (role == TeacherRole.HOMEROOM) {
                    return testRepository.findByClassroom_ClassName(teacher.getAssignedClass());
                }
                // PRINCIPAL: רואה הכל
            }
        }
        return testRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Test> getTestById(@PathVariable Long id) {
        return testRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Test createTest(@Valid @RequestBody Test test,
                           @RequestHeader(value = "X-Teacher-Id", required = false) Long teacherId) {
        if (test.getSubjectId() != null) {
            subjectRepository.findById(test.getSubjectId()).ifPresent(test::setSubject);
        }
        if (test.getClassroomId() != null) {
            classroomRepository.findById(test.getClassroomId()).ifPresent(test::setClassroom);
        }
        if (teacherId != null) {
            teacherRepository.findById(teacherId).ifPresent(test::setTeacher);
        }
        Test saved = testRepository.save(test);
        aiService.parseMasterExam(saved);
        return testRepository.findById(saved.getId()).orElse(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Test> updateTest(
            @PathVariable Long id,
            @RequestBody Test updated,
            @RequestHeader(value = "X-Teacher-Id", required = false) Long teacherId) {
        Optional<Test> opt = testRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Test test = opt.get();
        if (teacherId != null) {
            Teacher teacher = teacherRepository.findById(teacherId).orElse(null);
            TeacherRole role = teacher != null && teacher.getRole() != null ? teacher.getRole() : TeacherRole.HOMEROOM;
            if (role == TeacherRole.SUBJECT) {
                // מורה מקצועית: רשאית לערוך רק מבחנים במקצוע שלה
                if (test.getSubject() == null || !test.getSubject().getName().equals(
                        teacher.getSpecializedSubject())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                }
            } else if (role == TeacherRole.HOMEROOM) {
                // מחנכת: רשאית לערוך רק מבחנים של הכיתה שלה
                if (test.getClassroom() == null || !test.getClassroom().getClassName().equals(
                        teacher.getAssignedClass())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                }
            }
            // PRINCIPAL: אין הגבלה
        }
        if (updated.getTitle() != null && !updated.getTitle().isBlank()) {
            test.setTitle(updated.getTitle());
        }
        if (updated.getSubjectId() != null) {
            subjectRepository.findById(updated.getSubjectId()).ifPresent(test::setSubject);
        }
        if (updated.getClassroomId() != null) {
            classroomRepository.findById(updated.getClassroomId()).ifPresent(test::setClassroom);
        }
        boolean newQuestionPaper = updated.getQuestionPaperFile() != null
                                   && !updated.getQuestionPaperFile().isBlank();
        boolean newSolutionFile  = updated.getMasterSolutionFile() != null
                                   && !updated.getMasterSolutionFile().isBlank();
        if (newQuestionPaper) test.setQuestionPaperFile(updated.getQuestionPaperFile());
        if (newSolutionFile)  test.setMasterSolutionFile(updated.getMasterSolutionFile());

        Test saved = testRepository.save(test);

        // אם הועלה שאלון חדש — חלץ שאלות מחדש (ה-AI יוסיף אותן לרשימה הקיימת)
        if (newQuestionPaper) aiService.parseMasterExam(saved);

        return ResponseEntity.ok(testRepository.findById(saved.getId()).orElse(saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTest(
            @PathVariable Long id,
            @RequestHeader(value = "X-Teacher-Id", required = false) Long teacherId) {
        Optional<Test> opt = testRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Test test = opt.get();
        if (teacherId != null) {
            Teacher teacher = teacherRepository.findById(teacherId).orElse(null);
            TeacherRole role = teacher != null && teacher.getRole() != null ? teacher.getRole() : TeacherRole.HOMEROOM;
            if (role == TeacherRole.SUBJECT) {
                if (test.getSubject() == null || !test.getSubject().getName().equals(
                        teacher.getSpecializedSubject())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                }
            } else if (role == TeacherRole.HOMEROOM) {
                if (test.getClassroom() == null || !test.getClassroom().getClassName().equals(
                        teacher.getAssignedClass())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                }
            }
            // PRINCIPAL: אין הגבלה
        }
        submissionRepository.deleteAll(submissionRepository.findByTestId(id));
        testRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
