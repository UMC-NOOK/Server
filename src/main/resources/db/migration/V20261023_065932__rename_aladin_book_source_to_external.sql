-- 외부 도서 공급자를 알라딘에서 YES24로 바꾸면서 도서 출처 명칭을 공급자 중립 이름으로 바꾼다.
-- 기존 알라딘 도서는 삭제하지 않고 EXTERNAL 도서로 유지한다.

UPDATE book
SET source_type = 'EXTERNAL'
WHERE source_type = 'ALADIN';

ALTER TABLE book RENAME COLUMN aladin_link TO purchase_link;

DROP INDEX IF EXISTS ux_book_aladin_isbn13;

CREATE UNIQUE INDEX ux_book_external_isbn13
    ON book (isbn13)
    WHERE source_type = 'EXTERNAL'
      AND isbn13 IS NOT NULL;
