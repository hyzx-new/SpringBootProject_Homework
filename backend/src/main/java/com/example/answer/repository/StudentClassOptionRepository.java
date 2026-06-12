package com.example.answer.repository;

import com.example.answer.entity.StudentClassOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentClassOptionRepository extends JpaRepository<StudentClassOption, Long> {

    List<StudentClassOption> findAllByOrderByCollegeAscMajorAscClassNameAsc();

    boolean existsByCollegeAndMajorAndClassName(String college, String major, String className);
}
