# jblesk Microfone (Fabric 1.20.1)

Um microfone para o **jblesk**: conectado a uma **jbleskão**, a sua voz (pelo **Simple Voice Chat**) sai pela caixa de som, para todo mundo que estiver perto dela.

## Requisitos

Fabric Loader e Fabric API para a 1.20.1, mais os mods **jblesk** (`caixademusica`) e **Simple Voice Chat** (`voicechat`), todos no cliente e no servidor.

## Como usar

1. Fabrique o **Microfone**: um lingote de ferro em cima, redstone no meio e um graveto embaixo (coluna de 3). Também fica na aba **Ferramentas** do criativo.
2. **Botão direito numa jbleskão** com o microfone na mão: ele fica conectado àquela caixa (o microfone passa a brilhar).
3. **Segure o microfone** (em qualquer mão) e fale no chat de voz: a sua voz sai pela caixa, e quem estiver perto dela ouve.
4. **Botão direito no ar** mostra a conexão; **agachado + botão direito no ar** desconecta.

## Regras

- Falando pelo microfone, a voz normal (proximidade) é cortada: ela só sai pela caixa, como num sistema de som. Para a voz sair pela caixa **e** normalmente, troque `CANCEL_ORIGINAL` para `false` em `MicVoicechatPlugin.java`.
- **Sussurrando**, ou falando num **grupo** do Simple Voice Chat, o microfone não é usado (a voz segue normal). Dá para sussurrar para falar sem passar pela caixa.
- Quem está falando **não escuta a própria voz** saindo da caixa (evita eco). Peça a outro jogador para conferir.
- O alcance da voz na caixa é a distância de voz configurada no Simple Voice Chat.
- Se a jbleskão for quebrada ou ficar em área descarregada, o microfone deixa de passar a voz até a caixa existir de novo.
- A música da jbleskão e a voz tocam juntas: o jblesk toca a música em cada PC, e a voz vem pelo Simple Voice Chat.
- Por enquanto só funciona com o **bloco** jbleskão (não com a jblesk portátil nem com as caixas de animais/barcos).

## Como gerar o .jar

### Opção 1: GitHub (sem instalar nada)

Este mod fica na pasta `jbleskmic/` do repositório **mods-udas**. A cada alteração, o build "jbleskmic - gerar o .jar" roda sozinho na aba **Actions** (uns 3 a 5 minutos): baixe o artefato **jbleskmic**. Dentro do zip está o `jbleskmic-1.0.0.jar`.

### Opção 2: no seu PC

Com o **JDK 17 ou 21**: `./gradlew build` (ou `gradlew.bat build` no Windows). O jar fica em `build/libs/jbleskmic-1.0.0.jar`.

Se o Gradle reclamar que não achou `de.maxhenkel.voicechat:voicechat-api:2.6.20`, troque a versão em `gradle.properties` (`voicechat_api_version`) por outra publicada em https://maven.maxhenkel.de/repository/public/de/maxhenkel/voicechat/voicechat-api/.

## Juntar no mod jblesk

Este mod é separado porque ele só usa a jbleskão pelo id do bloco (`caixademusica:music_box`). Se quiser colocar dentro do projeto do jblesk, copie as classes de `com/jbleskmic/mod`, o `"voicechat"` em `entrypoints` do `fabric.mod.json`, a dependência `compileOnly` do `build.gradle`, a receita e os textos.

## Licença

MIT (veja `LICENSE`).
