package app.nook.focus.domain;

import app.nook.focus.domain.enums.ThemeName;
import app.nook.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "themes")
public class Theme extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "theme_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private ThemeName name;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

}
