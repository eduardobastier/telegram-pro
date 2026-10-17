#!/usr/bin/env bash
# Prepares ./Telegram: official Telegram at the commit in UPSTREAM_REF + patches/ applied as commits.
# Usage: scripts/setup.sh [upstream-ref]
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
UPSTREAM_URL="https://github.com/DrKLO/Telegram.git"
REF="${1:-$(cat "$ROOT/UPSTREAM_REF")}"
DIR="$ROOT/Telegram"

if [ ! -d "$DIR/.git" ]; then
    git init -q "$DIR"
fi
if [ -n "$(git -C "$DIR" status --porcelain --untracked-files=no)" ]; then
    echo "Telegram/ has uncommitted changes; commit them and run scripts/export-patches.sh first." >&2
    exit 1
fi

echo "==> Fetching upstream $REF"
git -C "$DIR" fetch -q --depth 1 "$UPSTREAM_URL" "$REF"
git -C "$DIR" checkout -q -B pro FETCH_HEAD
git -C "$DIR" submodule update -q --init --recursive --depth 1

echo "==> Applying patches"
git -C "$DIR" -c user.name="${GIT_AUTHOR_NAME:-Telegram Pro}" -c user.email="${GIT_AUTHOR_EMAIL:-telegram-pro@users.noreply.github.com}" \
    am -q --3way "$ROOT"/patches/*.patch || {
    echo "Patches did not apply cleanly. Fix conflicts in Telegram/, then:" >&2
    echo "  git -C Telegram add <files> && git -C Telegram am --continue && scripts/export-patches.sh" >&2
    exit 1
}
echo "==> Ready: Telegram/ is upstream $(git -C "$DIR" rev-parse --short FETCH_HEAD) + $(ls "$ROOT"/patches/*.patch | wc -l | tr -d ' ') patches"
