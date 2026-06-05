package com.example.exam_manager.repository;

import com.example.exam_manager.model.Classroom;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassroomRepository extends JpaRepository<Classroom, Long> {
    java.util.List<Classroom> findByTeacher_Id(Long teacherId);
    java.util.List<Classroom> findByClassName(String className);
}
