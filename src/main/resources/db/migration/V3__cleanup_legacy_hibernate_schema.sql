ALTER TABLE book DROP COLUMN IF EXISTS cover_image_url;
ALTER TABLE users DROP COLUMN IF EXISTS profile_url;

UPDATE library
SET page = 0
WHERE page IS NULL;

ALTER TABLE library ALTER COLUMN page SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_category_mall_type_name'
    ) THEN
        ALTER TABLE category
            ADD CONSTRAINT uk_category_mall_type_name UNIQUE (mall_type, category_name);
    END IF;
END $$;

ALTER TABLE book DROP CONSTRAINT IF EXISTS book_source_type_check;
ALTER TABLE book_timelines DROP CONSTRAINT IF EXISTS book_timelines_type_check;
ALTER TABLE category DROP CONSTRAINT IF EXISTS category_mall_type_check;
ALTER TABLE library DROP CONSTRAINT IF EXISTS library_reading_status_check;
ALTER TABLE records DROP CONSTRAINT IF EXISTS records_emotion_check;
ALTER TABLE search_history DROP CONSTRAINT IF EXISTS search_history_search_type_check;
ALTER TABLE themes DROP CONSTRAINT IF EXISTS themes_name_check;
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_status_check;
