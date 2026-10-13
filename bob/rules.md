# ParityProof — Bob project rules (general, all modes)

These rules load in every Bob conversation on this project. Drop this file into
`.bob/rules/parity-proof.md` (project root) or `~/.bob/rules/` (global) — see
https://bob.ibm.com/docs/ide/configuration/rules (directory-based configuration).

The source of truth for behaviour is the **COBOL reference runtime**, never the Java
twin and never the tests. If a parity diff says the Java differs from COBOL, the Java
is wrong — do not "fix" the test or the rule catalog to match.

## Parity-first rules (non-negotiable)
1. **A change is not done until parity is 100%.** Every edit to `java/` or `cobol/`
   must ship with a parity test run (`bash run.sh`). "Tests pass" is not enough; the
   field-level parity diff against the COBOL reference is the acceptance test.
2. **Never alter the COBOL reference programs' behaviour.** `cobol/*.cbl`
   (ACCOUNT-INT, TXN-POST) are the oracle. Modernize the Java, not the COBOL.
3. **Reproduce COBOL semantics exactly, including bugs.** Preserve COBOL `ROUNDED`
   semantics and its **high-order truncation on field overflow** (PIC 9(9)V99 silently
   drops digits that don't fit). These are latent hazards in the original program and
   are preserved deliberately for parity (see comment in `java/AccountInterest.java`).
4. **Cite COBOL source lines in every Java comment** that maps a rule, so a business
   rule can be traced to the exact COBOL statement that produces it.

## Numeric mapping rules
5. **COMP-3 / packed decimal and `PIC 9(n)V99` -> java.math.BigDecimal**. (COMP-3 is a storage
   format, not a different number: the value maps to BigDecimal either way. The parity files
   are DISPLAY text, so COMP-3 fields live in working storage — see `TXN-POST`.)
   - Integer part + implied decimals -> `new BigDecimal(BigInteger, scale)`.
   - `SIGN LEADING SEPARATE` -> a leading `+`/`-` char then the digits; a zero result is `+`.
   - Never use `double`/`float` — money must not lose precision.
   - Set a rounding mode explicitly at every `setScale` call; use `RoundingMode.HALF_UP`
     in this project because that matches `COBOL ROUNDED` (COBOL rounds on the final
     intermediate to the target PIC decimal places).
6. **Truncation:** if a value is wider than its PIC (e.g. 10 integer digits into
   `9(9)V99`), COBOL drops the high-order digits. The Java `zeroPad` must reproduce
   this (keep the low-order `width` digits, keep the sign) — **do not throw or silently
   zero-fill**. Applies to COMP-3 targets too (observed: GnuCOBOL stores
   999999999.99 + 0.01 into `S9(9)V99 COMP-3` as 0.00).

## Copybook mapping rules
7. **Copybook -> Java record class.** Each `01` group becomes a `record`/POJO with one
   field per elementary item; layout is fixed-width, from `PIC` sizes.
8. **Fixed-width I/O for parity.** Both sides read the same flat input (`ACCOUNTS.DAT`)
   and write the same fixed-width format (`PIC 9(9)` pad to 9, `PIC 9(9)V99` to 11, no
   decimal point), so the diff is byte-for-byte.
9. **Keep `parity/copybook.txt` in sync** with `cobol/copybook.cpy` — the runner derives
   field offsets from it.

## Process rules
10. **Scope changes with the team first.** Lock the program scope and rule catalog before
    generating Java (Plan mode).
11. **Update `catalog/` alongside code.** When a program change lands, the business-rule
    catalog for that program gets a matching entry (for Karthik's review).
12. **Do not generate mock evidence.** Hook logs, parity runs and diffs come only from
    real `run.sh` executions; never commit fabricated session logs.