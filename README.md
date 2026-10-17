# ParityProof — COBOL → Java parity modernization (IBM Bob Hackathon, Track 2)

We rebuild COBOL batch programs in Java 21 — and **prove the Java behaves exactly like the
original** by running both on identical inputs and comparing every output field, byte-for-byte.

Why this can win: many teams show Bob generating code. Fewer show *proof* the generated code is
correct. This is the first question any bank asks.

## Status (proof-of-concept — runnable today)
- ✅ Two real COBOL batch programs run under **GnuCOBOL** (`cobc`): `ACCOUNT-INT` (interest) and
  `TXN-POST` (signed amounts, COMP-3 packed balances). Re-verified on 2026-10-13 with
  **GnuCOBOL 3.2.0** (macOS/Homebrew). The Debian/Ubuntu `gnucobol` package used by the
  Dockerfile and CI has not been version-checked; earlier notes saying "3.1" are unconfirmed.
- ✅ Java 21 twins produce **byte-identical** output: **100% field parity — 64/64 fields**
  (ACCOUNT-INT 24/24 over 6 records, TXN-POST 40/40 over 8 records). Edge cases include a balance
  at the 9(9)V99 maximum, where COBOL silently truncates the high-order digit on overflow, and
  COMP-3 overflow/sign cases; the twins reproduce that behaviour.
- ✅ The parity runner **catches real divergences**: an injected half-cent rounding change drops
  ACCOUNT-INT parity to ~83% and pinpoints the exact fields (INTEREST / NEW-BAL). A real PASS and
  an injected-FAIL run are committed under `evidence/runs/` (the FAIL is a deliberate mutation,
  not a Bob change).
- ✅ A static dashboard and a CI workflow are written (see below). The CI workflow has **not yet
  run on GitHub** (pushing it needs a workflow-scoped token). The dashboard is **live on GitHub
  Pages**: <https://bpmforbusiness.github.io/parityproof/>.

## What's built vs. planned (honest)
Every folder below **exists in this repo right now** unless marked *(planned)*:
```
bob/        governance: rules.md · skills/cobol-explainer/SKILL.md · hooks/ (parity.sh, hooks.example.json, README)
            · mcp/README.md (PLANNED — nothing wired) · EVIDENCE-TODO.md · evidence-files/ (PLACEHOLDERS only)
cobol/      ACCOUNT-INT.cbl + copybook.cpy · TXN-POST.cbl + txn-post.cpy — original programs we wrote;
            the GnuCOBOL reference runtime
java/       AccountInterest.java · TxnPost.java — the modernized twins
parity/     GenInput + GenTxnInput (edge cases) · ParityRunner (reads programs.txt) · copybook layouts
            → result.json (per program, per record, per field)
evidence/   runs/<UTC-ts>/ (inputs, both outputs, result.json) + latest/ + hooks/   ← real, committed runs
catalog/    ACCOUNT-INT.md · TXN-POST.md — business-rule catalogs (DRAFT, for Karthik's review)
dashboard/  index.html — static page (live on GitHub Pages): overall + per-program parity, per-field rate, mismatch drill-down
deck/       pitch_deck.pdf + index.html source — 8-slide submission deck
.github/    workflows/ci.yml — runs run.sh, fails the build unless parity is 100%
            (kept LOCAL and gitignored — pushing it needs a workflow-scoped token; never run on GitHub yet)
Dockerfile  GnuCOBOL + Java reference image (not built or tested here)
run.sh      the whole pipeline, all programs; non-zero exit unless every program is at 100%
```
- **Bob adoption evidence**: partly done. Committed: Bob's replies and our reviews of them
  (`bob/evidence-files/sessions/`), six screenshots with captions (`bob/evidence-files/screenshots/`)
  and one real hook firing (`evidence/hooks/`). *(Planned — see bob/EVIDENCE-TODO.md)*: a session
  export and the Bobalytics export.
- **MCP context for Bob**: *(planned — nothing wired; see bob/mcp/README.md)*.
- **Migration onto AWS CardDemo source**: *(planned — license to be confirmed first)*.

