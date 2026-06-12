package com.example.answer.service;

import com.example.answer.auth.AuthContext;
import com.example.answer.auth.TokenAuthenticationFilter;
import com.example.answer.common.BusinessException;
import com.example.answer.dto.AuthUser;
import com.example.answer.dto.PasswordChangeRequest;
import com.example.answer.dto.ProfileDto;
import com.example.answer.dto.ProfileUpdateRequest;
import com.example.answer.entity.StudentProfile;
import com.example.answer.entity.UserAccount;
import com.example.answer.repository.StudentProfileRepository;
import com.example.answer.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;

@Service
public class ProfileService {

    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final long tokenHours;

    public ProfileService(
            UserRepository userRepository,
            StudentProfileRepository studentProfileRepository,
            PasswordEncoder passwordEncoder,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${app.auth.token-hours}") long tokenHours
    ) {
        this.userRepository = userRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.tokenHours = tokenHours;
    }

    @Transactional(readOnly = true)
    public ProfileDto me() {
        UserAccount user = currentUserAccount();
        return toDto(user);
    }

    @Transactional
    public ProfileDto update(ProfileUpdateRequest request, String authorizationHeader) {
        UserAccount user = currentUserAccount();
        user.setRealName(request.realName().trim());
        user.setAvatarUrl(StringUtils.hasText(request.avatarUrl()) ? request.avatarUrl().trim() : null);
        refreshSession(authorizationHeader, user);
        return toDto(user);
    }

    @Transactional
    public void changePassword(PasswordChangeRequest request) {
        if (request.newPassword().length() < 6) {
            throw new BusinessException("新密码长度至少 6 位");
        }
        UserAccount user = currentUserAccount();
        if (!passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())) {
            throw new BusinessException("原密码错误");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    }

    private UserAccount currentUserAccount() {
        AuthUser current = AuthContext.currentUser();
        return userRepository.findById(current.id())
                .orElseThrow(() -> new BusinessException("当前账号不存在"));
    }

    private ProfileDto toDto(UserAccount user) {
        StudentProfile student = studentProfileRepository.findByUserId(user.getId()).orElse(null);
        return new ProfileDto(
                user.getId(),
                user.getUsername(),
                user.getRealName(),
                user.getRole().name(),
                user.getAvatarUrl(),
                student == null ? null : student.getId(),
                student == null ? null : student.getStudentNo(),
                student == null ? null : student.getClassName(),
                student == null ? null : student.getGrade(),
                student == null ? null : student.getCollege(),
                student == null ? user.getMajor() : student.getMajor()
        );
    }

    private void refreshSession(String authorizationHeader, UserAccount user) {
        if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith("Bearer ")) {
            return;
        }
        Long studentId = studentProfileRepository.findByUserId(user.getId())
                .map(StudentProfile::getId)
                .orElse(null);
        String major = studentProfileRepository.findByUserId(user.getId())
                .map(StudentProfile::getMajor)
                .orElse(user.getMajor());
        AuthUser authUser = new AuthUser(
                user.getId(),
                user.getUsername(),
                user.getRealName(),
                user.getRole().name(),
                studentId,
                user.getAvatarUrl(),
                major
        );
        String token = authorizationHeader.substring(7);
        try {
            redisTemplate.opsForValue().set(
                    TokenAuthenticationFilter.TOKEN_PREFIX + token,
                    objectMapper.writeValueAsString(authUser),
                    Duration.ofHours(tokenHours)
            );
        } catch (JsonProcessingException ex) {
            throw new BusinessException("登录状态刷新失败");
        }
    }
}
