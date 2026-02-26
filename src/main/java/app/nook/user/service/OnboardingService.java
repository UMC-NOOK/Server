package app.nook.user.service;

import app.nook.book.domain.Category;
import app.nook.book.domain.enums.MallType;
import app.nook.book.exception.BookErrorCode;
import app.nook.book.repository.CategoryRepository;
import app.nook.book.service.FileStorageService;
import app.nook.global.exception.CustomException;
import app.nook.global.response.ErrorCode;
import app.nook.library.domain.enums.ReadingStatus;
import app.nook.library.repository.LibraryRepository;
import app.nook.user.domain.User;
import app.nook.user.dto.OnboardingDto;
import app.nook.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OnboardingService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final LibraryRepository libraryRepository;
    private final FileStorageService fileStorageService;


    @Transactional
    public OnboardingDto.CompleteResponse completeOnboarding(
            Long userId,
            OnboardingDto.CompleteRequest request
    ) {
        User user = getUser(userId);

        List<Category> selected = request.getCategories().stream()
                .distinct()
                .map(name -> categoryRepository.findByMallTypeAndCategoryName(MallType.BOOK, name)
                        .orElseThrow(() -> new CustomException(BookErrorCode.CATEGORY_NOT_FOUND)))
                .toList();

        if (selected.isEmpty() || selected.size() > 2) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        Category preferred = choosePreferredCategory(user, selected);
        String profileUrl = user.getProfileUrl();
        if (request.getProfileImage() != null && !request.getProfileImage().isEmpty()) {
            profileUrl = fileStorageService.uploadProfile(request.getProfileImage());
        }

        user.updateOnboarding(
                request.getGoal(),
                request.getNickname(),
                profileUrl,
                preferred
        );

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
        return new OnboardingDto.GoalResponse(user.getGoal(), remaining);
    }

    @Transactional
    public OnboardingDto.GoalUpdateResponse updateGoal(
            Long userId,
            OnboardingDto.GoalUpdateRequest request
    ) {
        User user = getUser(userId);
        user.updateGoal(request.goal());
        return new OnboardingDto.GoalUpdateResponse(user.getGoal());
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
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
