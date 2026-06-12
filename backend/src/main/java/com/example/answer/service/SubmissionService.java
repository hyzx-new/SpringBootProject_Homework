package com.example.answer.service;

import com.example.answer.auth.AuthContext;
import com.example.answer.common.BusinessException;
import com.example.answer.common.PageUtils;
import com.example.answer.dto.AnswerItemRequest;
import com.example.answer.dto.AnswerResultDto;
import com.example.answer.dto.AnswerScoreRequest;
import com.example.answer.dto.AuthUser;
import com.example.answer.dto.PageResult;
import com.example.answer.dto.SubmissionDto;
import com.example.answer.dto.SubmitPaperRequest;
import com.example.answer.entity.Paper;
import com.example.answer.entity.Question;
import com.example.answer.entity.QuestionType;
import com.example.answer.entity.StudentAnswer;
import com.example.answer.entity.StudentProfile;
import com.example.answer.entity.Submission;
import com.example.answer.repository.PaperRepository;
import com.example.answer.repository.QuestionRepository;
import com.example.answer.repository.StudentAnswerRepository;
import com.example.answer.repository.StudentProfileRepository;
import com.example.answer.repository.SubmissionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final StudentAnswerRepository studentAnswerRepository;
    private final PaperRepository paperRepository;
    private final QuestionRepository questionRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final PaperAccessService paperAccessService;

    public SubmissionService(
            SubmissionRepository submissionRepository,
            StudentAnswerRepository studentAnswerRepository,
            PaperRepository paperRepository,
            QuestionRepository questionRepository,
            StudentProfileRepository studentProfileRepository,
            PaperAccessService paperAccessService
    ) {
        this.submissionRepository = submissionRepository;
        this.studentAnswerRepository = studentAnswerRepository;
        this.paperRepository = paperRepository;
        this.questionRepository = questionRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.paperAccessService = paperAccessService;
    }

    @Transactional
    public SubmissionDto submit(Long paperId, SubmitPaperRequest request) {
        AuthUser user = AuthContext.currentUser();
        if (!"STUDENT".equals(user.role())) {
            throw new BusinessException("只有学生可以提交答案");
        }
        StudentProfile student = currentStudent();
        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new BusinessException("试卷不存在"));
        paperAccessService.requireCurrentUserAccess(paper);
        LocalDateTime now = LocalDateTime.now();
        if (Boolean.FALSE.equals(paper.getPublished())) {
            throw new BusinessException("试卷尚未发布");
        }
        if (now.isBefore(paper.getStartTime())) {
            throw new BusinessException("试卷尚未开始");
        }
        if (now.isAfter(paper.getEndTime())) {
            throw new BusinessException("试卷已结束");
        }

        List<Question> questions = questionRepository.findByPaperIdOrderByOrderNoAsc(paperId);
        if (questions.isEmpty()) {
            throw new BusinessException("试卷还没有题目");
        }
        Map<Long, String> answers = request.answers().stream()
                .collect(Collectors.toMap(AnswerItemRequest::questionId, AnswerItemRequest::answer, (left, right) -> right));

        Submission submission = new Submission();
        submission.setPaper(paper);
        submission.setStudent(student);
        submission.setScore(BigDecimal.ZERO);
        submission.setTotalScore(BigDecimal.ZERO);
        submissionRepository.save(submission);

        BigDecimal totalScore = BigDecimal.ZERO;
        BigDecimal actualScore = BigDecimal.ZERO;
        List<StudentAnswer> answerRows = new ArrayList<>();

        for (Question question : questions) {
            String answer = answers.getOrDefault(question.getId(), "");
            boolean correct = isCorrect(question, answer);
            BigDecimal answerScore = correct ? question.getScore() : BigDecimal.ZERO;
            totalScore = totalScore.add(question.getScore());
            actualScore = actualScore.add(answerScore);

            StudentAnswer row = new StudentAnswer();
            row.setSubmission(submission);
            row.setQuestion(question);
            row.setAnswerText(answer);
            row.setCorrectAnswer(question.getCorrectAnswer());
            row.setCorrectFlag(correct);
            row.setScore(answerScore);
            answerRows.add(row);
        }

        submission.setScore(actualScore);
        submission.setTotalScore(totalScore);
        studentAnswerRepository.saveAll(answerRows);
        return toDto(submission, answerRows);
    }

    @Transactional(readOnly = true)
    public PageResult<SubmissionDto> list(Long paperId, int page, int size) {
        AuthUser user = AuthContext.currentUser();
        Page<Submission> submissions;
        if ("STUDENT".equals(user.role())) {
            StudentProfile student = currentStudent();
            submissions = submissionRepository.findByStudentIdAndAccessiblePaperOrderBySubmittedAtDesc(
                    student.getId(),
                    paperAccessService.normalizeMajor(student.getMajor()),
                    PageUtils.of(page, size, Sort.unsorted())
            );
        } else if (paperId != null) {
            Paper paper = findPaper(paperId);
            paperAccessService.requireCurrentUserAccess(paper);
            submissions = submissionRepository.findByPaperIdOrderBySubmittedAtDesc(
                    paperId,
                    PageUtils.of(page, size, Sort.unsorted())
            );
        } else if ("TEACHER".equals(user.role())) {
            submissions = submissionRepository.findAccessibleByMajorOrderBySubmittedAtDesc(
                    paperAccessService.currentUserMajor(),
                    PageUtils.of(page, size, Sort.unsorted())
            );
        } else {
            submissions = submissionRepository.findAllByOrderBySubmittedAtDesc(PageUtils.of(page, size, Sort.unsorted()));
        }
        return PageResult.from(submissions.map(submission -> toDto(submission, List.of())));
    }

    @Transactional(readOnly = true)
    public SubmissionDto get(Long id) {
        Submission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("提交记录不存在"));
        AuthUser user = AuthContext.currentUser();
        if ("STUDENT".equals(user.role()) && !submission.getStudent().getId().equals(currentStudent().getId())) {
            throw new BusinessException("不能查看其他学生的成绩");
        }
        paperAccessService.requireCurrentUserAccess(submission.getPaper());
        return toDto(submission, studentAnswerRepository.findBySubmissionId(id));
    }

    @Transactional
    public SubmissionDto updateAnswerScore(Long submissionId, Long answerId, AnswerScoreRequest request) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new BusinessException("提交记录不存在"));
        StudentAnswer answer = studentAnswerRepository.findById(answerId)
                .orElseThrow(() -> new BusinessException("答题记录不存在"));
        if (!answer.getSubmission().getId().equals(submissionId)) {
            throw new BusinessException("答题记录不属于该提交记录");
        }
        paperAccessService.requireCurrentUserAccess(submission.getPaper());
        BigDecimal fullScore = answer.getQuestion().getScore();
        if (request.score().compareTo(BigDecimal.ZERO) < 0 || request.score().compareTo(fullScore) > 0) {
            throw new BusinessException("本题得分必须在 0 到满分之间");
        }
        answer.setScore(request.score());
        answer.setCorrectFlag(request.correct() == null ? request.score().compareTo(fullScore) == 0 : request.correct());

        List<StudentAnswer> answers = studentAnswerRepository.findBySubmissionId(submissionId);
        BigDecimal totalScore = answers.stream()
                .map(StudentAnswer::getScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        submission.setScore(totalScore);
        return toDto(submission, answers);
    }

    @Transactional(readOnly = true)
    public List<Submission> exportRows(Long paperId, String className) {
        List<Submission> rows;
        if (paperId != null) {
            Paper paper = findPaper(paperId);
            paperAccessService.requireCurrentUserAccess(paper);
            rows = submissionRepository.findByPaperIdOrderBySubmittedAtDesc(paperId);
        } else if ("TEACHER".equals(AuthContext.currentUser().role())) {
            rows = submissionRepository.findAccessibleByMajorOrderBySubmittedAtDesc(paperAccessService.currentUserMajor());
        } else {
            rows = submissionRepository.findAllByOrderBySubmittedAtDesc();
        }
        if (!StringUtils.hasText(className)) {
            return rows;
        }
        String normalizedClassName = className.trim();
        return rows.stream()
                .filter(submission -> normalizedClassName.equals(submission.getStudent().getClassName()))
                .toList();
    }

    private Paper findPaper(Long paperId) {
        return paperRepository.findById(paperId)
                .orElseThrow(() -> new BusinessException("试卷不存在"));
    }

    private StudentProfile currentStudent() {
        AuthUser user = AuthContext.currentUser();
        if (user.studentId() != null) {
            return studentProfileRepository.findById(user.studentId())
                    .orElseThrow(() -> new BusinessException("学生档案不存在"));
        }
        return studentProfileRepository.findByUserId(user.id())
                .orElseThrow(() -> new BusinessException("学生档案不存在"));
    }

    private SubmissionDto toDto(Submission submission, List<StudentAnswer> answers) {
        Paper paper = submission.getPaper();
        StudentProfile student = submission.getStudent();
        return new SubmissionDto(
                submission.getId(),
                paper.getId(),
                paper.getTitle(),
                student.getId(),
                student.getStudentNo(),
                student.getUser().getRealName(),
                student.getClassName(),
                student.getGrade(),
                student.getCollege(),
                submission.getScore(),
                submission.getTotalScore(),
                submission.getSubmittedAt(),
                answers.stream().map(this::toAnswerDto).toList()
        );
    }

    private AnswerResultDto toAnswerDto(StudentAnswer answer) {
        return new AnswerResultDto(
                answer.getId(),
                answer.getQuestion().getId(),
                answer.getQuestion().getType(),
                answer.getQuestion().getTitle(),
                answer.getAnswerText(),
                answer.getCorrectAnswer(),
                Boolean.TRUE.equals(answer.getCorrectFlag()),
                answer.getScore(),
                answer.getQuestion().getScore()
        );
    }

    private boolean isCorrect(Question question, String answer) {
        if (question.getType() == QuestionType.SHORT_ANSWER) {
            return false;
        }
        if (question.getType() == QuestionType.MULTIPLE_CHOICE) {
            return normalizeMultiple(answer).equals(normalizeMultiple(question.getCorrectAnswer()));
        }
        return normalize(answer).equals(normalize(question.getCorrectAnswer()));
    }

    private Set<String> normalizeMultiple(String value) {
        if (value == null) {
            return Set.of();
        }
        return Arrays.stream(value.trim().split("[,，;；\\s]+"))
                .map(item -> item.trim().replaceAll("[.。、]$", "").toUpperCase(Locale.ROOT))
                .filter(item -> !item.isBlank())
                .collect(Collectors.toSet());
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }
}
