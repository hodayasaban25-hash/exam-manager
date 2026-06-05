package com.example.exam_manager.repository;

import com.example.exam_manager.model.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {
    Optional<Teacher> findByIdentityNumber(String identityNumber);
    Optional<Teacher> findByEmail(String email);
}
