# mods-udas

Mods de Minecraft (Fabric) do edugcs. Cada mod fica na sua própria pasta, com README, licença e projeto Gradle próprios.

| Mod | O que faz | Minecraft |
|---|---|---|
| [**jblesk**](jblesk/) | **jbleskão** (bloco) e **jblesk** (caixa de som portátil): tocam músicas e playlists do YouTube, SoundCloud ou links diretos de áudio para todo mundo que estiver perto. Pausa, pular na barra, volume pessoal, alcance perto/longe, e a caixa também toca do inventário, do baú do burro e do barco com baú. | 1.20.1 (Fabric) |
| [**batraquizar**](batraquizar/) | O **Batraquizador**, uma poção: jogue a versão arremessável em alguém e ele vira sapo (pequeno, rápido, pulando alto, coaxando e sem conseguir atacar) até o efeito passar ou beber leite; depois ganha a Bênção do Príncipe, que protege por 10 minutos. E a **Poção de Retorsão**, que devolve os efeitos ruins para quem os jogou. | 1.20.1 (Fabric) |
| [**jbleskmic**](jbleskmic/) | **jblesk Microfone**: conectado a uma jbleskão, a voz de quem segura o microfone (pelo **Simple Voice Chat**) sai pela caixa de som, para todo mundo que estiver perto dela. Precisa do jblesk e do Simple Voice Chat. | 1.20.1 (Fabric) |
| [**cassino**](cassino/) | **Cassino**: fichas e trocador de fichas, **caça-níquel**, **blackjack**, **roleta**, **corrida de cavalos**, **jogo de dados**, **cara ou coroa**, **Plinko de Slime**, **Campo Minado** e **Loteria de Mobs**, com saldo da casa compartilhado e o magnata do servidor. | 1.20.1 (Fabric) |
| [**transparencia**](transparencia/) | **Inventário Transparente**: o fundo do inventário (e de baús, fornalhas, mesas...) fica semitransparente para ver o mundo atrás; a tecla **I** troca entre 100%, 80%, 60%, 40% e 20%. Só visual: não precisa estar no servidor. | 1.20.1 (Fabric) |
| [**tops**](tops/) | **Tops do Servidor**: 35 rankings tirados das estatísticas do jogo (mortes, mobs, diamantes, peixes, horas, K/D, XP ganho...) com `/top`, `/top <categoria>`, `/status` e `/peixes`. Só precisa estar no servidor. | 1.20.1 (Fabric) |
| [**loja**](loja/) | **Loja de Baú**: placa `[loja]` num baú vira uma loja de troca (o dono define o item que vende, o que recebe e as quantidades), e placa `[privado]` tranca o baú (com amigos). Só precisa estar no servidor. | 1.20.1 (Fabric) |
| [**default-skill-trees-xp**](default-skill-trees-xp/) | **Default Skill Trees** (do Pufferfish) com a curva de XP alterada: cada nível das árvores de Combate e Mineração custa 5% a mais que o anterior. | 1.20.1 (Fabric) |

## Baixar o .jar

O GitHub gera o `.jar` sozinho a cada alteração: abra a aba **Actions**, entre no último build do mod e baixe o artefato (por exemplo, `jblesk`). Também dá para gerar no PC: veja o README de cada mod.

## Estrutura

```
mods-udas/
├── jblesk/          o mod jblesk (código, recursos, README e licença)
├── jbleskmic/       o jblesk Microfone (a voz do Simple Voice Chat saindo pela jbleskão)
├── batraquizar/     o mod Batraquizar (a poção que transforma em sapo e a de Retorsão)
├── cassino/         o mod Cassino (fichas e os jogos do cassino)
├── transparencia/   o mod Inventário Transparente
├── tops/            o mod Tops do Servidor (rankings e /status)
├── loja/            o mod Loja de Baú (lojas e baús trancados)
├── default-skill-trees-xp/  o Default Skill Trees com XP exponencial (só dados, sem código)
└── .github/
    └── workflows/   um workflow por mod, que gera o .jar
```

Para adicionar outro mod: crie uma pasta nova com o projeto e um workflow em `.github/workflows/`.
