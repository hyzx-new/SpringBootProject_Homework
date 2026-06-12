package com.example.answer.repository;

import com.example.answer.entity.ExamReminder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExamReminderRepository extends JpaRepository<ExamReminder, Long> {

    boolean existsByPaperIdAndStudentId(Long paperId, Long studentId);

    List<ExamReminder> findByStudentIdOrderByRemindAtDesc(Long studentId);
}
