package com.example.answer.repository;

import com.example.answer.entity.Paper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PaperRepository extends JpaRepository<Paper, Long> {

    List<Paper> findAllByOrderByStartTimeDesc();

    Page<Paper> findAllByOrderByStartTimeDesc(Pageable pageable);

    List<Paper> findByPublishedTrueOrderByStartTimeAsc();

    Page<Paper> findByPublishedTrueOrderByStartTimeAsc(Pageable pageable);

    @Query("""
            select p from Paper p
            where p.commonCourse = true
               or p.major = :major
            order by p.startTime desc
            """)
    Page<Paper> findAccessibleByMajorOrderByStartTimeDesc(@Param("major") String major, Pageable pageable);

    @Query("""
            select p from Paper p
            where p.published = true
              and (p.commonCourse = true
                   or p.major = :major)
            order by p.startTime asc
            """)
    Page<Paper> findPublishedAccessibleByMajorOrderByStartTimeAsc(@Param("major") String major, Pageable pageable);

    List<Paper> findByStartTimeBetween(LocalDateTime start, LocalDateTime end);
}
