#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'USAGE'
Usage: performance/k6/scripts/compare-reports.sh <label1> <report1.json> [<label2> <report2.json> ...]

Builds a single self-contained HTML page comparing two or more k6 JSON
reports produced by performance/k6/lib/summary.js — e.g. a 1-VU run and a
50-VU run of the record-image-upload scenario.

Example:
  RUN_ID=solo VUS=1 SCENARIO=record-image-upload make k6-scenario
  RUN_ID=burst100 VUS=100 SCENARIO=record-image-upload make k6-scenario

  performance/k6/scripts/compare-reports.sh \
    "1명" performance/k6/reports/record-image-upload-solo.json \
    "100명" performance/k6/reports/record-image-upload-burst100.json

Set OUT_FILE to control the output path
(default: performance/k6/reports/compare-<timestamp>.html).
USAGE
}

die() {
  echo "error: $*" >&2
  exit 2
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" || $# -eq 0 ]]; then
  usage
  exit 0
fi

(( $# >= 4 )) || die "need at least two <label> <report.json> pairs"
(( $# % 2 == 0 )) || die "labels and report paths must be given in pairs"

command -v jq >/dev/null 2>&1 || die "jq is required"

server_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
cd "$server_dir"

labels=()
reports=()
while [[ $# -gt 0 ]]; do
  labels+=("$1")
  [[ -f "$2" ]] || die "report not found: $2"
  reports+=("$2")
  shift 2
done

timestamp="$(date +%Y%m%d-%H%M%S)"
out_file="${OUT_FILE:-performance/k6/reports/compare-${timestamp}.html}"
mkdir -p "$(dirname "$out_file")"

STEP_NAMES=(
  "record-upload:create-book"
  "record-upload:issue-urls"
  "record-upload:put-image"
  "record-upload:create-record"
)

metric_field() {
  # metric_field <report.json> <metric-key> <values-field>
  jq -r --arg k "$2" --arg f "$3" '(.metrics[$k].values[$f]) // empty' "$1"
}

meta_field() {
  jq -r --arg f "$2" '(.metadata[$f]) // empty' "$1"
}

pct() {
  # pct <value> <max> -> width percentage, "0" when max is 0/empty/non-numeric
  awk -v v="${1:-0}" -v m="${2:-0}" 'BEGIN {
    if (m + 0 <= 0) { print "0"; exit }
    p = (v + 0) / (m + 0) * 100
    if (p < 0) p = 0
    if (p > 100) p = 100
    printf "%.1f", p
  }'
}

fmt_ms() {
  awk -v v="${1:-}" 'BEGIN { if (v == "") { print "-"; exit } printf "%.0f ms", v }'
}

fmt_pct() {
  awk -v v="${1:-}" 'BEGIN { if (v == "") { print "-"; exit } printf "%.2f%%", v * 100 }'
}

max_of() {
  # max_of <values...> -> largest numeric value, ignoring blanks
  local best=""
  for value in "$@"; do
    [[ -n "$value" ]] || continue
    if [[ -z "$best" ]] || awk -v a="$value" -v b="$best" 'BEGIN{exit !(a>b)}'; then
      best="$value"
    fi
  done
  echo "$best"
}

bar_rows() {
  # bar_rows <fmt-fn> <value1> [value2 ...]
  local fmt_fn="$1"
  shift
  local values=("$@")
  local max
  max="$(max_of "${values[@]}")"
  local i
  for i in "${!labels[@]}"; do
    local value="${values[$i]:-}"
    local width
    width="$(pct "$value" "$max")"
    local display
    display="$("$fmt_fn" "$value")"
    cat <<ROW
        <div class="bar-row">
          <span class="bar-label">${labels[$i]}</span>
          <div class="bar-track"><div class="bar-fill run-${i}" style="width:${width}%"></div></div>
          <span class="bar-value">${display}</span>
        </div>
ROW
  done
}

metric_block() {
  # metric_block <title> <fmt-fn> <metric-key> <values-field>
  local title="$1" fmt_fn="$2" metric_key="$3" values_field="$4"
  local values=()
  local i
  for i in "${!reports[@]}"; do
    values+=("$(metric_field "${reports[$i]}" "$metric_key" "$values_field")")
  done
  echo "      <h3>${title}</h3>"
  echo "      <div class=\"bar-group\">"
  bar_rows "$fmt_fn" "${values[@]}"
  echo "      </div>"
}

meta_rows_html=""
for i in "${!reports[@]}"; do
  report="${reports[$i]}"
  run_id="$(meta_field "$report" run_id)"
  k6_env="$(meta_field "$report" k6_env)"
  base_url="$(meta_field "$report" base_url)"
  git_sha="$(meta_field "$report" git_commit_sha)"
  iterations="$(metric_field "$report" iterations count)"
  meta_rows_html+="        <tr><td>${labels[$i]}</td><td>${run_id:--}</td><td>${k6_env:--}</td><td>${base_url:--}</td><td>${git_sha:--}</td><td>${iterations:--}</td><td>${report}</td></tr>
"
done

overview_block="$(metric_block "종단(end-to-end) 업로드 소요 시간 - 평균" fmt_ms record_upload_journey_duration avg)"
p95_block="$(metric_block "종단 업로드 소요 시간 - p95" fmt_ms record_upload_journey_duration "p(95)")"
max_block="$(metric_block "종단 업로드 소요 시간 - 최댓값" fmt_ms record_upload_journey_duration max)"
success_block="$(metric_block "기록 작성 성공률" fmt_pct record_upload_journey_success rate)"
failed_block="$(metric_block "HTTP 실패율 (전체)" fmt_pct http_req_failed rate)"

step_blocks=""
for step in "${STEP_NAMES[@]}"; do
  duration_key="http_req_duration{name:${step}}"
  failed_key="http_req_failed{name:${step}}"
  step_blocks+="$(metric_block "${step} - 평균 응답시간" fmt_ms "$duration_key" avg)
"
  step_blocks+="$(metric_block "${step} - p95 응답시간" fmt_ms "$duration_key" "p(95)")
"
  step_blocks+="$(metric_block "${step} - 실패율" fmt_pct "$failed_key" rate)
"
done

legend_html=""
for i in "${!labels[@]}"; do
  legend_html+="        <span class=\"legend-item\"><span class=\"legend-swatch run-${i}\"></span>${labels[$i]}</span>
"
done

cat > "$out_file" <<HTML
<!doctype html>
<html lang="ko">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>record-image-upload 부하 비교 (${timestamp})</title>
<style>
  :root {
    color-scheme: light dark;
    --bg: #ffffff;
    --fg: #1a1a1a;
    --muted: #6b7280;
    --border: #e5e7eb;
    --track: #f1f5f9;
    --run-0: #2563eb;
    --run-1: #dc2626;
    --run-2: #059669;
    --run-3: #d97706;
  }
  @media (prefers-color-scheme: dark) {
    :root {
      --bg: #111418;
      --fg: #e5e7eb;
      --muted: #9ca3af;
      --border: #2a2f37;
      --track: #1c2128;
    }
  }
  * { box-sizing: border-box; }
  body {
    margin: 0;
    padding-block: 24px;
    padding-inline: 16px;
    background: var(--bg);
    color: var(--fg);
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Pretendard, sans-serif;
    font-size: 14px;
    line-height: 1.5;
  }
  main { max-width: 860px; margin: 0 auto; }
  h1 { font-size: 20px; margin-bottom: 4px; }
  .subtitle { color: var(--muted); margin-top: 0; margin-bottom: 20px; }
  h2 { font-size: 16px; margin-top: 32px; border-bottom: 1px solid var(--border); padding-bottom: 6px; }
  h3 { font-size: 13px; color: var(--muted); font-weight: 600; margin: 20px 0 8px; }
  table { width: 100%; border-collapse: collapse; margin-top: 8px; overflow-x: auto; display: block; }
  th, td { text-align: left; padding: 6px 8px; border-bottom: 1px solid var(--border); white-space: nowrap; font-size: 12px; }
  th { color: var(--muted); font-weight: 600; }
  .legend { margin: 12px 0 4px; display: flex; gap: 16px; flex-wrap: wrap; }
  .legend-item { display: inline-flex; align-items: center; gap: 6px; font-size: 12px; color: var(--muted); }
  .legend-swatch { width: 10px; height: 10px; border-radius: 2px; display: inline-block; }
  .bar-group { display: flex; flex-direction: column; gap: 6px; }
  .bar-row { display: grid; grid-template-columns: minmax(70px, 20%) 1fr auto; align-items: center; gap: 10px; }
  .bar-label { font-size: 12px; color: var(--muted); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  .bar-track { background: var(--track); border-radius: 4px; height: 16px; overflow: hidden; }
  .bar-fill { height: 100%; border-radius: 4px; min-width: 2px; }
  .bar-value { font-size: 12px; font-variant-numeric: tabular-nums; min-width: 70px; text-align: right; }
  .run-0 { background: var(--run-0); }
  .run-1 { background: var(--run-1); }
  .run-2 { background: var(--run-2); }
  .run-3 { background: var(--run-3); }
  .legend-swatch.run-0 { background: var(--run-0); }
  .legend-swatch.run-1 { background: var(--run-1); }
  .legend-swatch.run-2 { background: var(--run-2); }
  .legend-swatch.run-3 { background: var(--run-3); }
  .note { color: var(--muted); font-size: 12px; margin-top: 32px; }
</style>
</head>
<body>
<main>
  <h1>사진 업로드 기록 작성 - 동시 사용자 비교</h1>
  <p class="subtitle">performance/k6/scenarios/record-image-upload.js 결과 비교 · 생성 시각 ${timestamp}</p>

  <div class="legend">
${legend_html}  </div>

  <h2>실행 정보</h2>
  <table>
    <tr><th>구간</th><th>run_id</th><th>환경</th><th>base_url</th><th>commit</th><th>iterations</th><th>report 파일</th></tr>
${meta_rows_html}  </table>

  <h2>종단(End-to-End) 업로드 소요 시간</h2>
  <p class="subtitle">이미지 업로드 URL 발급 → 이미지 PUT 업로드 → 기록 생성까지, 한 사람이 "저장" 버튼을 누르고 완료될 때까지 걸린 실제 체감 시간입니다.</p>
${overview_block}
${p95_block}
${max_block}

  <h2>성공률 / 실패율</h2>
${success_block}
${failed_block}

  <h2>단계별 응답 시간</h2>
  <p class="subtitle">업로드 URL 발급, 이미지 PUT, 기록 생성 각 단계에서 병목이 어디인지 구분합니다.</p>
${step_blocks}

  <p class="note">
    로컬/스테이징 측정값은 코드 변경 전후의 상대 비교 용도이며, 운영 용량 산정 근거로 사용하지 않습니다.
    실제 원본 지표는 각 report 파일(위 표의 report 파일 경로)에서 확인할 수 있습니다.
  </p>
</main>
</body>
</html>
HTML

printf 'wrote %s\n' "$out_file"
