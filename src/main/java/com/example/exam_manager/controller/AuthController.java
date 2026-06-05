package com.example.exam_manager.controller;

import com.example.exam_manager.model.Teacher;
import com.example.exam_manager.model.TeacherRole;
import com.example.exam_manager.repository.TeacherRepository;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final TeacherRepository teacherRepository;

    public AuthController(TeacherRepository teacherRepository) {
        this.teacherRepository = teacherRepository;
    }

    // מחלקה רגילה במקום record — מניעת בעיות deserialization של Jackson עם Java records
    static class LoginRequest {
        @JsonProperty("identityNumber") private String identityNumber;
        @JsonProperty("password")       private String password;
        public String getIdentityNumber() { return identityNumber; }
        public String getPassword()       { return password; }
    }

    static class ForgotPasswordRequest {
        @JsonProperty("email") private String email;
        public String getEmail() { return email; }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody Teacher teacher) {
        if (teacherRepository.findByIdentityNumber(teacher.getIdentityNumber()).isPresent()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "תעודת זהות " + teacher.getIdentityNumber() + " כבר קיימת במערכת"));
        }
        // המורה הראשונה במערכת מקבלת אוטומטית תפקיד מנהל
        if (teacherRepository.count() == 0) {
            teacher.setRole(TeacherRole.PRINCIPAL);
        } else if (teacher.getRole() == null) {
            teacher.setRole(TeacherRole.HOMEROOM);
        }
        return ResponseEntity.ok(teacherRepository.save(teacher));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        String idNum    = req.getIdentityNumber();
        String password = req.getPassword();

        System.out.println("[AUTH] ניסיון התחברות עם ת\"ז: " + idNum);

        if (idNum == null || idNum.isBlank() || password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "יש לספק תעודת זהות וסיסמה"));
        }

        Optional<Teacher> teacherOpt = teacherRepository.findByIdentityNumber(idNum.trim());
        if (teacherOpt.isEmpty()) {
            System.out.println("[AUTH] לא נמצא מורה עם ת\"ז: " + idNum);
            return ResponseEntity.status(401).body(Map.of("error", "תעודת זהות או סיסמה שגויים"));
        }

        Teacher teacher = teacherOpt.get();
        String storedPassword = teacher.getPassword();
        System.out.println("[AUTH] סיסמה שמורה קיימת: " + (storedPassword != null));

        if (storedPassword == null || !storedPassword.equals(password)) {
            System.out.println("[AUTH] סיסמה שגויה עבור ת\"ז: " + idNum);
            return ResponseEntity.status(401).body(Map.of("error", "תעודת זהות או סיסמה שגויים"));
        }

        System.out.println("[AUTH] התחברות הצליחה: " + teacher.getFullName());
        return ResponseEntity.ok(teacher);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest req) {
        String email = req.getEmail();
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "יש לספק כתובת אימייל"));
        }

        Optional<Teacher> teacherOpt = teacherRepository.findByEmail(email.trim());
        if (teacherOpt.isEmpty()) {
            // מאבטח — לא חושפים אם המייל קיים
            System.out.println("[AUTH] בקשת שחזור סיסמה עבור מייל לא קיים: " + email);
        } else {
            Teacher teacher = teacherOpt.get();
            // סביבת פיתוח — הדפסה לטרמינל במקום שליחת מייל
            System.out.println("=================================================");
            System.out.println("[AUTH] שחזור סיסמה למורה: " + teacher.getFullName());
            System.out.println("[AUTH] אימייל: " + teacher.getEmail());
            System.out.println("[AUTH] סיסמה נוכחית: " + teacher.getPassword());
            System.out.println("[AUTH] קישור לאיפוס (פיתוח): http://localhost:8080/?reset=true");
            System.out.println("=================================================");
        }

        return ResponseEntity.ok(Map.of("message", "אם כתובת האימייל קיימת במערכת, הוראות שחזור הסיסמה נשלחו אליה"));
    }
}
