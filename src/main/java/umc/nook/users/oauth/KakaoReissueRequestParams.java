package umc.nook.users.oauth;

import lombok.Getter;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Getter
@Builder
public class KakaoReissueRequestParams {

    @JsonProperty("grant_type")
    private final String grantType = "refresh_token";

    @JsonProperty("client_id")
    private final String clientId;

    @JsonProperty("client_secret")
    private final String clientSecret;

    @JsonProperty("refresh_token")
    private final String refreshToken;

    public MultiValueMap<String, String> toMultiValueMap() {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("grant_type", grantType);
        map.add("client_id", clientId);
        map.add("refresh_token", refreshToken);
        map.add("client_secret", clientSecret);
        return map;
    }
}
