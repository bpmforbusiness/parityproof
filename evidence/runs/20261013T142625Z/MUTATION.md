# INJECTED MUTATION — expected FAIL (not a Bob change, not a real regression)

Produced by Claude Code on 2026-10-13 to show the parity gate catches real divergence.
`java/AccountInterest.java` interest rounding was temporarily changed from `RoundingMode.HALF_UP`
to `RoundingMode.DOWN`, `bash run.sh` was run (exit 1), and the file was then restored with
`git checkout` (HALF_UP is back in the repo). The result.json, inputs and outputs here are the real
output of that run. ACCOUNT-INT: 20/24 fields; TXN-POST (unmodified): 40/40.
