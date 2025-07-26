package umc.nook.users.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "profile")
@Getter
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
    private String alias; // 별명

    @Enumerated(EnumType.STRING)
    @Column(name = "character_color", nullable = false)
    @Builder.Default
    private CharacterColor characterColor = CharacterColor.ORANGE;

    @Enumerated(EnumType.STRING)
    @Column(name = "background_pattern", nullable = false)
    @Builder.Default
    private BackgroundPattern backgroundPattern = BackgroundPattern.NONE;

}
