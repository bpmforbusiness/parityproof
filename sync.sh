#!/usr/bin/env bash
# ParityProof check-in helper: stage everything, commit, push to origin/main.
#   ./sync.sh                        -> auto message
#   ./sync.sh "feat: add X"          -> your message
set -euo pipefail
msg="${1:-chore: check-in $(date -u +%Y-%m-%dT%H:%MZ)}"
git add -A
if git diff --cached --quiet; then
    echo "nothing to commit"
    exit 0
fi
git commit -q -m "$msg"
branch="$(git branch --show-current)"
git push -q origin "HEAD:$branch"
echo "synced $branch -> $(git rev-parse --short HEAD)"