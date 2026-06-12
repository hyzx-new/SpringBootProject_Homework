package com.example.answer.service;

import com.example.answer.auth.AuthContext;
import com.example.answer.common.BusinessException;
import com.example.answer.common.PageUtils;
import com.example.answer.dto.AuthUser;
import com.example.answer.dto.PageResult;
import com.example.answer.dto.PaperDto;
import com.example.answer.dto.PaperRequest;
import com.example.answer.dto.QuestionDto;
import com.example.answer.dto.QuestionRequest;
import com.example.answer.entity.Paper;
import com.example.answer.entity.Question;
import com.example.answer.entity.UserAccount;
import com.example.answer.repository.PaperRepository;
import com.example.answer.repository.QuestionRepository;
import com.example.answer.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class PaperService {

    private final PaperRepository paperRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final PaperAccessService paperAccessService;

    public PaperService(
            PaperRepository paperRepository,
            QuestionRepository questionRepository,
            UserRepository userRepository,
            PaperAccessService paperAccessService
    ) {
        this.paperRepository = paperRepository;
        this.questionRepository = questionRepository;
        this.userRepository = userRepository;
        this.paperAccessService = paperAccessService;
    }

    @Transactional(readOnly = true)
    public PageResult<PaperDto> list(int page, int size) {
        AuthUser user = AuthContext.currentUser();
        Page<Paper> papers;
        if ("ADMIN".equals(user.role())) {
            papers = paperRepository.findAllByOrderByStartTimeDesc(PageUtils.of(page, size, Sort.unsorted()));
        } else if ("TEACHER".equals(user.role())) {
            papers = paperRepository.findAccessibleByMajorOrderByStartTimeDesc(
                    paperAccessService.currentUserMajor(),
                    PageUtils.of(page, size, Sort.unsorted())
            );
        } else {
            papers = paperRepository.findPublishedAccessibleByMajorOrderByStartTimeAsc(
                    paperAccessService.currentUserMajor(),
                    PageUtils.of(page, size, Sort.unsorted())
            );
        }
        return PageResult.from(papers.map(this::toDto));
    }

    @Transactional(readOnly = true)
    public PaperDto get(Long id) {
        return toDto(findViewablePaper(id));
    }

    @Transactional
    public PaperDto create(PaperRequest request) {
        validateWindow(request.startTime(), request.endTime());
        Paper paper = new Paper();
        fillPaper(paper, request);
        UserAccount creator = userRepository.findById(AuthContext.currentUser().id()).orElse(null);
        paper.setCreatedBy(creator);
        return toDto(paperRepository.save(paper));
    }

    @Transactional
    public PaperDto update(Long id, PaperRequest request) {
        validateWindow(request.startTime(), request.endTime());
        Paper paper = findAccessiblePaper(id);
        fillPaper(paper, request);
        return toDto(paper);
    }

    @Transactional
    public void delete(Long id) {
        Paper paper = findAccessiblePaper(id);
        questionRepository.deleteByPaperId(id);
        paperRepository.delete(paper);
    }

    @Transactional(readOnly = true)
    public List<QuestionDto> questions(Long paperId) {
        findViewablePaper(paperId);
        boolean includeAnswer = AuthContext.isAdminOrTeacher();
        return questionRepository.findByPaperIdOrderByOrderNoAsc(paperId).stream()
                .map(question -> toQuestionDto(question, includeAnswer))
                .toList();
    }

    @Transactional
    public QuestionDto createQuestion(Long paperId, QuestionRequest request) {
        Paper paper = findAccessiblePaper(paperId);
        validateQuestion(request);
        Question question = new Question();
        question.setPaper(paper);
        fillQuestion(question, request);
        question.setOrderNo(request.orderNo() == null ? nextOrderNo(paperId) : request.orderNo());
        Question saved = questionRepository.save(question);
        recalculateTotalScore(paperId);
        return toQuestionDto(saved, true);
    }

    @Transactional
    public QuestionDto updateQuestion(Long paperId, Long questionId, QuestionRequest request) {
        findAccessiblePaper(paperId);
        validateQuestion(request);
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new BusinessException("题目不存在"));
        if (!question.getPaper().getId().equals(paperId)) {
            throw new BusinessException("题目不属于该试卷");
        }
        fillQuestion(question, request);
        if (request.orderNo() != null) {
            question.setOrderNo(request.orderNo());
        }
        recalculateTotalScore(paperId);
        return toQuestionDto(question, true);
    }

    @Transactional
    public void deleteQuestion(Long paperId, Long questionId) {
        findAccessiblePaper(paperId);
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new BusinessException("题目不存在"));
        if (!question.getPaper().getId().equals(paperId)) {
            throw new BusinessException("题目不属于该试卷");
        }
        questionRepository.delete(question);
        recalculateTotalScore(paperId);
    }

    private Paper findPaper(Long id) {
        return paperRepository.findById(id)
                .orElseThrow(() -> new BusinessException("试卷不存在"));
    }

    private Paper findViewablePaper(Long id) {
        Paper paper = findAccessiblePaper(id);
        if ("STUDENT".equals(AuthContext.currentUser().role()) && !Boolean.TRUE.equals(paper.getPublished())) {
            throw new BusinessException("试卷尚未发布");
        }
        return paper;
    }

    private Paper findAccessiblePaper(Long id) {
        Paper paper = findPaper(id);
        paperAccessService.requireCurrentUserAccess(paper);
        return paper;
    }

    private void fillPaper(Paper paper, PaperRequest request) {
        paper.setTitle(request.title().trim());
        paper.setDescription(request.description());
        paper.setStartTime(request.startTime());
        paper.setEndTime(request.endTime());
        paper.setPublished(request.published() == null || request.published());
        boolean commonCourse = Boolean.TRUE.equals(request.commonCourse());
        paper.setCommonCourse(commonCourse);
        if (commonCourse) {
            paper.setMajor(null);
            return;
        }

        String major = paperAccessService.normalizeMajor(request.major());
        AuthUser user = AuthContext.currentUser();
        if (!"ADMIN".equals(user.role())) {
            String currentMajor = paperAccessService.currentUserMajor();
            if (major == null) {
                major = currentMajor;
            }
            if (currentMajor == null) {
                throw new BusinessException("当前教师未设置专业，只能创建公共课试卷");
            }
            if (!currentMajor.equals(major)) {
                throw new BusinessException("教师只能维护本专业试卷");
            }
        }
        if (major == null) {
            throw new BusinessException("非公共课试卷必须设置专业");
        }
        paper.setMajor(major);
    }

    private void validateWindow(LocalDateTime startTime, LocalDateTime endTime) {
        if (!startTime.isBefore(endTime)) {
            throw new BusinessException("试卷开始时间必须早于结束时间");
        }
    }

    private void validateQuestion(QuestionRequest request) {
        if (request.score() == null || request.score().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("题目分值必须大于 0");
        }
    }

    private void fillQuestion(Question question, QuestionRequest request) {
        question.setTitle(request.title().trim());
        question.setType(request.type());
        question.setOptionsText(request.optionsText());
        question.setCorrectAnswer(request.correctAnswer().trim());
        question.setScore(request.score());
    }

    private Integer nextOrderNo(Long paperId) {
        return questionRepository.findByPaperIdOrderByOrderNoAsc(paperId).stream()
                .map(Question::getOrderNo)
                .filter(orderNo -> orderNo != null)
                .max(Comparator.naturalOrder())
                .map(max -> max + 1)
                .orElse(1);
    }

    private void recalculateTotalScore(Long paperId) {
        Paper paper = findPaper(paperId);
        BigDecimal total = questionRepository.findByPaperIdOrderByOrderNoAsc(paperId).stream()
                .map(Question::getScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        paper.setTotalScore(total);
    }

    private PaperDto toDto(Paper paper) {
        return new PaperDto(
                paper.getId(),
                paper.getTitle(),
                paper.getDescription(),
                paper.getStartTime(),
                paper.getEndTime(),
                paper.getTotalScore(),
                paper.getPublished(),
                paper.getMajor(),
                paper.getCommonCourse(),
                paper.getCreatedBy() == null ? null : paper.getCreatedBy().getRealName()
        );
    }

    private QuestionDto toQuestionDto(Question question, boolean includeAnswer) {
        return new QuestionDto(
                question.getId(),
                question.getPaper().getId(),
                question.getTitle(),
                question.getType(),
                question.getOptionsText(),
                includeAnswer ? question.getCorrectAnswer() : null,
                question.getScore(),
                question.getOrderNo()
        );
    }
}
