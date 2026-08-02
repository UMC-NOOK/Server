package app.nook.user.oauth;

import app.nook.admin.AdminAccessChecker;
import app.nook.global.exception.CustomException;
import app.nook.global.response.AuthErrorCode;
import app.nook.global.response.OAuthErrorCode;
import app.nook.user.domain.User;
import app.nook.user.domain.enums.UserRole;
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
    private final AdminAccessChecker adminAccessChecker;

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
    @Value("${auth.kakao.unlink-uri}")
    private String kakaoUnlinkUri;
    @Value("${auth.kakao.admin-key}")
    private String kakaoAdminKey;

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

        // 관리자 이메일(env) 기준으로 role 동기화 (ADMIN_EMAILS 에 있으면 ADMIN, 아니면 USER)
        user.syncRole(adminAccessChecker.isAdmin(user.getEmail()) ? UserRole.ADMIN : UserRole.USER);

        // 재가입 정책: soft delete(유예중) 계정은 로그인 대신 recovery token(1시간)을 발급한다.
        // 에러로 막으면 복구하려 할 때 OAuth code(일회용)를 다시 받아야 하므로, 임시 토큰으로 넘긴다.
        // 클라이언트는 recoveryRequired=true 를 보고 확인 후 POST /auth/recover 를 호출한다.
        // (hard delete 된 계정은 레코드가 없으므로 위에서 신규 생성됨)
        if (user.isDeleted()) {
            String recoveryToken = jwtProvider.createRecoveryToken(user);
            log.info("[OAUTH] 탈퇴 유예중 계정 로그인 시도 → recovery token 발급. userId={}", user.getId());
            return UserDTO.LoginResponse.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .nickName(user.getNickName())
                    .recoveryRequired(true)
                    .recoveryToken(recoveryToken)
                    .build();
        }

        return issueLoginResponse(user);
    }

    private UserDTO.LoginResponse issueLoginResponse(User user) {
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
                .onboardingCompleted(user.isOnboardingCompleted())
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
            log.error("Google token request failed.", e);
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
            log.error("Kakao token request failed.", e);
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


    /**
     * 소셜 계정 연결 해제 (회원탈퇴 시 호출)
     * <p>
     * - 카카오: Admin Key 로 서버에서 강제 unlink ({@code target_id_type=user_id})
     * - 구글: 서버 관리자용 강제 unlink API 가 없어 별도 처리하지 않음
     * - 현재 카카오, 구글 뿐이지만 추후 확장성을 위해 분기하는 메서드를 별도루 둠
     */
    public void unlinkUser(User user) {
        String provider = user.getProvider();
        if (provider == null) {
            return;
        }
        switch (provider.toUpperCase()) {
            case "KAKAO" -> unlinkKakao(user.getProviderId());
            case "GOOGLE", "DEV" -> {
                // no-op: 위 주석 참고
            }
            default -> log.warn("[UNLINK] 알 수 없는 provider={}, unlink 생략", provider);
        }
    }


    /**
     * 카카오 계정 연결 해제 (unlink)
     * @param kakaoUserId
     */
    private void unlinkKakao(String kakaoUserId) {
        if (kakaoUserId == null || kakaoUserId.isBlank()) {
            log.warn("[KAKAO UNLINK] providerId 없음, unlink 생략");
            return;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set(HttpHeaders.AUTHORIZATION, "KakaoAK " + kakaoAdminKey);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("target_id_type", "user_id");
        body.add("target_id", kakaoUserId);

        try {
            restClientBuilder.build()
                    .post()
                    .uri(kakaoUnlinkUri)
                    .headers(httpHeaders -> httpHeaders.addAll(headers))
                    .body(body)
                    .retrieve()
                    .body(String.class);
            log.info("[KAKAO UNLINK] success. targetId={}", kakaoUserId);
        } catch (Exception e) {
            // unlink 실패는 best-effort 처리 (삭제 트랜잭션 롤백 방지)
            log.error("[KAKAO UNLINK] failed (best-effort, 삭제 계속 진행). targetId={}", kakaoUserId, e);
        }
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

}