## Dashboard on GitHub Pages
**Live: <https://bpmforbusiness.github.io/parityproof/>** (checked 2026-10-17: shows 100%, 64/64
fields, 2/2 programs). The static dashboard needs no build step; Pages serves the repo root from
`main` (`index.html` redirects to `dashboard/`, `.nojekyll` disables Jekyll). It shows whatever is
committed in `evidence/latest/result.json`, so push a fresh `bash run.sh` run to update it, and do
not push a failing run to `evidence/latest` while the public link is being judged.

## Run the pipeline (local, no Docker needed)
Prereqs: `gnucobol` (cobc) + Java 21.  Then:
```bash
bash run.sh     # generate inputs -> compile+run COBOL -> run Java -> field-by-field parity diff
# each run is captured under evidence/runs/<UTC-ts>/ with evidence/latest/ pointing at the newest;
# exits non-zero unless every program in parity/programs.txt is at 100% field parity
```

## How a change flows
COBOL → Bob explains (cobol-explainer skill) → rule catalog → Java twin changed (the twins here were
written with Claude Code; Bob has so far made a comment-only edit) → parity hook runs
compile→run→parity → `evidence/hooks/<ts>.log` + CI gate. The "mismatch → Bob diagnoses and fixes"
loop is the intended design; it has not been demonstrated with Bob yet.

## The four Bob governance layers (bob/)
- `bob/rules.md` — global rules: COMP-3/PIC → BigDecimal, preserve COBOL `ROUNDED` + truncation,
  copybook → Java record, cite source lines, every change ships a parity run. (Land as
  `.bob/rules/parity-proof.md`.)
- `bob/skills/cobol-explainer/` — SKILL.md that explains a COBOL program as business rules in a
  fixed format (rule ID, description, source lines, inputs, outputs, edge cases). (Land as
  `.bob/skills/cobol-explainer/`.)
- `bob/hooks/parity.sh` — compile → run → parity; exits non-zero on any parity FAIL and logs to
  `evidence/hooks/`. `hooks.example.json` is the fragment for `.bob/settings.json` (format checked
  against bob.ibm.com/docs). Per those docs a `PostToolUse` hook is **non-blocking**, so it
  records parity after an edit rather than stopping Bob. It has fired inside a Bob session once
  (Bob's `apply_diff` edit, `evidence/hooks/20261017T163338Z.log`, 64/64 PASS); a FAIL firing
  has not been captured.
- `bob/mcp/README.md` — MCP context is **PLANNED (not wired)** — recorded honestly, with the
  security rule from the docs (secrets via env vars, never committed).

## Judging fit
- **Tool Usage & Depth of Adoption (30%)** — the `bob/` layer (rules, skill, hook, MCP plan) and
  real `evidence/` files (committed runs, hook logs) are in the repo, including one **real hook
  firing** from a Bob edit, Bob's own rule explanation with our review of its errors, and
  screenshots (skill used, hooks active, edit). Still missing: a session export and the Bobalytics
  export (`bob/EVIDENCE-TODO.md`). Bob's dedicated Plan mode was not used (plans came from Ask mode).
  The Java twins and the catalogs were drafted with Claude Code; Bob explained rules and made a
  comment-only edit.
- **Innovation & Relevance (30%)** — parity against a **real COBOL reference**. To be transparent
  about provenance: the reference programs are **original programs the team wrote**, validated by
  running them under GnuCOBOL — not a curated sample and not a staged toy. Migration onto
  **AWS CardDemo** source is the intended scale-up; its license is still to be confirmed before any
  of its code enters the repo.
- **Functionality & Execution (30%)** — `bash run.sh` gates on 100% parity across two programs;
  a CI workflow that does the same is written but has not run on GitHub; the dashboard is built
  and hosted on GitHub Pages.
- **Presentation (10%)** — 8-slide pitch deck (`deck/pitch_deck.pdf`) + a 3-min demo that includes
  a genuine captured mismatch (the injected-FAIL run at `evidence/runs/20261013T134034Z`).

## License / provenance
The proof programs are original work we wrote; CardDemo (AWS) license to be confirmed before its code
enters. See `cobol/copybook.cpy`, `cobol/txn-post.cpy` and `catalog/`.