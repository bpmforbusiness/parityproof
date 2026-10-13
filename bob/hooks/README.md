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
- Confirmed tool name: `write_file`. Bob's other edit tools' names are **not confirmed** by
  the docs we read, so the example only matches `write_file`.
- Not confirmed: that this exact hook has fired inside a Bob session. The one committed
  `evidence/hooks/*.log` was produced by running `parity.sh` directly. Real hook firings must be
  copied from the Oracle VM (see `bob/EVIDENCE-TODO.md`).
