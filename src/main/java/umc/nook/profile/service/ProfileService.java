package umc.nook.profile.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import umc.nook.common.exception.CustomException;
import umc.nook.common.response.ErrorCode;
import umc.nook.profile.domain.Profile;
import umc.nook.profile.dto.ProfileRequestDTO;
import umc.nook.profile.repository.ProfileRepository;
import umc.nook.users.domain.User;
import umc.nook.users.service.CustomUserDetails;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;

    @Transactional
    public Long updateProfile(ProfileRequestDTO dto, CustomUserDetails user) {

        Long userId = user.getUser().getUserId();

        Profile profile = profileRepository.findByUser_UserId(userId)
                .orElseThrow(()-> new CustomException(ErrorCode.PROFILE_NOT_FOUND));

        if (dto.getAlias() != null) {
            profile.setAlias(dto.getAlias());
        }
        if (dto.getCharacterColor() != null) {
            profile.setCharacterColor(dto.getCharacterColor());
        }
        if (dto.getBackgroundPattern() != null) {
            profile.setBackgroundPattern(dto.getBackgroundPattern());
        }

        return userId;
    }
}
