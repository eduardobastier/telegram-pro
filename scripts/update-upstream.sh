#!/usr/bin/env bash
# Moves the base to the latest official Telegram commit and re-applies the patches.
# Usage: scripts/update-upstream.sh [upstream-ref]   (default: latest master)
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
NEW_REF="${1:-$(git ls-remote https://github.com/DrKLO/Telegram.git refs/heads/master | cut -f1)}"
OLD_REF="$(cat "$ROOT/UPSTREAM_REF")"

if [ "$NEW_REF" = "$OLD_REF" ]; then
    echo "Already on upstream $OLD_REF"
    exit 0
fi

"$ROOT/scripts/setup.sh" "$NEW_REF"
echo "$NEW_REF" > "$ROOT/UPSTREAM_REF"
"$ROOT/scripts/export-patches.sh" "$NEW_REF"
echo "Base updated: $OLD_REF -> $NEW_REF"
