package com.example.answer.service;

import com.example.answer.common.BusinessException;
import com.example.answer.common.PageUtils;
import com.example.answer.dto.PageResult;
import com.example.answer.dto.StudentDto;
import com.example.answer.dto.StudentRequest;
import com.example.answer.entity.Role;
import com.example.answer.entity.StudentProfile;
import com.example.answer.entity.UserAccount;
import com.example.answer.repository.StudentProfileRepository;
import com.example.answer.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class StudentService {

    private static final String STUDENT_NO_PREFIX = "205244";
    private static final int STUDENT_NO_BASE_SUFFIX = 3662;

    private final StudentProfileRepository studentProfileRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public StudentService(
            StudentProfileRepository studentProfileRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.studentProfileRepository = studentProfileRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public PageResult<StudentDto> list(String keyword, int page, int size) {
        return PageResult.from(studentProfileRepository.search(
                keyword,
                PageUtils.of(page, size, Sort.by(Sort.Direction.DESC, "id"))
        ).map(this::toDto));
    }

    @Transactional(readOnly = true)
    public StudentDto get(Long id) {
        return studentProfileRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new BusinessException("学生不存在"));
    }

    @Transactional
    public StudentDto create(StudentRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessException("用户名已存在");
        }
        String studentNo = resolveStudentNo(request.studentNo(), null);
        if (studentProfileRepository.existsByStudentNo(studentNo)) {
            throw new BusinessException("学号已存在");
        }

        UserAccount user = new UserAccount();
        user.setUsername(request.username().trim());
        user.setPasswordHash(passwordEncoder.encode(StringUtils.hasText(request.password()) ? request.password() : "123456"));
        user.setRealName(request.realName().trim());
        user.setRole(Role.STUDENT);
        user.setEnabled(request.enabled() == null || request.enabled());
        user.setMajor(request.major().trim());
        userRepository.save(user);

        StudentProfile profile = new StudentProfile();
        profile.setUser(user);
        fillProfile(profile, request, studentNo);
        return toDto(studentProfileRepository.save(profile));
    }

    @Transactional
    public StudentDto update(Long id, StudentRequest request) {
        StudentProfile profile = studentProfileRepository.findById(id)
                .orElseThrow(() -> new BusinessException("学生不存在"));
        UserAccount user = profile.getUser();
        userRepository.findByUsername(request.username())
                .filter(existing -> !existing.getId().equals(user.getId()))
                .ifPresent(existing -> {
                    throw new BusinessException("用户名已存在");
                });
        String studentNo = resolveStudentNo(request.studentNo(), profile.getStudentNo());
        if (studentProfileRepository.existsByStudentNoAndIdNot(studentNo, id)) {
            throw new BusinessException("学号已存在");
        }
        user.setUsername(request.username().trim());
        user.setRealName(request.realName().trim());
        user.setEnabled(request.enabled() == null || request.enabled());
        user.setMajor(request.major().trim());
        if (StringUtils.hasText(request.password())) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        fillProfile(profile, request, studentNo);
        return toDto(profile);
    }

    @Transactional
    public void delete(Long id) {
        StudentProfile profile = studentProfileRepository.findById(id)
                .orElseThrow(() -> new BusinessException("学生不存在"));
        UserAccount user = profile.getUser();
        studentProfileRepository.delete(profile);
        userRepository.delete(user);
    }

    private void fillProfile(StudentProfile profile, StudentRequest request, String studentNo) {
        profile.setStudentNo(studentNo);
        profile.setClassName(request.className().trim());
        profile.setGrade(request.grade().trim());
        profile.setCollege(request.college().trim());
        profile.setMajor(request.major().trim());
    }

    private String resolveStudentNo(String requestedStudentNo, String existingStudentNo) {
        if (StringUtils.hasText(requestedStudentNo)) {
            String studentNo = requestedStudentNo.trim();
            if (!studentNo.matches("\\d{10}")) {
                throw new BusinessException("学号必须为 10 位数字，例如 2052443662");
            }
            return studentNo;
        }
        if (StringUtils.hasText(existingStudentNo)) {
            return existingStudentNo;
        }
        return generateStudentNo();
    }

    private String generateStudentNo() {
        return studentProfileRepository.findTopByStudentNoStartingWithOrderByStudentNoDesc(STUDENT_NO_PREFIX)
                .map(StudentProfile::getStudentNo)
                .map(lastNo -> {
                    int suffix = Integer.parseInt(lastNo.substring(STUDENT_NO_PREFIX.length()));
                    return STUDENT_NO_PREFIX + String.format("%04d", suffix + 1);
                })
                .orElse(STUDENT_NO_PREFIX + STUDENT_NO_BASE_SUFFIX);
    }

    private StudentDto toDto(StudentProfile profile) {
        UserAccount user = profile.getUser();
        return new StudentDto(
                profile.getId(),
                user.getId(),
                user.getUsername(),
                user.getRealName(),
                profile.getStudentNo(),
                profile.getClassName(),
                profile.getGrade(),
                profile.getCollege(),
                profile.getMajor(),
                user.getEnabled(),
                user.getAvatarUrl()
        );
    }
}
