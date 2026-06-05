package com.example.exam_manager.controller;

import com.example.exam_manager.dto.ClassReportDTO;
import com.example.exam_manager.dto.StudentAverageDTO;
import com.example.exam_manager.model.Classroom;
import com.example.exam_manager.model.Student;
import com.example.exam_manager.model.Submission;
import com.example.exam_manager.model.Teacher;
import com.example.exam_manager.model.TeacherRole;
import com.example.exam_manager.model.Test;
import com.example.exam_manager.repository.ClassroomRepository;
import com.example.exam_manager.repository.StudentRepository;
import com.example.exam_manager.repository.SubmissionRepository;
import com.example.exam_manager.repository.TeacherRepository;
import com.example.exam_manager.repository.TestRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/classrooms")
public class ClassroomController {

    private final ClassroomRepository classroomRepository;
    private final StudentRepository studentRepository;
    private final SubmissionRepository submissionRepository;
    private final TeacherRepository teacherRepository;
    private final TestRepository testRepository;

    public ClassroomController(ClassroomRepository classroomRepository,
                               StudentRepository studentRepository,
                               SubmissionRepository submissionRepository,
                               TeacherRepository teacherRepository,
                               TestRepository testRepository) {
        this.classroomRepository = classroomRepository;
        this.studentRepository   = studentRepository;
        this.submissionRepository = submissionRepository;
        this.teacherRepository   = teacherRepository;
        this.testRepository      = testRepository;
    }

    @GetMapping
    public List<Classroom> getAllClassrooms(
            @RequestHeader(value = "X-Teacher-Id", required = false) Long teacherId) {
        if (teacherId != null) {
            Teacher teacher = teacherRepository.findById(teacherId).orElse(null);
            if (teacher != null && teacher.getRole() == TeacherRole.HOMEROOM) {
                // מחנכת רואה רק את הכיתה שלה
                return classroomRepository.findByClassName(teacher.getAssignedClass());
            }
        }
        return classroomRepository.findAll();
    }

