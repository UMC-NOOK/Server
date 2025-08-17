package umc.nook.users.oauth;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import umc.nook.common.exception.CustomException;
import umc.nook.common.response.ErrorCode;
import umc.nook.profile.domain.Profile;
import umc.nook.users.domain.KakaoRefreshToken;
import umc.nook.users.domain.RoleType;
import umc.nook.users.domain.Status;
import umc.nook.users.domain.User;
import umc.nook.users.dto.UserDTO;
import umc.nook.users.redis.KakaoRefreshTokenRedisRepository;
import umc.nook.users.repository.UserRepository;
import umc.nook.users.service.JwtProvider;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class OAuthService {

    private final UserRepository userRepository;
    private final KakaoRefreshTokenRedisRepository kakaoRefreshTokenRedisRepository;
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

    @Value("${auth.kakao.unlink-uri}")
    private String kakaoUnlinkUri;

    @Value("${auth.kakao.admin-key}")
    private String kakaoAdminKey;

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
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            HttpEntity<?> req = new HttpEntity<>(headers);

            ResponseEntity<Map> res = restTemplate.exchange(
                    kakaoMemberInfoRequestUri,
                    HttpMethod.POST,
                    req,
                    Map.class
            );

            Map body = res.getBody();
            if (!res.getStatusCode().is2xxSuccessful() || body == null) {
                log.error("카카오 사용자 정보 요청 실패 status={}, body={}", res.getStatusCode(), body);
                throw new CustomException(ErrorCode.INVALID_OAUTH_TOKEN);
            }
            return body;
        } catch (RestClientResponseException e) {
            log.error("카카오 사용자 정보 요청 실패 status={}, body={}", e.getRawStatusCode(), e.getResponseBodyAsString());
            throw new CustomException(ErrorCode.INVALID_OAUTH_TOKEN);
        } catch (Exception e) {
            log.error("카카오 사용자 정보 요청 예외", e);
            throw new CustomException(ErrorCode.INVALID_OAUTH_TOKEN);
        }
    }


    /**
     * 사용자 저장 로직
     */
    private User saveUser(OAuth2Attribute oAuth2Attribute) {
        String nickName = oAuth2Attribute.getNickname();
        String email = oAuth2Attribute.getEmail();
        Long kakaoUserId = oAuth2Attribute.getKakaoUserId();

        User user = User.builder()
                .role(RoleType.USER)
                .email(email)
                .nickname(nickName)
                .kakaoUserId(kakaoUserId)
                .status(Status.ACTIVE)
                .isKakao(true)
                .build();

        Profile profile = Profile.builder()
                .build();

        user.setProfile(profile);

        return user;
    }

    /**
     * 로그인 및 회원가입 통합 처리 (인가코드 기반)
     */
    @Transactional
    public UserDTO.KakaoLoginResponseDTO loginWithKakaoCode(String code) {
        KakaoResponseParams newToken = getKakaoAccessToken(code);
        Map<String, Object> userAttribute = getKakaoUserAttributes(newToken.getAccessToken());
        OAuth2Attribute attributes = OAuth2Attribute.of("kakao", userAttribute);

        Optional<User> findUser = userRepository.findByEmail(attributes.getEmail());
        Optional<User> findKakaoUser = userRepository.findByKakaoUserId(attributes.getKakaoUserId());
        User user;
        if (findKakaoUser.isPresent()) {
            user = findKakaoUser.get();
            log.info("기존 사용자 로그인: {}", user.getEmail());
        } else if (findUser.isPresent()){
            user = findUser.get();
            log.info("기존 사용자 로그인: {}", user.getKakaoUserId());
        }else {
            log.info("새로운 사용자 생성: {}", attributes.getEmail());
            user = saveUser(attributes);
            userRepository.save(user);
        }

        String accessToken = jwtProvider.createAccessToken(user);
        String refreshToken = jwtProvider.createRefreshToken(user);
        UserDTO.KakaoTokenResponseDTO jwtTokenResponse = new UserDTO.KakaoTokenResponseDTO(accessToken,refreshToken);

        KakaoRefreshToken token = kakaoRefreshTokenRedisRepository.findByUserId(user.getUserId())
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
        kakaoRefreshTokenRedisRepository.save(token);
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
    public String buildKakaoLogoutRedirectUrl() {
        UriComponentsBuilder b = UriComponentsBuilder.fromHttpUrl(kakaoLogoutUri)
                .queryParam("client_id", kakaoClientId)
                .queryParam("logout_redirect_uri", kakaoRedirectUri);
        return b.toUriString();
    }


    /**
     * 카카오 계정 연결 해제 (Admin Key 기반)
     */
    public void unlinkWithAdminKey(Long kakaoUserId) {
        if (kakaoUserId == null) {
            throw new CustomException(ErrorCode.INVALID_USER_ID);
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set(AUTHORIZATION_HEADER, "KakaoAK " + kakaoAdminKey);
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("target_id_type", "user_id");
            body.add("target_id", String.valueOf(kakaoUserId));

            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    kakaoUnlinkUri,
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("Kakao admin unlink failed. status={}, body={}",
                        response.getStatusCode(), response.getBody());
                throw new CustomException(ErrorCode.KAKAO_UNLINK_FAILED);
            }

            Object id = response.getBody().get("id"); // 성공 시 해제된 사용자 회원번호
            log.info("Kakao admin unlink success. kakaoUserId={}", id);

        } catch (RestClientResponseException e) {
            log.warn("Kakao admin unlink failed. status={}, body={}",
                    e.getRawStatusCode(), e.getResponseBodyAsString());
            throw new CustomException(ErrorCode.KAKAO_UNLINK_FAILED);
        } catch (Exception e) {
            log.warn("Kakao admin unlink unexpected error", e);
            throw new CustomException(ErrorCode.KAKAO_UNLINK_FAILED);
        }
    }

}

