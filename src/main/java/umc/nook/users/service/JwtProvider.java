package umc.nook.users.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import umc.nook.common.exception.CustomException;
import umc.nook.common.response.ErrorCode;
import umc.nook.users.domain.RefreshToken;
import umc.nook.users.domain.User;
import umc.nook.users.redis.RefreshTokenRedisRepository;

import java.security.Key;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtProvider {

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    @Value("${jwt.secret}")
    private String secretKey;

    private Key key;
    private final long ACCESS_EXP = 1000L * 60 * 60; // 1시간
    private final long REFRESH_EXP = 1000L * 60 * 60 * 24 * 3; // 3일

    private final RefreshTokenRedisRepository refreshTokenRepository;
    private final CustomUserDetailService customUserDetailService;

    @PostConstruct
    public void init() {
        byte[] bytes = Base64.getDecoder().decode(secretKey);
        this.key = Keys.hmacShaKeyFor(bytes);
    }

    public String createAccessToken(User user) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(user.getEmail())
                .claim("userId", user.getUserId())
                .claim("role", user.getRole().name())
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + ACCESS_EXP))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String createRefreshToken(User user, String accessToken) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + REFRESH_EXP);

        String token = Jwts.builder()
                .setSubject(user.getEmail())
                .claim("userId", user.getUserId())
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        String tokenId = UUID.randomUUID().toString();

        RefreshToken refreshToken = RefreshToken.builder()
                .tokenId(tokenId)
                .userId(user.getUserId())
                .accessToken(accessToken)
                .refreshToken(token)
                .expiration(LocalDateTime.ofInstant(expiry.toInstant(), ZoneId.systemDefault()))
                .build();

        refreshTokenRepository.save(refreshToken);
        return tokenId;
    }

    public UsernamePasswordAuthenticationToken getAuthentication(String email) {
        UserDetails userDetails = customUserDetailService.loadUserByUsername(email);
        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }

    public String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearer) && bearer.startsWith(BEARER_PREFIX)) {
            return bearer.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    public Claims parseClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            Date expiration = getExpiration(token);
            log.info("AccessToken 유효. 만료 예정 시간 = {}", expiration);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("AccessToken 만료됨. exp = {}", e.getClaims().getExpiration());
            throw new CustomException(ErrorCode.TOKEN_EXPIRED);

        } catch (MalformedJwtException | SignatureException | IllegalArgumentException e) {
            log.error("잘못된 토큰: {}", e.getMessage());
            throw new JwtException("TOKEN_INVALID");

        } catch (JwtException e) {
            log.error("JWT 파싱 에러: {}", e.getMessage());
            throw new JwtException("TOKEN_INVALID");
        }
    }
    public Optional<String> extractRefreshToken(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        return Arrays.stream(request.getCookies())
                .filter(cookie -> "refreshToken".equals(cookie.getName()))
                .map(cookie -> cookie.getValue())
                .findFirst();
    }

    public Long parseRefreshToken(String token) {
        if (validateToken(token)) {
            Claims claims = parseClaims(token);
            return claims.get("userId",Long.class);
        }
        throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
    }
    public Date getExpiration(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getExpiration();
    }

    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }
}