package umc.nook.users.oauth;

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
import umc.nook.readingrooms.domain.Role;
import umc.nook.users.domain.RoleType;
import umc.nook.users.domain.Status;
import umc.nook.users.domain.User;
import umc.nook.users.dto.UserDTO;
import umc.nook.users.repository.UserRepository;
import umc.nook.users.service.JwtProvider;

import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class OAuthService {

    private final UserRepository userRepository;
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
    private String getKakaoAccessToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        KakaoParams kakaoParams = KakaoParams.of(kakaoClientId, kakaoClientSecret, kakaoRedirectUri, code);

        HttpEntity<MultiValueMap<String, String>> request =
                new HttpEntity<>(kakaoParams.toMultiValueMap(), headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(kakaoTokenRequestUri, request, Map.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return (String) response.getBody().get("access_token");
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
    public UserDTO.LoginResponseDTO loginWithKakaoCode(String code) {
        // 1. 카카오 액세스 토큰 요청
        String kakaoAccessToken = getKakaoAccessToken(code);

        // 2. 카카오 사용자 정보 요청
        Map<String, Object> userAttribute = getKakaoUserAttributes(kakaoAccessToken);

        // 3. 카카오 사용자 정보를 OAuth2Attribute 객체로 변환
        OAuth2Attribute attributes = OAuth2Attribute.of("kakao",  userAttribute);

        // 4. 사용자 존재 여부 확인
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

        // 5. JWT 토큰 발급
        String accessToken = jwtProvider.createAccessToken(user);
        String refreshToken = jwtProvider.createRefreshToken(user);

        UserDTO.TokenResponseDto jwtTokenResponse = new UserDTO.TokenResponseDto(accessToken,refreshToken);

        return new UserDTO.LoginResponseDTO(user, jwtTokenResponse);
    }

    /**
     *  카카오 토큰 재발급
     */
    public KakaoReissueParams reissueKakaoToken(String refreshToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "refresh_token");
        params.add("client_id", kakaoClientId);
        params.add("client_secret", kakaoClientSecret);
        params.add("refresh_token", refreshToken);

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<KakaoReissueParams> response = restTemplate.exchange(
                    kakaoTokenRequestUri,
                    HttpMethod.POST,
                    requestEntity,
                    KakaoReissueParams.class
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

