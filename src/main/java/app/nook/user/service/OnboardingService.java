package app.nook.user.service;

import app.nook.book.domain.Category;
import app.nook.book.domain.enums.MallType;
import app.nook.book.exception.BookErrorCode;
import app.nook.book.repository.CategoryRepository;
import app.nook.book.service.FileStorageService;
import app.nook.global.exception.CustomException;
import app.nook.global.response.CommonErrorCode;
import app.nook.library.domain.enums.ReadingStatus;
import app.nook.library.repository.LibraryRepository;
import app.nook.user.domain.User;
import app.nook.user.dto.OnboardingDto;
import app.nook.user.event.ProfileImageCleanupEvent;
import app.nook.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class OnboardingService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final LibraryRepository libraryRepository;
    private final FileStorageService fileStorageService;
    private final ApplicationEventPublisher eventPublisher;


    @Transactional
    public OnboardingDto.CompleteResponse completeOnboarding(
            Long userId,
            OnboardingDto.CompleteRequest request
    ) {
        log.info("[ONBOARDING_COMPLETE_START] userId={}, hasProfileImage={}, categoryCount={}",
                userId,
                request.getProfileImage() != null && !request.getProfileImage().isEmpty(),
                request.getCategories() == null ? 0 : request.getCategories().size());

        User user = getUser(userId);

        List<String> categories = request.getCategories();

        if (categories == null || categories.isEmpty() || categories.size() > 2) {
            throw new CustomException(CommonErrorCode.INVALID_REQUEST);
        }
        if (categories.stream().anyMatch(c -> c == null || c.isBlank())) {
            throw new CustomException(CommonErrorCode.INVALID_REQUEST);
        }
        if (categories.stream().distinct().count() != categories.size()) {
            throw new CustomException(CommonErrorCode.INVALID_REQUEST);
        }

        List<Category> selected = categories.stream()
                .map(name -> categoryRepository.findByMallTypeAndCategoryName(MallType.BOOK, name)
                        .orElseThrow(() -> new CustomException(BookErrorCode.CATEGORY_NOT_FOUND)))
                .toList();

        if (selected.isEmpty() || selected.size() > 2) {
            log.warn("[ONBOARDING_INVALID_CATEGORIES] userId={}, size={}", userId, selected.size());
            throw new CustomException(CommonErrorCode.INVALID_REQUEST);
        }

        Category preferred = choosePreferredCategory(user, selected);

        String oldProfileUrl = user.getProfileUrl();
        String profileUrl = oldProfileUrl;
        boolean hasNewProfileImage = request.getProfileImage() != null && !request.getProfileImage().isEmpty();
        boolean uploadedNewProfileImage = false;

        try {
            if (hasNewProfileImage) {
                profileUrl = fileStorageService.uploadProfile(request.getProfileImage());
                uploadedNewProfileImage = true;
            }

            user.updateOnboarding(
                    request.getGoal(),
                    request.getNickname(),
                    profileUrl,
                    preferred
            );

            publishProfileCleanupEventIfNeeded(hasNewProfileImage, oldProfileUrl, profileUrl);
        } catch (RuntimeException e) {
            if (uploadedNewProfileImage && profileUrl != null && !profileUrl.isBlank() && !profileUrl.equals(oldProfileUrl)) {
                fileStorageService.deleteProfileByUrl(profileUrl);
            }
            throw e;
        }

        log.info("[ONBOARDING_COMPLETE_SUCCESS] userId={}, preferredCategory={}, goal={}",
                userId, preferred.getCategoryName(), request.getGoal());

        return new OnboardingDto.CompleteResponse(
                true,
                preferred.getCategoryName(),
                user.getOnboardingCompletedAt()
        );

    }

    public OnboardingDto.StatusResponse getOnboardingStatus(Long userId) {
        User user = getUser(userId);
        return new OnboardingDto.StatusResponse(
                user.needsOnboarding(),
                user.getOnboardingCompletedAt()
        );
    }

    public OnboardingDto.GoalResponse getGoal(Long userId) {
        User user = getUser(userId);
        long finishedCount = libraryRepository.countByUserAndReadingStatus(user, ReadingStatus.FINISHED);
        int remaining = Math.max(0, user.getGoal() - (int) finishedCount);

        log.info("[GOAL_STATUS] userId={}, goal={}, finishedCount={}, remaining={}",
                userId, user.getGoal(), finishedCount, remaining);

        return new OnboardingDto.GoalResponse(user.getGoal(), remaining);
    }

    @Transactional
    public OnboardingDto.GoalUpdateResponse updateGoal(
            Long userId,
            OnboardingDto.GoalUpdateRequest request
    ) {
        User user = getUser(userId);
        short before = user.getGoal();

        user.updateGoal(request.goal());
        log.info("[GOAL_UPDATE] userId={}, from={}, to={}", userId, before, user.getGoal());

        return new OnboardingDto.GoalUpdateResponse(user.getGoal());
    }

    private void publishProfileCleanupEventIfNeeded(boolean hasNewProfileImage, String oldProfileUrl, String newProfileUrl) {
        if (!hasNewProfileImage || oldProfileUrl == null || oldProfileUrl.isBlank() || oldProfileUrl.equals(newProfileUrl)) {
            return;
        }
        eventPublisher.publishEvent(new ProfileImageCleanupEvent(oldProfileUrl));
    }

    private Category choosePreferredCategory(User user, List<Category> selected) {
        Category current = user.getPreferredCategory();
        if (current != null) {
            for (Category c : selected) {
                if (c.getId().equals(current.getId())) {
                    return current;
                }
            }
        }
        if (selected.size() == 1) {
            return selected.get(0);
        }
        return selected.get(ThreadLocalRandom.current().nextInt(selected.size()));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(CommonErrorCode.USER_NOT_FOUND));
    }
}
