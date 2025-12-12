package com.hot6.backend.board.question;

import com.hot6.backend.board.answer.AnswerRepository;
import com.hot6.backend.board.answer.AnswerService;
import com.hot6.backend.board.answer.aiAnswer.AiAnswerService;
import com.hot6.backend.board.hashtagQuestion.Hashtag_QuestionService;
import com.hot6.backend.board.question.images.QuestionImageService;
import com.hot6.backend.board.question.model.Question;
import com.hot6.backend.board.question.model.QuestionDto;
import com.hot6.backend.board.question.model.QuestionListResponse;
import com.hot6.backend.common.exception.BaseException;
import com.hot6.backend.common.BaseResponseStatus;
import com.hot6.backend.pet.PetRepository;
import com.hot6.backend.pet.model.Pet;
import com.hot6.backend.user.model.User;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class QuestionService {

    private final AnswerService answerService;
    private final QuestionRepository questionRepository;
    private final Hashtag_QuestionService hashtagService;
    private final QuestionImageService questionImageService;
//    private final AiAnswerService aiAnswerService;
    private final PetRepository petRepository;
    private final AnswerRepository answerRepository;

    @Transactional(readOnly = false)
    public void create(QuestionDto.QuestionRequest dto, List<MultipartFile> images) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) auth.getPrincipal();

        Question question = dto.toEntity();
        question.setUser(currentUser);
        questionRepository.save(question);

        try {
            if (dto.getPetIdxList() != null && !dto.getPetIdxList().isEmpty()) {
                List<Pet> pets = petRepository.findAllById(dto.getPetIdxList());
                pets.forEach(pet -> pet.setQuestion(question));
                petRepository.saveAll(pets);
            }

            if (dto.getTags() != null && !dto.getTags().isEmpty()) {
                hashtagService.saveTags(dto.getTags(), question.getIdx());
            }

            if (images != null && !images.isEmpty()) {
                questionImageService.saveImages(images, question);
            }

//            String aiContent = aiAnswerService.generateAnswer(question.getQTitle(), question.getContent());
//            answerService.createAiAnswerForQuestion(question, aiContent);

        } catch (BaseException be) {
            throw be;
        } catch (Exception e) {
            throw new BaseException(BaseResponseStatus.QUESTION_CREATE_FAILED);
        }
    }

    public QuestionListResponse list(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        List<Question> questions = questionRepository.findAllWithHashtags(pageable);
        long totalCount = questionRepository.countAll();

        List<QuestionDto.QuestionResponse> content = questions.stream()
                .map(q -> QuestionDto.QuestionResponse.from(q, answerService.countByQuestionIdx(q.getIdx())))
                .toList();

        int currentPage = page + 1;
        int totalPages = (int) Math.ceil((double) totalCount / size);
        int pageGroupStart = ((currentPage - 1) / 10) * 10 + 1;
        int pageGroupEnd = Math.min(pageGroupStart + 9, totalPages);

        List<Integer> visiblePages = new ArrayList<>();
        for (int i = pageGroupStart; i <= pageGroupEnd; i++) {
            visiblePages.add(i);
        }

        return QuestionListResponse.builder()
                .content(content)
                .currentPage(currentPage)
                .totalPages(totalPages)
                .pageGroupStart(pageGroupStart)
                .pageGroupEnd(pageGroupEnd)
                .visiblePages(visiblePages)
                .build();
    }

    public QuestionListResponse search(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        List<Question> questions = questionRepository.searchWithHashtags(keyword, pageable);
        long totalCount = questionRepository.countByKeyword(keyword);

        List<QuestionDto.QuestionResponse> content = questions.stream()
                .map(q -> QuestionDto.QuestionResponse.from(q, answerService.countByQuestionIdx(q.getIdx())))
                .toList();

        int currentPage = page + 1;
        int totalPages = (int) Math.ceil((double) totalCount / size);
        int pageGroupSize = 10;
        int pageGroupStart = ((currentPage - 1) / pageGroupSize) * pageGroupSize + 1;
        int pageGroupEnd = Math.min(pageGroupStart + pageGroupSize - 1, totalPages);

        List<Integer> visiblePages = new ArrayList<>();
        for (int i = pageGroupStart; i <= pageGroupEnd; i++) {
            visiblePages.add(i);
        }

        return QuestionListResponse.builder()
                .content(content)
                .currentPage(currentPage)
                .totalPages(totalPages)
                .pageGroupStart(pageGroupStart)
                .pageGroupEnd(pageGroupEnd)
                .visiblePages(visiblePages)
                .build();
    }

    public QuestionDto.QuestionResponse read(Long idx) {
        Question question = questionRepository.findWithAssociationsById(idx)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.QUESTION_NOT_FOUND));

        return QuestionDto.QuestionResponse.from(
                question,
                answerService.countByQuestionIdx(question.getIdx())
        );
    }

    @Transactional(readOnly = false)
    public void update(Long idx, QuestionDto.QuestionRequest dto, List<MultipartFile> images) {
        Question question = questionRepository.findById(idx)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.QUESTION_NOT_FOUND));

        try {
            question.setQTitle(dto.getQTitle());
            question.setContent(dto.getContent());
            question.setSelected(dto.isSelected());
            questionRepository.save(question);

            hashtagService.deleteByQuestionIdx(idx);
            if (dto.getTags() != null) {
                hashtagService.saveTags(dto.getTags(), idx);
            }

            if (dto.getRemovedImageUrls() != null) {
                questionImageService.deleteImagesByUrls(dto.getRemovedImageUrls());
            }

            if (images != null && !images.isEmpty()) {
                questionImageService.saveImages(images, question);
            }

            List<Pet> existingPets = petRepository.findAllByQuestion(question);
            existingPets.forEach(p -> p.setQuestion(null));
            petRepository.saveAll(existingPets);

            if (dto.getPetIdxList() != null) {
                List<Pet> newPets = petRepository.findAllById(dto.getPetIdxList());
                newPets.forEach(pet -> pet.setQuestion(question));
                petRepository.saveAll(newPets);
            }

        } catch (Exception e) {
            throw new BaseException(BaseResponseStatus.QUESTION_UPDATE_FAILED);
        }
    }

    @Transactional(readOnly = false)
    public void delete(Long idx) {
        Question question = questionRepository.findById(idx)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.QUESTION_NOT_FOUND));
        try {
            questionImageService.deleteImagesByQuestion(idx);
            answerService.deleteByQuestionIdx(idx);
            hashtagService.deleteByQuestionIdx(idx);

            List<Pet> relatedPets = petRepository.findAllByQuestion(question);
            relatedPets.forEach(pet -> pet.setQuestion(null));
            petRepository.saveAll(relatedPets);

            questionRepository.delete(question);
        } catch (Exception e) {
            throw new BaseException(BaseResponseStatus.QUESTION_DELETE_FAILED);
        }
    }

    public List<QuestionDto.UserQuestionResponse> findUserQuestions(Long userId) {
        List<Question> questions = questionRepository.findByUser_IdxOrderByCreatedAtDesc(userId);

        return questions.stream()
                .map(q -> QuestionDto.UserQuestionResponse.from(q, answerRepository.countByQuestion(q)))
                .toList();
    }
}

