---
name: cobol-explainer
description: Explain a COBOL batch program as a business-rule catalog in a fixed format, with the rules later used to write and parity-check the Java twin. Use when asked to explain or document a COBOL program, or to draft catalog entries for Karthik's review.
---

Explain any COBOL batch program as a business-rule catalog in the fixed format below,
so a business analyst can review it and the Java twin can be written from it.

## Output format (one entry per business rule)
For every distinct rule the program implements, emit an entry with exactly these
fields, in this order:

- **ID**: `ACCT-INT-R001` style, one per rule.
- **Description**: plain-English, no COBOL jargon — what the business says must happen.
- **Source lines**: the exact `cobol/<program>.cbl` line(s) that implement this rule
  (cite line numbers).
- **Inputs**: the fields this rule reads (name, PIC, meaning).
- **Outputs**: the fields this rule writes (name, PIC, meaning).
- **Edge cases**: any boundary you can see in the PICs / arithmetic (zero, negative,
  half-cent rounding, field-limit overflow/truncation, empty file).

## Rules to follow when explaining
1. Treat the **COBOL reference source as truth** — do not invent rules the code does not
   implement, and do not omit ones it does.
2. Call out **implied behaviour explicitly**: `ROUNDED`, truncation on PIC overflow,
   sign handling, anything a reviewer would miss.
3. Do not translate to Java in this step. The catalog is business-rule language only.
4. Keep every entry to the exact field set above — fixed format, so Karthik can review
   them quickly and the harness can consume them.

## Output header
Start with a one-line summary: program name, what it does, and total rule count,
then the rule entries, then a blank line and a one-line note of anything you flagged
as uncertain.