package umc.nook.users.oauth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Getter
@Builder
@AllArgsConstructor
public class KakaoRequestParams {
    private String grantType;
    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String code;

    public static KakaoRequestParams of(String clientId, String clientSecret, String redirectUri, String code) {
        return KakaoRequestParams.builder()
                .grantType("authorization_code")
                .clientId(clientId)
                .clientSecret(clientSecret)
                .redirectUri(redirectUri)
                .code(code)
                .build();
    }

    public MultiValueMap<String, String> toMultiValueMap() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", this.grantType);
        params.add("client_id", this.clientId);
        params.add("client_secret", this.clientSecret);
        params.add("redirect_uri", this.redirectUri);
        params.add("code", this.code);
        return params;
    }
}