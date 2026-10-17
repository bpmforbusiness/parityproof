# Business review checklist — for Karthik (≈15 minutes)

Review `ACCOUNT-INT.md` and `TXN-POST.md`. For each question tick **Agree / Disagree / Need info** and add
a note. Nothing here is decided yet; these are the open points the drafts flagged. When you finish, fill in
the sign-off box and tell Abdul — only then can the catalogs be called "reviewed".

## ACCOUNT-INT (interest run)
| # | Question | Agree / Disagree / Need info | Notes |
|---|---|---|---|
| A1 | R001: the 3.5% rate is a fixed constant in the program. Is it an **annual** rate applied once per run, or a per-period rate? (The code does not say.) | | |
| A2 | R001: rounding to the nearest cent, half away from zero — is that the intended business rounding? | | |
| A3 | R002: new balance = balance + rounded interest. Correct? | | |
| A4 | R004: a balance near 999,999,999.99 produces a new balance that **silently loses its top digit** (e.g. becomes 034,999,999.99). Should production **reject / flag** this instead of truncating? (The Java twin copies the truncation on purpose, for parity.) | | |

## TXN-POST (transaction posting)
| # | Question | Agree / Disagree / Need info | Notes |
|---|---|---|---|
| T1 | R001: end balance = opening balance + signed amount (credit +, debit −). Correct? | | |
| T2 | R002: overdraft = end balance **below zero** (exactly zero is not an overdraft). The program only flags; it charges no fee and does not reject. Is that intended? | | |
| T3 | R003: the batch total accumulates across the **whole file**, never per account. Is a file-wide total the intended meaning? | | |
| T4 | R005: the running batch total is written on **every** output record (only the last holds the grand total). Should it be a single trailer record instead? | | |
| T5 | There is **no grouping by account**: the same account twice gives two independent rows, each using only its own opening balance. Intended? | | |
| T6 | R004: sums that exceed 999,999,999.99 silently wrap (e.g. +0.01 on the maximum becomes +0.00, no error). Should production guard against it? | | |

## Sign-off
- Reviewer name: ______________________   Date: ______________
- Result: ☐ Catalogs reviewed, no changes  ☐ Reviewed, changes requested (see notes)
