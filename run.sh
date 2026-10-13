#!/usr/bin/env bash
# ParityProof — run the full COBOL->Java parity pipeline locally.
# Prereqs: gnucobol (cobc), Java 21.  No Docker needed for the local slice.
# Every run is captured timestamped under evidence/runs/<UTC-ts>/ with evidence/latest
# pointing at the newest run, so the dashboard + CI read one stable location.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "== 1/5 generate inputs (edge cases) =="
cd "$ROOT/parity"
java GenInput.java
cp ACCOUNTS.DAT "$ROOT/cobol/"

echo "== 2/5 compile + run COBOL reference (GnuCOBOL) =="
cd "$ROOT/cobol"
rm -f OUT-COBOL.DAT                 # a crash leaves it absent -> runner reports clean FAIL, never a stale diff
cobc -free -x ACCOUNT-INT.cbl -o acct_int
./acct_int
test -s OUT-COBOL.DAT && echo "COBOL reference output: $(wc -l < OUT-COBOL.DAT) records" || { echo "NO COBOL OUTPUT"; exit 1; }

echo "== 3/5 run Java modernized twin =="
cd "$ROOT/java"
rm -f OUT-JAVA.DAT                  # same guard for the Java side
cp "$ROOT/cobol/ACCOUNTS.DAT" .
java AccountInterest.java

echo "== 4/5 field-level parity diff =="
cd "$ROOT/parity"
java ParityRunner.java

echo "== 5/5 capture timestamped evidence =="
TS=$(date -u +%Y%m%dT%H%M%SZ)
RUN="$ROOT/evidence/runs/$TS"
mkdir -p "$RUN"
cp result.json "$RUN/result.json"
cp "$ROOT/cobol/OUT-COBOL.DAT" "$RUN/out-cobol.dat"
cp "$ROOT/java/OUT-JAVA.DAT"   "$RUN/out-java.dat"
cp "$ROOT/cobol/ACCOUNTS.DAT"  "$RUN/accounts.dat"
rm -rf "$ROOT/evidence/latest"
cp -r "$RUN" "$ROOT/evidence/latest"
echo "evidence -> $RUN  (latest -> evidence/latest)"

echo "== parity gate =="
if python3 -c "import json,sys; sys.exit(0 if json.load(open('$RUN/result.json'))['pass'] else 1)"; then
  echo "PARITY PASS ✅"
else
  echo "PARITY FAIL ❌ (see $RUN/result.json)"; exit 1
fi