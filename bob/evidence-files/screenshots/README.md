# Screenshots of Bob (IBM Bob IDE 2.2.3), taken by Abdul on 2026-10-17

Captured by Abdul in the Bob IDE and saved here unedited (06 was retaken). Captions and caveats written by Claude Code
after viewing each image. Bobcoin figures are as shown in Bob's task header.

| File | What it shows | Caveat |
|---|---|---|
| `01-skills-tab.png` | Bob Settings → Skills: `cobol-explainer` listed with scope **Workspace** and the toggle on, next to Bob's built-in skills. Right panel: the comment-edit task (`format()` Javadoc), "Applied diff to java/TxnPost.java", reply "done"; mode selector reads **Agent**; task cost 0.132 Bobcoins. | Taken after the edit, so the chat on the right is the later task. |
| `02-hooks-tab.png` | Bob Settings → Hooks: **Workspace hooks for parityproof (1 active)** — PostToolUse, matcher `^(write_file\|write_to_file\|apply_diff\|insert_content\|search_and_replace)$`, status Active, handler `sh .bob/...`. Global hooks: 0 active. | Full-screen capture (shows menu bar and Dock). The temporary names-only diagnostic hook had already been removed. |
| `03-ask-explanation.png` | Ask mode: Bob shows **"Used skill cobol-explainer"** and loads the skill text, then reads `cobol/TXN-POST.cbl`. Task cost 0.158 Bobcoins. | The prompt shown is the instruction text copied literally ("explain cobol/TXN-POST.cbl in the cobol-explainer format. Screenshot the skill prompt and the reply."). The reply itself is not visible in this capture. Whether this run produced either saved reply in `../sessions/` is not recorded. |
| `04-plan.png` | Bob's plan for a comment-only change to `fit()` in `TxnPost.java`, citing COBOL lines. Cost 0.079 Bobcoins. | The mode selector reads **Ask**, not Plan: this plan came from Ask mode. Bob's dedicated Plan mode has not been used. |
| `05-code-edit.png` | The "Implement the plan" task partway through: Bob reads files and the review file, "1 file changed". Cost 0.654 Bobcoins. | Mid-task capture; does not show the final diff or the parity table. |
| `06-hook-log.png` | The new hook log `evidence/hooks/20261017T163338Z.log` open in Bob's editor (run on `/Users/abdulqaadir/...`): ACCOUNT-INT 24/24, TXN-POST 40/40, overall 64/64 PASS. Explorer and terminal `ls` list it next to `bob-tool-names-20261017.log` and the earlier logs; the run folder `20261017T163340Z` is visible. | Retaken after the first capture showed the old Oct 13 log. The screenshot shows the log's content, not the moment the hook fired; the firing is evidenced by the log plus `bob-tool-names-20261017.log`. |

Nothing in these images was edited. They are supporting evidence; the committed hook log and run
under `evidence/` are the authoritative record.
