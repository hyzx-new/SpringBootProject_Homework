package com.example.answer.service;

import com.example.answer.auth.AuthContext;
import com.example.answer.common.BusinessException;
import com.example.answer.dto.AuthUser;
import com.example.answer.entity.Paper;
import com.example.answer.entity.StudentProfile;
import com.example.answer.entity.UserAccount;
import com.example.answer.repository.StudentProfileRepository;
import com.example.answer.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PaperAccessService {

    private final StudentProfileRepository studentProfileRepository;
    private final UserRepository userRepository;

    public PaperAccessService(StudentProfileRepository studentProfileRepository, UserRepository userRepository) {
        this.studentProfileRepository = studentProfileRepository;
        this.userRepository = userRepository;
    }

    public boolean canCurrentUserAccess(Paper paper) {
        AuthUser user = AuthContext.currentUser();
        if ("ADMIN".equals(user.role())) {
            return true;
        }
        return isAvailableToMajor(paper, currentUserMajor(user));
    }

    public void requireCurrentUserAccess(Paper paper) {
        if (!canCurrentUserAccess(paper)) {
            throw new BusinessException("无权查看非本专业试卷");
        }
    }

    public String currentUserMajor() {
        return currentUserMajor(AuthContext.currentUser());
    }

    public boolean isAvailableToMajor(Paper paper, String major) {
        if (Boolean.TRUE.equals(paper.getCommonCourse())) {
            return true;
        }
        String paperMajor = normalizeMajor(paper.getMajor());
        String userMajor = normalizeMajor(major);
        return paperMajor != null && paperMajor.equals(userMajor);
    }

    public String normalizeMajor(String major) {
        return StringUtils.hasText(major) ? major.trim() : null;
    }

    private String currentUserMajor(AuthUser user) {
        if ("STUDENT".equals(user.role())) {
            StudentProfile student = user.studentId() == null
                    ? studentProfileRepository.findByUserId(user.id())
                            .orElseThrow(() -> new BusinessException("学生档案不存在"))
                    : studentProfileRepository.findById(user.studentId())
                            .orElseThrow(() -> new BusinessException("学生档案不存在"));
            return normalizeMajor(student.getMajor());
        }

        UserAccount account = userRepository.findById(user.id())
                .orElseThrow(() -> new BusinessException("当前账号不存在"));
        return normalizeMajor(account.getMajor());
    }
}
