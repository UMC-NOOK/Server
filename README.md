# NOOK Server

**NOOK** 프로젝트의 백엔드 레포지토리입니다.  
Spring Boot 기반의 서버로 구성되어 있습니다.

---

## 👤 Backend Developers

<div align="center">

|                                Backend                                |                               Backend                               |                                Backend                                 |
|:---------------------------------------------------------------------:|:-------------------------------------------------------------------:|:----------------------------------------------------------------------:|
| <img src="https://github.com/JiwonLee42.png" width="150" /> | <img src="https://github.com/kimdanha.png" width="150" /> | <img src="https://github.com/kjhyeon0620.png" width="150" /> |
| [이지원](https://github.com/JiwonLee42)<br />서재, 회원 기능 개발 및 배포 | [김단하](https://github.com/kimdanha)<br />리딩룸 기능 개발 | [김주현](https://github.com/kjhyeon0620)<br />라운지, 책 리뷰 기능 개발 |

</div>

---

## ✉️ Commit Message Rules

> 서버 개발을 위한 **Git Commit Convention**입니다.

- 구현 도중에도 **작은 단위로 자주 커밋**해 주세요.
- 기능이 완성되지 않았더라도 **자신의 브랜치에 커밋**은 가능합니다.

### 📌 Commit Convention

- 형식: `[태그] 커밋 내용`
- 태그는 **영문 대문자**로 작성합니다.

| 태그       | 설명                                 |
|------------|--------------------------------------|
| FEAT       | 새로운 기능 추가                     |
| FIX        | 버그 수정                            |
| CHORE      | 기타 자잘한 수정 (빌드, 설정 등)     |
| DOCS       | 문서 수정 (README, 주석 등)          |
| INIT       | 초기 설정 (프로젝트 초기 구조 세팅)  |
| TEST       | 테스트 코드 추가 / 수정              |
| RENAME     | 파일, 폴더명 변경                    |
| STYLE      | 코드 포맷팅, 세미콜론, 코드 변화 없음 |
| REFACTOR   | 코드 리팩토링 (기능 변화 없음)       |

#### ✅ 예시

#### ✅ 예시

[FEAT] 검색 API 추가
[FIX] Redis 연결 오류 해결
[REFACTOR] JWT 필터 구조 개선

---

## 💻 Github Management

### ✅ Workflow: **Gitflow Workflow**

- `develop` – 모든 기능 통합 브랜치
- `feature/*` – 기능 단위 개발 브랜치
- `hotfix/*` – 긴급 수정 브랜치
- `release/*` – 배포용 브랜치

> 기능 개발 시 `develop` 브랜치에서 파생된 `feature/` 브랜치에서 작업합니다.  
> 완료되면 Pull Request를 통해 병합하세요.

---

### 📍 Gitflow 규칙

- `develop` 브랜치에 **직접 커밋 금지**
- 작업 전 반드시 **issue 등록**
- PR 생성 시 해당 **이슈 연결**
- **모든 PR은 코드리뷰를 거쳐 merge**
- 기능은 **작은 단위**로 나눠 브랜치 생성
- 기능 완료 후 PR → 리뷰 후 develop에 merge

---

### ❗️ Branch Naming Convention

| 브랜치 유형 | 규칙 예시                            |
|-------------|--------------------------------------|
| develop     | `develop`                            |
| feature     | `feature/#3-user-post-api`           |
| fix         | `fix/#12-schedule-get-api`           |
| release     | `release/1.0.0`                      |
| hotfix      | `hotfix/#20-login-expire-fix`        |

---
