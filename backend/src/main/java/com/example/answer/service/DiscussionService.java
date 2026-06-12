package com.example.answer.service;

import com.example.answer.auth.AuthContext;
import com.example.answer.common.BusinessException;
import com.example.answer.common.PageUtils;
import com.example.answer.dto.AuthUser;
import com.example.answer.dto.DiscussionDetailDto;
import com.example.answer.dto.DiscussionPostDto;
import com.example.answer.dto.DiscussionPostRequest;
import com.example.answer.dto.DiscussionReplyDto;
import com.example.answer.dto.DiscussionReplyRequest;
import com.example.answer.dto.PageResult;
import com.example.answer.entity.DiscussionPost;
import com.example.answer.entity.DiscussionReply;
import com.example.answer.entity.Paper;
import com.example.answer.entity.UserAccount;
import com.example.answer.repository.DiscussionPostRepository;
import com.example.answer.repository.DiscussionReplyRepository;
import com.example.answer.repository.PaperRepository;
import com.example.answer.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DiscussionService {

    private final DiscussionPostRepository postRepository;
    private final DiscussionReplyRepository replyRepository;
    private final PaperRepository paperRepository;
    private final UserRepository userRepository;
    private final PaperAccessService paperAccessService;

    public DiscussionService(
            DiscussionPostRepository postRepository,
            DiscussionReplyRepository replyRepository,
            PaperRepository paperRepository,
            UserRepository userRepository,
            PaperAccessService paperAccessService
    ) {
        this.postRepository = postRepository;
        this.replyRepository = replyRepository;
        this.paperRepository = paperRepository;
        this.userRepository = userRepository;
        this.paperAccessService = paperAccessService;
    }

    @Transactional(readOnly = true)
    public PageResult<DiscussionPostDto> list(String keyword, Long paperId, int page, int size) {
        if ("ADMIN".equals(AuthContext.currentUser().role())) {
            return PageResult.from(postRepository.search(
                    keyword,
                    paperId,
                    PageUtils.of(page, size, Sort.unsorted())
            ).map(this::toPostDto));
        }
        return PageResult.from(postRepository.searchAccessible(
                keyword,
                paperId,
                paperAccessService.currentUserMajor(),
                PageUtils.of(page, size, Sort.unsorted())
        ).map(this::toPostDto));
    }

    @Transactional(readOnly = true)
    public DiscussionDetailDto get(Long id) {
        DiscussionPost post = postRepository.findById(id)
                .orElseThrow(() -> new BusinessException("讨论不存在"));
        requirePaperAccess(post);
        return toDetailDto(post);
    }

    @Transactional
    public DiscussionDetailDto create(DiscussionPostRequest request) {
        DiscussionPost post = new DiscussionPost();
        post.setTitle(request.title().trim());
        post.setContent(request.content().trim());
        if (request.paperId() != null) {
            Paper paper = paperRepository.findById(request.paperId())
                    .orElseThrow(() -> new BusinessException("关联试卷不存在"));
            paperAccessService.requireCurrentUserAccess(paper);
            post.setPaper(paper);
        }
        post.setAuthor(currentUserAccount());
        return toDetailDto(postRepository.save(post));
    }

    @Transactional
    public DiscussionDetailDto reply(Long postId, DiscussionReplyRequest request) {
        DiscussionPost post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException("讨论不存在"));
        requirePaperAccess(post);
        DiscussionReply reply = new DiscussionReply();
        reply.setPost(post);
        reply.setContent(request.content().trim());
        reply.setAuthor(currentUserAccount());
        replyRepository.save(reply);
        post.touch();
        return toDetailDto(post);
    }

    @Transactional
    public void deletePost(Long id) {
        DiscussionPost post = postRepository.findById(id)
                .orElseThrow(() -> new BusinessException("讨论不存在"));
        requirePaperAccess(post);
        if (!canDelete(post.getAuthor())) {
            throw new BusinessException("只能删除自己发布的讨论");
        }
        replyRepository.deleteByPostId(id);
        postRepository.delete(post);
    }

    @Transactional
    public DiscussionDetailDto deleteReply(Long postId, Long replyId) {
        DiscussionPost post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException("讨论不存在"));
        requirePaperAccess(post);
        DiscussionReply reply = replyRepository.findById(replyId)
                .orElseThrow(() -> new BusinessException("回复不存在"));
        if (!reply.getPost().getId().equals(postId)) {
            throw new BusinessException("回复不属于该讨论");
        }
        if (!canDelete(reply.getAuthor())) {
            throw new BusinessException("只能删除自己发布的回复");
        }
        replyRepository.delete(reply);
        post.touch();
        return toDetailDto(post);
    }

    private DiscussionDetailDto toDetailDto(DiscussionPost post) {
        return new DiscussionDetailDto(
                toPostDto(post),
                replyRepository.findByPostIdOrderByCreatedAtAsc(post.getId()).stream()
                        .map(this::toReplyDto)
                        .toList()
        );
    }

    private DiscussionPostDto toPostDto(DiscussionPost post) {
        UserAccount author = post.getAuthor();
        Paper paper = post.getPaper();
        return new DiscussionPostDto(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                paper == null ? null : paper.getId(),
                paper == null ? null : paper.getTitle(),
                author == null ? null : author.getId(),
                author == null ? "未知用户" : author.getRealName(),
                author == null ? null : author.getRole().name(),
                author == null ? null : author.getAvatarUrl(),
                replyRepository.countByPostId(post.getId()),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                canDelete(author)
        );
    }

    private DiscussionReplyDto toReplyDto(DiscussionReply reply) {
        UserAccount author = reply.getAuthor();
        return new DiscussionReplyDto(
                reply.getId(),
                reply.getPost().getId(),
                reply.getContent(),
                author == null ? null : author.getId(),
                author == null ? "未知用户" : author.getRealName(),
                author == null ? null : author.getRole().name(),
                author == null ? null : author.getAvatarUrl(),
                reply.getCreatedAt(),
                canDelete(author)
        );
    }

    private UserAccount currentUserAccount() {
        return userRepository.findById(AuthContext.currentUser().id())
                .orElseThrow(() -> new BusinessException("当前账号不存在"));
    }

    private void requirePaperAccess(DiscussionPost post) {
        if (post.getPaper() != null) {
            paperAccessService.requireCurrentUserAccess(post.getPaper());
        }
    }

    private boolean canDelete(UserAccount author) {
        AuthUser current = AuthContext.currentUser();
        return "ADMIN".equals(current.role()) || author != null && author.getId().equals(current.id());
    }
}
