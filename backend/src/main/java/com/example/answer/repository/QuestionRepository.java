package com.example.answer.repository;

import com.example.answer.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findByPaperIdOrderByOrderNoAsc(Long paperId);

    void deleteByPaperId(Long paperId);
}
