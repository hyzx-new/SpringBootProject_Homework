package com.example.answer.service;

import com.example.answer.auth.AuthContext;
import com.example.answer.common.BusinessException;
import com.example.answer.common.PageUtils;
import com.example.answer.dto.PageResult;
import com.example.answer.dto.UserDto;
import com.example.answer.dto.UserRequest;
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
public class UserService {

    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            StudentProfileRepository studentProfileRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public PageResult<UserDto> list(int page, int size) {
        return PageResult.from(userRepository.findAll(PageUtils.of(page, size, Sort.by(Sort.Direction.DESC, "id")))
                .map(this::toDto));
    }

    @Transactional
    public UserDto create(UserRequest request) {
        if (request.role() == Role.STUDENT) {
            throw new BusinessException("学生账号请在学生管理中创建");
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessException("用户名已存在");
        }
        UserAccount user = new UserAccount();
        user.setUsername(request.username().trim());
        user.setPasswordHash(passwordEncoder.encode(StringUtils.hasText(request.password()) ? request.password() : "123456"));
        user.setRealName(request.realName().trim());
        user.setRole(request.role());
        user.setEnabled(request.enabled() == null || request.enabled());
        user.setMajor(resolveMajor(request));
        return toDto(userRepository.save(user));
    }

    @Transactional
    public UserDto update(Long id, UserRequest request) {
        UserAccount user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("账号不存在"));
        if (user.getId().equals(AuthContext.currentUser().id()) && request.enabled() != null && !request.enabled()) {
            throw new BusinessException("不能禁用当前登录账号");
        }
        userRepository.findByUsername(request.username())
                .filter(existing -> !existing.getId().equals(user.getId()))
                .ifPresent(existing -> {
                    throw new BusinessException("用户名已存在");
                });
        if (request.role() == Role.STUDENT) {
            throw new BusinessException("学生账号请在学生管理中维护");
        }
        user.setUsername(request.username().trim());
        user.setRealName(request.realName().trim());
        user.setRole(request.role());
        user.setEnabled(request.enabled() == null || request.enabled());
        user.setMajor(resolveMajor(request));
        if (StringUtils.hasText(request.password())) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        return toDto(user);
    }

    @Transactional
    public void delete(Long id) {
        if (id.equals(AuthContext.currentUser().id())) {
            throw new BusinessException("不能删除当前登录账号");
        }
        userRepository.deleteById(id);
    }

    private UserDto toDto(UserAccount user) {
        String major = user.getRole() == Role.STUDENT
                ? studentProfileRepository.findByUserId(user.getId())
                        .map(StudentProfile::getMajor)
                        .orElse(user.getMajor())
                : user.getMajor();
        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getRealName(),
                user.getRole().name(),
                user.getEnabled(),
                user.getAvatarUrl(),
                major
        );
    }

    private String resolveMajor(UserRequest request) {
        if (request.role() == Role.TEACHER && !StringUtils.hasText(request.major())) {
            throw new BusinessException("教师账号必须设置专业");
        }
        return StringUtils.hasText(request.major()) ? request.major().trim() : null;
    }
}
