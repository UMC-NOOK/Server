package app.nook.user.domain;

import app.nook.book.domain.Category;
import app.nook.book.domain.SearchHistory;
import app.nook.global.common.BaseEntity;
import app.nook.user.domain.enums.UserRole;
import app.nook.user.domain.enums.UserStatus;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@JsonNaming(value = PropertyNamingStrategies.SnakeCaseStrategy.class)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private UserStatus status = UserStatus.ACTIVE;

    private Short goal = 0;

    @Enumerated(EnumType.STRING)
    private UserRole role;

    private String provider;

    private String providerId;

    private String email;

    private Short chatUsage = 0;

    private LocalDateTime deletedAt;

    @Column(name = "profile_image_key", columnDefinition = "TEXT")
    private String profileImageKey;

    private String nickName;

    private LocalDateTime onboardingCompletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preferred_category_id")
    private Category preferredCategory;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SearchHistory> searchHistories = new ArrayList<>();

    @Builder
    public User(
            String provider,
            String providerId,
            String email,
            String nickName,
            UserRole role

    ) {
        this.provider = provider;
        this.providerId = providerId;
        this.email = email;
        this.nickName = nickName;
        this.role = role;
    }

    public static User fromToken(Long id, String email, UserRole role) {
        User user = new User();
        user.id = id;
        user.email = email;
        user.role = role;
        user.status = UserStatus.ACTIVE;
        return user;
    }

    public void increaseChatUsage() {
        this.chatUsage++;
    }

    public void updateOnboarding(short goal, String nickName, String profileImageKey, Category preferredCategory) {
        this.goal = goal;
        this.nickName = nickName;
        this.profileImageKey = profileImageKey;
        this.preferredCategory = preferredCategory;

        if (this.onboardingCompletedAt == null) {
            this.onboardingCompletedAt = LocalDateTime.now();
        }
    }

    public void updateGoal(short goal) {
        this.goal = goal;
    }

    public void updateNickName(String nickName) {
        this.nickName = nickName;
    }

    public void updateProfileImage(String profileImageKey) {
        this.profileImageKey = profileImageKey;
    }

    public boolean isOnboardingCompleted() {
        return this.onboardingCompletedAt != null;
    }

    /** 회원탈퇴 — soft delete */
    public void withdraw() {
        this.status = UserStatus.DELETED;
        this.deletedAt = LocalDateTime.now();
    }

    /** 탈퇴 후 동일 소셜 계정으로 재로그인 시 계정 복구 */
    public void reactivate() {
        this.status = UserStatus.ACTIVE;
        this.deletedAt = null;
    }

    public boolean isDeleted() {
        return this.status == UserStatus.DELETED;
    }

    /** 관리자 이메일 목록(env) 기준으로 role 을 동기화한다. */
    public void syncRole(UserRole role) {
        if (this.role != role) {
            this.role = role;
        }
    }
}
