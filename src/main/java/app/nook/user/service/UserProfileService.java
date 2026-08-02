package app.nook.user.service;

import app.nook.global.exception.CustomException;
import app.nook.global.response.AuthErrorCode;
import app.nook.r2.service.PresignedUrlService;
import app.nook.user.domain.User;
import app.nook.user.dto.UserProfileDto;
import app.nook.user.event.ProfileImageCleanupEvent;
import app.nook.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserProfileService {

    private final UserRepository userRepository;
    private final PresignedUrlService presignedUrlService;
    private final ApplicationEventPublisher eventPublisher;

    public UserProfileDto.MyPageResponse getMyPage(Long userId) {
        User user = getUser(userId);
        String profileImageUrl = presignedUrlService.resolveImageUrl(user.getId(), user.getProfileImageKey());

        return new UserProfileDto.MyPageResponse(
                user.getId(),
                user.getNickName(),
                user.getEmail(),
                profileImageUrl
        );
    }

    // 닉네임 업데이트
    @Transactional
    public UserProfileDto.NickNameUpdateResponse updateNickName(Long userId, String nickName) {
        User user = getUser(userId);
        user.updateNickName(nickName);
        return new UserProfileDto.NickNameUpdateResponse(user.getNickName());
    }

    // 프로필 이미지 업데이트
    @Transactional
    public UserProfileDto.ProfileImageUpdateResponse updateProfileImage(Long userId, String profileImageKey) {
        User user = getUser(userId);

        presignedUrlService.validateOwnedImageKey(userId, profileImageKey, "profile");

        String oldKey = user.getProfileImageKey();
        user.updateProfileImage(profileImageKey);

        if (oldKey != null && !oldKey.isBlank() && !oldKey.equals(profileImageKey)) {
            eventPublisher.publishEvent(new ProfileImageCleanupEvent(oldKey));
        }

        String profileImageUrl = presignedUrlService.resolveImageUrl(userId, profileImageKey);
        return new UserProfileDto.ProfileImageUpdateResponse(profileImageUrl);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));
    }
}
