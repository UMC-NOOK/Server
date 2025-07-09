package umc.nook.readingrooms.domain;

import jakarta.persistence.*;
import lombok.*;
import umc.nook.BaseTimeEntity;
import umc.nook.users.domain.User;

import java.time.LocalDateTime;

@Entity
@Table(name = "reading_rooms_users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ReadingRoomUser extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reading_room_id", nullable = false)
    private ReadingRoom readingRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;
}
