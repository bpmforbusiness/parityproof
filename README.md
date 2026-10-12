# ParityProof — COBOL → Java parity modernization (IBM Bob Hackathon, Track 2)

We rebuild COBOL batch programs in Java 21 — and **prove the Java behaves exactly like the
original** by running both on identical inputs and comparing every output field, byte-for-byte.

Why this can win: many teams show Bob generating code. Fewer show *proof* the generated code is
correct. This is the first question any bank asks.

## Status (proof-of-concept — runnable today)
- ✅ Real COBOL batch runs under **GnuCOBOL 3.1** (the "open item" — resolved for a correct program).
- ✅ Java 21 twin produces **byte-identical** output: **100% field parity** (20/20, 5 edge-case records).
- ✅ The parity runner **catches real divergences**: an injected half-cent rounding change drops parity
  to 90% and pinpoints the exact fields (INTEREST / NEW-BAL).
- ⏳ Production slice to follow: CardDemo cards, COMP-3/BigDecimal, DB2/JCL, Azure dashboard, IBM Bob.

## Layout
```
bob/        rules.md · skills/ · hooks/ · mcp/        (Abdul — the Bob governance layers)
cobol/      ACCOUNT-INT.cbl + copybook.cpy + GnuCOBOL  (the reference runtime)
java/       AccountInterest.java                       (the modernized twin)
parity/     GenInput (edge cases) + ParityRunner → result.json
evidence/   auto-collected hook logs, diffs, parity runs   ← proves deep tool adoption
dashboard/  (Azure SWA — Satish lane; CI builds it)
.github/    ci.yml — runs COBOL + Java + parity on every push, gates on parity PASS
Dockerfile  GnuCOBOL reference runtime (Satish lane)
```

## Run the pipeline (local, no Docker needed)
Prereqs: `gnucobol` (cobc) + Java 21.  Then:
```bash
bash run.sh     # generate inputs -> compile+run COBOL -> run Java -> field-by-field parity diff
cat evidence/result.json
```
CI (`./.github/workflows/ci.yml`) does the same on every push and **fails the build if parity is not 100%.

## How a change flows
COBOL → Bob explains → rule catalog → Bob writes Java → hook runs parity → mismatch → Bob fixes → green.

## The four Bob governance layers (bob/)
- `bob/rules.md` — global rules (sources cited, per-language style, every fix ships a test).
- `bob/skills/` — COBOL/RPG explainer + codegen skills.
- `bob/hooks/` — checkpoint hook: **compile → test → parity**; fails the step on red.
- `bob/mcp/` — in-session data/schema context (MCP servers).

## Judging fit
Tool Usage & Depth of Adoption (30%) — `evidence/` + the four layers, not narrated.
Innovation & Relevance (30%) — parity against a real COBOL reference, not a staged app.
Functionality & Execution (30%) — green CI, live dashboard. Presentation (10%) — 3-min demo w/ a caught mismatch.

## License / provenance
Proof uses an original COBOL program we wrote for the demo; CardDemo (AWS) license to be confirmed
before its code enters. See `cobol/copybook.cpy`.