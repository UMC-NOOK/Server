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

    public static OAuth2Attribute of(String attributeKey, Map<String, Object> attributes) {
        return ofKakao(attributeKey, attributes);
    }

    private static OAuth2Attribute ofKakao(String attributeKey, Map<String, Object> attributes) {
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        String nickname = null;

        if (kakaoAccount != null) {
            Map<String, Object> kakaoProfile = (Map<String, Object>) kakaoAccount.get("profile");
            if (kakaoProfile != null) {
                nickname = (String) kakaoProfile.get("nickname");
            }
        }

        return OAuth2Attribute.builder()
                .nickname(nickname)
                .attributes(attributes)
                .attributeKey(attributeKey)
                .build();
    }

    public Map<String, Object> convertToMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("attributeKey", attributeKey);
        map.put("nickname", nickname);
        return map;
    }
}