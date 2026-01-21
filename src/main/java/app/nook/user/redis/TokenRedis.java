package app.nook.user.redis;

import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@RedisHash(value = "token", timeToLive = 60*60*24*3)
public class TokenRedis {

    @Id
    private Long id;

    private String refreshToken;

    @Indexed
    private String accessToken;
}