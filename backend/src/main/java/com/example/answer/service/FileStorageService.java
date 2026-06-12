package com.example.answer.service;

import com.example.answer.auth.AuthContext;
import com.example.answer.common.BusinessException;
import com.example.answer.common.PageUtils;
import com.example.answer.dto.AuthUser;
import com.example.answer.dto.PageResult;
import com.example.answer.dto.UploadedFileDto;
import com.example.answer.entity.UploadedFile;
import com.example.answer.entity.UserAccount;
import com.example.answer.repository.UploadedFileRepository;
import com.example.answer.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private final UploadedFileRepository uploadedFileRepository;
    private final UserRepository userRepository;
    private final Path uploadRoot;

    public FileStorageService(
            UploadedFileRepository uploadedFileRepository,
            UserRepository userRepository,
            @Value("${app.upload.dir:uploads}") String uploadDir
    ) {
        this.uploadedFileRepository = uploadedFileRepository;
        this.userRepository = userRepository;
        this.uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    @Transactional(readOnly = true)
    public PageResult<UploadedFileDto> list(int page, int size) {
        return PageResult.from(uploadedFileRepository.findAllByOrderByCreatedAtDesc(
                PageUtils.of(page, size, Sort.unsorted())
        ).map(this::toDto));
    }

    @Transactional
    public UploadedFileDto upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的文件");
        }
        AuthUser current = AuthContext.currentUser();
        UserAccount uploader = userRepository.findById(current.id())
                .orElseThrow(() -> new BusinessException("上传用户不存在"));

        String originalName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "upload.bin" : file.getOriginalFilename());
        if (originalName.contains("..")) {
            throw new BusinessException("文件名不合法");
        }
        String extension = "";
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex >= 0) {
            extension = originalName.substring(dotIndex);
        }
        String storedName = UUID.randomUUID().toString().replace("-", "") + extension;
        Path target = uploadRoot.resolve(storedName).normalize();
        if (!target.startsWith(uploadRoot)) {
            throw new BusinessException("文件路径不合法");
        }

        try {
            Files.createDirectories(uploadRoot);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new BusinessException("文件保存失败：" + ex.getMessage());
        }

        UploadedFile uploadedFile = new UploadedFile();
        uploadedFile.setOriginalName(originalName);
        uploadedFile.setStoredName(storedName);
        uploadedFile.setContentType(file.getContentType());
        uploadedFile.setFileSize(file.getSize());
        uploadedFile.setStoragePath(target.toString());
        uploadedFile.setUploader(uploader);
        return toDto(uploadedFileRepository.save(uploadedFile));
    }

    @Transactional(readOnly = true)
    public UploadedFile getFile(Long id) {
        return uploadedFileRepository.findById(id)
                .orElseThrow(() -> new BusinessException("文件不存在"));
    }

    public Resource loadAsResource(UploadedFile uploadedFile) {
        Path path = Path.of(uploadedFile.getStoragePath()).toAbsolutePath().normalize();
        if (!path.startsWith(uploadRoot)) {
            throw new BusinessException("文件路径不合法");
        }
        try {
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new BusinessException("文件不存在或不可读");
            }
            return resource;
        } catch (MalformedURLException ex) {
            throw new BusinessException("文件读取失败");
        }
    }

    @Transactional
    public void delete(Long id) {
        UploadedFile uploadedFile = uploadedFileRepository.findById(id)
                .orElseThrow(() -> new BusinessException("文件不存在"));
        if (!canDelete(uploadedFile)) {
            throw new BusinessException("只能删除自己上传的文件");
        }
        uploadedFileRepository.delete(uploadedFile);
        try {
            Files.deleteIfExists(Path.of(uploadedFile.getStoragePath()).toAbsolutePath().normalize());
        } catch (IOException ex) {
            throw new BusinessException("文件删除失败：" + ex.getMessage());
        }
    }

    private UploadedFileDto toDto(UploadedFile uploadedFile) {
        UserAccount uploader = uploadedFile.getUploader();
        return new UploadedFileDto(
                uploadedFile.getId(),
                uploadedFile.getOriginalName(),
                uploadedFile.getContentType(),
                uploadedFile.getFileSize(),
                uploader == null ? null : uploader.getId(),
                uploader == null ? "未知用户" : uploader.getRealName(),
                uploadedFile.getCreatedAt(),
                "/api/files/" + uploadedFile.getId() + "/download",
                canDelete(uploadedFile)
        );
    }

    private boolean canDelete(UploadedFile uploadedFile) {
        AuthUser current = AuthContext.currentUser();
        return "ADMIN".equals(current.role())
                || uploadedFile.getUploader() != null && uploadedFile.getUploader().getId().equals(current.id());
    }
}
