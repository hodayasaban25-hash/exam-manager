package com.example.exam_manager.controller;

import com.example.exam_manager.model.Teacher;
import com.example.exam_manager.model.TeacherRole;
import com.example.exam_manager.repository.TeacherRepository;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teachers")
public class TeacherController {

    private final TeacherRepository teacherRepository;

    public TeacherController(TeacherRepository teacherRepository) {
        this.teacherRepository = teacherRepository;
    }

    static class UpdateRoleRequest {
        @JsonProperty("role")               private String role;
        @JsonProperty("specializedSubject") private String specializedSubject;
        @JsonProperty("assignedClass")      private String assignedClass;
        public String getRole()               { return role; }
        public String getSpecializedSubject() { return specializedSubject; }
        public String getAssignedClass()      { return assignedClass; }
    }

    // רשימת כל המורות — מנהל בלבד
    @GetMapping
    public ResponseEntity<?> getAllTeachers(
            @RequestHeader(value = "X-Teacher-Id", required = false) Long requesterId) {
        if (!isPrincipal(requesterId)) {
            return ResponseEntity.status(403).body(Map.of("error", "גישה מותרת למנהל בלבד"));
        }
        return ResponseEntity.ok(teacherRepository.findAll());
    }

    // עדכון תפקיד ומקצוע — מנהל בלבד
    @PutMapping("/{id}")
    public ResponseEntity<?> updateTeacherRole(
            @PathVariable Long id,
            @RequestBody UpdateRoleRequest req,
            @RequestHeader(value = "X-Teacher-Id", required = false) Long requesterId) {
        if (!isPrincipal(requesterId)) {
            return ResponseEntity.status(403).body(Map.of("error", "גישה מותרת למנהל בלבד"));
        }
        Teacher teacher = teacherRepository.findById(id).orElse(null);
        if (teacher == null) return ResponseEntity.notFound().build();
        try {
            teacher.setRole(TeacherRole.valueOf(req.getRole()));
        } catch (IllegalArgumentException | NullPointerException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "תפקיד לא תקין"));
        }
        TeacherRole newRole = teacher.getRole();
        teacher.setSpecializedSubject(
                newRole == TeacherRole.SUBJECT && req.getSpecializedSubject() != null
                        && !req.getSpecializedSubject().isBlank()
                        ? req.getSpecializedSubject().trim() : null);
        teacher.setAssignedClass(
                newRole == TeacherRole.HOMEROOM && req.getAssignedClass() != null
                        && !req.getAssignedClass().isBlank()
                        ? req.getAssignedClass().trim() : null);
        return ResponseEntity.ok(teacherRepository.save(teacher));
    }

    private boolean isPrincipal(Long requesterId) {
        if (requesterId == null) return false;
        Teacher requester = teacherRepository.findById(requesterId).orElse(null);
        return requester != null && requester.getRole() == TeacherRole.PRINCIPAL;
    }
}
