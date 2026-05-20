package app.nook.user.oauth;

import app.nook.global.exception.CustomException;
import app.nook.global.response.AuthErrorCode;
import app.nook.global.response.OAuthErrorCode;
import app.nook.user.domain.User;
import app.nook.user.domain.enums.UserRole;
import app.nook.user.domain.enums.UserStatus;
import app.nook.user.dto.OAuthDTO;
import app.nook.user.dto.UserDTO;
import app.nook.user.jwt.JwtProvider;
import app.nook.user.redis.TokenRedis;
import app.nook.user.redis.TokenRedisRepository;
import app.nook.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAuthService {

    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final TokenRedisRepository tokenRedisRepository;

    @Value("${auth.google.client-id}")
    private String googleClientId;
    @Value("${auth.google.client-secret}")
    private String googleClientSecret;
    @Value("${auth.google.redirect-uri}")
    private String googleRedirectUri;
    @Value("${auth.google.token-request-uri}")
    private String googleTokenUri;
    @Value("${auth.google.user-info-uri}")
    private String googleUserInfoUri;

    @Value("${auth.kakao.client-id}")
    private String kakaoClientId;
    @Value("${auth.kakao.client-secret}")
    private String kakaoClientSecret;
    @Value("${auth.kakao.redirect-uri}")
    private String kakaoRedirectUri;
    @Value("${auth.kakao.token-request-uri}")
    private String kakaoTokenUri;
    @Value("${auth.kakao.user-info-uri}")
    private String kakaoUserInfoUri;

    @Transactional
    public UserDTO.LoginResponse login(
            String provider,
            String code
    ) {
        OAuthDTO.TokenWrapper token = requestToken(provider, code);
        Map<String, Object> attributes =
                requestUserInfo(provider, token.accessToken());

        OAuthAttribute oauthAttribute =
                OAuthAttribute.of(provider, "id", attributes);

        User user = userRepository.findByEmail(oauthAttribute.getEmail())
                .orElseGet(() -> createUser(provider, oauthAttribute));

        if (user.getStatus() == UserStatus.DELETED) {
            throw new CustomException(AuthErrorCode.USER_INACTIVE);
        }

        String accessToken = jwtProvider.createAccessToken(user);
        String refreshToken = jwtProvider.createRefreshToken();

        tokenRedisRepository.save(
                TokenRedis.builder()
                        .id(user.getId())
                        .refreshToken(refreshToken)
                        .accessToken(accessToken)
                        .build()
        );

        return UserDTO.LoginResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickName(user.getNickName())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }


    // code로 token을 요청하는 메서드( provider마다 분기 )

    private OAuthDTO.TokenWrapper requestToken(String provider, String code) {
        return switch (provider.toLowerCase()) {
            case "google" -> requestGoogleToken(code);
            case "kakao" -> requestKakaoToken(code);
            default -> throw new CustomException(AuthErrorCode.INVALID_OAUTH_PROVIDER);
        };
    }

    private OAuthDTO.TokenWrapper requestGoogleToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        OAuthDTO.GoogleOAuthRequestParams params =
                new OAuthDTO.GoogleOAuthRequestParams(
                        "authorization_code",
                        googleClientId,
                        googleClientSecret,
                        googleRedirectUri,
                        code
                );

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", params.grantType());
        body.add("client_id", params.clientId());
        body.add("client_secret", params.clientSecret());
        body.add("redirect_uri", params.redirectUri());
        body.add("code", params.code());

        try {
            String resBody = restClientBuilder.build()
                    .post()
                    .uri(googleTokenUri)
                    .headers(httpHeaders -> httpHeaders.addAll(headers))
                    .body(body)
                    .retrieve()
                    .body(String.class);

            OAuthDTO.GoogleOAuthTokenResponse token =
                    objectMapper.readValue(resBody,
                            OAuthDTO.GoogleOAuthTokenResponse.class);

            return OAuthDTO.TokenWrapper.fromGoogle(token);

        } catch (Exception e) {
            logTokenRequestFailure("Google", e);
            throw new CustomException(OAuthErrorCode.INVALID_OAUTH_TOKEN);
        }
    }

    private OAuthDTO.TokenWrapper requestKakaoToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        OAuthDTO.KakaoOAuthRequestParams params =
                new OAuthDTO.KakaoOAuthRequestParams(
                        "authorization_code",
                        kakaoClientId,
                        kakaoRedirectUri,
                        code,
                        kakaoClientSecret
                );

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", params.grantType());
        body.add("client_id", params.clientId());
        body.add("redirect_uri", params.redirectUri());
        body.add("code", params.code());
        body.add("client_secret", params.clientSecret());

        try {
            OAuthDTO.KakaoOAuthTokenResponse token =
                    restClientBuilder.build()
                            .post()
                            .uri(kakaoTokenUri)
                            .headers(httpHeaders -> httpHeaders.addAll(headers))
                            .body(body)
                            .retrieve()
                            .body(OAuthDTO.KakaoOAuthTokenResponse.class);

            return OAuthDTO.TokenWrapper.fromKakao(token);

        } catch (Exception e) {
            logTokenRequestFailure("Kakao", e);
            throw new CustomException(OAuthErrorCode.INVALID_OAUTH_TOKEN);
        }
    }


    // userinfo
    private Map<String, Object> requestUserInfo(String provider, String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        return switch (provider.toLowerCase()) {
            case "google" -> restClientBuilder.build()
                    .get()
                    .uri(googleUserInfoUri)
                    .headers(httpHeaders -> httpHeaders.addAll(headers))
                    .retrieve()
                    .body(Map.class);

            case "kakao" -> restClientBuilder.build()
                    .post()
                    .uri(kakaoUserInfoUri)
                    .headers(httpHeaders -> httpHeaders.addAll(headers))
                    .retrieve()
                    .body(Map.class);

            default -> throw new CustomException(AuthErrorCode.INVALID_OAUTH_PROVIDER);
        };
    }


    // 회원가입 처리 메서드
    private User createUser(String provider, OAuthAttribute attr) {
        User user = User.builder()
                .email(attr.getEmail())
                .nickName(attr.getNickname())
                .provider(provider.toUpperCase())
                .providerId(attr.getProviderId())
                .role(UserRole.USER)
                .build();

        return userRepository.save(user);
    }

    private void logTokenRequestFailure(String provider, Exception e) {
        if (e instanceof RestClientResponseException responseException) {
            log.error("{} token request failed. status={}, body={}",
                    provider,
                    responseException.getStatusCode(),
                    responseException.getResponseBodyAsString(),
                    e);
            return;
        }

        log.error("{} token request failed.", provider, e);
    }

}
