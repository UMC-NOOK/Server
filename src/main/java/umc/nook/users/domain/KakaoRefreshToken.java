package umc.nook.users.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@RedisHash(value = "kakaoRefreshToken", timeToLive = 60 * 60 * 24 * 3)
public class KakaoRefreshToken {

    @Id
    private String tokenId; // 쿠키에 저장할 ID

    private Long userId; // 사용자 식별자

    private String refreshToken; // 실제 Kakao refresh token

    private String accessToken; // Kakao access token


    public void updateToken(String refreshToken, String accessToken) {
        this.refreshToken = refreshToken;
        this.accessToken = accessToken;
    }
}
