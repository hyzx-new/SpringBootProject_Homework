package com.example.answer.repository;

import com.example.answer.entity.StudentAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentAnswerRepository extends JpaRepository<StudentAnswer, Long> {

    List<StudentAnswer> findBySubmissionId(Long submissionId);
}
