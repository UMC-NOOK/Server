package app.nook.global.fixture;

import app.nook.user.domain.User;
import app.nook.user.domain.enums.UserRole;
import org.springframework.test.util.ReflectionTestUtils;

public class UserFixture {

    // Service 단위테스트용 — ID 있음
    public static User user() {
        User user = User.builder()
                .email("user@test.com")
                .nickName("user")
                .role(UserRole.USER)
                .provider("GOOGLE")
                .providerId("provider-1")
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);
        return user;
    }

    // 다른 유저가 필요한 권한 테스트용
    public static User anotherUser() {
        User user = User.builder()
                .email("other@test.com")
                .nickName("other")
                .role(UserRole.USER)
                .provider("GOOGLE")
                .providerId("provider-2")
                .build();
        ReflectionTestUtils.setField(user, "id", 2L);
        return user;
    }
}
