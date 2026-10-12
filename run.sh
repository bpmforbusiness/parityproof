#!/usr/bin/env bash
# ParityProof — run the full COBOL->Java parity pipeline locally.
# Prereqs: gnucobol (cobc), Java 21.  No Docker needed for the local slice.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "== 1/5 generate inputs (edge cases) =="
cd "$ROOT/parity"
java GenInput.java
cp ACCOUNTS.DAT "$ROOT/cobol/"

echo "== 2/5 compile + run COBOL reference (GnuCOBOL) =="
cd "$ROOT/cobol"
cobc -free -x ACCOUNT-INT.cbl -o acct_int
./acct_int
test -s OUT-COBOL.DAT && echo "COBOL reference output: $(wc -l < OUT-COBOL.DAT) records" || { echo "NO COBOL OUTPUT"; exit 1; }

echo "== 3/5 run Java modernized twin =="
cd "$ROOT/java"
cp "$ROOT/cobol/ACCOUNTS.DAT" .
java AccountInterest.java

echo "== 4/5 field-level parity diff =="
cd "$ROOT/parity"
java ParityRunner.java

echo "== 5/5 result =="
mkdir -p "$ROOT/evidence"
cp result.json "$ROOT/evidence/result.json"
cp "$ROOT/cobol/OUT-COBOL.DAT" "$ROOT/evidence/out-cobol.dat"
cp "$ROOT/java/OUT-JAVA.DAT"   "$ROOT/evidence/out-java.dat"
cp "$ROOT/cobol/ACCOUNTS.DAT"  "$ROOT/evidence/accounts.dat"
echo "artifacts -> $ROOT/evidence/"