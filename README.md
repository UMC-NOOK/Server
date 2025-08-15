# NOOK Server

**NOOK** 프로젝트의 백엔드 레포지토리입니다.  
Spring Boot 기반의 서버로 구성되어 있으며, 책과 독서 활동을 중심으로 한 독서 플랫폼의 핵심 기능을 제공합니다.

## 🛠 Tech Stack
<img src="https://skillicons.dev/icons?i=spring,mysql,redis,aws,docker,github,gradle" height="50">

---

## 📚 목차

1. [프로젝트 소개](#-프로젝트-소개)
2. [백엔드 팀원 소개](#-백엔드-팀원-소개)
3. [기술 스택](#-기술-스택)
4. [ERD](#-erd)
5. [서버 아키텍처](#-서버-아키텍처)
6. [프로젝트 구조](#-프로젝트-구조)
7. [브랜치 전략](#-브랜치-전략)
8. [Github 관리 규칙](#-github-관리-규칙)

---

## 📖 프로젝트 소개

**NOOK**는 독서 기록, 서재 관리, 독서 모임(리딩룸), 책 리뷰 등 다양한 독서 활동을 지원하는 서비스입니다.  
사용자는 자신의 독서 활동을 기록하고 공유하며, 다른 사용자들과 소통할 수 있습니다.

**주요 기능**
- 개인 서재 및 독서 상태 관리 (읽는 중 / 완독 / 찜)
- 독서 기록 작성 및 AI와의 대화를 통한 독서 감상문 생성
- 리딩룸(독서 모임) 기능
- 책 리뷰 및 평점 기능
- 라운지(책 검색 및 조회) 기능

---

## 👤 백엔드 팀원 소개

<div align="center">

|                                Backend                                |                               Backend                               |                                Backend                                 |
|:---------------------------------------------------------------------:|:-------------------------------------------------------------------:|:----------------------------------------------------------------------:|
|      <img src="https://github.com/JiwonLee42.png" width="150" />      | <img src="https://github.com/kimdanha.png" width="150" /> | <img src="https://github.com/kjhyeon0620.png" width="150" /> |
| [이지원](https://github.com/JiwonLee42)<br />서재, 회원, 독서 기록 기능 개발 및 서버 배포 | [김단하](https://github.com/kimdanha)<br />리딩룸 기능 개발 | [김주현](https://github.com/kjhyeon0620)<br />라운지, 책 리뷰 기능 개발 |

</div>

---

## 🛠 기술 스택

| 구분 | 기술 |
|------|------|
| **Language** | Java 17 |
| **Framework** | Spring Boot 3.x |
| **ORM** | Spring Data JPA, QueryDSL |
| **Database** | MySQL, Redis |
| **Build Tool** | Gradle |
| **API Docs** | SpringDoc OpenAPI (Swagger) |
| **Infra** | AWS EC2, RDS |
| **Deploy** | Docker, GitHub Actions (CI/CD) |
| **기타** | JWT 인증, OAuth2(Kakao) |

## 🗂 ERD

> 프로젝트의 데이터베이스 구조입니다.

<img width="2110" alt="ERD" src="https://github.com/user-attachments/assets/004d1c07-f797-4cde-ba58-64879caa3304" />

---

## 🖥 서버 아키텍처

> NOOK 서비스의 서버 구성도입니다.

<img width="1229" alt="Server Architecture" src="https://github.com/user-attachments/assets/57c9179a-341e-4405-a54b-7f2e4d56365e" />

---

## 📂 프로젝트 구조

 ```plaintext
src
 └── main
     ├── java                     # 애플리케이션 소스 코드
     │   └── umc
     │       └── nook
     │           ├── BaseTimeEntity.java
     │           ├── NookApplication.java
     │           ├── aladin         # API 연동 모듈
     │           ├── book           # 책 정보 도메인
     │           ├── bookshelves    # 서재 관련 기능
     │           ├── common         # 공통 설정, 예외 처리
     │           ├── lounge         # 라운지
     │           ├── profile        # 사용자 프로필
     │           ├── readingrooms   # 리딩룸
     │           ├── records        # 독서 기록
     │           ├── review         # 책 리뷰
     │           ├── search         # 검색 기능
     │           └── users          # 회원 및 인증
```
## 🌿 브랜치 전략

### Workflow: **Gitflow Workflow**

- `develop` – 모든 기능 통합 브랜치
- `feature/*` – 기능 단위 개발 브랜치
- `hotfix/*` – 긴급 수정 브랜치
- `release/*` – 배포용 브랜치

> 기능 개발 시 `develop` 브랜치에서 파생된 `feature/` 브랜치에서 작업합니다.  
> 완료되면 Pull Request를 통해 병합하세요.

---

## 📍 Github 관리 규칙

- `develop` 브랜치에 **직접 커밋 금지**
- 작업 전 반드시 **issue 등록**
- PR 생성 시 해당 **이슈 연결**
- **모든 PR은 코드리뷰 후 merge**
- 기능은 **작은 단위**로 나눠 브랜치 생성
- 기능 완료 후 PR → 리뷰 → develop 병합
