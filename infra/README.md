# NOOK Terraform

dev, prod, monitoring은 state를 완전히 분리한다.

```text
infra/
├── bootstrap/
│   └── state/          # S3 버킷 + DynamoDB 락 테이블 (최초 1회 실행)
├── modules/
│   ├── ec2/            # EC2 + SG + IAM + EIP
│   ├── network/        # VPC + 서브넷 + IGW + 라우팅
│   └── dns/            # Route53 hosted zone + A records
└── envs/
    ├── dev/            # 기존 t3.micro EC2 import (vpc_id 변수 사용)
    ├── prod/           # network 모듈로 VPC 직접 생성 → EC2 → DNS 체이닝
    └── monitoring/     # prod VPC 공유 (vpc_id 변수로 참조)
```

## 환경별 구성

| 환경 | VPC 관리 방식 | DB | 현재 실행 범위 |
|---|---|---|---|
| dev | 기존 VPC import — vpc_id 변수 직접 지정 | Supabase | plan 검토 후 apply |
| prod | network 모듈로 신규 VPC 생성 | Supabase | plan만 |
| monitoring | prod VPC 공유 — vpc_id 변수에 prod output 사용 | 없음 | plan만 |

Supabase 접속 정보는 Terraform에서 관리하지 않는다. 각 서버의 `/secrets/.env.prod` 등에 별도 주입해 state에 DB 비밀번호가 들어가지 않게 한다.

## 모듈 의존 관계 (prod 기준)

```
modules/network  →  vpc_id, public_subnet_a_id
     ↓
modules/ec2      →  instance_id, public_ip, private_ip
     ↓
modules/dns      →  Route53 A record (enable_route53 = true 일 때만)
```

dev는 기존 VPC를 import한 상태라 network 모듈을 사용하지 않고 vpc_id를 변수로 직접 받는다.

## 0. (최초 1회) Remote State 부트스트랩

팀 협업 또는 CI 연동 전에 S3 backend를 먼저 만들어야 한다.

```bash
cd infra/bootstrap/state
terraform init
terraform apply -var="aws_account_id=123456789012"
```

완료 후 각 `envs/*/versions.tf` 의 backend 블록 주석을 해제하고 `terraform init -migrate-state` 실행.

## 1. vars 준비

예제 파일을 복사하고 실제 값으로 채운다.

```bash
cp infra/vars/prod.tfvars.example infra/vars/prod.tfvars
```

**prod** 에서 확인할 값:
- `vpc_cidr`, `public_subnet_a_cidr`: 신규 VPC이므로 원하는 CIDR 지정
- `ami_id`: Ubuntu 22.04 LTS (ap-northeast-2)
- `monitoring_cidrs`: monitoring apply 후 private IP `/32` 추가
- `enable_route53 = false` → apply 후 IP 확인 → `true` 로 변경해 DNS 생성

**monitoring** 에서 확인할 값:
- `vpc_id`: prod apply 후 출력되는 `vpc_id` output 값 사용
- `application_cidrs`: prod + dev private IP `/32`

**dev** 에서 확인할 값:
- `vpc_id`, `public_subnet_id`: 현재 EC2와 동일한 값 유지
- `existing_security_group_ids`: 전환 중 유지할 기존 SG

## 2. 기존 EC2를 dev state로 가져오기

```bash
cd infra/envs/dev
terraform init
terraform import -var-file=../../vars/dev.tfvars module.server.aws_instance.this i-xxxxxxxx
# EIP가 있다면
terraform import -var-file=../../vars/dev.tfvars 'module.server.aws_eip.this[0]' eipalloc-xxxxxxxx
```

## 3. 변경 검토 및 적용

```bash
terraform fmt -recursive ../..
terraform validate
terraform plan -var-file=../../vars/<env>.tfvars -out=<env>.tfplan
terraform show <env>.tfplan
terraform apply <env>.tfplan
```

다음 조건이면 적용 중단 후 vars/import 상태 재확인:
- 기존 EC2가 `destroy` 또는 `replace`로 표시됨
- 예상하지 않은 EBS 축소
- 현재 EIP 연결 해제
- SSH/HTTP/HTTPS 접근 경로 소멸

## 4. prod DNS 활성화 순서

Route53을 바로 활성화하면 기존 DNS 레코드가 남아 있는 동안 충돌할 수 있다.

1. `enable_route53 = false` 로 apply → prod EC2 + EIP 생성
2. `terraform output dns_name_servers` 확인 (enable 후)
3. `enable_route53 = true`, `create_route53_zone = true` 로 재apply → 호스팅 영역 생성
4. 도메인 등록 대행사(가비아 등)의 NS 레코드를 Route53 name server로 교체
5. TTL 만료 후 기존 DNS 레코드 제거

## 5. CI — Terraform Plan 자동화

`infra/**` 경로 변경이 포함된 PR이 열리면 `.github/workflows/tf-plan.yml` 이 dev/prod/monitoring 세 환경에 대해 `terraform plan` 을 실행하고 결과를 PR 코멘트로 붙인다.

**GitHub Secret 설정 (각 환경 tfvars를 base64로 인코딩):**

```bash
base64 -i infra/vars/dev.tfvars | pbcopy   # → GitHub Secret: DEV_TFVARS
base64 -i infra/vars/prod.tfvars | pbcopy  # → GitHub Secret: PROD_TFVARS
base64 -i infra/vars/monitoring.tfvars | pbcopy  # → GitHub Secret: MONITORING_TFVARS
```

## 보안 기본값

- 인터넷 공개: app 서버 80, 443만
- 관리자 CIDR만 허용: 22, Grafana 3000
- monitoring private IP만 허용: Actuator 9091, Redis Exporter 9121
- dev/prod private IP만 허용: Loki 3100
- EC2 metadata: IMDSv2 필수
- EBS: 암호화 + gp3
- T2/T3 CPU 크레딧: `unlimited` (크레딧 고갈로 인한 성능 저하 방지)
