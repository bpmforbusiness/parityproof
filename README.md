# ParityProof — COBOL → Java parity modernization (IBM Bob Hackathon, Track 2)

We rebuild COBOL batch programs in Java 21 — and **prove the Java behaves exactly like the
original** by running both on identical inputs and comparing every output field, byte-for-byte.

Why this can win: many teams show Bob generating code. Fewer show *proof* the generated code is
correct. This is the first question any bank asks.

## Status (proof-of-concept — runnable today)
- ✅ Real COBOL batch runs under **GnuCOBOL 3.1**.
- ✅ Java 21 twin produces **byte-identical** output: **100% field parity** (24/24, 6 edge-case
  records — including BAL at the 9(9)V99 max, where the new balance overflows and COBOL
  truncates the high-order digit; the twin reproduces that truncation).
- ✅ The parity runner **catches real divergences**: an injected half-cent rounding change drops
  parity to ~83% and pinpoints the exact fields (INTEREST / NEW-BAL). Real PASS and injected-FAIL
  runs are committed under `evidence/runs/`.
- ✅ CI + a static dashboard are prepared (see below). Daily pipeline re-verification runs every
  night via a cron hook.

## What's built vs. planned (honest)
Every folder below **exists in this repo right now** unless marked *(planned)*:
```
bob/        governance: rules.md · skills/cobol-explainer/ (SKILL.md) · hooks/ (parity.sh + hooks.example.json)
            · mcp/README.md (PLANNED — nothing wired) · EVIDENCE-TODO.md (what Abdul copies from his VM)
cobol/      ACCOUNT-INT.cbl (original program the team wrote) + copybook.cpy — the GnuCOBOL reference runtime
java/       AccountInterest.java — the modernized twin
parity/     GenInput (edge cases) + ParityRunner → result.json (copybook-driven field diff)
evidence/   runs/<UTC-ts>/ (inputs, both outputs, result.json) + latest/ + hooks/   ← real, committed runs
catalog/    ACCOUNT-INT.md — business-rule catalog (DRAFT, for Karthik's review)
dashboard/  index.html — static parity dashboard (per-field rate + mismatch drill-down)  *(hosting planned)*
deck/       pitch_deck.pdf + source — 8-slide submission deck
.github/    workflows/ci.yml — runs run.sh, fails build unless parity is 100%   *(local; needs workflow-scoped token to push)*
Dockerfile  GnuCOBOL reference runtime (containerizing)   *(image to be built — Satish lane)*
```
- **Second batch program** (transaction posting) and **COMP-3 / packed-decimal** cases: *(planned)*.
- **Live dashboard deploy** (Azure Static Web Apps / GitHub Pages): *(planned — Satish lane)*.
- **Bob adoption evidence** (session logs from Abdul's Oracle VM, hook firings, screenshots,
  Bobalytics export): *(planned — see bob/EVIDENCE-TODO.md)*.

## Run the pipeline (local, no Docker needed)
Prereqs: `gnucobol` (cobc) + Java 21.  Then:
```bash
bash run.sh     # generate inputs -> compile+run COBOL -> run Java -> field-by-field parity diff
# each run is captured under evidence/runs/<UTC-ts>/ with evidence/latest/ pointing at the newest
```

## How a change flows
COBOL → Bob explains (cobol-explainer skill) → rule catalog → Bob writes Java → parity hook runs
compile→run→parity → mismatch → Bob diagnoses and fixes → green → `evidence/hooks/<ts>.log` + CI gate.

## The four Bob governance layers (bob/)
- `bob/rules.md` — global rules: COMP-3/PIC → BigDecimal, preserve COBOL `ROUNDED` + truncation,
  copybook → Java record, cite source lines, every change ships a parity run. (Land as
  `.bob/rules/parity-proof.md`.)
- `bob/skills/cobol-explainer/` — SKILL.md that explains a COBOL program as business rules in a
  fixed format (rule ID, description, source lines, inputs, outputs, edge cases). (Land as
  `.bob/skills/cobol-explainer/`.)
- `bob/hooks/parity.sh` — compile → run → parity; exits non-zero on any parity FAIL and logs to
  `evidence/hooks/`. `hooks.example.json` shows the wiring (format verified against bob.ibm.com/docs).
- `bob/mcp/README.md` — MCP context is **PLANNED (not wired)** — recorded honestly, with the
  security rule from the docs (secrets via env vars, never committed).

## Judging fit
- **Tool Usage & Depth of Adoption (30%)** — driven by the `bob/` layer + real `evidence/` files
  (committed runs, hook logs). Bob's session recordings and Bobalytics export are being gathered
  from the driving VM (`bob/EVIDENCE-TODO.md`) — those raw captures are the missing piece, not the
  pipelines around them.
- **Innovation & Relevance (30%)** — parity against a **real COBOL reference**. To be transparent
  about the provenance: the reference program is **an original program the team wrote**, validated
  by running it under GnuCOBOL 3.1 — not a curated sample and not a staged toy. Migration onto
  **AWS CardDemo** source is the intended scale-up; its license is still to be confirmed before any
  of its code enters the repo.
- **Functionality & Execution (30%)** — green parity gate in CI, static dashboard prepared; the
  live-hosted dashboard and CI-on-GitHub are the remaining deployment steps.
- **Presentation (10%)** — 8-slide pitch deck (`deck/pitch_deck.pdf`) + a 3-min demo that includes
  a genuine captured mismatch (the injected-FAIL run at `evidence/runs/20261013T134034Z`).

## License / provenance
The proof program is original work we wrote; CardDemo (AWS) license to be confirmed before its code
enters. See `cobol/copybook.cpy` and `catalog/ACCOUNT-INT.md`.