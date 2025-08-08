package umc.nook.profile.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.nook.profile.domain.Profile;
import umc.nook.users.domain.User;

import java.util.Optional;

public interface ProfileRepository extends JpaRepository<Profile, Long> {
    Optional<Profile> findByUser(User user);
}
