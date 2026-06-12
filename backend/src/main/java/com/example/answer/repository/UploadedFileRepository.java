package com.example.answer.repository;

import com.example.answer.entity.UploadedFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UploadedFileRepository extends JpaRepository<UploadedFile, Long> {

    Page<UploadedFile> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
