# Bob replies: Ask / Plan / Code prompts, 2026-10-17 (second session)

**Provenance:** the three replies below were copied from the IBM Bob IDE chat and pasted to
Claude Code by Abdul on 2026-10-17, then saved here unedited between the VERBATIM markers.
Claude Code did not write or alter them. The prompts used, the exact Bob mode of each reply, and
whether the `cobol-explainer` skill was activated are **not recorded in this file** (Abdul to add
from the session / screenshots). Review: `2026-10-17-ask-plan-code.REVIEW.md`.

Order, as pasted: (1) Ask-style explanation of cobol/TXN-POST.cbl, (2) plan for a comment-only
edit to java/TxnPost.java, (3) summary after the edit and `bash run.sh`.

---------------- (1) VERBATIM BOB REPLY: explanation (start) ----------------

---

**TXN-POST** — Batch transaction posting: for each input transaction, computes the end-of-day balance (opening balance + signed amount), sets an overdraft flag, maintains a running batch total, and writes one output record per transaction. **7 business rules.**

---

### TXN-POST-R001 — Read transactions sequentially until end-of-file

- **ID:** TXN-POST-R001
- **Description:** The program reads every record from `TXNS.DAT` one at a time, in order, and processes each before moving to the next. When no more records exist the loop terminates.
- **Source lines:** 36–53 (`PERFORM UNTIL WS-EOF = 'Y'`, `READ IN-FILE AT END … NOT AT END …`)
- **Inputs:** `WS-EOF` PIC X — loop-sentinel flag (`N` = continue, `Y` = stop)
- **Outputs:** `WS-EOF` PIC X — set to `'Y'` at end-of-file (line 38)
- **Edge cases:** If `TXNS.DAT` is empty the loop body never executes; `OUT-TXN-COBOL.DAT` will be an empty file. The program does not error or branch on zero records.

---

### TXN-POST-R002 — Compute end-of-day balance as opening balance plus transaction amount

- **ID:** TXN-POST-R002
- **Description:** For every transaction, the account's end-of-day balance equals the opening balance for that day plus the signed transaction amount. A credit transaction increases the balance; a debit decreases it.
- **Source lines:** 40 (`COMPUTE WS-ENDBAL = T-OPENBAL + T-AMT`)
- **Inputs:**
  - `T-OPENBAL` PIC S9(9)V99 SIGN LEADING SEPARATE — opening balance, max 999 999 999.99
  - `T-AMT` PIC S9(7)V99 SIGN LEADING SEPARATE — transaction amount, max ±9 999 999.99
- **Outputs:** `WS-ENDBAL` PIC S9(9)V99 COMP-3 — computed end balance stored in packed decimal working storage
- **Edge cases:**
  - The arithmetic is performed in COMP-3 working storage before assignment; no `ROUNDED` clause is present, so any sub-cent intermediate is **truncated**, not rounded.
  - If the true result exceeds 9 digits on the integer side (i.e. > 999 999 999.99 or < −999 999 999.99), COBOL performs **high-order digit truncation** silently — the low-order 9 integer digits are kept, the overflow is lost with no error. E.g. 999 999 999.99 + 0.01 → 1 000 000 000.00 truncates to 0.00.
  - Zero opening balance with any signed amount is valid and produces the amount itself as the end balance.

---

### TXN-POST-R003 — Accumulate a running batch total across all transactions

- **ID:** TXN-POST-R003
- **Description:** As each transaction is posted, its signed amount is added to a running total for the batch. This total reflects the net of all credits and debits processed so far in the run, and is reset to zero only at program start.
- **Source lines:** 30 (`WS-BATCHTOT … VALUE 0`), 41 (`ADD T-AMT TO WS-BATCHTOT`)
- **Inputs:** `T-AMT` PIC S9(7)V99 SIGN LEADING SEPARATE — signed transaction amount
- **Outputs:** `WS-BATCHTOT` PIC S9(9)V99 COMP-3 — cumulative batch total in packed decimal working storage
- **Edge cases:**
  - `WS-BATCHTOT` is initialised to zero at program start (line 30) and is never reset mid-run; it is a single-pass accumulator.
  - Same overflow/truncation risk as R002: if the cumulative total exceeds ±999 999 999.99, high-order digits are silently dropped.
  - Alternating equal credits and debits will correctly cancel to zero.

---

### TXN-POST-R004 — Set overdraft flag when end-of-day balance is negative

- **ID:** TXN-POST-R004
- **Description:** If the computed end-of-day balance is less than zero (i.e. the account would be overdrawn after posting), the overdraft flag on the output record is set to `'Y'`. Otherwise it is set to `'N'`.
- **Source lines:** 42–46 (`IF WS-ENDBAL < 0 MOVE 'Y' TO O-ODFLAG ELSE MOVE 'N' TO O-ODFLAG`)
- **Inputs:** `WS-ENDBAL` PIC S9(9)V99 COMP-3 — the end balance computed in R002
- **Outputs:** `O-ODFLAG` PIC X — single character overdraft flag (`Y` = overdrawn, `N` = not overdrawn)
- **Edge cases:**
  - A balance of exactly **zero is not an overdraft** (`< 0` is strict); `O-ODFLAG` = `'N'`.
  - Because `WS-ENDBAL` is signed (`S9(9)V99`), negative values are representable and will trigger the flag correctly.
  - If truncation in R002 silently wraps a very large positive result to a negative value, the flag will fire incorrectly — this is a latent hazard of the overflow behaviour.

