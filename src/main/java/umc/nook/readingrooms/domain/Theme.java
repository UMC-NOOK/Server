package umc.nook.readingrooms.domain;

import jakarta.persistence.*;
import lombok.*;
import umc.nook.BaseTimeEntity;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "theme")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Theme extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String imageUrl;

    private String bgmUrl;

    @OneToMany(mappedBy = "theme", cascade = CascadeType.ALL, orphanRemoval = false)
    private List<ReadingRoom> readingRooms = new ArrayList<>();

}
