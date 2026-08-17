# Flyway 마이그레이션 가이드

NOOK 서버의 PostgreSQL 스키마는 Flyway 마이그레이션으로 관리합니다. 모든 마이그레이션은 `src/main/resources/db/migration`에 둡니다.

## 버전 규칙

신규 마이그레이션은 UTC 기준 날짜와 시간을 사용합니다.

```text
VyyyyMMdd_HHmmss__snake_case_description.sql
```

예시는 다음과 같습니다.

```text
V20260814_063000__add_library_status_index.sql
```

- `yyyyMMdd`는 UTC 날짜입니다.
- `HHmmss`는 UTC 시간입니다.
- 설명은 변경 목적이 드러나는 영문 소문자 `snake_case`로 작성합니다.
- 동일한 초에 여러 마이그레이션을 만들었다면 각각 다른 타임스탬프를 사용합니다.

기존 `V1`부터 `V6`까지는 날짜 규칙 도입 전에 생성된 레거시 마이그레이션입니다. Flyway 적용 이력을 보존하기 위해 이름과 내용을 변경하지 않습니다.

## 파일 생성

저장소 루트에서 다음 명령을 실행하고 `description`을 실제 변경 내용으로 바꿉니다.

```bash
touch "src/main/resources/db/migration/V$(date -u +%Y%m%d_%H%M%S)__description.sql"
```

예를 들어 서재 상태 인덱스를 추가한다면 다음과 같이 생성합니다.

```bash
touch "src/main/resources/db/migration/V$(date -u +%Y%m%d_%H%M%S)__add_library_status_index.sql"
```

## 작성 원칙

1. 하나의 마이그레이션에는 하나의 명확한 목적만 담습니다.
2. 공유 브랜치에 병합된 마이그레이션은 수정, 삭제하거나 이름을 바꾸지 않습니다.
3. 이미 적용된 변경을 보완해야 한다면 더 높은 버전의 새 마이그레이션으로 roll-forward 합니다.
4. 애플리케이션 코드와 스키마 변경의 배포 순서를 고려해 이전 버전과의 호환성을 유지합니다.
5. Flyway의 `out-of-order` 옵션은 활성화하지 않습니다.

## PR 전 검증

먼저 작업 브랜치를 대상 브랜치의 최신 상태로 갱신합니다. 그다음 대상 브랜치를 인자로 전달해 마이그레이션을 검증합니다.

```bash
./scripts/validate-flyway-migrations.sh origin/develop-demo
./gradlew clean build
```

검증 스크립트는 다음 조건을 확인합니다.

- 신규 파일이 날짜·시간 버전 형식을 따르는지
- 날짜와 시간이 실제로 유효한지
- 버전이 중복되지 않았는지
- 기존 마이그레이션이 변경 또는 삭제되지 않았는지
- 신규 버전이 대상 브랜치의 최신 버전보다 큰지

오래된 작업 브랜치의 버전이 대상 브랜치의 최신 버전보다 낮다면, 아직 공유 환경에 적용되지 않았는지 확인한 뒤 현재 UTC 시각으로 파일명을 다시 생성합니다. 공유 환경에 적용된 파일은 이름을 바꾸지 않고 새 마이그레이션으로 보완합니다.

GitHub Actions에서도 Pull Request 대상 커밋을 기준으로 같은 검증을 실행합니다.

## 실패 대응

- 파일명 오류: `VyyyyMMdd_HHmmss__snake_case_description.sql` 형식으로 수정합니다.
- 중복 또는 낮은 버전: 대상 브랜치를 최신화한 뒤 현재 UTC 시각으로 버전을 다시 생성합니다.
- 기존 파일 변경: 변경을 되돌리고 새 마이그레이션으로 작성합니다.
- 적용 실패: 실패 원인을 수정한 새 마이그레이션으로 roll-forward 합니다. 공유 DB의 `flyway_schema_history`를 임의로 수정하지 않습니다.
