SHELL := /usr/bin/env bash

override K6_COMMAND_LINE_VARIABLES := $(strip $(foreach variable,$(.VARIABLES),$(if $(filter command line,$(origin $(variable))),$(variable))))
ifneq ($(K6_COMMAND_LINE_VARIABLES),)
$(error Pass runtime values through the process environment before make: $(K6_COMMAND_LINE_VARIABLES))
endif

K6_RUNNER := performance/k6/scripts/run-k6.sh

.PHONY: k6-up k6-down k6-smoke k6-seed k6-seed-cleanup k6-mixed-read k6-dry-mixed-read k6-global-search k6-scenario k6-env-init \
	k6-test k6-test-syntax k6-test-runner k6-test-seed k6-test-summary k6-test-single-api k6-test-cache k6-test-options k6-test-compose

k6-env-init:
	@env_file="$${ENV_FILE:-performance/k6/env/monitoring.env}"; \
		mkdir -p performance/k6/state; \
		if [[ "$${env_file}" != /dev/null && ! -f "$${env_file}" ]]; then \
			echo "error: monitoring env file not found: $${env_file}" >&2; \
			echo "Share the real env securely for runtime commands; examples are used only by make k6-test." >&2; \
			exit 2; \
	fi

k6-up: k6-env-init
	@ENV_FILE="$${ENV_FILE:-performance/k6/env/monitoring.env}" GRAFANA_PORT="$${GRAFANA_PORT:-3001}" \
		docker compose -f docker-compose.monitoring.yml up -d

k6-down:
	@ENV_FILE="$${ENV_FILE:-performance/k6/env/monitoring.env}" GRAFANA_PORT="$${GRAFANA_PORT:-3001}" \
		docker compose -f docker-compose.monitoring.yml down

k6-smoke: k6-env-init
	@K6_ENV="$${K6_ENV:-$${ENV:-local}}" $(K6_RUNNER) smoke

k6-seed: k6-env-init
	@K6_ENV="$${K6_ENV:-$${ENV:-local}}" $(K6_RUNNER) seed

k6-seed-cleanup: k6-env-init
	@K6_ENV="$${K6_ENV:-$${ENV:-local}}" $(K6_RUNNER) cleanup-seed

k6-mixed-read: k6-env-init
	@K6_ENV="$${K6_ENV:-$${ENV:-local}}" $(K6_RUNNER) mixed-read "jps$${JOURNEYS_PER_SECOND:-1}"

k6-global-search: k6-env-init
	@K6_ENV="$${K6_ENV:-$${ENV:-local}}" $(K6_RUNNER) books-search-global

k6-scenario: k6-env-init
	@if [[ -z "$${SCENARIO:-}" ]]; then \
		echo "error: SCENARIO is required. Example: SCENARIO=books-user make k6-scenario" >&2; \
		exit 2; \
	fi
	@K6_ENV="$${K6_ENV:-$${ENV:-local}}" $(K6_RUNNER) "$${SCENARIO}"

k6-dry-mixed-read: k6-env-init
	@K6_ENV="$${K6_ENV:-$${ENV:-local}}" K6_DRY_RUN=1 $(K6_RUNNER) mixed-read "jps$${JOURNEYS_PER_SECOND:-1}"

k6-test: k6-test-syntax k6-test-runner k6-test-seed k6-test-summary k6-test-single-api k6-test-cache k6-test-options k6-test-compose

k6-test-syntax:
	@bash -n performance/k6/scripts/run-k6.sh performance/k6/scripts/seed-state.sh performance/k6/scripts/evict-stats-cache.sh performance/k6/scripts/verify-runner.sh performance/k6/scripts/verify-seed-runner.sh performance/k6/scripts/verify-summary.sh performance/k6/scripts/verify-single-api.sh performance/k6/scripts/verify-cache.sh performance/k6/scripts/verify-options.sh
	@sh -n performance/k6/scripts/target-policy.sh performance/k6/scripts/k6-entrypoint.sh

k6-test-runner:
	@bash performance/k6/scripts/verify-runner.sh

k6-test-seed:
	@ENV_FILE=performance/k6/env/monitoring.env.example bash performance/k6/scripts/verify-seed-runner.sh

k6-test-summary:
	@ENV_FILE=performance/k6/env/monitoring.env.example bash performance/k6/scripts/verify-summary.sh

k6-test-single-api:
	@ENV_FILE=performance/k6/env/monitoring.env.example bash performance/k6/scripts/verify-single-api.sh

k6-test-cache:
	@ENV_FILE=performance/k6/env/monitoring.env.example bash performance/k6/scripts/verify-cache.sh

k6-test-options:
	@ENV_FILE=performance/k6/env/monitoring.env.example bash performance/k6/scripts/verify-options.sh

k6-test-compose:
	@ENV_FILE=performance/k6/env/monitoring.env.example K6_DOCKER_USER="$$(id -u):$$(id -g)" \
		docker compose -f docker-compose.monitoring.yml --profile loadtest config --quiet
