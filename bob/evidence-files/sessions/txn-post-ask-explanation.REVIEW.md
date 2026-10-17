# Review of Bob's TXN-POST explanation

Reviewed 2026-10-17 by Claude Code against `cobol/TXN-POST.cbl` (57 lines) and `cobol/txn-post.cpy`.
Bob's text itself is in `txn-post-ask-explanation.md` and is unedited.

## Verified correct
- Every line citation matches the file: 34–56 (open/loop/close/stop), 40 (`COMPUTE`),
  41 (`ADD`), 42–46 (overdraft test), 47–51 (moves + `WRITE`), 50 (`MOVE WS-BATCHTOT`).
- Field names and PIC clauses are accurate, including the S9(7)/S9(9) width asymmetry.
- Zero results are written with a leading `+` (matches observed GnuCOBOL 3.2.0 output).
- R006 is a finding the human-drafted catalog had missed: `O-BATCHTOT` is a running total
  repeated on every record, so only the last record carries the file-level grand total.
  Added to `catalog/TXN-POST.md` as R005, credited to Bob.

## Errors / overstatements in Bob's reply
1. **False: "no such file [txn-post.cpy] was found in the repository."** `cobol/txn-post.cpy`
   exists and is tracked in git (`git ls-files cobol`).
2. **Misleading (R002):** "truncated (not rounded) to 2 decimal places". Both operands already
   have 2 decimals, so no decimal truncation occurs in that statement. The overflow
   statement in the same rule is correct.
3. **Overstated (R003):** "observed GnuCOBOL behaviour: 1,000,000,000.00 → 0.00". That was
   observed for `WS-ENDBAL` (record 6 in `parity/GenTxnInput.java`), not for `WS-BATCHTOT`.
   Batch-total overflow is not exercised by the test fixtures.
4. **Skill-format slip:** the closing note says "Java must reproduce this exactly", although the
   cobol-explainer skill says not to translate to Java in this step.
5. **Counting:** Bob counts the file loop (R001) and the output assembly (R005) as rules;
   these are program mechanics, and R006 overlaps R003. The human catalog keeps 5 rules.

## Not recorded (Abdul to add)
Which Bob mode was used, the exact prompt, whether the `cobol-explainer` skill was activated
and any permission prompt, plus screenshots and a session export.
