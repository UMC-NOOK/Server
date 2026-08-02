package app.nook.record.repository;

import app.nook.record.domain.RecordImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RecordImageRepository extends JpaRepository<RecordImage,Long> {

    /**
     * 특정 유저가 가진 모든 기록 이미지의 S3 key 조회.
     * (record → library → user 경로로 소유자 판별)
     * hard delete 시 S3 실물 파일을 삭제하기 위해 사용한다.
     */
    @Query("SELECT ri.key FROM RecordImage ri " +
            "WHERE ri.record.library.user.id = :userId " +
            "AND ri.key IS NOT NULL")
    List<String> findKeysByUserId(@Param("userId") Long userId);

    /** 고아 이미지 대조용 — 사용 중인 모든 기록 이미지 key */
    @Query("SELECT ri.key FROM RecordImage ri WHERE ri.key IS NOT NULL")
    List<String> findAllKeys();
}
