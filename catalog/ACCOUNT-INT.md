# ACCOUNT-INT — Business Rule Catalog

> **Status: DRAFT for business review (Karthik).** Auto-drafted from `cobol/ACCOUNT-INT.cbl`
> by Bob (cobol-explainer skill) and not yet reviewed for business sense against the
> intended lending/banking context. Review each entry, then flip this header to
> "Reviewed".

**Program:** ACCOUNT-INT — a monthly interest run over a fixed-width account file.
**What it does:** for each account, computes interest at a fixed 3.5% on the balance,
adds it to produce a new balance, and writes a fixed-width result file.
**Rules found:** 4

---

## R001 — Compute monthly interest
- **Description:** Interest is calculated on the account's posted balance at a fixed
  annual rate of 3.5%, then **rounded to the nearest cent** using COBOL `ROUNDED`
  semantics (round half away from zero on the final cent).
- **Source lines:** `cobol/ACCOUNT-INT.cbl` 29 (`WS-RATE PIC 9V999 VALUE 0.035`),
  38 (`COMPUTE WS-INTEREST ROUNDED = BAL * WS-RATE`).
- **Inputs:** BAL (PIC 9(9)V99, implied 2 decimals); WS-RATE (PIC 9V999 = 0.035).
- **Outputs:** WS-INTEREST (PIC 9(9)V99, implied 2 decimals).
- **Edge cases:** half-cent boundaries (e.g. 0.005 → rounds to 0.01); balances so small
  the interest rounds to 0.00; the rate is fixed constant (3.5%), not derived.

## R002 — New balance = balance + rounded interest
- **Description:** The new end-of-period balance is the starting balance plus the
  **rounded** interest (not the un-rounded product).
- **Source lines:** `cobol/ACCOUNT-INT.cbl` 27 (`WS-NEWBAL PIC 9(9)V99`),
  39 (`COMPUTE WS-NEWBAL = BAL + WS-INTEREST`).
- **Inputs:** BAL (PIC 9(9)V99); WS-INTEREST (PIC 9(9)V99, already rounded).
- **Outputs:** WS-NEWBAL (PIC 9(9)V99).
- **Edge cases:** the new balance itself overflows 9(9)V99 (see R004).

## R003 — Fixed-width output record
- **Description:** Each account writes a fixed-width 42-char record:
  account number (9), balance (9(9)V99 → 11 chars, implied decimals, no decimal point),
  interest, new balance. The twin must reproduce this layout byte-for-byte.
- **Source lines:** `cobol/ACCOUNT-INT.cbl` 20–24 (`OUT-REC`: O-ACCT, O-BAL, O-INT,
  O-NEWBAL), 40–44 (MOVE + WRITE).
- **Inputs:** ACCT-NO (9), BAL (9(9)V99), WS-INTEREST, WS-NEWBAL.
- **Outputs:** full `OUT-REC` (42 chars/record, line-sequential).
- **Edge cases:** implied decimal with no literal decimal point in the file — the diff
  and the Java twin must both use the same implied-decimal convention.

## R004 — Field-limit truncation on overflow (latent hazard)
- **Description:** PIC 9(9)V99 maxes at 999,999,999.99. If a computed value exceeds the
  field width, COBOL **silently truncates the high-order digits** rather than raising an
  error. This is preserved deliberately: the Java twin must reproduce it for parity.
- **Source lines:** `cobol/ACCOUNT-INT.cbl` 26–27 (WS fields 9(9)V99), 39 (compute);
  preserved behaviour in `java/AccountInterest.java` `zeroPad`.
- **Inputs:** any 9(9)V99 field whose value overflows 999,999,999.99
  (e.g. BAL 999,999,999.99 → NEW-BAL 1,034,999,999.99).
- **Outputs:** the truncated 11-char value (e.g. 034,999,999.99).
- **Edge cases:** BAL at field max (999,999,999.99); NEW-BAL crossing into 10 integer
  digits. **This is a real hazard in the original program** — a balance this large is
  corrupted in the output. Flag for the business whether this should be a guard in a
  production modernization (it is retained as-is for parity, not silently "fixed").

---

_Flagged for review:_ R004 is the only genuinely suspicious business behaviour; R001's
"annual" rate is an assumption (the rate is a hard-coded constant).