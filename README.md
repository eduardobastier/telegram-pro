# Telegram Pro

Telegram para Android (arm64) baseado no [Telegram oficial](https://github.com/DrKLO/Telegram), com recursos inspirados no [Nekogram](https://github.com/Nekogram/Nekogram):

- Download e upload mais rápidos
- Tradução de chats inteiros sem Premium, com vários provedores (Google, Microsoft, Yandex, Lingocloud, Baidu, Sogou, Telegram)
- Estilo de tradução, texto original, idioma de destino, "não traduzir" e tradução automática
- Transcrição de áudios sem Premium (no aparelho ou Cloudflare Workers AI) e transcrição automática
- Configurações → Telegram Pro

Downloads: [Releases](https://github.com/eduardobastier/telegram-pro/releases) e o canal [@Channel_TelegramPro](https://t.me/Channel_TelegramPro).

## Branches

| Branch | Uso |
|---|---|
| `dev` | Desenvolvimento. Todo trabalho novo entra aqui. |
| `main` | Deploy. Cada commit gera um build; versão nova no `CHANGELOG.md` gera uma Release e um post no canal. |

O histórico é um fork do Telegram oficial: as mudanças do Telegram Pro são commits por cima do código do Telegram.

## Publicar uma versão

1. Trabalhe na `dev` e teste no aparelho.
2. Adicione a seção `## vX.Y.Z` no topo do `CHANGELOG.md`.
3. Leve para a `main` (`git checkout main && git merge dev && git push`).

O workflow **Build** compila o APK assinado, cria a Release `vX.Y.Z` com o APK e envia para o canal. Commits na `main` sem versão nova só geram o build (artefato no Actions).

## Atualizar a base (Telegram oficial)

Automático: o workflow **Update upstream** roda todo dia. Quando sai uma versão nova do Telegram oficial, ele faz merge na `main`, cria a versão `vX.Y.Z+1` no changelog, roda o build (Release + canal) e atualiza a `dev`. Se houver conflito, nada é publicado e uma issue é aberta.

Manual (com rebase):

```bash
git remote add upstream https://github.com/DrKLO/Telegram.git   # uma vez
git fetch upstream
git checkout dev && git rebase upstream/master
```

## Build local

```bash
git clone --recurse-submodules https://github.com/eduardobastier/telegram-pro.git
cd telegram-pro && ./gradlew :TMessagesProj_App:assembleAfatRelease
```

Requer JDK 21, Android SDK 36, NDK 27.2.12479018 e CMake 3.22.1. Credenciais em `~/.gradle/gradle.properties` (nunca no repositório):

```properties
PRO_APP_ID=...
PRO_APP_HASH=...
PRO_KEYSTORE_FILE=/caminho/telegrampro.jks
PRO_KEYSTORE_PASSWORD=...
PRO_KEY_ALIAS=telegrampro
PRO_KEY_PASSWORD=...
```

### Secrets do repositório (Settings → Secrets → Actions)

| Secret | Valor |
|---|---|
| `PRO_APP_ID`, `PRO_APP_HASH` | Credenciais de my.telegram.org |
| `PRO_KEYSTORE_BASE64` | `base64 -i telegrampro.jks` |
| `PRO_KEYSTORE_PASSWORD`, `PRO_KEY_ALIAS`, `PRO_KEY_PASSWORD` | Dados da chave |
| `TELEGRAM_BOT_TOKEN` | Token do bot (@BotFather), admin do canal |
| `TELEGRAM_CHAT_ID` | ID do canal |

## Licença

GPL-2.0, como o Telegram oficial. A biblioteca de tradução `app.nekogram.translator` é GPL-3.0.
