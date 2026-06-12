package com.example.answer.service;

import com.example.answer.auth.AuthContext;
import com.example.answer.common.BusinessException;
import com.example.answer.dto.ReminderDto;
import com.example.answer.entity.ExamReminder;
import com.example.answer.entity.Paper;
import com.example.answer.entity.StudentProfile;
import com.example.answer.repository.ExamReminderRepository;
import com.example.answer.repository.PaperRepository;
import com.example.answer.repository.StudentProfileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ReminderService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ExamReminderRepository reminderRepository;
    private final PaperRepository paperRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final PaperAccessService paperAccessService;
    private final int daysBefore;

    public ReminderService(
            ExamReminderRepository reminderRepository,
            PaperRepository paperRepository,
            StudentProfileRepository studentProfileRepository,
            PaperAccessService paperAccessService,
            @Value("${app.reminder.days-before}") int daysBefore
    ) {
        this.reminderRepository = reminderRepository;
        this.paperRepository = paperRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.paperAccessService = paperAccessService;
        this.daysBefore = daysBefore;
    }

    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void generateUpcomingReminders() {
        LocalDate targetDate = LocalDate.now().plusDays(daysBefore);
        LocalDateTime start = targetDate.atStartOfDay();
        LocalDateTime end = targetDate.plusDays(1).atStartOfDay();
        List<Paper> papers = paperRepository.findByStartTimeBetween(start, end).stream()
                .filter(paper -> Boolean.TRUE.equals(paper.getPublished()))
                .toList();
        if (papers.isEmpty()) {
            return;
        }
        List<StudentProfile> students = studentProfileRepository.findAll();
        for (Paper paper : papers) {
            for (StudentProfile student : students) {
                if (!paperAccessService.isAvailableToMajor(paper, student.getMajor())) {
                    continue;
                }
                if (reminderRepository.existsByPaperIdAndStudentId(paper.getId(), student.getId())) {
                    continue;
                }
                ExamReminder reminder = new ExamReminder();
                reminder.setPaper(paper);
                reminder.setStudent(student);
                reminder.setRemindAt(LocalDateTime.now());
                reminder.setMessage("试卷《" + paper.getTitle() + "》将在 "
                        + paper.getStartTime().format(FORMATTER) + " 开始，请提前准备。");
                reminderRepository.save(reminder);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<ReminderDto> myReminders() {
        StudentProfile student = currentStudent();
        return reminderRepository.findByStudentIdOrderByRemindAtDesc(student.getId()).stream()
                .filter(reminder -> paperAccessService.isAvailableToMajor(reminder.getPaper(), student.getMajor()))
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public ReminderDto markRead(Long id) {
        StudentProfile student = currentStudent();
        ExamReminder reminder = reminderRepository.findById(id)
                .orElseThrow(() -> new BusinessException("提醒不存在"));
        if (!reminder.getStudent().getId().equals(student.getId())) {
            throw new BusinessException("不能操作其他学生的提醒");
        }
        reminder.setReadFlag(true);
        return toDto(reminder);
    }

    private StudentProfile currentStudent() {
        if (!"STUDENT".equals(AuthContext.currentUser().role())) {
            throw new BusinessException("只有学生可以查看个人提醒");
        }
        Long studentId = AuthContext.currentUser().studentId();
        if (studentId != null) {
            return studentProfileRepository.findById(studentId)
                    .orElseThrow(() -> new BusinessException("学生档案不存在"));
        }
        return studentProfileRepository.findByUserId(AuthContext.currentUser().id())
                .orElseThrow(() -> new BusinessException("学生档案不存在"));
    }

    private ReminderDto toDto(ExamReminder reminder) {
        return new ReminderDto(
                reminder.getId(),
                reminder.getPaper().getId(),
                reminder.getPaper().getTitle(),
                reminder.getMessage(),
                reminder.getRemindAt(),
                reminder.getReadFlag()
        );
    }
}
