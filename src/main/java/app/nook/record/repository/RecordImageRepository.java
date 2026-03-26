package app.nook.record.repository;

import app.nook.record.domain.RecordImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecordImageRepository extends JpaRepository<RecordImage,Long> {
}
