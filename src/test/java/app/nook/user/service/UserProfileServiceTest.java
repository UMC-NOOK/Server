package app.nook.user.service;

import app.nook.global.exception.CustomException;
import app.nook.global.fixture.UserFixture;
import app.nook.global.response.AuthErrorCode;
import app.nook.r2.service.PresignedUrlService;
import app.nook.user.domain.User;
import app.nook.user.dto.UserProfileDto;
import app.nook.user.event.ProfileImageCleanupEvent;
import app.nook.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserProfile 서비스 테스트")
class UserProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PresignedUrlService presignedUrlService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private PlatformTransactionManager transactionManager;

    @InjectMocks
    private UserProfileService userProfileService;

    @Nested
    @DisplayName("프로필 정보 수정")
    class UpdateProfile {

        @Test
        @DisplayName("닉네임과 이미지를 함께 변경하고 이전 이미지는 정리 이벤트를 발행한다")
        void 프로필정보_수정_성공() {
            User user = UserFixture.user();
            ReflectionTestUtils.setField(user, "profileImageKey", "profile/users/1/old.png");
            stubTransaction();
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(presignedUrlService.resolveImageUrl(1L, "profile/users/1/new.png"))
                    .willReturn("https://cdn.example.com/new.png");

            UserProfileDto.ProfileUpdateResponse response = userProfileService.updateProfile(
                    1L,
                    "새닉네임",
                    "profile/users/1/new.png"
            );

            assertThat(user.getNickName()).isEqualTo("새닉네임");
            assertThat(user.getProfileImageKey()).isEqualTo("profile/users/1/new.png");
            assertThat(response.nickName()).isEqualTo("새닉네임");
            assertThat(response.profileImageUrl()).isEqualTo("https://cdn.example.com/new.png");
            verify(presignedUrlService).validateOwnedImageKey(1L, "profile/users/1/new.png", "profile");
            verify(eventPublisher).publishEvent(new ProfileImageCleanupEvent("profile/users/1/old.png"));
        }

        @Test
        @DisplayName("같은 이미지를 다시 설정하면 정리 이벤트를 발행하지 않는다")
        void 프로필정보_수정_동일이미지() {
            User user = UserFixture.user();
            ReflectionTestUtils.setField(user, "profileImageKey", "profile/users/1/current.png");
            stubTransaction();
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(presignedUrlService.resolveImageUrl(1L, "profile/users/1/current.png"))
                    .willReturn("https://cdn.example.com/current.png");

            userProfileService.updateProfile(1L, "새닉네임", "profile/users/1/current.png");

            verify(eventPublisher, never()).publishEvent(any(ProfileImageCleanupEvent.class));
        }

        @Test
        @DisplayName("사용자가 없으면 USER_NOT_FOUND 예외를 던진다")
        void 프로필정보_수정_유저없음() {
            stubTransaction();
            given(userRepository.findById(1L)).willReturn(Optional.empty());

            CustomException exception = assertThrows(CustomException.class,
                    () -> userProfileService.updateProfile(1L, "새닉네임", "profile/users/1/new.png"));

            assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.USER_NOT_FOUND);
        }
    }

    private void stubTransaction() {
        given(transactionManager.getTransaction(any())).willReturn(new SimpleTransactionStatus());
    }
}
