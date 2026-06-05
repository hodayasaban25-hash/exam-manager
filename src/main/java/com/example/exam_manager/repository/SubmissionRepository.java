package com.example.exam_manager.repository;

import com.example.exam_manager.model.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    List<Submission> findByTestId(Long testId);
    List<Submission> findByStudent_Id(Long studentId);
}
