package umc.nook.users.domain;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;
import umc.nook.BaseTimeEntity;

import java.time.LocalDateTime;


@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@RedisHash(value = "jwtToken", timeToLive = 60*60*24*3)
public class RefreshToken extends BaseTimeEntity {


    @Id
    private String tokenId;

    private Long userId;

    @Indexed
    private String refreshToken;

    private LocalDateTime expiration;

    @Indexed
    private String accessToken;
}
