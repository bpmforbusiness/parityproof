# Business review checklist — for Karthik (≈15 minutes)

Review `ACCOUNT-INT.md` and `TXN-POST.md`. For each question tick **Agree / Disagree / Need info** and add
a note. Nothing here is decided yet; these are the open points the drafts flagged. When you finish, fill in
the sign-off box and tell Abdul — only then can the catalogs be called "reviewed".

> **Status: PROPOSED ANSWERS, NOT YET CONFIRMED BY KARTHIK (2026-10-17).** Every question has a
> proposed answer below, **prepared on Karthik's behalf by Abdul / Claude Code — Karthik has not
> reviewed or approved them.** Two items (**A1**, **T5**) are `Need info`: they need a business
> decision, not a code change. Code claims were re-checked by running `bash run.sh` (64/64 fields,
> 100% parity PASS). Karthik should confirm or overrule each answer and then sign below.

## ACCOUNT-INT (interest run)
| # | Question | Proposed answer (not Karthik's) | Notes |
|---|---|---|---|
| A1 | R001: the 3.5% rate is a fixed constant in the program. Is it an **annual** rate applied once per run, or a per-period rate? (The code does not say.) | **Need info** | Code has no period awareness: `WS-RATE PIC 9V999 VALUE 0.035` applied once per account per run, with no date/accrual field — the run cadence decides the meaning. The draft contradicts itself (calls the run *monthly*, the rate *annual*). A monthly run at 3.5%/month is **~51% effective annual** (1.035^12 = 1.511). Decide: annual run, or monthly run at rate/12. **Blocking the catalog.** |
| A2 | R001: rounding to the nearest cent, half away from zero — is that the intended business rounding? | **Agree** | `COMPUTE WS-INTEREST ROUNDED` is COBOL half-away-from-zero; the Java twin's `HALF_UP` matches. Standard for currency/interest (differs from banker's rounding). Suggest documenting that it favours the account at exactly half a cent. |
| A3 | R002: new balance = balance + rounded interest. Correct? | **Agree** | Line 39 adds the already-rounded `WS-INTEREST`, not the raw product. Consistent with A2. |
| A4 | R004: a balance near 999,999,999.99 produces a new balance that **silently loses its top digit** (e.g. becomes 034,999,999.99). Should production **reject / flag** this instead of truncating? (The Java twin copies the truncation on purpose, for parity.) | **Agree** | No `ON SIZE ERROR` anywhere, so 999,999,999.99 → 034,999,999.99 with no error raised. Keep the truncation in the parity twin (it is *why* parity holds); a production modernization should guard it (reject/flag). This is the audit's genuine finding. |

## TXN-POST (transaction posting)
| # | Question | Proposed answer (not Karthik's) | Notes |
|---|---|---|---|
| T1 | R001: end balance = opening balance + signed amount (credit +, debit −). Correct? | **Agree** | Line 40; fixtures confirm (rec 1: +1000.00 + 250.50 = +1250.50). |
| T2 | R002: overdraft = end balance **below zero** (exactly zero is not an overdraft). The program only flags; it charges no fee and does not reject. Is that intended? | **Agree** | `IF WS-ENDBAL < 0`; exactly zero is explicitly not an overdraft (fixture rec 3: 5.00 − 5.00 = 0.00 → flag `N`). Flag-only is coherent for a posting program that is not a fee engine — but if a fee is charged elsewhere, record where, so the absent logic is not misread as an omission. |
| T3 | R003: the batch total accumulates across the **whole file**, never per account. Is a file-wide total the intended meaning? | **Agree** | A file-wide net *is* the batch control total by definition. Assumption to record: nothing downstream expects per-account subtotals. |
| T4 | R005: the running batch total is written on **every** output record (only the last holds the grand total). Should it be a single trailer record instead? | **Agree** (contract needed) | Confirmed in evidence: the field climbs 250.50 → 100.25 → … → 95.26 across all 8 records, so only the last holds the grand total and any consumer reading an earlier record sees a partial figure. Either write a trailer, or state explicitly in the catalog that the last record carries the total. |
| T5 | There is **no grouping by account**: the same account twice gives two independent rows, each using only its own opening balance. Intended? | **Need info** | **Blocking.** Depends on a fact the catalog does not state: can the input file hold more than one row per account? If yes (e.g. a day's postings), each row using only its own `T-OPENBAL` yields **wrong balances** for the second and later rows — they should chain — and the output is not order-independent. If the file is guaranteed one row per account, the behaviour is fine. State the input-file contract. |
| T6 | R004: sums that exceed 999,999,999.99 silently wrap (e.g. +0.01 on the maximum becomes +0.00, no error). Should production guard against it? | **Agree** | Guard needed. Demonstrated with *valid* input: rec 7 `−99999999999 − 5.00` → **−4.99**, flag `Y`. Production should raise `ON SIZE ERROR` / reject. |

## Additional findings (not in the original question set)

- **CORRECTION (Claude Code, verified 2026-10-17): the "malformed positive-overflow fixture" claim that
  was here is wrong.** `parity/GenTxnInput.java` line 22 contains `"+99999999999"` (eleven nines); the
  generated `TXNS.DAT` record 6 is `000000006+99999999999+000000001`, and the COBOL output for it is
  `+00000000000`. So that case does prove "999,999,999.99 + 0.01 wraps to +0.00". Nothing needs fixing.
- **Input contract is undocumented.** Both A1 (rate period) and T5 (account grouping) turn on facts that
  exist only outside the code — run cadence and input-file cardinality. The catalogs should name both.

## Sign-off
- Reviewer name: ______________________ (**not signed — Karthik has not reviewed**)   Date: ______________
- Result: ☐ Catalogs reviewed, no changes  ☐ Reviewed, changes requested (see notes)

> Proposed changes (not yet approved by the reviewer): **A1** (settle the rate period — blocking),
> **T5** (state the input-file contract — blocking), **A4/T6** (add overflow guards in production; keep
> truncation only in the parity twin), **T4** (trailer, or an explicit last-record contract).
