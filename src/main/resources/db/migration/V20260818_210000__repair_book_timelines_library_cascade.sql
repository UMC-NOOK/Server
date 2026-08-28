-- Hibernate ddl-auto가 생성한 이름 없는 FK가 남아 있으면 V5의 명명된 FK와 중복될 수 있다.
-- 모든 book_timelines.library_id -> library FK를 제거한 뒤, CASCADE FK 하나만 다시 생성한다.
DO $$
DECLARE
    constraint_name TEXT;
BEGIN
    FOR constraint_name IN
        SELECT conname
        FROM pg_constraint
        WHERE contype = 'f'
          AND conrelid = 'book_timelines'::regclass
          AND confrelid = 'library'::regclass
    LOOP
        EXECUTE format('ALTER TABLE book_timelines DROP CONSTRAINT %I', constraint_name);
    END LOOP;
END $$;

ALTER TABLE book_timelines
    ADD CONSTRAINT fk_book_timelines_library
        FOREIGN KEY (library_id) REFERENCES library (library_id) ON DELETE CASCADE;
