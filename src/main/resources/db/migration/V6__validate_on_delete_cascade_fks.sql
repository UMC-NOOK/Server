-- V5 에서 NOT VALID 로 추가한 FK 들을 검증한다.
-- VALIDATE CONSTRAINT 는 SHARE UPDATE EXCLUSIVE 락만 잡아 읽기/쓰기를 막지 않는다.

ALTER TABLE library VALIDATE CONSTRAINT fk_library_user;
ALTER TABLE library VALIDATE CONSTRAINT fk_library_book;

ALTER TABLE search_history VALIDATE CONSTRAINT fk_search_history_user;

ALTER TABLE book_view_history VALIDATE CONSTRAINT fk_book_view_history_user;
ALTER TABLE book_view_history VALIDATE CONSTRAINT fk_book_view_history_book;

ALTER TABLE records VALIDATE CONSTRAINT fk_records_library;

ALTER TABLE record_images VALIDATE CONSTRAINT fk_record_images_record;

ALTER TABLE focuses VALIDATE CONSTRAINT fk_focuses_library;

ALTER TABLE book_timelines VALIDATE CONSTRAINT fk_book_timelines_library;
