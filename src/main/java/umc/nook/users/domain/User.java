package umc.nook.users.domain;

import jakarta.persistence.*;
import lombok.*;
import umc.nook.BaseTimeEntity;
import umc.nook.readingrooms.domain.ReadingRoomUser;
import umc.nook.review.domain.Review;
import umc.nook.search.domain.RecentQuery;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(length = 25, unique = true)
    private String email;

    private String password;

    @Column(length = 30)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoleType role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Enumerated(EnumType.STRING)
    @Column(name = "character_color")
    @Builder.Default
    private CharacterColor characterColor = CharacterColor.ORANGE;

    //TODO: 별명 추가

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReadingRoomUser> joinedRooms = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecentQuery> recentQueries = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Review> reviews = new ArrayList<>();
}
