package app.nook.user.service;

import app.nook.book.domain.Category;
import app.nook.book.domain.enums.MallType;
import app.nook.book.exception.BookErrorCode;
import app.nook.book.repository.CategoryRepository;
import app.nook.global.exception.CustomException;
import app.nook.global.response.ErrorCode;
import app.nook.library.domain.enums.ReadingStatus;
import app.nook.library.repository.LibraryRepository;
import app.nook.r2.service.PresignedUrlService;
import app.nook.user.domain.User;
import app.nook.user.domain.enums.UserRole;
import app.nook.user.dto.OnboardingDto;
import app.nook.user.event.ProfileImageCleanupEvent;
import app.nook.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyShort;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OnboardingServiceTest {

    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private LibraryRepository libraryRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private PresignedUrlService presignedUrlService;

    @InjectMocks
    private OnboardingService onboardingService;

    private User user;
    private Category fiction;
    private Category essay;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .email("test@nook.com")
                .nickName("oldNick")
                .provider("google")
                .providerId("pid")
                .role(UserRole.USER)
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);

        fiction = Category.of(MallType.BOOK, "소설/시/희곡", 1);
        essay = Category.of(MallType.BOOK, "에세이", 2);
        ReflectionTestUtils.setField(fiction, "id", 101L);
        ReflectionTestUtils.setField(essay, "id", 102L);
    }

    @Test
    @DisplayName("completeOnboarding 성공: 단일 카테고리 + 이미지 없음")
    void completeOnboarding_success_singleCategory_noImage() {
        OnboardingDto.CompleteRequest req = new OnboardingDto.CompleteRequest();
        req.setGoal((short) 30);
        req.setNickname("newNick");
        req.setCategories(List.of("소설/시/희곡"));

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(categoryRepository.findByMallTypeAndCategoryName(MallType.BOOK, "소설/시/희곡"))
                .willReturn(Optional.of(fiction));

        OnboardingDto.CompleteResponse res = onboardingService.completeOnboarding(1L, req);

        assertThat(res.onboardingCompleted()).isTrue();
        assertThat(res.preferredCategory()).isEqualTo("소설/시/희곡");
        assertThat(res.completedAt()).isNotNull();
        assertThat(user.getGoal()).isEqualTo((short) 30);
        assertThat(user.getNickName()).isEqualTo("newNick");
        assertThat(user.getPreferredCategory().getId()).isEqualTo(101L);
        verify(presignedUrlService, never()).validateOwnedImageKey(anyLong(), any(), any());
    }

    @Test
    @DisplayName("completeOnboarding 성공: 2개 선택 시 기존 preferred 유지")
    void completeOnboarding_success_keepCurrentPreferred() {
        ReflectionTestUtils.setField(user, "preferredCategory", essay);

        OnboardingDto.CompleteRequest req = new OnboardingDto.CompleteRequest();
        req.setGoal((short) 20);
        req.setNickname("nick");
        req.setCategories(List.of("소설/시/희곡", "에세이"));

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(categoryRepository.findByMallTypeAndCategoryName(MallType.BOOK, "소설/시/희곡"))
                .willReturn(Optional.of(fiction));
        given(categoryRepository.findByMallTypeAndCategoryName(MallType.BOOK, "에세이"))
                .willReturn(Optional.of(essay));

        OnboardingDto.CompleteResponse res = onboardingService.completeOnboarding(1L, req);

        assertThat(res.preferredCategory()).isEqualTo("에세이");
        assertThat(user.getPreferredCategory().getId()).isEqualTo(102L);
    }

    @Test
    @DisplayName("completeOnboarding 성공: 프로필 이미지 업로드")
    void completeOnboarding_success_withProfileImage() {
        OnboardingDto.CompleteRequest req = new OnboardingDto.CompleteRequest();
        req.setGoal((short) 10);
        req.setNickname("imgNick");
        req.setCategories(List.of("소설/시/희곡"));
        req.setProfileImageKey("profile/users/1/a.png");

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(categoryRepository.findByMallTypeAndCategoryName(MallType.BOOK, "소설/시/희곡"))
                .willReturn(Optional.of(fiction));

        onboardingService.completeOnboarding(1L, req);

        assertThat(user.getProfileImageKey()).isEqualTo("profile/users/1/a.png");
        verify(presignedUrlService).validateOwnedImageKey(1L, "profile/users/1/a.png", "profile");
    }

    @Test
    @DisplayName("completeOnboarding 실패 시 새로 업로드한 프로필 이미지를 정리한다")
    void completeOnboarding_fail_cleanupUploadedProfileImage() {
        User spyUser = spy(user);
        OnboardingDto.CompleteRequest req = new OnboardingDto.CompleteRequest();
        req.setGoal((short) 10);
        req.setNickname("imgNick");
        req.setCategories(List.of("소설/시/희곡"));
        req.setProfileImageKey("profile/users/1/new.png");

        given(userRepository.findById(1L)).willReturn(Optional.of(spyUser));
        given(categoryRepository.findByMallTypeAndCategoryName(MallType.BOOK, "소설/시/희곡"))
                .willReturn(Optional.of(fiction));
        doThrow(new RuntimeException("db fail")).when(spyUser)
                .updateOnboarding(anyShort(), anyString(), anyString(), any());

        assertThrows(RuntimeException.class, () -> onboardingService.completeOnboarding(1L, req));

        verify(presignedUrlService).deleteFile("profile/users/1/new.png");
    }

    @Test
    @DisplayName("completeOnboarding 성공 시 기존 프로필 이미지 정리 이벤트를 발행한다")
    void completeOnboarding_success_publishProfileCleanupEvent() {
        ReflectionTestUtils.setField(user, "profileImageKey", "profile/users/1/old.png");

        OnboardingDto.CompleteRequest req = new OnboardingDto.CompleteRequest();
        req.setGoal((short) 10);
        req.setNickname("imgNick");
        req.setCategories(List.of("소설/시/희곡"));
        req.setProfileImageKey("profile/users/1/new.png");

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(categoryRepository.findByMallTypeAndCategoryName(MallType.BOOK, "소설/시/희곡"))
                .willReturn(Optional.of(fiction));

        onboardingService.completeOnboarding(1L, req);

        assertThat(user.getProfileImageKey()).isEqualTo("profile/users/1/new.png");
        verify(eventPublisher).publishEvent(new ProfileImageCleanupEvent("profile/users/1/old.png"));
    }

    @Test
    @DisplayName("completeOnboarding 실패: 카테고리 0개")
    void completeOnboarding_fail_emptyCategories() {
        OnboardingDto.CompleteRequest req = new OnboardingDto.CompleteRequest();
        req.setGoal((short) 10);
        req.setNickname("nick");
        req.setCategories(List.of());

        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        CustomException ex = assertThrows(CustomException.class,
                () -> onboardingService.completeOnboarding(1L, req));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    @Test
    @DisplayName("completeOnboarding 실패: 카테고리 3개")
    void completeOnboarding_fail_threeCategories() {
        OnboardingDto.CompleteRequest req = new OnboardingDto.CompleteRequest();
        req.setGoal((short) 10);
        req.setNickname("nick");
        req.setCategories(List.of("소설/시/희곡", "에세이", "경제경영"));

        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        CustomException ex = assertThrows(CustomException.class,
                () -> onboardingService.completeOnboarding(1L, req));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    @Test
    @DisplayName("completeOnboarding 실패: 존재하지 않는 카테고리")
    void completeOnboarding_fail_categoryNotFound() {
        OnboardingDto.CompleteRequest req = new OnboardingDto.CompleteRequest();
        req.setGoal((short) 10);
        req.setNickname("nick");
        req.setCategories(List.of("없는카테고리"));

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(categoryRepository.findByMallTypeAndCategoryName(MallType.BOOK, "없는카테고리"))
                .willReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class,
                () -> onboardingService.completeOnboarding(1L, req));
        assertThat(ex.getErrorCode()).isEqualTo(BookErrorCode.CATEGORY_NOT_FOUND);
    }

    @Test
    @DisplayName("getOnboardingStatus: 완료 전")
    void getOnboardingStatus_beforeComplete() {
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        OnboardingDto.StatusResponse res = onboardingService.getOnboardingStatus(1L);

        assertThat(res.needsOnboarding()).isTrue();
        assertThat(res.completedAt()).isNull();
    }

    @Test
    @DisplayName("getOnboardingStatus: 완료 후")
    void getOnboardingStatus_afterComplete() {
        user.updateOnboarding((short) 12, "nick", null, fiction);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        OnboardingDto.StatusResponse res = onboardingService.getOnboardingStatus(1L);

        assertThat(res.needsOnboarding()).isFalse();
        assertThat(res.completedAt()).isNotNull();
    }

    @Test
    @DisplayName("getGoal: remainingCount = max(0, goal - finished)")
    void getGoal_remainingCount() {
        user.updateGoal((short) 10);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(libraryRepository.countByUserAndReadingStatus(user, ReadingStatus.FINISHED)).willReturn(3L);

        OnboardingDto.GoalResponse res = onboardingService.getGoal(1L);

        assertThat(res.goal()).isEqualTo((short) 10);
        assertThat(res.remainingCount()).isEqualTo(7);
    }

    @Test
    @DisplayName("getGoal: finished가 goal보다 크면 0")
    void getGoal_floorZero() {
        user.updateGoal((short) 5);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(libraryRepository.countByUserAndReadingStatus(user, ReadingStatus.FINISHED)).willReturn(99L);

        OnboardingDto.GoalResponse res = onboardingService.getGoal(1L);

        assertThat(res.remainingCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("updateGoal 성공")
    void updateGoal_success() {
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        OnboardingDto.GoalUpdateResponse res =
                onboardingService.updateGoal(1L, new OnboardingDto.GoalUpdateRequest((short) 50));

        assertThat(res.goal()).isEqualTo((short) 50);
        assertThat(user.getGoal()).isEqualTo((short) 50);
    }

    @Test
    @DisplayName("user 조회 실패 시 USER_NOT_FOUND")
    void userNotFound() {
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class,
                () -> onboardingService.getOnboardingStatus(999L));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
    }
}
