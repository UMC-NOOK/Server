package umc.nook.users.domain;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;
import umc.nook.BaseTimeEntity;

import java.time.LocalDateTime;


@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@RedisHash(value = "jwtToken", timeToLive = 60*60*24*3)
public class RefreshToken {

    @Id
    private String tokenId;

    private Long userId;

    private String refreshToken;

    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime expiration;

    private String accessToken;
}
