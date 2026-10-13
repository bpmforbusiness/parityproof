# TXN-POST — Business Rule Catalog

> **Status: DRAFT for business review (Karthik).** Drafted from `cobol/TXN-POST.cbl` by Claude
> Code, following `bob/skills/cobol-explainer/SKILL.md`. Not produced by a Bob session. Line
> citations were checked against the file; business meaning has not been reviewed.

**Program:** TXN-POST — a batch transaction-posting run over a fixed-width transaction file.
**What it does:** for each transaction, adds the signed amount to the account's opening balance
to get an end balance, flags overdrafts, and keeps a running batch total across the file.
**Rules found:** 4

---

## R001 — End balance = opening balance + signed amount
- **Description:** The account's end balance is its opening balance plus the transaction amount.
  A positive amount is a credit, a negative amount a debit. The result is held in a packed
  decimal (COMP-3) working field.
- **Source lines:** `cobol/TXN-POST.cbl` 19–20 (inputs), 29 (`WS-ENDBAL PIC S9(9)V99 COMP-3`),
  40 (`COMPUTE WS-ENDBAL = T-OPENBAL + T-AMT`).
- **Inputs:** T-OPENBAL (PIC S9(9)V99, sign leading separate); T-AMT (PIC S9(7)V99, sign leading separate).
- **Outputs:** WS-ENDBAL (PIC S9(9)V99 COMP-3) → O-ENDBAL (PIC S9(9)V99, sign leading separate).
- **Edge cases:** negative opening balance; zero amount; result exactly zero (written with a
  `+` sign — there is no negative zero in the output); sum exceeding 999,999,999.99 (see R004).

## R002 — Overdraft flag
- **Description:** A transaction is flagged `Y` when the end balance is below zero, otherwise `N`.
  An end balance of exactly zero is **not** an overdraft.
- **Source lines:** `cobol/TXN-POST.cbl` 42–46 (`IF WS-ENDBAL < 0`), 26 (`O-ODFLAG PIC X`).
- **Inputs:** WS-ENDBAL.
- **Outputs:** O-ODFLAG (PIC X: `Y`/`N`).
- **Edge cases:** exactly zero; a negative opening balance that a credit does not fully clear;
  an overflowing sum, whose flag is computed from the already-truncated balance (see R004).
  The program does not charge a fee or reject the transaction — it only flags.

## R003 — Running batch total
- **Description:** A running total of all transaction amounts seen so far in the file (credits
  minus debits) is kept across records, starting at zero, and written on every output record.
- **Source lines:** `cobol/TXN-POST.cbl` 30 (`WS-BATCHTOT PIC S9(9)V99 COMP-3 VALUE 0`),
  41 (`ADD T-AMT TO WS-BATCHTOT`), 50 (`MOVE WS-BATCHTOT TO O-BATCHTOT`).
- **Inputs:** T-AMT; the previous value of WS-BATCHTOT.
- **Outputs:** O-BATCHTOT (PIC S9(9)V99, sign leading separate), per record.
- **Edge cases:** the total can go negative; it is not reset per account (it is per file);
  overflow past 9(9)V99 is possible in principle but **not exercised by the current test
  fixtures** (it would need very many large transactions) — flagged as untested.

## R004 — Field-limit truncation on overflow (latent hazard)
- **Description:** If a computed value needs more than 9 integer digits, COBOL **silently drops
  the high-order digit(s)** and keeps the sign; no error is raised (no `ON SIZE ERROR`).
  Preserved deliberately: the Java twin reproduces it for parity.
- **Source lines:** `cobol/TXN-POST.cbl` 40 and 41 (no `ON SIZE ERROR`), 29–30 (S9(9)V99 targets);
  twin: `java/TxnPost.java` `fit()`.
- **Inputs:** any sum whose magnitude exceeds 999,999,999.99.
- **Outputs:** observed under GnuCOBOL 3.2 with the fixtures in `parity/GenTxnInput.java`:
  999,999,999.99 + 0.01 → stored as +0.00 (balance silently wraps to zero, flag `N`);
  −999,999,999.99 − 5.00 → stored as −4.99 (flag `Y`).
- **Edge cases:** balance at the field maximum plus any credit. **This is a real hazard in the
  original program**: a very large account can show a wrong, even zero, balance with no error.
  Flag for the business whether production should guard or reject instead; it is kept as-is here.

---

_Flagged for review:_ R004 (silent wrap-around of balances); R003's "per file, not per account"
scope is as implemented, which may not be what the business intends for a multi-account file.
