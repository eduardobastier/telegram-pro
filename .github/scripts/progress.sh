#!/usr/bin/env bash
# Live build status in the CI group: edits one message every minute with a progress bar,
# current stage, start time and estimated end (Brasília time).
#   progress.sh start                 sends the message, saves its id and starts the updater
#   progress.sh final <success|failure|cancelled> [extra html]
# Needs TELEGRAM_BOT_TOKEN, CI_CHAT_ID, BRANCH, RUN_URL, GH_TOKEN; Gradle output in $RUNNER_TEMP/gradle.log.
set -uo pipefail

API="https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}"
STATE="$RUNNER_TEMP/progress"
mkdir -p "$STATE"

brt() { TZ=America/Sao_Paulo date -d "@$1" +%H:%M; }

esc() { sed -e 's/&/\&amp;/g' -e 's/</\&lt;/g' -e 's/>/\&gt;/g'; }

header() {
    echo "$1 <b>$2</b>"
    echo
    echo "<b>Branch:</b> <code>${BRANCH}</code>"
    echo "<b>Versão:</b> $(cat "$STATE/version")"
    echo "<b>Base:</b> Telegram $(grep '^APP_VERSION_NAME=' gradle.properties | cut -d= -f2)"
    echo "<b>Commit:</b> <a href=\"${GITHUB_SERVER_URL}/${GITHUB_REPOSITORY}/commit/$(git rev-parse HEAD)\">$(git rev-parse --short HEAD)</a>"
}

stage() {
    local log="$RUNNER_TEMP/gradle.log" task
    [ -s "$log" ] || { echo "Preparando ambiente"; return; }
    task=$(grep -o '^> Task [^ ]*' "$log" | tail -n1)
    case "$task" in
        *NativeBuild*|*CMake*|*Ndk*|*JniLibs*|*NativeLibs*) echo "Compilando código nativo (C++)" ;;
        *Kotlin*|*Java*|*Javac*) echo "Compilando Java" ;;
        *minify*|*R8*|*Dex*|*dex*) echo "Otimizando e gerando DEX (R8)" ;;
        *package*|*assemble*|*Sign*|*sign*) echo "Empacotando e assinando o APK" ;;
        "") echo "Configurando o Gradle" ;;
        *) echo "Compilando" ;;
    esac
}

bar() {
    local filled=$(( $1 / 10 )) i out=""
    for i in 1 2 3 4 5 6 7 8 9 10; do
        if [ "$i" -le "$filled" ]; then out="${out}▓"; else out="${out}░"; fi
    done
    printf '%s' "$out"
}

edit() {
    curl -sS -o /dev/null "$API/editMessageText" \
        --form-string chat_id="$CI_CHAT_ID" --form-string message_id="$(cat "$STATE/message_id")" \
        --form-string parse_mode=HTML --form-string disable_web_page_preview=true --form-string text="$1" || true
}

render_running() {
    local start expected now elapsed percent eta remaining
    start=$(cat "$STATE/start")
    expected=$(cat "$STATE/expected")
    now=$(date +%s)
    elapsed=$(( now - start ))
    percent=$(( elapsed * 100 / expected ))
    [ "$percent" -gt 99 ] && percent=99
    eta=$(( start + expected ))
    remaining=$(( (eta - now) / 60 ))
    {
        header "🟡" "Build em andamento"
        echo
        echo "$(bar "$percent") ${percent}%"
        echo "<b>Etapa:</b> $(stage)"
        echo "<b>Início:</b> $(brt "$start") (Brasília)"
        if [ "$remaining" -gt 0 ]; then
            echo "<b>Previsão de término:</b> $(brt "$eta") (faltam ~${remaining} min)"
        else
            echo "<b>Previsão de término:</b> $(brt "$eta") (passando do tempo previsto)"
        fi
        echo "<a href=\"${RUN_URL}\">Logs</a>"
    }
}

case "$1" in
    start)
        date +%s > "$STATE/start"
        grep -m1 -o '^## v[0-9]*\.[0-9]*\.[0-9]*' CHANGELOG.md | cut -c4- > "$STATE/version"
        # expected duration: average of the last successful builds (default 47 min)
        gh run list --workflow build.yml --status success --limit 5 --json createdAt,updatedAt \
            --jq '[.[] | ((.updatedAt | fromdate) - (.createdAt | fromdate))] | if length > 0 then (add / length | floor) else 2820 end' \
            > "$STATE/expected" 2>/dev/null || echo 2820 > "$STATE/expected"
        [ -s "$STATE/expected" ] || echo 2820 > "$STATE/expected"
        curl -sS "$API/sendMessage" \
            --form-string chat_id="$CI_CHAT_ID" --form-string parse_mode=HTML \
            --form-string disable_web_page_preview=true --form-string text="$(render_running)" \
            | python3 -c "import json,sys; print(json.load(sys.stdin)['result']['message_id'])" > "$STATE/message_id" || true
        [ -s "$STATE/message_id" ] || exit 0
        (
            while [ ! -f "$STATE/done" ]; do
                sleep 60
                [ -f "$STATE/done" ] || edit "$(render_running)"
            done
        ) > /dev/null 2>&1 &
        echo $! > "$STATE/updater_pid"
        ;;
    final)
        touch "$STATE/done"
        [ -s "$STATE/updater_pid" ] && kill "$(cat "$STATE/updater_pid")" 2> /dev/null
        [ -s "$STATE/message_id" ] || exit 0
        start=$(cat "$STATE/start")
        now=$(date +%s)
        took=$(( (now - start) / 60 ))
        case "$2" in
            success) icon="✅"; title="Build concluído" ;;
            cancelled) icon="⚪"; title="Build cancelado" ;;
            *) icon="❌"; title="Build falhou" ;;
        esac
        edit "$(
            header "$icon" "$title"
            echo
            echo "<b>Início:</b> $(brt "$start") (Brasília)"
            echo "<b>Fim:</b> $(brt "$now") (Brasília)"
            echo "<b>Duração:</b> ${took} min"
            [ "$2" = "failure" ] && echo "<b>Etapa:</b> $(stage)"
            [ -n "${3:-}" ] && echo "$3"
            echo "<a href=\"${RUN_URL}\">Logs</a>"
        )"
        ;;
esac
