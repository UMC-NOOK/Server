-- 카테고리를 YES24 기준(국내도서 20개)으로 정리한다.
-- 1) 컴퓨터/모바일 -> IT
-- 2) 삭제할 카테고리(EBOOK 전체, 고전, 좋은부모)를 가리키는 도서·사용자 선호를 BOOK 카테고리로 옮긴 뒤 삭제
-- 3) 알라딘 카테고리 ID를 맞춤 추천용 YES24 베스트셀러 카테고리 코드로 교체

UPDATE category
SET category_name = 'IT'
WHERE mall_type = 'BOOK'
  AND category_name = '컴퓨터/모바일';

CREATE TEMP TABLE category_move AS
SELECT src.category_id AS src_id,
       dst.category_id AS dst_id
FROM category src
JOIN category dst
  ON dst.mall_type = 'BOOK'
 AND dst.category_name = CASE src.category_name
                             WHEN '고전' THEN '소설/시/희곡'
                             WHEN '좋은부모' THEN '가정/요리/뷰티'
                             WHEN '컴퓨터/모바일' THEN 'IT'
                             ELSE src.category_name
                         END
WHERE src.mall_type = 'EBOOK'
   OR src.category_name IN ('고전', '좋은부모');

UPDATE book b
SET category_id = m.dst_id
FROM category_move m
WHERE b.category_id = m.src_id;

UPDATE users u
SET preferred_category_id = m.dst_id
FROM category_move m
WHERE u.preferred_category_id = m.src_id;

DELETE FROM category c
USING category_move m
WHERE c.category_id = m.src_id;

DROP TABLE category_move;

ALTER TABLE category ADD COLUMN yes24_category_id VARCHAR(12);

UPDATE category
SET yes24_category_id = CASE category_name
    WHEN '가정/요리/뷰티' THEN '001001001'
    WHEN '건강/취미/레저' THEN '001001011'
    WHEN '경제경영' THEN '001001025'
    WHEN '과학' THEN '001001002'
    WHEN '전문서적' THEN '001001014'
    WHEN '만화' THEN '001001008'
    WHEN '사회과학' THEN '001001022'
    WHEN '소설/시/희곡' THEN '001001046'
    WHEN '어린이' THEN '001001016'
    WHEN '에세이' THEN '001001047'
    WHEN '여행' THEN '001001009'
    WHEN '역사' THEN '001001010'
    WHEN '예술/문화' THEN '001001007'
    WHEN '외국어' THEN '001001004'
    WHEN '유아' THEN '001001027'
    WHEN '인문학' THEN '001001019'
    WHEN '자기계발' THEN '001001026'
    WHEN '종교/역학' THEN '001001021'
    WHEN '청소년' THEN '001001005'
    WHEN 'IT' THEN '001001003'
END;

ALTER TABLE category ALTER COLUMN yes24_category_id SET NOT NULL;

ALTER TABLE category DROP COLUMN aladin_category_id;