    @PostMapping
    public Classroom createClassroom(
            @Valid @RequestBody Classroom classroom,
            @RequestHeader(value = "X-Teacher-Id", required = false) Long teacherId) {
        if (teacherId != null) {
            teacherRepository.findById(teacherId).ifPresent(classroom::setTeacher);
        }
        return classroomRepository.save(classroom);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Classroom> updateClassroom(
            @PathVariable Long id,
            @Valid @RequestBody Classroom updated,
            @RequestHeader(value = "X-Teacher-Id", required = false) Long teacherId) {
        Optional<Classroom> opt = classroomRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Classroom classroom = opt.get();
        if (teacherId != null && classroom.getTeacher() != null &&
                !teacherId.equals(classroom.getTeacher().getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        classroom.setClassName(updated.getClassName());
        return ResponseEntity.ok(classroomRepository.save(classroom));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClassroom(
            @PathVariable Long id,
            @RequestHeader(value = "X-Teacher-Id", required = false) Long teacherId) {
        Optional<Classroom> opt = classroomRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Classroom classroom = opt.get();
        if (teacherId != null && classroom.getTeacher() != null &&
                !teacherId.equals(classroom.getTeacher().getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        // מחיקת הגשות של תלמידים בכיתה זו, ואז מחיקת התלמידים
        List<Student> students = studentRepository.findByClassroomId(id);
        for (Student student : students) {
            submissionRepository.deleteAll(submissionRepository.findByStudent_Id(student.getId()));
        }
        studentRepository.deleteAll(students);
        // ניתוק מבחנים מהכיתה (לא מוחקים את המבחנים)
        List<Test> tests = testRepository.findByClassroom_Id(id);
        for (Test test : tests) {
            test.setClassroom(null);
            testRepository.save(test);
        }
        classroomRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{classroomId}/students")
    public ResponseEntity<List<Student>> getStudents(@PathVariable Long classroomId) {
        if (!classroomRepository.existsById(classroomId)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(studentRepository.findByClassroomId(classroomId));
    }

    @PostMapping("/{classroomId}/students")
    public ResponseEntity<Student> addStudent(@PathVariable Long classroomId,
                                              @Valid @RequestBody Student student) {
        return classroomRepository.findById(classroomId).map(classroom -> {
            student.setClassroom(classroom);
            return ResponseEntity.ok(studentRepository.save(student));
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{classroomId}/report")
    public ResponseEntity<ClassReportDTO> getClassReport(
            @PathVariable Long classroomId,
            @RequestHeader(value = "X-Teacher-Id", required = false) Long teacherId) {
        Classroom classroom = classroomRepository.findById(classroomId).orElse(null);
        if (classroom == null) return ResponseEntity.notFound().build();

        // קביעת פילטר מקצוע + בדיקת הרשאות לפי תפקיד
        final String subjectFilter;
        if (teacherId != null) {
            Teacher teacher = teacherRepository.findById(teacherId).orElse(null);
            if (teacher != null) {
                TeacherRole role = teacher.getRole() != null ? teacher.getRole() : TeacherRole.HOMEROOM;
                if (role == TeacherRole.HOMEROOM) {
                    // מחנכת: גישה רק לכיתה שלה, ללא הגבלת מקצוע
                    String assigned = teacher.getAssignedClass();
                    if (assigned == null || !assigned.equals(classroom.getClassName())) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                    }
                    subjectFilter = null;
                } else if (role == TeacherRole.SUBJECT && teacher.getSpecializedSubject() != null) {
                    subjectFilter = teacher.getSpecializedSubject();
                } else {
                    subjectFilter = null; // PRINCIPAL: ללא הגבלה
                }
            } else {
                subjectFilter = null;
            }
        } else {
            subjectFilter = null;
        }

        List<Student> students = studentRepository.findByClassroomId(classroomId);

        // ממוצע אישי לכל תלמיד (מסונן לפי מקצוע אם צריך)
        List<StudentAverageDTO> studentAverages = students.stream().map(s -> {
            List<Integer> studentGrades = submissionRepository.findByStudent_Id(s.getId()).stream()
                    .filter(sub -> subjectFilter == null || (sub.getTest() != null
                            && sub.getTest().getSubject() != null
                            && sub.getTest().getSubject().getName().equals(subjectFilter)))
                    .map(Submission::getTotalGrade)
                    .filter(g -> g != null)
                    .toList();
            double avg = studentGrades.isEmpty() ? 0
                    : studentGrades.stream().mapToInt(Integer::intValue).average().orElse(0);
            return new StudentAverageDTO(
                    s.getFirstName() + " " + s.getLastName(),
                    Math.round(avg * 10.0) / 10.0,
                    studentGrades.size()
            );
        }).toList();

        List<Integer> allGrades = students.stream()
                .flatMap(s -> submissionRepository.findByStudent_Id(s.getId()).stream()
                        .filter(sub -> subjectFilter == null || (sub.getTest() != null
                                && sub.getTest().getSubject() != null
                                && sub.getTest().getSubject().getName().equals(subjectFilter)))
                        .map(Submission::getTotalGrade)
                        .filter(g -> g != null))
                .toList();

        int total = allGrades.size();
        if (total == 0) {
            return ResponseEntity.ok(new ClassReportDTO(0, 0, 0, 0, 0, 0, 0, studentAverages));
        }

        double avg         = allGrades.stream().mapToInt(Integer::intValue).average().orElse(0);
        int excellentCount = (int) allGrades.stream().filter(g -> g >= 86).count();
        int goodCount      = (int) allGrades.stream().filter(g -> g >= 71 && g <= 85).count();
        int passingCount   = (int) allGrades.stream().filter(g -> g >= 56 && g <= 70).count();
        int failCount      = (int) allGrades.stream().filter(g -> g < 56).count();
        double successRate = (double)(excellentCount + goodCount + passingCount) / total * 100;

        return ResponseEntity.ok(new ClassReportDTO(
                Math.round(avg * 10.0) / 10.0,
                total,
                Math.round(successRate * 10.0) / 10.0,
                excellentCount, goodCount, passingCount, failCount,
                studentAverages
        ));
    }
}
