# Review of Bob's Ask / Plan / Code replies (2026-10-17)

Reviewed by Claude Code against `cobol/TXN-POST.cbl`, `cobol/txn-post.cpy`, the git diff of
`java/TxnPost.java` and `evidence/runs/20261017T162112Z/result.json`. Bob's text is unedited in
`2026-10-17-ask-plan-code.md`.

## (1) Explanation: what checks out
- Every line citation is correct: 36–53 loop, 38 AT END, 40 COMPUTE, 30/41, 42–46, 47–50, 51,
  19–20 input PICs, 24–25 and 27 output PICs, and copybook lines 5–6 (`T-OPENBAL`, `T-AMT`).
- Bob found `txn-post.cpy` this time (the first session wrongly claimed it did not exist).
- The "Uncertainty flag" is a real, useful finding: there is no per-account grouping, so a
  repeated account yields independent rows, while `WS-BATCHTOT` accumulates across all rows.
- Example `999,999,999.99 + 0.01 -> 0.00` matches the real GnuCOBOL 3.2.0 output.
- It stayed in the skill's format (ID, description, source lines, inputs, outputs, edge cases)
  and did not translate to Java.

## (1) Explanation: errors / overstatements
1. **R004 is wrong:** "If truncation in R002 silently wraps a very large positive result to a
   negative value, the flag will fire incorrectly." Truncation keeps the sign. In the real run a
   positive overflow stays `+0.00` with flag `N`; a negative overflow stays negative (`-4.99`, `Y`).
   A positive result never becomes negative.
2. **R002 is misleading:** "any sub-cent intermediate is truncated, not rounded." Both operands
   already have 2 decimals, so no sub-cent value arises. (Same remark was in the first session.)
3. **R003 overflow** is stated as a risk, not observed; batch-total overflow is not exercised by
   the fixtures (Bob later says so itself in the Code-mode summary).
4. **Counting:** "7 business rules" includes program mechanics (R001 file loop, R005 moves,
   R006 write, R007 sign layout). The human catalog keeps 5 business rules.

## (2) Plan
- Cited lines 29, 30, 40, 41 match the file. The planned comment text is accurate.
- The plan names **`search_and_replace`** as the tool it intends to use for the edit. That is
  the first indication of which tool Bob's Code mode used, and it was *not* covered by the
  original hook matcher `^write_file$` (see `bob/hooks/README.md`). The Code-mode summary does
  not confirm which tool actually ran.
- The plan predicts the output "PARITY OK — 0 field mismatches". The real output is
  `PARITY PASS ✅` / `overall parity = 64/64 fields (100.0%) | PASS`; the predicted wording was
  a guess, not a quote of the pipeline.
- Minor: the plan says line 59 for the single-line comment; in the file the Javadoc was one
  line directly above `fit()`.

## (3) Code-mode summary
- Parity claim confirmed: `evidence/runs/20261017T162112Z/result.json` shows pass, ACCOUNT-INT
  24/24, TXN-POST 40/40.
- The code diff is comment-only (verified from `git diff`, no logic lines changed). The comment
  cites lines 40 and 41, both observed overflow values, and the untested batch-total overflow.
- Bob says "7-line Javadoc"; the actual block is 8 physical lines including `/**` and `*/`.
- Bob cites project rule 4 and the REVIEW file: evidence that the project rules and repo files
  were being read in that session.

## Not recorded (Abdul to add)
The prompts used, which Bob mode produced each reply, the skill activation / permission prompt,
the name of the tool Bob actually used for the edit, and screenshots. No hook log exists for this
edit: the hook did not fire.
