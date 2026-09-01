# NOOK Server

**NOOK** 프로젝트의 백엔드 레포지토리입니다.  
Spring Boot 기반의 서버로 구성되어 있으며, 책과 독서 활동을 중심으로 한 독서 플랫폼의 핵심 기능을 제공합니다.

## 프로젝트 진행 기간
**1차**
UMC 8기 데모데이
2025.06 ~ 2025.08

**2차**
런칭 준비
2025.09 ~ ing

## 🛠 Tech Stack
<p>
  <strong>Language</strong><br />
  <img src="https://img.shields.io/badge/Java_17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" />
</p>
<p>
  <strong>Framework / Security / Test</strong><br />
  <img src="https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" />
  <img src="https://img.shields.io/badge/Spring_Data_JPA-6DB33F?style=for-the-badge&logo=spring&logoColor=white" />
  <img src="https://img.shields.io/badge/QueryDSL-005571?style=for-the-badge&logoColor=white" />
  <img src="https://img.shields.io/badge/Spring_Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white" />
  <img src="https://img.shields.io/badge/Actuator-6DB33F?style=for-the-badge&logo=spring&logoColor=white" />
  <img src="https://img.shields.io/badge/JUnit_5-25A162?style=for-the-badge&logo=junit5&logoColor=white" />
</p>
<p>
  <strong>Deploy</strong><br />
  <img src="https://img.shields.io/badge/Nginx-009639?style=for-the-badge&logo=nginx&logoColor=white" />
  <img src="https://img.shields.io/badge/AWS-ECR%20%7C%20EC2-232F3E?style=for-the-badge&logo=amazonaws&logoColor=white" />
  <img src="https://img.shields.io/badge/GitHub_Actions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white" />
  <img src="https://img.shields.io/badge/Loki-FCC624?style=for-the-badge&logo=grafana&logoColor=black" />
  <img src="https://img.shields.io/badge/Grafana-F46800?style=for-the-badge&logo=grafana&logoColor=white" />
  <img src="https://img.shields.io/badge/Prometheus-E6522C?style=for-the-badge&logo=prometheus&logoColor=white" />
</p>
<p>
  <strong>Database</strong><br />
  <img src="https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white" />
  <img src="https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white" />
</p>
<p>
  <strong>ETC</strong><br />
  <img src="https://img.shields.io/badge/REST_Docs-000000?style=for-the-badge&logo=asciidoctor&logoColor=white" />
  <img src="https://img.shields.io/badge/Swagger-85EA2D?style=for-the-badge&logo=swagger&logoColor=black" />
  <img src="https://img.shields.io/badge/JaCoCo-B4A76C?style=for-the-badge&logoColor=white" />
</p>

---

## 📚 목차

