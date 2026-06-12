package com.example.answer.repository;

import com.example.answer.entity.StudentProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudentProfileRepository extends JpaRepository<StudentProfile, Long> {

    Optional<StudentProfile> findByUserId(Long userId);

    Optional<StudentProfile> findByStudentNo(String studentNo);

    Optional<StudentProfile> findTopByStudentNoStartingWithOrderByStudentNoDesc(String prefix);

    boolean existsByStudentNo(String studentNo);

    boolean existsByStudentNoAndIdNot(String studentNo, Long id);

    @Query("""
            select s from StudentProfile s
            join fetch s.user u
            where :keyword is null
               or :keyword = ''
               or s.studentNo like concat('%', :keyword, '%')
               or s.className like concat('%', :keyword, '%')
               or s.grade like concat('%', :keyword, '%')
               or s.college like concat('%', :keyword, '%')
               or s.major like concat('%', :keyword, '%')
               or u.realName like concat('%', :keyword, '%')
            order by s.id desc
            """)
    Page<StudentProfile> search(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
            select s from StudentProfile s
            join fetch s.user u
            where :keyword is null
               or :keyword = ''
               or s.studentNo like concat('%', :keyword, '%')
               or s.className like concat('%', :keyword, '%')
               or s.grade like concat('%', :keyword, '%')
               or s.college like concat('%', :keyword, '%')
               or s.major like concat('%', :keyword, '%')
               or u.realName like concat('%', :keyword, '%')
            order by s.id desc
            """)
    List<StudentProfile> searchForExport(@Param("keyword") String keyword);

    @Query("""
            select distinct s.college, s.major, s.className
            from StudentProfile s
            where s.college is not null
              and s.college <> ''
              and s.major is not null
              and s.major <> ''
              and s.className is not null
              and s.className <> ''
            order by s.college, s.major, s.className
            """)
    List<Object[]> findDistinctCollegeMajorClassTriples();
}