---

### TXN-POST-R005 — Copy transaction inputs and computed values to the output record

- **ID:** TXN-POST-R005
- **Description:** Before writing, the program assembles the output record by copying the account number and transaction amount from the input, and copying the computed end balance and batch total from working storage.
- **Source lines:** 47–50 (`MOVE T-ACCT TO O-ACCT`, `MOVE T-AMT TO O-AMT`, `MOVE WS-ENDBAL TO O-ENDBAL`, `MOVE WS-BATCHTOT TO O-BATCHTOT`)
- **Inputs:**
  - `T-ACCT` PIC 9(9) — account identifier
  - `T-AMT` PIC S9(7)V99 SIGN LEADING SEPARATE — transaction amount
  - `WS-ENDBAL` PIC S9(9)V99 COMP-3 — end balance
  - `WS-BATCHTOT` PIC S9(9)V99 COMP-3 — batch total
- **Outputs:**
  - `O-ACCT` PIC 9(9) — account identifier on output record
  - `O-AMT` PIC S9(7)V99 SIGN LEADING SEPARATE — transaction amount on output record
  - `O-ENDBAL` PIC S9(9)V99 SIGN LEADING SEPARATE — end balance on output record (converted from COMP-3 to DISPLAY)
  - `O-BATCHTOT` PIC S9(9)V99 SIGN LEADING SEPARATE — batch total on output record (converted from COMP-3 to DISPLAY)
- **Edge cases:**
  - The MOVE from COMP-3 (`WS-ENDBAL`, `WS-BATCHTOT`) to SIGN LEADING SEPARATE display fields is an implicit format conversion; the value is preserved but any truncated high-order digits remain lost (no recovery).
  - Zero values are formatted with a leading `+` sign, per `SIGN LEADING SEPARATE` convention.

---

### TXN-POST-R006 — Write one output record per transaction

- **ID:** TXN-POST-R006
- **Description:** After assembling the output record, the program writes exactly one line to `OUT-TXN-COBOL.DAT` for each input transaction. The output record contains: account number, transaction amount, end-of-day balance, overdraft flag, and cumulative batch total.
- **Source lines:** 51 (`WRITE OUT-REC`)
- **Inputs:** `OUT-REC` — the assembled output record (O-ACCT, O-AMT, O-ENDBAL, O-ODFLAG, O-BATCHTOT)
- **Outputs:** One 44-character line appended to `OUT-TXN-COBOL.DAT` per input record, fixed-width layout: ACCT(9) + AMT(10) + ENDBAL(12) + ODFLAG(1) + BATCHTOT(12)
- **Edge cases:**
  - Output is written in the same order as input; no sorting or grouping is applied.
  - There is no error branch for a failed write — the program would abnormally terminate on I/O failure.

---

### TXN-POST-R007 — Sign representation: SIGN LEADING SEPARATE on all monetary display fields

- **ID:** TXN-POST-R007
- **Description:** All signed monetary fields in the input and output use a leading separate sign character (`+` or `−`) as the first byte of the field, followed by the digit string. A zero monetary value carries a `+` sign. This is a structural rule that governs how every signed field is read and written.
- **Source lines:** 19–20 (input PIC definitions), 24–25, 27 (output PIC definitions); copybook lines 5–6
- **Inputs / Outputs (all monetary signed display fields):**
  - `T-OPENBAL` PIC S9(9)V99 SIGN LEADING SEPARATE — 12 chars (1 sign + 9 int digits + 2 dec digits implied, no decimal point in file)
  - `T-AMT` PIC S9(7)V99 SIGN LEADING SEPARATE — 10 chars (1 sign + 7 int digits + 2 dec digits)
  - `O-AMT` PIC S9(7)V99 SIGN LEADING SEPARATE — 10 chars
  - `O-ENDBAL` PIC S9(9)V99 SIGN LEADING SEPARATE — 12 chars
  - `O-BATCHTOT` PIC S9(9)V99 SIGN LEADING SEPARATE — 12 chars
- **Edge cases:**
  - The decimal point is **implied** (the `V` in PIC); no decimal point character appears in the file. Parsers must know field widths from the layout to locate the decimal position.
  - Zero is always `+000000000` (with the appropriate digit count), never `−`.

---

