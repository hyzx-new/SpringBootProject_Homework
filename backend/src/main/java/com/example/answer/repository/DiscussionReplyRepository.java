package com.example.answer.repository;

import com.example.answer.entity.DiscussionReply;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DiscussionReplyRepository extends JpaRepository<DiscussionReply, Long> {

    List<DiscussionReply> findByPostIdOrderByCreatedAtAsc(Long postId);

    long countByPostId(Long postId);

    void deleteByPostId(Long postId);
}
