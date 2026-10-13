#!/usr/bin/env bash
# ParityProof parity hook: compile -> run -> parity (all programs), exits non-zero on any
# parity FAIL. Full output is logged to evidence/hooks/<utc-ts>.log so the run is provable.
#
# Wiring in Bob (per https://bob.ibm.com/docs/ide/configuration/lifecycle-hooks): hooks live under
# a "hooks" key in .bob/settings.json (workspace) or ~/.bob/settings/settings.json (global);
# see hooks.example.json and bob/hooks/README.md. NOTE: as a PostToolUse hook this is
# NON-BLOCKING — Bob ignores its stdout and exit code (exit 2 only blocks PreToolUse /
# UserPromptSubmit). The non-zero exit is therefore for CI, the Stop hook's own log, and manual
# runs; the evidence/hooks/*.log is what shows a FAIL happened.
set +e
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
TS=$(date -u +%Y%m%dT%H%M%SZ)
mkdir -p "$ROOT/evidence/hooks"
LOG="$ROOT/evidence/hooks/$TS.log"
cd "$ROOT"
bash run.sh >"$LOG" 2>&1
EC=$?
if grep -q "PARITY PASS" "$LOG"; then
  echo "hook: PARITY PASS ✅ (log $LOG)"
  exit 0
fi
echo "hook: PARITY FAIL ❌ (log $LOG)"
exit "$EC"