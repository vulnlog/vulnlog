#!/usr/bin/env bash
set -euo pipefail

VULNLOG_BIN="${VULNLOG_BIN:-modules/cli-app/build/native/nativeCompile/vulnlog}"
FIXTURE="${FIXTURE:-perf/perf.vl.yaml}"
THRESHOLD_MS="${THRESHOLD_MS:-1000}"
RUNS="${RUNS:-3}"
WARMUP="${WARMUP:-1}"
OUT_DIR="perf/results"

if [ ! -x "$VULNLOG_BIN" ]; then
  echo "error: native binary not found or not executable at $VULNLOG_BIN" >&2
  exit 2
fi
if [ ! -f "$FIXTURE" ]; then
  echo "error: fixture not found at $FIXTURE" >&2
  exit 2
fi
if ! command -v hyperfine >/dev/null 2>&1; then
  echo "error: hyperfine is not installed" >&2
  exit 2
fi
if ! command -v jq >/dev/null 2>&1; then
  echo "error: jq is not installed" >&2
  exit 2
fi

mkdir -p "$OUT_DIR"
WORK="$(mktemp -d)"
mkdir -p "$WORK/sup"
trap 'rm -rf "$WORK"' EXIT

run_bench() {
  local name="$1"; shift
  local json="$OUT_DIR/${name}.json"
  hyperfine \
    --warmup "$WARMUP" \
    --runs "$RUNS" \
    --export-json "$json" \
    --command-name "$name" \
    "$@"

  local mean_ms
  mean_ms=$(jq -r '.results[0].mean * 1000 | floor' "$json")
  echo "==> $name mean = ${mean_ms} ms (threshold ${THRESHOLD_MS} ms)"
  if [ "$mean_ms" -gt "$THRESHOLD_MS" ]; then
    echo "::error::$name exceeded threshold: ${mean_ms} ms > ${THRESHOLD_MS} ms"
    return 1
  fi
}

failed=0
run_bench validate "$VULNLOG_BIN validate $FIXTURE"                 || failed=1
run_bench report   "$VULNLOG_BIN report $FIXTURE -o $WORK/r.html"   || failed=1
run_bench suppress "$VULNLOG_BIN suppress $FIXTURE -o $WORK/sup"    || failed=1

exit "$failed"
