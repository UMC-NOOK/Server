package umc.nook.readingrooms.domain;

import jakarta.persistence.*;
import lombok.*;
import umc.nook.BaseTimeEntity;

@Entity
@Table(name = "reading_rooms_hashtags")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ReadingRoomHashtag extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reading_room_id", nullable = false)
    private ReadingRoom readingRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hashtag_id", nullable = false)
    private Hashtag hashtag;

}
