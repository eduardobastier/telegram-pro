#!/usr/bin/env bash
# Writes the commits made on top of upstream in ./Telegram back into patches/.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DIR="$ROOT/Telegram"
BASE="${1:-$(cat "$ROOT/UPSTREAM_REF")}"

rm -f "$ROOT"/patches/*.patch
git -C "$DIR" format-patch -q --zero-commit --no-signature "$BASE"..HEAD -o "$ROOT/patches"
ls "$ROOT/patches"
