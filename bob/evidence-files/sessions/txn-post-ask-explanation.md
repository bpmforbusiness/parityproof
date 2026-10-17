# Bob reply: business-rule explanation of cobol/TXN-POST.cbl

**Provenance:** the text under "VERBATIM BOB REPLY" was copied from the IBM Bob IDE chat and
pasted to Claude Code by Abdul on 2026-10-17, then saved here unedited. Claude Code did not
write or alter it. The prompt Bob was given, the Bob mode, and whether the `cobol-explainer`
skill was activated are **not recorded in this file** (Abdul to add from the session / screenshots).
The review of this reply is in `txn-post-ask-explanation.REVIEW.md`.

---------------- VERBATIM BOB REPLY (start) ----------------

**Program: TXN-POST** — batch transaction posting; reads signed transaction records, computes end-of-day balance, flags overdrafts, accumulates a running batch total. **6 rules.**

---

### TXN-POST-R001 — Sequential file loop with EOF sentinel

- **ID**: TXN-POST-R001
- **Description**: The program opens `TXNS.DAT` for input and `OUT-TXN-COBOL.DAT` for output, then reads records one at a time until the file is exhausted, after which both files are closed and the program ends.
- **Source lines**: [`cobol/TXN-POST.cbl` lines 34–56](cobol/TXN-POST.cbl)
- **Inputs**: `WS-EOF` PIC X — loop sentinel, initialised to `'N'`
- **Outputs**: `WS-EOF` PIC X — set to `'Y'` on AT END
- **Edge cases**: If `TXNS.DAT` is empty, the loop body never executes; `WS-BATCHTOT` remains `0` and no records are written to the output file.

---

### TXN-POST-R002 — End-of-day balance computation

- **ID**: TXN-POST-R002
- **Description**: For each transaction record, the account's end-of-day balance is the opening balance plus the signed transaction amount.
- **Source lines**: [`cobol/TXN-POST.cbl` line 40](cobol/TXN-POST.cbl)
- **Inputs**: `T-OPENBAL` PIC S9(9)V99 SIGN LEADING SEPARATE — account opening balance; `T-AMT` PIC S9(7)V99 SIGN LEADING SEPARATE — signed transaction amount (positive = credit, negative = debit)
- **Outputs**: `WS-ENDBAL` PIC S9(9)V99 COMP-3 — computed balance stored as packed decimal
- **Edge cases**: No `ROUNDED` clause — the result is **truncated** (not rounded) to 2 decimal places. If the integer part of the result exceeds 9 digits (> 999,999,999), COBOL silently drops the high-order digits (field-overflow truncation); no error is raised.

---

### TXN-POST-R003 — Running batch total accumulation

- **ID**: TXN-POST-R003
- **Description**: A running total of all signed transaction amounts is maintained across every record in the file. Each record's amount is added to the cumulative total before the record is written.
- **Source lines**: [`cobol/TXN-POST.cbl` line 41](cobol/TXN-POST.cbl)
- **Inputs**: `T-AMT` PIC S9(7)V99 SIGN LEADING SEPARATE — current record's signed transaction amount
- **Outputs**: `WS-BATCHTOT` PIC S9(9)V99 COMP-3 — cumulative batch total (packed decimal), initialised to `0` at program start via `VALUE 0`
- **Edge cases**: The accumulator is **never reset** within the program — it is a program-lifetime running total. Mixed-sign amounts (credits and debits) can net the total to zero or negative; the program does not reject a negative batch total. Overflow of S9(9)V99 causes silent high-order truncation (observed GnuCOBOL behaviour: 1,000,000,000.00 → 0.00).

---

### TXN-POST-R004 — Overdraft flag

- **ID**: TXN-POST-R004
- **Description**: If the computed end-of-day balance is strictly negative, the output record is marked as an overdraft. If the balance is zero or positive, the overdraft flag is cleared.
- **Source lines**: [`cobol/TXN-POST.cbl` lines 42–46](cobol/TXN-POST.cbl)
- **Inputs**: `WS-ENDBAL` PIC S9(9)V99 COMP-3 — the post-computation balance
- **Outputs**: `O-ODFLAG` PIC X — `'Y'` if `WS-ENDBAL < 0`, `'N'` otherwise
- **Edge cases**: Exactly zero balance is **not** an overdraft (`'N'`). The check operates on the already-truncated `WS-ENDBAL`, so any truncation artefact that produces a small negative value would still set `'Y'`.

---

### TXN-POST-R005 — Output record assembly and write

- **ID**: TXN-POST-R005
- **Description**: After computing the balance and overdraft flag, the program assembles and writes one output record per input record, containing: account number, transaction amount, end-of-day balance, overdraft flag, and running batch total.
- **Source lines**: [`cobol/TXN-POST.cbl` lines 47–51](cobol/TXN-POST.cbl)
- **Inputs**: `T-ACCT` PIC 9(9); `T-AMT` PIC S9(7)V99 SIGN LEADING SEPARATE; `WS-ENDBAL` PIC S9(9)V99 COMP-3; `O-ODFLAG` PIC X; `WS-BATCHTOT` PIC S9(9)V99 COMP-3
- **Outputs**: `OUT-REC` fields — `O-ACCT` PIC 9(9), `O-AMT` PIC S9(7)V99 SIGN LEADING SEPARATE, `O-ENDBAL` PIC S9(9)V99 SIGN LEADING SEPARATE, `O-ODFLAG` PIC X, `O-BATCHTOT` PIC S9(9)V99 SIGN LEADING SEPARATE
- **Edge cases**: `WS-ENDBAL` and `WS-BATCHTOT` are COMP-3 in working storage but output as SIGN LEADING SEPARATE display text — the `MOVE` performs an implicit packed-decimal → display conversion. A zero value on a signed DISPLAY field produces `+` as the leading sign character. Note the width asymmetry: `O-AMT` is S9(**7**)V99, `O-ENDBAL` is S9(**9**)V99.

---

### TXN-POST-R006 — Batch total echoed on every record (not a trailer)

- **ID**: TXN-POST-R006
- **Description**: The running batch total is written into **every** output record, not just a final trailer record. Each record therefore carries the cumulative total up to and including that record's transaction.
- **Source lines**: [`cobol/TXN-POST.cbl` lines 41, 50](cobol/TXN-POST.cbl)
- **Inputs**: `WS-BATCHTOT` PIC S9(9)V99 COMP-3 — value after `ADD T-AMT TO WS-BATCHTOT` for the current record
- **Outputs**: `O-BATCHTOT` PIC S9(9)V99 SIGN LEADING SEPARATE — present on each and every output record
- **Edge cases**: Only the **last** record carries the true file-level grand total. A downstream consumer reading any earlier record will see only a partial total. This is a latent semantic trap for consumers that assume `O-BATCHTOT` is a file summary field.

---

> **Flagged / uncertain**: (1) No `ROUNDED` on the `COMPUTE` at line 40 — truncation governs `WS-ENDBAL`, not rounding; Java must reproduce this exactly. (2) The header comment references `txn-post.cpy` as a copybook, but no such file was found in the repository — if it exists externally it may define additional fields not visible in this source.

---------------- VERBATIM BOB REPLY (end) ----------------
