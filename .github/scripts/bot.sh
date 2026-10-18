#!/usr/bin/env bash
# Telegram Pro CI bot: long-polls the bot for commands sent by the admin in the private CI group
# and answers them right away. Runs until STOP_AFTER seconds (GitHub jobs are limited to 6 h);
# the scheduled workflow starts the next run.
# Needs TELEGRAM_BOT_TOKEN, CI_CHAT_ID, CI_ADMIN_ID, GH_TOKEN, REPO_URL.
set -uo pipefail

API="https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}"
STOP_AFTER="${STOP_AFTER:-21000}"
TG=".github/scripts/tg.sh"

version_of() {
    git show "origin/$1:CHANGELOG.md" | grep -m1 -o '^## v[0-9]*\.[0-9]*\.[0-9]*' | cut -c4-
}

handle() {
    local line="$1" cmd arg
    cmd=$(echo "$line" | awk '{print $1}' | cut -d@ -f1)
    arg=$(echo "$line" | awk '{print $2}')
    echo "==> $cmd $arg"
    case "$cmd" in
        /buildmain|/builddev)
            local branch="${cmd#/build}"
            gh workflow run build.yml --ref main -f branch="$branch"
            $TG send "▶️ Build da <code>${branch}</code> solicitado. <a href=\"${REPO_URL}/actions/workflows/build.yml\">Acompanhar</a>"
            ;;
        /release)
            git fetch -q origin main dev
            local version
            version=$(version_of dev)
            if gh release view "$version" > /dev/null 2>&1; then
                $TG send "⚠️ A ${version} já foi publicada. Adicione uma versão nova no CHANGELOG.md da dev."
                return
            fi
            git checkout -q -B main origin/main
            if git merge --no-edit -m "Release ${version}" origin/dev && git push origin main; then
                gh workflow run build.yml --ref main -f branch=main
                $TG send "🚀 dev → main (${version}). Build iniciado; a Release e o post no canal saem ao terminar."
            else
                git merge --abort 2>/dev/null || true
                $TG send "❌ Não foi possível levar a dev para a main (conflito ou push recusado). Faça o merge manualmente."
            fi
            ;;
        /update)
            gh workflow run update-upstream.yml --ref main
            $TG send "🔄 Verificando nova versão do Telegram oficial…"
            ;;
        /status)
            git fetch -q origin main dev
            gh run list --limit 40 --json workflowName,status,conclusion,headBranch,displayTitle,url,createdAt,updatedAt > runs.json
            local latest
            latest=$(gh release view --json tagName --jq .tagName 2>/dev/null || echo "nenhuma")
            $TG send "$(python3 .github/scripts/status.py runs.json "$latest" "$(version_of main)" "$(version_of dev)")"
            ;;
        /help|/start)
            $TG send "🤖 <b>Telegram Pro CI — ajuda</b>

/buildmain — compila a branch <code>main</code>. Se a versão do topo do CHANGELOG.md ainda não tiver release, cria a GitHub Release e posta o APK no canal; senão só compila.

/builddev — compila a branch <code>dev</code> e envia o APK só aqui no grupo, para teste. Não cria release nem posta no canal.

/release — leva a <code>dev</code> para a <code>main</code> e publica a versão do topo do CHANGELOG.md (Release + canal). Recusa se essa versão já foi publicada.

/update — verifica agora se saiu versão nova do Telegram oficial; se sim, faz merge, cria a próxima versão e publica.

/status — mostra os últimos builds e a última release.

/help — esta mensagem.

Os avisos de início, fim e falha dos builds chegam aqui automaticamente."
            ;;
        *)
            $TG send "❓ Comando desconhecido: <code>${cmd}</code>. Envie /help para ver os comandos."
            ;;
    esac
}

git config user.name "github-actions[bot]"
git config user.email "41898282+github-actions[bot]@users.noreply.github.com"

offset=""
while [ "$SECONDS" -lt "$STOP_AFTER" ]; do
    if ! curl -sS --max-time 70 -o updates.json \
        "$API/getUpdates?timeout=50&allowed_updates=%5B%22message%22%5D${offset:+&offset=$offset}"; then
        sleep 5
        continue
    fi
    # one command per line; the first line is the next offset (empty when there were no updates)
    python3 - > batch.txt <<'PY'
import json, os
updates = json.load(open('updates.json')).get('result', [])
print(updates[-1]['update_id'] + 1 if updates else '')
for u in updates:
    m = u.get('message') or {}
    if str(m.get('chat', {}).get('id')) != os.environ['CI_CHAT_ID']:
        continue
    if str(m.get('from', {}).get('id')) != os.environ['CI_ADMIN_ID']:
        continue
    text = (m.get('text') or '').strip()
    if text.startswith('/'):
        print(text.replace('\n', ' '))
PY
    next_offset=$(head -n1 batch.txt)
    [ -n "$next_offset" ] && offset="$next_offset"
    tail -n +2 batch.txt | while IFS= read -r line; do
        [ -n "$line" ] && handle "$line"
    done
done

# confirm the last updates so the next run does not repeat them
[ -n "$offset" ] && curl -sS -o /dev/null "$API/getUpdates?offset=${offset}&timeout=0"
echo "Stopping after ${SECONDS}s; the next scheduled run takes over."
