#!/usr/bin/env python3
# Prints the /status message (HTML, pt-BR) for the CI group.
# Usage: status.py <runs.json from gh run list> <latest release tag> <main version> <dev version>
import json
import sys
from datetime import datetime, timezone, timedelta

BRT = timezone(timedelta(hours=-3))


def ago(iso):
    when = datetime.fromisoformat(iso.replace('Z', '+00:00'))
    minutes = int((datetime.now(timezone.utc) - when).total_seconds() // 60)
    if minutes < 1:
        return 'agora'
    if minutes < 60:
        return f'há {minutes} min'
    if minutes < 24 * 60:
        return f'há {minutes // 60} h'
    return when.astimezone(BRT).strftime('%d/%m %H:%M')


def duration(start, end):
    a = datetime.fromisoformat(start.replace('Z', '+00:00'))
    b = datetime.fromisoformat(end.replace('Z', '+00:00'))
    minutes = int((b - a).total_seconds() // 60)
    return f'{minutes // 60} h {minutes % 60:02d} min' if minutes >= 60 else f'{minutes} min'


def state(run):
    if run['status'] != 'completed':
        return '🟡', 'Em andamento' if run['status'] == 'in_progress' else 'Na fila'
    return {
        'success': ('✅', 'Concluído'),
        'failure': ('❌', 'Falhou'),
        'cancelled': ('⚪', 'Cancelado'),
    }.get(run['conclusion'], ('⚠️', run['conclusion'] or 'Desconhecido'))


runs = json.load(open(sys.argv[1]))
latest, main_version, dev_version = sys.argv[2], sys.argv[3], sys.argv[4]

builds = [r for r in runs if r['workflowName'] == 'Build'][:5]
updates = [r for r in runs if r['workflowName'] == 'Update upstream'][:1]

lines = [
    '📊 <b>Status do Telegram Pro</b>',
    '',
    f'📦 Última versão publicada: <b>{latest}</b>',
    f'🌿 main: {main_version} · dev: {dev_version}',
    '',
    '<b>Últimos builds</b>',
]
if not builds:
    lines.append('Nenhum build ainda.')
for run in builds:
    icon, label = state(run)
    # run-name is "Build <branch>"
    branch = run['displayTitle'].split(' ', 1)[1] if run['displayTitle'].startswith('Build ') else run['headBranch']
    detail = ago(run['createdAt'])
    if run['status'] == 'completed':
        detail += f' · levou {duration(run["createdAt"], run["updatedAt"])}'
    lines.append(f'{icon} <a href="{run["url"]}">{label}</a> · <code>{branch}</code> · {detail}')

lines += ['', '<b>Atualização da base</b>']
if updates:
    icon, label = state(updates[0])
    lines.append(f'{icon} Última verificação {ago(updates[0]["createdAt"])} ({label.lower()})')
else:
    lines.append('Nenhuma verificação ainda.')

print('\n'.join(lines))
