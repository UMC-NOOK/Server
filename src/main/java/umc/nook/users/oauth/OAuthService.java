package umc.nook.users.oauth;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import umc.nook.common.exception.CustomException;
import umc.nook.common.response.ErrorCode;
import umc.nook.users.domain.KakaoRefreshToken;
import umc.nook.users.domain.RoleType;
import umc.nook.users.domain.Status;
import umc.nook.users.domain.User;
import umc.nook.users.dto.UserDTO;
import umc.nook.users.repository.KakaoRefreshTokenRepository;
import umc.nook.users.repository.UserRepository;
import umc.nook.users.service.JwtProvider;

import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class OAuthService {

    private final UserRepository userRepository;
    private final KakaoRefreshTokenRepository kakaoRefreshTokenRepository;
    private final JwtProvider jwtProvider;
    private final RestTemplate restTemplate;

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String TOKEN_TYPE = "Bearer ";

    @Value("${auth.kakao.redirect-uri}")
    private String kakaoRedirectUri;

    @Value("${auth.kakao.client-id}")
    private String kakaoClientId;

    @Value("${auth.kakao.client-secret}")
    private String kakaoClientSecret;

    @Value("${auth.kakao.token-request-uri}")
    private String kakaoTokenRequestUri;

    @Value("${auth.kakao.member-info-request-uri}")
    private String kakaoMemberInfoRequestUri;

    @Value("${auth.kakao.logout-uri}")
    private String kakaoLogoutUri;

    /**
     * 카카오 인가 코드로부터 액세스 토큰 받기
     */
    private KakaoResponseParams getKakaoAccessToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        KakaoRequestParams kakaoParams = KakaoRequestParams.of(kakaoClientId, kakaoClientSecret, kakaoRedirectUri, code);

        HttpEntity<MultiValueMap<String, String>> request =
                new HttpEntity<>(kakaoParams.toMultiValueMap(), headers);

        try {
            ResponseEntity<KakaoResponseParams> response = restTemplate.postForEntity(
                    kakaoTokenRequestUri,
                    request,
                    KakaoResponseParams.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            }
        } catch (Exception e) {
            log.error("카카오 토큰 요청 실패", e);
            throw new CustomException(ErrorCode.INVALID_OAUTH_TOKEN);
        }

        throw new CustomException(ErrorCode.INVALID_OAUTH_TOKEN);
    }



    /**
     * 카카오 사용자 정보 조회
     */
    private Map<String, Object> getKakaoUserAttributes(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set(AUTHORIZATION_HEADER, TOKEN_TYPE + accessToken);
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<Object> requestEntity = new HttpEntity<>(headers);
            return restTemplate.exchange(
                    kakaoMemberInfoRequestUri,
                    HttpMethod.GET,
                    requestEntity,
                    Map.class
            ).getBody();

        } catch (Exception e) {
            log.error("카카오 사용자 정보 요청 실패", e);
            throw new CustomException(ErrorCode.INVALID_OAUTH_TOKEN);
        }
    }

    /**
     * 사용자 저장 로직
     */
    private User saveUser(OAuth2Attribute oAuth2Attribute) {
        String nickName = oAuth2Attribute.getNickname();

        return User.builder()
                .role(RoleType.USER)
                .email(nickName)
                .nickname(nickName)
                .status(Status.ACTIVE)
                .isKakao(true)
                .build();
    }

    /**
     * 로그인 및 회원가입 통합 처리 (인가코드 기반)
     */
    @Transactional
    public UserDTO.KakaoLoginResponseDTO loginWithKakaoCode(String code) {
        KakaoResponseParams newToken = getKakaoAccessToken(code);
        Map<String, Object> userAttribute = getKakaoUserAttributes(newToken.getAccessToken());
        OAuth2Attribute attributes = OAuth2Attribute.of("kakao", userAttribute);

        Optional<User> findUser = userRepository.findByEmail(attributes.getNickname());
        User user;
        if (findUser.isPresent()) {
            user = findUser.get();
            log.info("기존 사용자 로그인: {}", user.getEmail());
        } else {
            log.info("새로운 사용자 생성: {}", attributes.getNickname());
            user = saveUser(attributes);
            userRepository.save(user);
        }

        String accessToken = jwtProvider.createAccessToken(user);
        String refreshToken = jwtProvider.createRefreshToken(user);
        UserDTO.TokenResponseDto jwtTokenResponse = new UserDTO.TokenResponseDto(accessToken, refreshToken);

        KakaoRefreshToken token = kakaoRefreshTokenRepository.findByUserId(user.getUserId())
                .map(existing -> {
                    existing.updateToken(newToken.getRefreshToken(), newToken.getAccessToken(), (long) newToken.getRefreshTokenExpiresIn()); // update 메서드 활용
                    return existing;
                })
                .orElseGet(() -> KakaoRefreshToken.builder()
                        .userId(user.getUserId())
                        .refreshToken(newToken.getRefreshToken())
                        .accessToken(newToken.getAccessToken())
                        .refreshTokenExpiresIn((long) newToken.getRefreshTokenExpiresIn())
                        .build());
        kakaoRefreshTokenRepository.save(token);

        // 6. 응답 DTO 반환
        return new UserDTO.KakaoLoginResponseDTO(user, jwtTokenResponse, newToken.getRefreshToken());
    }


    /**
     *  카카오 토큰 재발급
     */
    public KakaoResponseParams reissueKakaoToken(String refreshToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        KakaoReissueRequestParams kakaoParams = KakaoReissueRequestParams.builder()
                .clientId(kakaoClientId)
                .clientSecret(kakaoClientSecret)
                .refreshToken(refreshToken)
                .build();

        HttpEntity<MultiValueMap<String, String>> requestEntity =
                new HttpEntity<>(kakaoParams.toMultiValueMap(), headers);

        try {
            ResponseEntity<KakaoResponseParams> response = restTemplate.exchange(
                    kakaoTokenRequestUri,
                    HttpMethod.POST,
                    requestEntity,
                    KakaoResponseParams.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            }
        } catch (Exception e) {
            log.error("카카오 토큰 재발급 요청 실패", e);
            throw new CustomException(ErrorCode.INVALID_OAUTH_TOKEN);
        }
        throw new CustomException(ErrorCode.INVALID_OAUTH_TOKEN);
    }


    // 카카오 로그아웃
    public void kakaoLogout(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", kakaoClientId);
        params.add("logout_redirect_uri", kakaoLogoutUri);

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(params, headers);

        try {
            restTemplate.exchange(
                    kakaoLogoutUri,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );
        } catch (Exception e) {
            log.error("카카오 로그아웃 요청 실패", e);
            throw new CustomException(ErrorCode.INVALID_OAUTH_TOKEN);
        }
    }
}

