package app.nook.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

public class OAuthDTO {


    public record TokenWrapper(
            String accessToken,
            String refreshToken
    ) {
        public static TokenWrapper fromGoogle(GoogleOAuthTokenResponse res) {
            return new TokenWrapper(
                    res.accessToken(),
                    res.refreshToken()
            );
        }

        public static TokenWrapper fromKakao(KakaoOAuthTokenResponse res) {
            return new TokenWrapper(
                    res.accessToken(),
                    res.refreshToken()
            );
        }
    }

    public record GoogleOAuthTokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") int expiresIn,
            @JsonProperty("scope") String scope,
            @JsonProperty("token_type") String tokenType,
            @JsonProperty("id_token") String idToken,
            @JsonProperty("refresh_token") String refreshToken
    ) {}

    public record GoogleOAuthRequestParams(
            @JsonProperty("grant_type") String grantType,
            @JsonProperty("client_id") String clientId,
            @JsonProperty("client_secret") String clientSecret,
            @JsonProperty("redirect_uri") String redirectUri,
            @JsonProperty("code") String code
    ) {}

    public record KakaoOAuthTokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("token_type") String tokenType,
            @JsonProperty("refresh_token") String refreshToken,
            @JsonProperty("expires_in") int expiresIn,
            @JsonProperty("refresh_token_expires_in") int refreshTokenExpiresIn,
            @JsonProperty("scope") String scope
    ) {}

    public record KakaoOAuthRequestParams(
            @JsonProperty("grant_type") String grantType,
            @JsonProperty("client_id") String clientId,
            @JsonProperty("redirect_uri") String redirectUri,
            @JsonProperty("code") String code,
            @JsonProperty("client_secret") String clientSecret
    ) {}

    @Getter
    @AllArgsConstructor
    public static class OAuthLoginRequest{
        private String provider;
        private String code;
    }

}
