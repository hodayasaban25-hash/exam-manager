package com.example.exam_manager.repository;

import com.example.exam_manager.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentRepository extends JpaRepository<Student, Long> {
    List<Student> findByClassroomId(Long classroomId);
}
