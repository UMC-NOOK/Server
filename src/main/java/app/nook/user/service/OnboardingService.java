package app.nook.user.service;

import app.nook.book.domain.Category;
import app.nook.book.domain.enums.MallType;
import app.nook.book.exception.BookErrorCode;
import app.nook.book.repository.CategoryRepository;
import app.nook.global.exception.CustomException;
import app.nook.global.response.ErrorCode;
import app.nook.user.domain.User;
import app.nook.user.dto.OnboardingDto;
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

    @Transactional
    public OnboardingDto.CompleteResponse completeOnboarding(
            User user,
            OnboardingDto.CompleteRequest request
    ) {
        List<Category> selected = request.categories().stream()
                .distinct()
                .map(name -> categoryRepository.findByMallTypeAndCategoryName(MallType.BOOK, name)
                        .orElseThrow(() -> new CustomException(BookErrorCode.CATEGORY_NOT_FOUND)))
                .toList();

        if (selected.isEmpty() || selected.size() > 2) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        Category preferred = choosePreferredCategory(user, selected);

        user.updateOnboarding(
                request.goal(),
                request.nickname(),
                request.profileUrl(),
                preferred
        );

        return new OnboardingDto.CompleteResponse(
                true,
                preferred.getCategoryName(),
                user.getOnboardingCompletedAt()
        );

    }

    public OnboardingDto.StatusResponse getOnboardingStatus(User user) {
        return new OnboardingDto.StatusResponse(
                user.needsOnboarding(),
                user.getOnboardingCompletedAt()
        );
    }

    @Transactional
    public OnboardingDto.GoalUpdateResponse updateGoal(
            User user,
            OnboardingDto.GoalUpdateRequest request
    ) {
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
}
