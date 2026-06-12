package com.example.answer.service;

import com.example.answer.auth.AuthContext;
import com.example.answer.auth.TokenAuthenticationFilter;
import com.example.answer.common.BusinessException;
import com.example.answer.dto.AuthUser;
import com.example.answer.dto.LoginRequest;
import com.example.answer.dto.LoginResponse;
import com.example.answer.dto.RegisterRequest;
import com.example.answer.entity.Role;
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
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final long tokenHours;

    public AuthService(
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

    @Transactional
    public LoginResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessException("用户名已存在");
        }
        if (request.role() != null && request.role() != Role.TEACHER) {
            throw new BusinessException("学生账号只能由管理员添加，公开注册仅支持教师账号");
        }
        if (!StringUtils.hasText(request.major())) {
            throw new BusinessException("教师账号必须设置专业");
        }

        UserAccount user = new UserAccount();
        user.setUsername(request.username().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRealName(request.realName().trim());
        user.setRole(Role.TEACHER);
        user.setEnabled(true);
        user.setMajor(request.major().trim());
        userRepository.save(user);

        return createSession(user, null);
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        UserAccount user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BusinessException("用户名或密码错误"));
        if (Boolean.FALSE.equals(user.getEnabled()) || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException("用户名或密码错误");
        }
        Long studentId = null;
        if (user.getRole() == Role.STUDENT) {
            studentId = studentProfileRepository.findByUserId(user.getId())
                    .map(StudentProfile::getId)
                    .orElse(null);
        }
        return createSession(user, studentId);
    }

    public AuthUser me() {
        return AuthContext.currentUser();
    }

    public void logout(String authorizationHeader) {
        if (StringUtils.hasText(authorizationHeader) && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7);
            redisTemplate.delete(TokenAuthenticationFilter.TOKEN_PREFIX + token);
        }
    }

    private LoginResponse createSession(UserAccount user, Long studentId) {
        String major = user.getRole() == Role.STUDENT
                ? studentProfileRepository.findByUserId(user.getId())
                        .map(StudentProfile::getMajor)
                        .orElse(null)
                : user.getMajor();
        AuthUser authUser = new AuthUser(
                user.getId(),
                user.getUsername(),
                user.getRealName(),
                user.getRole().name(),
                studentId,
                user.getAvatarUrl(),
                major
        );
        String token = UUID.randomUUID().toString().replace("-", "");
        try {
            redisTemplate.opsForValue().set(
                    TokenAuthenticationFilter.TOKEN_PREFIX + token,
                    objectMapper.writeValueAsString(authUser),
                    Duration.ofHours(tokenHours)
            );
        } catch (JsonProcessingException ex) {
            throw new BusinessException("登录状态创建失败");
        }
        return new LoginResponse(token, authUser);
    }
}
