package com.example.answer.repository;

import com.example.answer.entity.Submission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    List<Submission> findAllByOrderBySubmittedAtDesc();

    Page<Submission> findAllByOrderBySubmittedAtDesc(Pageable pageable);

    List<Submission> findByPaperIdOrderBySubmittedAtDesc(Long paperId);

    Page<Submission> findByPaperIdOrderBySubmittedAtDesc(Long paperId, Pageable pageable);

    List<Submission> findByStudentIdOrderBySubmittedAtDesc(Long studentId);

    Page<Submission> findByStudentIdOrderBySubmittedAtDesc(Long studentId, Pageable pageable);

    @Query(
            value = """
                    select s from Submission s
                    join s.paper p
                    where p.commonCourse = true
                       or p.major = :major
                    order by s.submittedAt desc
                    """,
            countQuery = """
                    select count(s) from Submission s
                    join s.paper p
                    where p.commonCourse = true
                       or p.major = :major
                    """
    )
    Page<Submission> findAccessibleByMajorOrderBySubmittedAtDesc(@Param("major") String major, Pageable pageable);

    @Query("""
            select s from Submission s
            join s.paper p
            where p.commonCourse = true
               or p.major = :major
            order by s.submittedAt desc
            """)
    List<Submission> findAccessibleByMajorOrderBySubmittedAtDesc(@Param("major") String major);

    @Query(
            value = """
                    select s from Submission s
                    join s.paper p
                    where s.student.id = :studentId
                      and (p.commonCourse = true
                           or p.major = :major)
                    order by s.submittedAt desc
                    """,
            countQuery = """
                    select count(s) from Submission s
                    join s.paper p
                    where s.student.id = :studentId
                      and (p.commonCourse = true
                           or p.major = :major)
                    """
    )
    Page<Submission> findByStudentIdAndAccessiblePaperOrderBySubmittedAtDesc(
            @Param("studentId") Long studentId,
            @Param("major") String major,
            Pageable pageable
    );
}
