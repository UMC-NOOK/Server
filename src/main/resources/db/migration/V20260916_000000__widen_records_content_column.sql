-- content 검증(@Length max=1000)과 DB 컬럼(VARCHAR(255))이 어긋나 있어서,
-- 255자를 넘는 기록 생성/수정이 "value too long for type character varying(255)"로 실패했다.
-- 검증 상한과 동일하게 컬럼을 넓힌다.
ALTER TABLE records
    ALTER COLUMN content TYPE VARCHAR(1000);
