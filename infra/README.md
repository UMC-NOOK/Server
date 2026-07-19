# NOOK Terraform

dev, prod, monitoring은 코드와 state를 완전히 분리한다. 현재 적용 대상은 기존 EC2를
전환하는 **dev만**이다. prod와 monitoring은 코드와 `plan`까지만 준비하고 승인 전에는
`apply`하지 않는다.

```text
infra/
├── modules/
│   ├── ec2/
│   └── rds/
├── envs/
│   ├── dev/               # 기존 t3.micro EC2를 그대로 import
│   ├── prod/              # 신규 EC2 + private PostgreSQL RDS, 적용 보류
│   └── monitoring/        # 신규 EC2, 적용 보류
└── vars/                  # 환경별 변수
```

## 환경별 구성

| 환경 | Terraform 리소스 | DB | 현재 실행 범위 |
|---|---|---|---|
| dev | 기존 `t3.micro` EC2를 import, 관리 SG/IAM/EIP | 기존 Supabase 유지 | `plan` 검토 후 `apply` |
| prod | 신규 `t3.small`, private RDS | PostgreSQL RDS | `plan`만 |
| monitoring | 신규 `t3.small` | 없음 | 코드만 보관 |

Supabase 접속 정보는 Terraform에서 관리하지 않는다. 서버의 `/secrets/.env.dev`에 별도로
주입하여 Terraform state에 DB 비밀번호가 들어가지 않게 한다. prod RDS의 마스터 비밀번호는
AWS가 생성하여 Secrets Manager에 저장한다.

## 1. vars 준비

예제 파일을 복사하고 실제 AWS 값으로 바꾼다. 실제 `.tfvars`와 로컬 state 파일은 Git에서
제외된다.

```bash
cp infra/vars/dev.tfvars.example infra/vars/dev.tfvars
```

dev에서 특히 확인할 값은 다음과 같다.

- `ami_id`, `vpc_id`, `public_subnet_id`: 현재 EC2의 값과 동일하게 지정
- `instance_type`: 기존 서버와 동일하게 `t3.micro` 유지
- `root_volume_size`: 현재 볼륨보다 작게 지정하지 않음
- `existing_security_group_ids`: 전환 중 유지할 현재 SG ID
- `create_eip`: 현재 EIP가 있거나 새 고정 IP를 사용할 때 `true`
- `monitoring_cidrs`: monitoring 보류 중에는 `[]`

각 환경은 별도 디렉터리의 로컬 state를 사용한다.

```text
infra/envs/dev/terraform.tfstate
infra/envs/prod/terraform.tfstate
infra/envs/monitoring/terraform.tfstate
```

state에는 리소스 정보와 민감한 값이 포함될 수 있으므로 Git에 커밋하지 않고 안전하게
백업해야 한다. 팀에서 동시에 Terraform을 실행하지 않는다.

## 2. 기존 EC2를 dev state로 가져오기

아래 명령의 ID를 현재 서버 값으로 교체한다. 먼저 instance만 import한다. 모듈이 새 관리용
SG와 IAM role을 만들며, `existing_security_group_ids`에 넣은 기존 SG는 전환 중 함께 유지한다.

```bash
cd infra/envs/dev
terraform init
terraform import -var-file=../../vars/dev.tfvars module.server.aws_instance.this i-xxxxxxxx
```

현재 서버가 EIP를 사용하고 `create_eip = true`라면 기존 EIP도 반드시 import한다. 여기에는
공인 IP가 아니라 allocation ID(`eipalloc-...`)를 사용한다.

```bash
terraform import -var-file=../../vars/dev.tfvars 'module.server.aws_eip.this[0]' eipalloc-xxxxxxxx
```

EIP가 없는데 `create_eip = true`이면 새 EIP가 생성된다. 인스턴스의 기존 일반 공인 IP는
바뀌므로 DNS 전환 계획을 먼저 확인한다.

## 3. dev 변경 검토 및 적용

```bash
terraform fmt -recursive ../..
terraform validate
terraform plan -var-file=../../vars/dev.tfvars -out=dev.tfplan
terraform show dev.tfplan
terraform apply dev.tfplan
```

다음 조건이면 적용을 중단하고 vars/import 상태를 다시 확인한다.

- 기존 EC2가 `destroy` 또는 `replace`로 표시됨
- 예상하지 않은 EBS 축소가 표시됨
- 현재 EIP 연결 해제가 표시됨
- SSH/HTTP/HTTPS 접근 경로가 사라짐

현재 dev는 `t3.micro`를 유지하므로 정상적인 plan에는 인스턴스 타입 변경이 없어야 한다.
기존 EC2에 대한 `user_data`는 import만으로 다시 실행되지 않을 수 있으므로 Docker 설치
상태는 별도로 확인한다.

전환이 끝나고 새 관리 SG의 22/80/443 접근을 검증한 뒤에만
`existing_security_group_ids = []`로 바꾸고 다시 적용한다.

## 4. prod와 monitoring은 plan까지만

prod는 신규 `t3.small` EC2와 private PostgreSQL RDS를 함께 만든다. RDS는 서로 다른 AZ의
private subnet 두 개 이상을 요구한다.

```bash
cp infra/vars/prod.tfvars.example infra/vars/prod.tfvars
cd infra/envs/prod
terraform init
terraform plan -var-file=../../vars/prod.tfvars
```

monitoring도 동일하게 준비할 수 있지만 현재는 `apply`하지 않는다.

```bash
cp infra/vars/monitoring.tfvars.example infra/vars/monitoring.tfvars
cd infra/envs/monitoring
terraform init
terraform plan -var-file=../../vars/monitoring.tfvars
```

monitoring을 실제 생성한 뒤 dev/prod의 `monitoring_cidrs`에 monitoring private IP `/32`를
넣으면 9091이 열린다. 그 전에는 9091 규칙 자체가 생성되지 않는다. 반대로 monitoring의
`application_cidrs`에는 dev/prod private IP `/32`를 넣어 Loki 3100 접근만 허용한다.

## 보안 기본값

- 인터넷 공개: app 서버의 80, 443만
- 관리자 CIDR만 허용: 22, Grafana 3000
- monitoring private IP만 허용: Actuator 9091
- dev/prod private IP만 허용: Loki 3100
- prod EC2 SG만 허용: RDS 5432
- EC2 metadata: IMDSv2 필수
- EBS/RDS: 암호화 활성화
- prod RDS: public access 비활성화, 삭제 방지 및 final snapshot 활성화

DNS는 사용하는 도메인 제공자에서 `dev.<domain>`을 dev EIP로, `api.<domain>`을 prod
EIP로 연결한다. dev는 EC2 호스트에 이미 설치된 nginx와 Certbot을 사용하고, prod는 Docker
nginx와 Certbot을 사용한다.

실제 `apply` 전에는 AWS 자격 증명과 대상 account ID가 일치하는지 확인해야 한다. provider의
`allowed_account_ids`가 다른 계정에 실수로 적용하는 것을 한 번 더 막는다.
