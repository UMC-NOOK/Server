-- 알라딘 API로 저장된 도서(표지·구매 링크·소개글이 알라딘 정보)를 30일 갱신 대상으로 만든다.
-- 다음 상세 조회 때 YES24 정보로 갱신되고, YES24에서 찾지 못하거나 제외되는 도서(전자책 등)는 기존 정보를 유지한다.
-- 배포 시 YES24를 일괄 호출하지 않는다.

UPDATE book
SET modified_date = now() - interval '31 days'
WHERE source_type = 'EXTERNAL'
  AND purchase_link LIKE '%aladin.co.kr%';
