SHELL := /usr/bin/env bash

ENV ?= local
K6_ENV ?= $(ENV)
JOURNEYS_PER_SECOND ?= 1
GRAFANA_PORT ?= 3001
ENV_FILE ?= performance/k6/env/monitoring.env

K6_RUNNER := performance/k6/scripts/run-k6.sh
K6_ENV_FILE := performance/k6/env/$(K6_ENV).env
K6_MIXED_READ_PROFILE := jps$(JOURNEYS_PER_SECOND)
K6_RUN = K6_ENV="$(K6_ENV)" K6_ENV_FILE="$(K6_ENV_FILE)" RUN_PREFIX="$(K6_ENV)" ENV_FILE="$(ENV_FILE)" GRAFANA_PORT="$(GRAFANA_PORT)" $(K6_RUNNER)

.PHONY: k6-up k6-down k6-smoke k6-seed k6-mixed-read k6-dry-mixed-read k6-global-search k6-scenario k6-env-init \
	k6-test k6-test-syntax k6-test-runner k6-test-options k6-test-compose

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

k6-up: k6-env-init
	@ENV_FILE="$(ENV_FILE)" GRAFANA_PORT="$(GRAFANA_PORT)" \
		docker compose -f docker-compose.monitoring.yml up -d

k6-down:
	@ENV_FILE="$(ENV_FILE)" GRAFANA_PORT="$(GRAFANA_PORT)" \
		docker compose -f docker-compose.monitoring.yml down

k6-smoke: k6-env-init
	@$(K6_RUN) smoke

k6-seed: k6-env-init
	@$(K6_RUN) seed

k6-mixed-read: k6-env-init
	@$(K6_RUN) mixed-read $(K6_MIXED_READ_PROFILE)

k6-global-search: k6-env-init
	@$(K6_RUN) books-search-global

k6-scenario: k6-env-init
	@if [[ -z "$${SCENARIO:-}" ]]; then \
		echo "error: SCENARIO is required. Example: make k6-scenario SCENARIO=books-user" >&2; \
		exit 2; \
	fi
	@$(K6_RUN) "$${SCENARIO}"

k6-dry-mixed-read: k6-env-init
	@K6_DRY_RUN=1 $(K6_RUN) mixed-read $(K6_MIXED_READ_PROFILE)

k6-test: k6-test-syntax k6-test-runner k6-test-options k6-test-compose

k6-test-syntax:
	@bash -n performance/k6/scripts/run-k6.sh performance/k6/scripts/verify-runner.sh performance/k6/scripts/verify-options.sh
	@sh -n performance/k6/scripts/target-policy.sh performance/k6/scripts/k6-entrypoint.sh

k6-test-runner:
	@performance/k6/scripts/verify-runner.sh

k6-test-options:
	@ENV_FILE="$(ENV_FILE)" bash performance/k6/scripts/verify-options.sh

k6-test-compose:
	@ENV_FILE="$(ENV_FILE)" K6_DOCKER_USER="$$(id -u):$$(id -g)" \
		docker compose -f docker-compose.monitoring.yml --profile loadtest config --quiet
