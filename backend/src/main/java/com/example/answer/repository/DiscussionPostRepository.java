package com.example.answer.repository;

import com.example.answer.entity.DiscussionPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DiscussionPostRepository extends JpaRepository<DiscussionPost, Long> {

    @Query(
            value = """
                    select p from DiscussionPost p
                    left join fetch p.author
                    left join fetch p.paper paper
                    where (:paperId is null or paper.id = :paperId)
                      and (:keyword is null
                           or :keyword = ''
                           or p.title like concat('%', :keyword, '%')
                           or p.content like concat('%', :keyword, '%'))
                    order by p.updatedAt desc
                    """,
            countQuery = """
                    select count(p) from DiscussionPost p
                    left join p.paper paper
                    where (:paperId is null or paper.id = :paperId)
                      and (:keyword is null
                           or :keyword = ''
                           or p.title like concat('%', :keyword, '%')
                           or p.content like concat('%', :keyword, '%'))
                    """
    )
    Page<DiscussionPost> search(@Param("keyword") String keyword, @Param("paperId") Long paperId, Pageable pageable);

    @Query(
            value = """
                    select p from DiscussionPost p
                    left join fetch p.author
                    left join fetch p.paper paper
                    where (:paperId is null or paper.id = :paperId)
                      and (paper.id is null
                           or paper.commonCourse = true
                           or paper.major = :major)
                      and (:keyword is null
                           or :keyword = ''
                           or p.title like concat('%', :keyword, '%')
                           or p.content like concat('%', :keyword, '%'))
                    order by p.updatedAt desc
                    """,
            countQuery = """
                    select count(p) from DiscussionPost p
                    left join p.paper paper
                    where (:paperId is null or paper.id = :paperId)
                      and (paper.id is null
                           or paper.commonCourse = true
                           or paper.major = :major)
                      and (:keyword is null
                           or :keyword = ''
                           or p.title like concat('%', :keyword, '%')
                           or p.content like concat('%', :keyword, '%'))
                    """
    )
    Page<DiscussionPost> searchAccessible(
            @Param("keyword") String keyword,
            @Param("paperId") Long paperId,
            @Param("major") String major,
            Pageable pageable
    );
}
