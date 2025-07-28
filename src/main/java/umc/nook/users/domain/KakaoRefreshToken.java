package umc.nook.users.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "kakao_refresh_token")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KakaoRefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false, length = 512)
    private String refreshToken;

    @Column(nullable = false, length = 512)
    private String accessToken;

    private Long refreshTokenExpiresIn;

    public void updateToken(String refreshToken, String accessToken, Long refreshTokenExpiresIn) {
        this.refreshToken = refreshToken;
        this.accessToken = accessToken;
        this.refreshTokenExpiresIn = refreshTokenExpiresIn;
    }
}
