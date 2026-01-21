package app.nook.user.oauth;

import app.nook.global.exception.CustomException;
import app.nook.global.response.ErrorCode;
import app.nook.user.domain.User;
import app.nook.user.dto.OAuthDTO;
import app.nook.user.dto.UserDTO;
import app.nook.user.jwt.JwtProvider;
import app.nook.user.redis.TokenRedis;
import app.nook.user.redis.TokenRedisRepository;
import app.nook.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAuthService {

    private final RestTemplate restTemplate;
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

    public UserDTO.LoginResponse login(
            String provider,
            String code,
            HttpServletResponse response
    ) {
        OAuthDTO.TokenWrapper token = requestToken(provider, code);
        Map<String, Object> attributes =
                requestUserInfo(provider, token.accessToken());

        OAuthAttribute oauthAttribute =
                OAuthAttribute.of(provider, "id", attributes);

        User user = userRepository.findByEmail(oauthAttribute.getEmail())
                .orElseGet(() -> createUser(provider, oauthAttribute));

        String accessToken = jwtProvider.createAccessToken(user);
        String refreshToken = jwtProvider.createRefreshToken();

        tokenRedisRepository.save(
                TokenRedis.builder()
                        .id(user.getId())
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .build()
        );

        ResponseCookie cookie = ResponseCookie.from("accessToken", accessToken)
                .httpOnly(true)
                .secure(false) // prod true
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofHours(1))
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return new UserDTO.LoginResponse(
                user.getId(),
                user.getEmail(),
                user.getNickName()
        );
    }


    // code로 token을 요청하는 메서드( provider마다 분기 )

    private OAuthDTO.TokenWrapper requestToken(String provider, String code) {
        return switch (provider.toLowerCase()) {
            case "google" -> requestGoogleToken(code);
            case "kakao" -> requestKakaoToken(code);
            default -> throw new CustomException(ErrorCode.INVALID_OAUTH_PROVIDER);
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
            ResponseEntity<String> res =
                    restTemplate.postForEntity(
                            googleTokenUri,
                            new HttpEntity<>(body, headers),
                            String.class
                    );

            OAuthDTO.GoogleOAuthTokenResponse token =
                    objectMapper.readValue(res.getBody(),
                            OAuthDTO.GoogleOAuthTokenResponse.class);

            return OAuthDTO.TokenWrapper.fromGoogle(token);

        } catch (Exception e) {
            log.error("Google token request failed", e);
            throw new CustomException(ErrorCode.INVALID_OAUTH_TOKEN);
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
                    restTemplate.postForEntity(
                            kakaoTokenUri,
                            new HttpEntity<>(body, headers),
                            OAuthDTO.KakaoOAuthTokenResponse.class
                    ).getBody();

            return OAuthDTO.TokenWrapper.fromKakao(token);

        } catch (Exception e) {
            log.error("Kakao token request failed", e);
            throw new CustomException(ErrorCode.INVALID_OAUTH_TOKEN);
        }
    }


    // userinfo
    private Map<String, Object> requestUserInfo(String provider, String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        return switch (provider.toLowerCase()) {
            case "google" -> restTemplate.exchange(
                    googleUserInfoUri,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
            ).getBody();

            case "kakao" -> restTemplate.exchange(
                    kakaoUserInfoUri,
                    HttpMethod.POST,
                    new HttpEntity<>(headers),
                    Map.class
            ).getBody();

            default -> throw new CustomException(ErrorCode.INVALID_OAUTH_PROVIDER);
        };
    }


    // 회원가입 처리 메서드
    private User createUser(String provider, OAuthAttribute attr) {
        User user = User.builder()
                .email(attr.getEmail())
                .nickName(attr.getNickname())
                .provider(provider.toUpperCase())
                .providerId(attr.getProviderId())
                .build();

        return userRepository.save(user);
    }
}