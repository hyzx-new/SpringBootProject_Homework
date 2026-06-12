package com.example.answer.controller;

import com.example.answer.common.ApiResponse;
import com.example.answer.dto.PasswordChangeRequest;
import com.example.answer.dto.ProfileDto;
import com.example.answer.dto.ProfileUpdateRequest;
import com.example.answer.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/me")
    public ApiResponse<ProfileDto> me() {
        return ApiResponse.ok(profileService.me());
    }

    @PutMapping("/me")
    public ApiResponse<ProfileDto> update(
            @Valid @RequestBody ProfileUpdateRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.ok(profileService.update(request, authorization));
    }

    @PutMapping("/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody PasswordChangeRequest request) {
        profileService.changePassword(request);
        return ApiResponse.ok();
    }
}
