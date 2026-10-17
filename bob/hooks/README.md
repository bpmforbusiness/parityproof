# bob/hooks

- `parity.sh` — runs `bash run.sh` (generate inputs → COBOL → Java → parity for every program)
  and writes the full log to `evidence/hooks/<UTC-ts>.log`. Exits non-zero on any parity FAIL.
- `hooks.example.json` — the `hooks` fragment to merge into **`.bob/settings.json`**
  (workspace) or `~/.bob/settings/settings.json` (global). Format source:
  <https://bob.ibm.com/docs/ide/configuration/lifecycle-hooks>.

## What the docs confirm — and what we have not confirmed
- Confirmed: hooks go under a `hooks` key in `settings.json`; `type` is `command`;
  `matcher` is a regex on the tool name (PreToolUse/PostToolUse only); `timeout` is in seconds.
- Confirmed: **`PostToolUse` is non-blocking** (stdout ignored; exit 2 has no effect). Only
  `PreToolUse` and `UserPromptSubmit` can block, with exit 2. So this hook *records* parity
  after an edit; it does not stop Bob. To make Bob stop on a FAIL you would need a different
  design (e.g. a PreToolUse gate) — not built.
- Tool names: the hooks page only uses `write_file` as an example. **Confirmed on 2026-10-17:**
  Bob's `apply_diff` edit triggers a `PostToolUse` hook; a names-only diagnostic recorded
  `grep`, `read_file` and `apply_diff` (`evidence/hooks/bob-tool-names-20261017.log`). The earlier
  matcher `^write_file$` did not match an edit and no log appeared. The example matcher also lists
  `write_to_file`, `insert_content` and `search_and_replace` (names from Bob's tools page);
  those three have **not** been seen firing.
- Real firing: Bob's `apply_diff` of `java/TxnPost.java` produced `evidence/hooks/20261017T163338Z.log`
  and `evidence/runs/20261017T163340Z` (64/64 PASS). Because PostToolUse is non-blocking, a FAIL
  would be recorded in the log but would not stop Bob; a FAIL firing has not been captured.
