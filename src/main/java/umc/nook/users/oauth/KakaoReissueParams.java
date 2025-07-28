package umc.nook.users.oauth;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class KakaoReissueParams {
    private String token_type;                  // 토큰 타입 (bearer)
    private String access_token;                // 새 액세스 토큰
    private String id_token;                    // ID 토큰 (선택적)
    private Integer expires_in;                 // 액세스 토큰 만료 시간 (초)
    private String refresh_token;               // 새 리프레시 토큰 (조건부 제공)
    private Integer refresh_token_expires_in;   // 리프레시 토큰 만료 시간 (초)
}
