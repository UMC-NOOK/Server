SHELL := /usr/bin/env bash

ENV ?= local
K6_ENV ?= $(ENV)
JOURNEYS_PER_SECOND ?= 1
GRAFANA_PORT ?= 3001
ENV_FILE ?= performance/k6/env/monitoring.env

K6_RUNNER := performance/k6/scripts/run-k6.sh
K6_ENV_FILE := performance/k6/env/$(K6_ENV).env
K6_MIXED_READ_PROFILE := jps$(JOURNEYS_PER_SECOND)

K6_OPTIONAL_ENV := BASE_URL MANAGEMENT_BASE_URL TOKEN K6_ACCESS_TOKEN K6_REFRESH_TOKEN K6_USER_EMAIL K6_USER_NICKNAME \
	SEED_RUN_ID RUN_ID TARGET_RPS DURATION PRE_ALLOCATED_VUS MAX_VUS P95_THRESHOLD_MS FAILED_RATE_THRESHOLD MAX_DROPPED_ITERATIONS \
	VUS ITERATIONS MAX_DURATION K6_BOOK_ID K6_LIBRARY_ID K6_RECORD_ID K6_TIMELINE_ID K6_SEARCH_KEYWORD K6_DRY_RUN \
	K6_ENABLE_EXTERNAL_API K6_GLOBAL_SEARCH_KEYWORDS K6_GLOBAL_P95_THRESHOLD_MS K6_GLOBAL_FAILED_RATE_THRESHOLD K6_GLOBAL_USER_POOL_SIZE \
	SEED_BOOKS SEED_RECORDS_PER_BOOK SEED_FOCUS_SESSIONS SEED_BOOK_TITLE_PREFIX SEED_RECORD_PREFIX \
	K6_REQUIRE_CONFIRM K6_PROD_BASE_URL_PATTERN CONFIRM_PROD_LOADTEST
K6_EXTRA_ENV := $(foreach var,$(K6_OPTIONAL_ENV),$(if $($(var)),$(var)='$($(var))'))

.PHONY: k6-up k6-down k6-smoke k6-seed k6-mixed-read k6-dry-mixed-read k6-global-search k6-scenario k6-test-options k6-env-init k6-guard-prod

k6-env-init:
	@mkdir -p performance/k6/env performance/k6/state
	@if [[ ! -f "$(ENV_FILE)" && -f performance/k6/env/monitoring.env.example ]]; then \
		cp performance/k6/env/monitoring.env.example "$(ENV_FILE)"; \
		echo "created $(ENV_FILE)"; \
	fi
	@if [[ ! -f "$(K6_ENV_FILE)" && -f performance/k6/env/$(K6_ENV).env.example ]]; then \
		cp performance/k6/env/$(K6_ENV).env.example "$(K6_ENV_FILE)"; \
		echo "created $(K6_ENV_FILE)"; \
	fi

k6-guard-prod:
	@if [[ "$(K6_ENV)" == "prod" && "$(CONFIRM_PROD_LOADTEST)" != "yes" ]]; then \
		echo "error: prod load test requires CONFIRM_PROD_LOADTEST=yes" >&2; \
		exit 2; \
	fi

k6-up: k6-env-init
	@ENV_FILE="$(ENV_FILE)" GRAFANA_PORT="$(GRAFANA_PORT)" \
		docker compose -f docker-compose.monitoring.yml --profile apm up -d

k6-down:
	@ENV_FILE="$(ENV_FILE)" GRAFANA_PORT="$(GRAFANA_PORT)" \
		docker compose -f docker-compose.monitoring.yml --profile apm down

k6-smoke: k6-env-init k6-guard-prod
	@$(K6_EXTRA_ENV) K6_ENV="$(K6_ENV)" K6_ENV_FILE="$(K6_ENV_FILE)" RUN_PREFIX="$(K6_ENV)" \
		ENV_FILE="$(ENV_FILE)" GRAFANA_PORT="$(GRAFANA_PORT)" \
		$(K6_RUNNER) smoke

k6-seed: k6-env-init k6-guard-prod
	@$(K6_EXTRA_ENV) K6_ENV="$(K6_ENV)" K6_ENV_FILE="$(K6_ENV_FILE)" RUN_PREFIX="$(K6_ENV)" \
		ENV_FILE="$(ENV_FILE)" GRAFANA_PORT="$(GRAFANA_PORT)" \
		$(K6_RUNNER) seed

k6-mixed-read: k6-env-init k6-guard-prod
	@JOURNEYS_PER_SECOND="$(JOURNEYS_PER_SECOND)" $(K6_EXTRA_ENV) K6_ENV="$(K6_ENV)" K6_ENV_FILE="$(K6_ENV_FILE)" RUN_PREFIX="$(K6_ENV)" \
		ENV_FILE="$(ENV_FILE)" GRAFANA_PORT="$(GRAFANA_PORT)" \
		$(K6_RUNNER) mixed-read $(K6_MIXED_READ_PROFILE)

k6-global-search: k6-env-init k6-guard-prod
	@$(K6_EXTRA_ENV) K6_ENV="$(K6_ENV)" K6_ENV_FILE="$(K6_ENV_FILE)" RUN_PREFIX="$(K6_ENV)" \
		ENV_FILE="$(ENV_FILE)" GRAFANA_PORT="$(GRAFANA_PORT)" \
		$(K6_RUNNER) books-search-global

k6-scenario: k6-env-init k6-guard-prod
	@if [[ -z "$(SCENARIO)" ]]; then \
		echo "error: SCENARIO is required. Example: make k6-scenario SCENARIO=books-user" >&2; \
		exit 2; \
	fi
	@$(K6_EXTRA_ENV) K6_ENV="$(K6_ENV)" K6_ENV_FILE="$(K6_ENV_FILE)" RUN_PREFIX="$(K6_ENV)" \
		ENV_FILE="$(ENV_FILE)" GRAFANA_PORT="$(GRAFANA_PORT)" \
		$(K6_RUNNER) "$(SCENARIO)"

k6-dry-mixed-read: k6-env-init k6-guard-prod
	@JOURNEYS_PER_SECOND="$(JOURNEYS_PER_SECOND)" $(K6_EXTRA_ENV) K6_ENV="$(K6_ENV)" K6_ENV_FILE="$(K6_ENV_FILE)" RUN_PREFIX="$(K6_ENV)" K6_DRY_RUN=1 \
		ENV_FILE="$(ENV_FILE)" GRAFANA_PORT="$(GRAFANA_PORT)" \
		$(K6_RUNNER) mixed-read $(K6_MIXED_READ_PROFILE)

k6-test-options:
	@ENV_FILE="$(ENV_FILE)" bash performance/k6/scripts/verify-options.sh
