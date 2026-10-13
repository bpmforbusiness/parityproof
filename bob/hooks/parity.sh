#!/usr/bin/env bash
# Bob lifecycle hook for ParityProof: compile -> run -> parity, and FAIL LOUDLY
# on any parity FAIL (non-zero exit blocks the Bob step). Full output is logged to
# evidence/hooks/<utc-ts>.log so tool adoption is provable, not narrated.
# Install as `sh .bob/hooks/parity.sh` via a hooks config (see hooks.example.json).
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