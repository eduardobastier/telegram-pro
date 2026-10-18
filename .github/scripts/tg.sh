#!/usr/bin/env bash
# Messages to the private CI group.
#   tg.sh send "<html text>"
#   tg.sh file <path> "<html caption>"
# Needs TELEGRAM_BOT_TOKEN and CI_CHAT_ID. Never fails the job (notifications are best effort).
set -uo pipefail
API="https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}"
case "$1" in
    send)
        curl -sS -o /dev/null --fail-with-body "$API/sendMessage" \
            --form-string chat_id="$CI_CHAT_ID" --form-string parse_mode=HTML \
            --form-string disable_web_page_preview=true --form-string text="$2" || true
        ;;
    file)
        curl -sS -o /dev/null --fail-with-body "$API/sendDocument" \
            --form-string chat_id="$CI_CHAT_ID" --form-string parse_mode=HTML \
            --form-string caption="$3" -F document=@"$2" || true
        ;;
esac
