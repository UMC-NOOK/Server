package umc.nook.readingrooms.domain;

import jakarta.persistence.*;
import lombok.*;
import umc.nook.BaseTimeEntity;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "reading_rooms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ReadingRoom extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 30, nullable = false)
    private String name;

    @Column(length = 30, nullable = false)
    private String description;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "theme_id", nullable = false)
    private Theme theme;

    @OneToMany(mappedBy = "readingRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReadingRoomHashtag> hashtags = new ArrayList<>();

    @OneToMany(mappedBy = "readingRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReadingRoomUser> users = new ArrayList<>();


}
