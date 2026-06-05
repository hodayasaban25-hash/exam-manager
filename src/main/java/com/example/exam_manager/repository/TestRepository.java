package com.example.exam_manager.repository;

import com.example.exam_manager.model.Test;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestRepository extends JpaRepository<Test, Long> {
    java.util.List<Test> findByTeacher_Id(Long teacherId);
    java.util.List<Test> findByClassroom_Id(Long classroomId);
    java.util.List<Test> findBySubject_Name(String subjectName);
    java.util.List<Test> findByClassroom_ClassName(String className);
}
