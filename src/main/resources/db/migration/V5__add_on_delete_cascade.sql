-- 회원탈퇴(hard delete) 및 도서 삭제 시 연관 데이터가 DB 제약으로 함께 삭제되도록
-- 기존 FK 제약을 ON DELETE CASCADE 로 재정의한다.
-- (엔티티에는 @OnDelete(action = OnDeleteAction.CASCADE) 로 동일하게 명시)

-- 1. library : user 삭제 → library 삭제, book 삭제 → library 삭제
ALTER TABLE library DROP CONSTRAINT IF EXISTS fk_library_user;
ALTER TABLE library
    ADD CONSTRAINT fk_library_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE library DROP CONSTRAINT IF EXISTS fk_library_book;
ALTER TABLE library
    ADD CONSTRAINT fk_library_book
        FOREIGN KEY (book_id) REFERENCES book (book_id) ON DELETE CASCADE;

-- 2. search_history : user 삭제 → 검색 기록 삭제
ALTER TABLE search_history DROP CONSTRAINT IF EXISTS fk_search_history_user;
ALTER TABLE search_history
    ADD CONSTRAINT fk_search_history_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

-- 3. book_view_history : user 삭제 / book 삭제 → 조회 기록 삭제
ALTER TABLE book_view_history DROP CONSTRAINT IF EXISTS fk_book_view_history_user;
ALTER TABLE book_view_history
    ADD CONSTRAINT fk_book_view_history_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE book_view_history DROP CONSTRAINT IF EXISTS fk_book_view_history_book;
ALTER TABLE book_view_history
    ADD CONSTRAINT fk_book_view_history_book
        FOREIGN KEY (book_id) REFERENCES book (book_id) ON DELETE CASCADE;

-- 4. records : library 삭제 → 기록 삭제
ALTER TABLE records DROP CONSTRAINT IF EXISTS fk_records_library;
ALTER TABLE records
    ADD CONSTRAINT fk_records_library
        FOREIGN KEY (library_id) REFERENCES library (library_id) ON DELETE CASCADE;

-- 5. record_images : record 삭제 → 이미지 메타 삭제 (S3 실물은 앱에서 별도 삭제)
ALTER TABLE record_images DROP CONSTRAINT IF EXISTS fk_record_images_record;
ALTER TABLE record_images
    ADD CONSTRAINT fk_record_images_record
        FOREIGN KEY (record_id) REFERENCES records (record_id) ON DELETE CASCADE;

-- 6. focuses : library 삭제 → 포커스 삭제
ALTER TABLE focuses DROP CONSTRAINT IF EXISTS fk_focuses_library;
ALTER TABLE focuses
    ADD CONSTRAINT fk_focuses_library
        FOREIGN KEY (library_id) REFERENCES library (library_id) ON DELETE CASCADE;

-- 7. book_timelines : library 삭제 → 타임라인 삭제
ALTER TABLE book_timelines DROP CONSTRAINT IF EXISTS fk_book_timelines_library;
ALTER TABLE book_timelines
    ADD CONSTRAINT fk_book_timelines_library
        FOREIGN KEY (library_id) REFERENCES library (library_id) ON DELETE CASCADE;
