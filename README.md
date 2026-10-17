# Telegram Pro

Telegram para Android (arm64) baseado no [Telegram oficial](https://github.com/DrKLO/Telegram), com recursos inspirados no [Nekogram](https://github.com/Nekogram/Nekogram):

- Download e upload mais rápidos
- Tradução de chats inteiros sem Premium, com vários provedores (Google, Microsoft, Yandex, Lingocloud, Baidu, Sogou, Telegram)
- Estilo de tradução, texto original, idioma de destino, "não traduzir" e tradução automática
- Configurações → Telegram Pro

## Como o repositório funciona

Este repositório **não** guarda o código inteiro do Telegram, só as mudanças:

| Arquivo | O que é |
|---|---|
| `UPSTREAM_REF` | Commit do Telegram oficial usado como base |
| `patches/` | As mudanças do Telegram Pro, um patch por commit |
| `CHANGELOG.md` | Novidades de cada versão (vão para a descrição no canal) |
| `scripts/` | Scripts para preparar, atualizar e exportar |

## Uso local

```bash
scripts/setup.sh            # baixa o Telegram oficial em ./Telegram e aplica os patches
cd Telegram && ./gradlew :TMessagesProj_App:assembleAfatRelease
```

Credenciais em `~/.gradle/gradle.properties` (nunca no repositório):

```properties
PRO_APP_ID=...
PRO_APP_HASH=...
PRO_KEYSTORE_FILE=/caminho/telegrampro.jks
PRO_KEYSTORE_PASSWORD=...
PRO_KEY_ALIAS=telegrampro
PRO_KEY_PASSWORD=...
```

## Mudar ou adicionar recursos

1. Edite o código em `./Telegram` e faça commit lá (`git -C Telegram commit ...`).
2. Rode `scripts/export-patches.sh` para atualizar `patches/`.
3. Faça commit de `patches/` neste repositório.

## Atualizar a base (Telegram oficial)

Automático: o workflow **Update upstream** roda todo dia. Quando sai uma versão nova do Telegram oficial:

1. Reaplica os patches na nova base.
2. Se aplicarem sem conflito: adiciona a versão no `CHANGELOG.md`, cria a tag (`vX.Y.Z+1`), gera o APK e posta no canal.
3. Se der conflito: não publica nada e abre um PR para resolver manualmente.

Manual:

```bash
scripts/update-upstream.sh   # usa o master mais recente do DrKLO/Telegram
```

Se algum patch der conflito, resolva em `./Telegram`, rode `git -C Telegram am --continue` e depois `scripts/export-patches.sh`.

## Publicar uma versão

1. Adicione a seção `## vX.Y.Z` no `CHANGELOG.md`.
2. `git tag vX.Y.Z && git push --tags`

O workflow **Build** gera o APK assinado e envia para o canal do Telegram com o link do changelog da base e as mudanças do Telegram Pro na descrição.

### Secrets necessários (Settings → Secrets → Actions)

| Secret | Valor |
|---|---|
| `PRO_APP_ID`, `PRO_APP_HASH` | Credenciais de my.telegram.org |
| `PRO_KEYSTORE_BASE64` | `base64 -i telegrampro.jks` |
| `PRO_KEYSTORE_PASSWORD`, `PRO_KEY_ALIAS`, `PRO_KEY_PASSWORD` | Dados da chave |
| `TELEGRAM_BOT_TOKEN` | Token do bot (@BotFather), admin do canal |
| `TELEGRAM_CHAT_ID` | `@seucanal` ou o ID numérico (-100...) |

## Licença

GPL-2.0, como o Telegram oficial. A biblioteca de tradução `app.nekogram.translator` é GPL-3.0.
