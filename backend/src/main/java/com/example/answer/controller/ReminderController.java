package com.example.answer.controller;

import com.example.answer.common.ApiResponse;
import com.example.answer.dto.ReminderDto;
import com.example.answer.service.ReminderService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reminders")
public class ReminderController {

    private final ReminderService reminderService;

    public ReminderController(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<List<ReminderDto>> myReminders() {
        return ApiResponse.ok(reminderService.myReminders());
    }

    @PutMapping("/{id}/read")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<ReminderDto> markRead(@PathVariable Long id) {
        return ApiResponse.ok(reminderService.markRead(id));
    }

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<Void> generate() {
        reminderService.generateUpcomingReminders();
        return ApiResponse.ok();
    }
}
