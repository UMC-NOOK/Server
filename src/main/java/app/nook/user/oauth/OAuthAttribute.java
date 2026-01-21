package app.nook.user.oauth;

import lombok.*;

import java.util.Map;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder(access = AccessLevel.PRIVATE)
public class OAuthAttribute {

    private Map<String, Object> attributes;
    private String attributeKey;

    private String nickname;
    private String email;
    private String picture;

    private String providerId;

    public static OAuthAttribute of(
            String provider,
            String attributeKey,
            Map<String, Object> attributes
    ) {
        return switch (provider.toLowerCase()) {
            case "google" -> ofGoogle(attributeKey, attributes);
            case "kakao" -> ofKakao(attributeKey, attributes);
            default -> throw new IllegalArgumentException("Unsupported provider: " + provider);
        };
    }

    // 구글 로그인
    private static OAuthAttribute ofGoogle(
            String attributeKey,
            Map<String, Object> attributes
    ) {
        return OAuthAttribute.builder()
                .providerId((String) attributes.get("sub"))
                .email((String) attributes.get("email"))
                .nickname((String) attributes.get("name"))
                .picture((String) attributes.get("picture"))
                .attributeKey(attributeKey)
                .attributes(attributes)
                .build();
    }


    // 카카오 로그인
    @SuppressWarnings("unchecked")
    private static OAuthAttribute ofKakao(
            String attributeKey,
            Map<String, Object> attributes
    ) {
        Map<String, Object> kakaoAccount =
                (Map<String, Object>) attributes.get("kakao_account");

        String email = null;
        String nickname = null;
        String picture = null;

        if (kakaoAccount != null) {
            Map<String, Object> profile =
                    (Map<String, Object>) kakaoAccount.get("profile");

            if (profile != null) {
                nickname = (String) profile.get("nickname");
                picture = (String) profile.get("profile_image_url");
            }

            email = (String) kakaoAccount.get("email");
        }

        String providerId = attributes.get("id") != null
                ? String.valueOf(attributes.get("id"))
                : null;

        return OAuthAttribute.builder()
                .providerId(providerId)
                .email(email)
                .nickname(nickname)
                .picture(picture)
                .attributeKey(attributeKey)
                .attributes(attributes)
                .build();
    }
}