1. [프로젝트 소개](#-프로젝트-소개)
2. [백엔드 팀원 소개](#-백엔드-팀원-소개)
3. [기술 스택](#-기술-스택)
4. [ERD](#-erd)
5. [서버 아키텍처](#-서버-아키텍처)
6. [프로젝트 구조](#-프로젝트-구조)
7. [브랜치 전략](#-브랜치-전략)
8. [데이터베이스 마이그레이션](#-데이터베이스-마이그레이션)
9. [Github 관리 규칙](#-github-관리-규칙)

---

## 📖 프로젝트 소개

**NOOK**는 독서 기록과 서재 관리에 집중한 독서 플랫폼입니다.
사용자는 책을 검색하고 서재에 담은 뒤, 독서 기록과 집중 기록을 남기고 자신의 독서 흐름을 타임라인으로 확인할 수 있습니다.

**주요 기능**
- 개인 서재 및 독서 상태 관리
- 독서 기록 작성, 조회, 삭제
- 포커스 시작/종료 및 월별 통계 조회
- 도서 검색, 추천 도서 및 베스트셀러 조회
- 온보딩, OAuth 로그인, JWT 기반 인증
- Cloudflare R2 presigned URL 기반 이미지 업로드

---

## 👤 백엔드 팀원 소개

<div align="center">

|                                         Backend                                         |                                  Backend                                  |
|:---------------------------------------------------------------------------------------:|:-------------------------------------------------------------------------:|
|               <img src="https://github.com/JiwonLee42.png" width="150" />               |       <img src="https://github.com/kjhyeon0620.png" width="150" />        |
| [이지원](https://github.com/JiwonLee42)<br />서재 및 포커스 홈 화면 통계<br> 독서기록 기능 <br>회원 관련 기능(소셜 로그인 등)<br>사진 업로드 기능 | [김주현](https://github.com/kjhyeon0620)<br />도서 검색 및 등록(알라딘 API 사용) <br>온보딩<br> 책 상세 및 타임라인 조회 |

</div>

---

## 🛠 기술 스택

| 카테고리 | 기술 |
|------|------|
| **Language** | Java 17 |
| **Framework** | Spring Boot, Spring Data JPA, QueryDSL |
| **Security** | Spring Security, JWT, OAuth2 |
| **Database** | PostgreSQL(Supabase), Redis |
| **Test** | JUnit 5, H2(Test), JaCoCo |
| **Monitoring** | Spring Actuator, Prometheus, Grafana, Loki |
| **Deployment / Infra** | Nginx, AWS, Docker, GitHub Actions |
| **Documentation** | Spring REST Docs, Swagger(OpenAPI) |
| **Storage / External API** | Cloudflare R2, Aladin API |

## 🗂 ERD

> 프로젝트의 데이터베이스 구조입니다.

<img width="2110" alt="ERD" src="https://github.com/user-attachments/assets/004d1c07-f797-4cde-ba58-64879caa3304" />

---

## 🖥 서버 아키텍처

> NOOK 서비스의 서버 구성도입니다.(이전 버전, 추후 수정 예정)

<img width="1229" alt="Server Architecture" src="https://github.com/user-attachments/assets/57c9179a-341e-4405-a54b-7f2e4d56365e" />

---

## 📂 프로젝트 구조

```plaintext
src
 └── main
     ├── java
     │   └── app
     │       └── nook
     │           ├── NookApplication.java
     │           ├── aladin         # 알라딘 API 연동
     │           ├── book           # 도서, 추천, 검색, 카테고리
     │           ├── focus          # 집중 세션/테마
     │           ├── global         # 공통 설정, 응답, 예외, API 버전
     │           ├── group          # 그룹 관련 확장 패키지
     │           ├── library        # 서재, 독서 상태, 통계
     │           ├── r2             # 이미지 presigned URL 발급
     │           ├── record         # 독서 기록, 감정, 기록 이미지
     │           ├── redis          # Redis 연동 유틸/서비스
     │           ├── search         # 검색 관련 확장 패키지
     │           ├── timeline       # 서재 타임라인
     │           └── user           # 회원, 인증, OAuth, JWT, 온보딩
     └── resources
         └── application.yml
```

도메인 내부는 대체로 아래 구조를 따릅니다.

```plaintext
{domain}
 ├── controller
 ├── service
 ├── repository
 ├── domain
 ├── dto
 ├── converter
 ├── exception
 └── event
```

추가로 현재 API는 `@Api1Version` 기반으로 `/api/v1/**` prefix를 사용합니다.

## 🌿 브랜치 전략

### Workflow

- 기본 통합 브랜치: `develop-demo`
- 기능 개발 브랜치: `feature/*`
- 긴급 수정 브랜치: `hotfix/*`

> 기능 개발 시 통합 브랜치에서 파생된 기능 브랜치에서 작업합니다.
> 완료되면 Pull Request를 통해 리뷰 후 병합합니다.

---

## 🗄 데이터베이스 마이그레이션

데이터베이스 스키마는 Flyway로 관리합니다. 마이그레이션 생성과 버전 관리 규칙은 [Flyway 마이그레이션 가이드](docs/flyway-migration-guide.md)를 참고해 주세요.

---

## 📍 Github 관리 규칙

- 기본 API 문서는 Swagger(`/swagger-ui/index.html`)와 REST Docs(`/docs/index.html`)로 관리
- `/api/**` 경로는 기본적으로 인증 필요, `/api/v1/auth/**` 등 일부 공개 경로만 예외
- 작업 전 이슈를 등록하고 PR에서 연결
- 모든 PR은 코드리뷰 후 merge
- 테스트와 문서 생성을 위해 `./gradlew clean build` 기준으로 검증
- 컨트롤러는 얇게 유지하고 비즈니스 로직은 서비스 계층에 배치
