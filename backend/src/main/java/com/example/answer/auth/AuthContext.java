package com.example.answer.auth;

import com.example.answer.common.BusinessException;
import com.example.answer.dto.AuthUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class AuthContext {

    private AuthContext() {
    }

    public static AuthUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthUser authUser)) {
            throw new BusinessException("请先登录");
        }
        return authUser;
    }

    public static boolean isAdminOrTeacher() {
        AuthUser user = currentUser();
        return "ADMIN".equals(user.role()) || "TEACHER".equals(user.role());
    }
}
