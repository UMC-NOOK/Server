package umc.nook.readingrooms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.nook.readingrooms.domain.Hashtag;
import umc.nook.readingrooms.domain.HashtagName;

import java.util.Optional;

public interface HashtagRepository extends JpaRepository<Hashtag, Long> {
    Optional<Hashtag> findByName(HashtagName name);
}
