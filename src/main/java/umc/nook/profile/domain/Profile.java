package umc.nook.profile.domain;

import jakarta.persistence.*;
import lombok.*;
import umc.nook.users.domain.User;

@Entity
@Table(name = "profile")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Profile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long profileId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 30)
    @Builder.Default
    private String alias = "프로 독자"; // 별명

    @Enumerated(EnumType.STRING)
    @Column(name = "character_color", nullable = false)
    @Builder.Default
    private CharacterColor characterColor = CharacterColor.ORANGE;

    @Enumerated(EnumType.STRING)
    @Column(name = "background_pattern", nullable = false)
    @Builder.Default
    private BackgroundPattern backgroundPattern = BackgroundPattern.NONE;

    public void update(String alias, CharacterColor color, BackgroundPattern pattern) {
        this.alias = alias;
        this.characterColor = color;
        this.backgroundPattern = pattern;
    }

}
