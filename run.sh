#!/usr/bin/env bash
# ParityProof — run the full COBOL->Java parity pipeline locally, for every program in
# parity/programs.txt (ACCOUNT-INT, TXN-POST).
# Prereqs: gnucobol (cobc), Java 21.  No Docker, no Python needed for the local slice.
# Every run is captured timestamped under evidence/runs/<UTC-ts>/ with evidence/latest
# pointing at the newest run, so the dashboard + CI read one stable location.
# Exit status: 0 only if every program is at 100% field parity.
set -uo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# A crashed step must still produce a recorded FAIL, so steps below do not abort the script:
# outputs are deleted first, and the runner reports a missing/empty output as a clean FAIL.
step_failed=0
try() { "$@" || { echo "!! step failed (rc=$?): $*"; step_failed=1; }; }

echo "== 1/5 generate inputs (edge cases) =="
cd "$ROOT/parity"
rm -f ACCOUNTS.DAT TXNS.DAT result.json
try java GenInput.java
try java GenTxnInput.java
cp ACCOUNTS.DAT TXNS.DAT "$ROOT/cobol/" 2>/dev/null
cp ACCOUNTS.DAT TXNS.DAT "$ROOT/java/"  2>/dev/null

echo "== 2/5 compile + run COBOL reference (GnuCOBOL) =="
cd "$ROOT/cobol"
rm -f OUT-COBOL.DAT OUT-TXN-COBOL.DAT acct_int txn_post   # crash => absent => runner reports FAIL, never a stale diff
try cobc -free -x ACCOUNT-INT.cbl -o acct_int
try ./acct_int
try cobc -free -x TXN-POST.cbl -o txn_post
try ./txn_post
for f in OUT-COBOL.DAT OUT-TXN-COBOL.DAT; do
  if [ -s "$f" ]; then echo "COBOL reference output $f: $(wc -l < "$f" | tr -d ' ') records"; else echo "NO COBOL OUTPUT: $f"; fi
done

echo "== 3/5 run Java modernized twins =="
cd "$ROOT/java"
rm -f OUT-JAVA.DAT OUT-TXN-JAVA.DAT                        # same guard for the Java side
try java AccountInterest.java
try java TxnPost.java

echo "== 4/5 field-level parity diff (per program) =="
cd "$ROOT/parity"
java ParityRunner.java
runner_rc=$?

echo "== 5/5 capture timestamped evidence =="
TS=$(date -u +%Y%m%dT%H%M%SZ)
RUN="$ROOT/evidence/runs/$TS"
mkdir -p "$RUN/ACCOUNT-INT" "$RUN/TXN-POST"
cp result.json "$RUN/result.json" 2>/dev/null
cp "$ROOT/cobol/ACCOUNTS.DAT"        "$RUN/ACCOUNT-INT/input.dat"      2>/dev/null
cp "$ROOT/cobol/OUT-COBOL.DAT"       "$RUN/ACCOUNT-INT/out-cobol.dat"  2>/dev/null
cp "$ROOT/java/OUT-JAVA.DAT"         "$RUN/ACCOUNT-INT/out-java.dat"   2>/dev/null
cp "$ROOT/cobol/TXNS.DAT"            "$RUN/TXN-POST/input.dat"         2>/dev/null
cp "$ROOT/cobol/OUT-TXN-COBOL.DAT"   "$RUN/TXN-POST/out-cobol.dat"     2>/dev/null
cp "$ROOT/java/OUT-TXN-JAVA.DAT"     "$RUN/TXN-POST/out-java.dat"      2>/dev/null
rm -rf "$ROOT/evidence/latest"
cp -R "$RUN" "$ROOT/evidence/latest"
echo "evidence -> $RUN  (latest -> evidence/latest)"

echo "== parity gate =="
if [ "$runner_rc" -eq 0 ] && [ "$step_failed" -eq 0 ]; then
  echo "PARITY PASS ✅"
else
  echo "PARITY FAIL ❌ (runner rc=$runner_rc, step failures=$step_failed; see $RUN/result.json)"
  exit 1
fi
