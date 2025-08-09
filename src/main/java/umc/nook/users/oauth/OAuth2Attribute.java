package umc.nook.users.oauth;

import lombok.*;

import java.util.HashMap;
import java.util.Map;

@Builder(access = AccessLevel.PRIVATE)
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OAuth2Attribute {
    private Map<String, Object> attributes;
    private String attributeKey;
    private String nickname;

    private String email;
    private Long kakaoUserId;

    public static OAuth2Attribute of(String attributeKey, Map<String, Object> attributes) {
        return ofKakao(attributeKey, attributes);
    }

    private static OAuth2Attribute ofKakao(String attributeKey, Map<String, Object> attributes) {
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        String nickname = null;
        String email = null;

        if (kakaoAccount != null) {
            Map<String, Object> kakaoProfile = (Map<String, Object>) kakaoAccount.get("profile");
            if (kakaoProfile != null) {
                nickname = (String) kakaoProfile.get("nickname");
                email = (String) kakaoProfile.get("email");
            }
        }

        Long kakaoUserId = attributes.get("id") != null ? ((Number) attributes.get("id")).longValue() : null;

        return OAuth2Attribute.builder()
                .nickname(nickname)
                .attributes(attributes)
                .email(email)
                .kakaoUserId(kakaoUserId)
                .attributeKey(attributeKey)
                .build();
    }

    public Map<String, Object> convertToMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("attributeKey", attributeKey);
        map.put("nickname", nickname);
        map.put("email", email);
        return map;
    }
}