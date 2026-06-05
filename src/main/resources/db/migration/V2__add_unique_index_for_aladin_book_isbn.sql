CREATE UNIQUE INDEX ux_book_aladin_isbn13
    ON book (isbn13)
    WHERE source_type = 'ALADIN'
      AND isbn13 IS NOT NULL;
