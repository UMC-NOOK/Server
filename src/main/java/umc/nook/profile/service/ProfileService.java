package umc.nook.profile.service;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import umc.nook.common.exception.CustomException;
import umc.nook.common.response.ErrorCode;
import umc.nook.profile.domain.BackgroundPattern;
import umc.nook.profile.domain.CharacterColor;
import umc.nook.profile.domain.Profile;
import umc.nook.profile.dto.ProfileResponseDTO;
import umc.nook.profile.repository.ProfileRepository;
import umc.nook.users.domain.User;
import umc.nook.users.repository.UserRepository;
import umc.nook.users.service.CustomUserDetails;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;

    @Transactional
    public Long updateProfile(String alias, CharacterColor characterColor,
                              BackgroundPattern backgroundPattern, CustomUserDetails user) {

        Profile profile = profileRepository.findByUser(user.getUser())
                .orElseThrow(() -> new CustomException(ErrorCode.PROFILE_NOT_FOUND));

        profile.update(alias, characterColor, backgroundPattern);
        return user.getUser().getUserId();
    }

    @Transactional(readOnly = true)
    public ProfileResponseDTO getProfile(CustomUserDetails user) {

        Profile profile = profileRepository.findByUser(user.getUser())
                .orElseThrow(() -> new CustomException(ErrorCode.PROFILE_NOT_FOUND));

        return ProfileResponseDTO.builder()
                .alias(profile.getAlias())
                .characterColor(profile.getCharacterColor())
                .backgroundPattern(profile.getBackgroundPattern())
                .build();
    }

    @Transactional
    public Long updateNickname(String nickname, CustomUserDetails userDetails) {

        User user = userDetails.getUser();

        user.updateNickname(nickname);
        return user.getUserId();

    }
}
