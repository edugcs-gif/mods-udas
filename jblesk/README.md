# jblesk (Fabric 1.20.1)

> O mod se chama **jblesk**. O id interno continua `caixademusica` (assim as caixas e os itens já criados nos mundos não somem), e o bloco se chama **jbleskão** (cubo preto com o alto-falante verde e ciano na frente, virado para quem o coloca).

Um bloco (e uma **caixa de som portátil**) que toca músicas e playlists do **YouTube**, **SoundCloud**, Bandcamp, Vimeo, Twitch ou **links diretos de áudio** (MP3, OGG...).
O som sai do bloco, como um toca-discos, e todo mundo perto escuta a mesma parte da música.

## Como usar no jogo (o jbleskão, o bloco)

1. Faça o bloco (ou pegue no criativo, aba **Blocos funcionais**):

   ```
   Tábua   Bloco musical    Tábua
   Tábua   Toca-discos      Tábua
   Tábua   Pó de redstone   Tábua
   ```
2. Clique com o botão direito na caixa.
3. Cole um link (vídeo, playlist do YouTube, música do SoundCloud, link de MP3...) **ou digite o nome da música**. Com o nome, ela busca no YouTube e toca o primeiro resultado.
4. Os botões da tela, da esquerda para a direita: **anterior**, **tocar** (o link digitado, ou continuar se estiver pausada), **pausar/continuar**, **parar** e **próxima**. A playlist fica em loop.

A tela mostra a **capa**, o **título**, o **tempo** (`00:00/03:25`) e uma **barra de progresso**: clique ou arraste nela para **pular** para aquele ponto da música (para todo mundo perto).

- **Volume pessoal** (botões `-`, `+` e mudo, de 0 a 150%): vale só para você e fica salvo em `config/caixademusica-client.properties`. Além disso, o volume geral é o controle **Toca-discos/Blocos musicais** nas opções de som.
- **Alcance** (as duas barras de baixo): *Perto* é até onde o volume é total e *Longe* é até onde ainda se ouve; entre as duas o volume vai caindo. Vale para todos e fica salvo na caixa. Padrão do bloco: perto 8, longe 64 (máximo 128).
- Cada jogador só baixa o áudio quando chega perto da caixa.
- Quem chega depois entra na música no ponto certo.

## jblesk (a caixa de som portátil)

Uma versão da caixa que você leva com você. O item se chama **jblesk** e a imagem dele é um alto-falante portátil azul-marinho com logo laranja. O som **acompanha quem a carrega** e todo mundo perto escuta.

1. Faça o item (ou pegue no criativo, aba **Blocos funcionais**):

   ```
   ·        Barbante   ·
   Ferro    jbleskão (o bloco)          Ferro
   Ferro    Pó de redstone              Ferro
   ```
2. Segure na mão e clique com o botão direito no ar. Abre a mesma tela da caixa. Escolha a música e aperte **Tocar**.
3. Depois é só guardar no **inventário** (mão, barra ou mochila): ela **continua tocando**, o som acompanha você e ela brilha quando está tocando.
4. Com a caixa guardada, a tecla **B** (dá para trocar nos Controles, em "Diversos") abre a tela dela para pausar, trocar de música etc.

- Só uma caixa portátil toca por jogador: a primeira que estiver tocando.
- O servidor procura as caixas tocando a cada meio segundo. Tirar ou colocar a caixa no inventário pode levar até esse tempo para valer.
- A tela é a mesma da caixa (pausa, pular, volume pessoal, alcance). Padrão do alcance: perto 4, longe 32.
- O que está tocando, pausado e o alcance ficam salvos no item.

## O baú do burro e do barco é uma caixa de som

**Burro, mula e lhama com baú**, e **barcos com baú**, já são caixas de som: não precisa de item nenhum. O som sai do animal ou do barco e acompanha ele (inclusive para quem está montado).

- Para abrir a tela da caixa do baú: chegue perto (ou monte) e aperte **B**, ou clique com o botão direito no animal/barco segurando um **jbleskão** (o bloco). O clique normal continua montando, e o clique agachado continua abrindo o baú.
- A tecla B procura primeiro o burro/barco em que você está montado ou o mais perto (até 4 blocos); se não houver, abre a caixa portátil do inventário.
- A caixa fica guardada no próprio animal ou barco (some se ele morrer ou for quebrado). Padrão do alcance: perto 6, longe 48.

## Instalação

O `.jar` precisa estar **no servidor e no cliente de todos os jogadores** (precisa do Fabric API).

## Limitações

- **Plataformas:** para o jar ficar menor (~18 MB em vez de ~33 MB), as bibliotecas de áudio vêm só para Windows 64 bits, Linux 64 bits, Linux ARM64 e macOS. Quem joga em Windows de 32 bits, ARM de 32 bits ou Linux musl (Alpine) não consegue tocar áudio (o jogo avisa na tela da caixa). Para incluir essas plataformas de volta, apague o bloco `exclude 'natives/...'` do `build.gradle`.
- **Anterior e próxima só existem em playlist ou Mix.** Num link de **um vídeo só**, os botões avisam "Sem playlist". O link curto do YouTube com lista (`youtu.be/ID?list=...`, o que o botão "Compartilhar" gera) a biblioteca lê como um vídeo só; desde a 1.6.1 o mod converte esse link para o formato completo (`youtube.com/watch?v=ID&list=...`) e a playlist inteira funciona.
- **Spotify não funciona.** O áudio do Spotify é protegido (DRM), então nenhum mod consegue tocá-lo. Use o link do YouTube da mesma playlist, ou digite o nome da música.
- O YouTube muda o site com frequência. Se um dia parar de tocar vídeos do YouTube, atualize `youtube_source_version` no `gradle.properties` para a versão mais nova de <https://github.com/lavalink-devs/youtube-source/releases> e gere o jar de novo.
- Desde a 1.1.1 o mod usa uma versão de teste (snapshot) do youtube-source e uma lista própria de clientes do YouTube (com o `IOS`). A release 1.18.2 não conseguia tocar muitos clipes de gravadora ("All clients failed to load the item": o YouTube respondia "This video requires login" para quase todos os clientes). Quando sair uma release nova, troque o snapshot por ela.
- Links de **Mix** do YouTube (`list=RD...`) não são sempre iguais: o YouTube monta o Mix de forma diferente para cada pessoa, então só a primeira música é a mesma para todos. Para tocar a mesma coisa para todo mundo, use um vídeo ou uma playlist normal.
- Cada jogador baixa o áudio direto do YouTube/SoundCloud no próprio PC.

## Como gerar o .jar

### Opção 1: GitHub (sem instalar nada)

Este mod mora no repositório [mods-udas](https://github.com/edugcs-gif/mods-udas), na pasta `jblesk/`.

1. No repositório, abra a aba **Actions** e escolha o workflow **jblesk - gerar o .jar**. Ele roda sozinho a cada alteração em `jblesk/` (uns 3 a 5 minutos); também dá para rodar à mão em **Run workflow**.
2. Clique no build que terminou e baixe o artefato **jblesk**. Dentro do zip está o `jblesk-1.6.2.jar`.

### Opção 2: no seu PC

Precisa do **JDK 21** (por exemplo, o Temurin 21). Na pasta `jblesk/` do projeto:

```bash
./gradlew build        # Git Bash / Linux / Mac
gradlew.bat build      # Prompt do Windows
```

O jar fica em `build/libs/jblesk-1.6.2.jar`.

Para testar sem montar o modpack, use `./gradlew runClient`.