> **Uncertainty flag:** The program processes one transaction amount per record and one opening balance per record — there is no grouping by account. This means if the same account appears twice in `TXNS.DAT`, two independent output rows are written with independent end balances (each using only that row's `T-OPENBAL`). The `WS-BATCHTOT` accumulates across all rows regardless of account. This appears intentional but is worth confirming with the business owner. No deduplication or carry-forward balance logic exists in the code.

---------------- (1) VERBATIM BOB REPLY: explanation (end) ----------------

---------------- (2) VERBATIM BOB REPLY: plan (start) ----------------

Good — I have everything I need. Here is the plan.

---

## Plan: add a clearer comment above `fit()` in [`TxnPost.java`](java/TxnPost.java)

### What the change is

Replace the single-line Javadoc on [`fit()`](java/TxnPost.java:60) (line 59) with a multi-line block comment that:

1. Explicitly names the COBOL behaviour being reproduced.
2. Cites the exact COBOL source lines that trigger it.
3. Explains what "keep the sign" means for the edge case where the result rounds to zero.

**No logic changes.** The method body on lines 61–65 is untouched.

---

### Step-by-step

#### Step 1 — Identify the exact line to replace

Current line 59:
```java
/** Store into S9(9)V99: keep the low-order 11 digits and the sign (COBOL high-order truncation). */
```

This is the only line that changes.

#### Step 2 — Write the replacement comment

```java
/**
 * Reproduces COBOL high-order truncation on S9(9)V99 COMP-3 working-storage fields.
 *
 * TXN-POST.cbl lines 29-30 declare WS-ENDBAL and WS-BATCHTOT as S9(9)V99 COMP-3
 * with no ON SIZE ERROR clause.  Lines 40-41 write arithmetic results directly
 * into those fields:
 *
 *   40: COMPUTE WS-ENDBAL  = T-OPENBAL + T-AMT
 *   41: ADD T-AMT TO WS-BATCHTOT
 *
 * When the result has more than 9 integer digits, GnuCOBOL silently discards the
 * high-order digit(s) and preserves the sign — it keeps the low-order (INT_DIGITS
 * + SCALE) unscaled digits.  Observed example: 999999999.99 + 0.01 stores as 0.00.
 * The sign of a zero result is positive (SIGN LEADING SEPARATE renders it as '+').
 * This truncation is a latent hazard in the original program; it is reproduced here
 * deliberately so the Java output remains byte-for-byte identical to the COBOL output.
 */
```

#### Step 3 — Verify the COBOL lines cited are correct

From the file already read, [`TXN-POST.cbl`](cobol/TXN-POST.cbl:29):

| Line | Content |
|------|---------|
| 29 | `01  WS-ENDBAL   PIC S9(9)V99 COMP-3.` |
| 30 | `01  WS-BATCHTOT PIC S9(9)V99 COMP-3 VALUE 0.` |
| 40 | `COMPUTE WS-ENDBAL = T-OPENBAL + T-AMT` |
| 41 | `ADD T-AMT TO WS-BATCHTOT` |

All four lines match the proposed citation exactly. ✓

#### Step 4 — Make the edit in Agent mode

Switch to **Agent mode** and ask Bob to apply a `search_and_replace` on [`java/TxnPost.java`](java/TxnPost.java:59): replace the single `/** Store into … */` line with the multi-line block comment above. Nothing else in the file changes.

#### Step 5 — Verify parity

Run the parity harness:

```bash
bash run.sh
```

Expected outcome:
- Both COBOL and Java produce output files.
- The diff section reports **0 lines differ**.
- The summary prints something like `PARITY OK — 0 field mismatches`.

Because the method body is unchanged, the numerical output is identical to the pre-edit run. If `run.sh` reports any mismatch, the edit touched more than the comment — roll it back immediately and re-inspect.

---

### Risk assessment

| Concern | Status |
|---------|--------|
| Logic change | None — only the Javadoc string is modified |
| Compilation risk | Minimal; multi-line `/** … */` is valid Java anywhere a single-line `/** … */` is |
| Parity impact | Zero — comments do not affect bytecode |
| Rule catalog update needed? | No — no business rule was added or changed |

The only acceptance gate is `bash run.sh` returning **PARITY OK**. Ready to proceed in Agent mode whenever you are.

---------------- (2) VERBATIM BOB REPLY: plan (end) ----------------

---------------- (3) VERBATIM BOB REPLY: code-mode summary (start) ----------------

Parity is 100%. Here's the summary:

**Change made:** The `fit()` method comment in [`java/TxnPost.java`](java/TxnPost.java:59) was expanded from a single line to a 7-line Javadoc block. No logic was touched. The updated comment now:
- Cites the exact COBOL source lines (40 and 41) per project rule 4
- Names both COMPUTE targets (`WS-ENDBAL`, `WS-BATCHTOT`) with their `no ON SIZE ERROR` note
- Records the two concrete observed GnuCOBOL overflow examples from catalog R004
- Flags that `WS-BATCHTOT` overflow is unexercised by the current fixtures (per REVIEW item 3 and catalog R003)

**Parity results:**

| Program | Fields matched | Result |
|---|---|---|
| ACCOUNT-INT | 24 / 24 | ✅ PASS |
| TXN-POST | 40 / 40 | ✅ PASS |
| **Overall** | **64 / 64 (100%)** | **✅ PASS** |

---------------- (3) VERBATIM BOB REPLY: code-mode summary (end) ----------------
