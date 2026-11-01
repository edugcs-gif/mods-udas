# mods-udas

Mods de Minecraft (Fabric) do edugcs. Cada mod fica na sua própria pasta, com README, licença e projeto Gradle próprios.

| Mod | O que faz | Minecraft |
|---|---|---|
| [**jblesk**](jblesk/) | **jbleskão** (bloco) e **jblesk** (caixa de som portátil): tocam músicas e playlists do YouTube, SoundCloud ou links diretos de áudio para todo mundo que estiver perto. Pausa, pular na barra, volume pessoal, alcance perto/longe, e a caixa também toca do inventário, do baú do burro e do barco com baú. | 1.20.1 (Fabric) |
| [**batraquizar**](batraquizar/) | O **Batraquizador**, uma poção: jogue a versão arremessável em alguém e ele vira sapo (pequeno, rápido, pulando alto, coaxando e sem conseguir atacar) até o efeito passar ou beber leite; depois ganha a Bênção do Príncipe, que protege por 10 minutos. E a **Poção de Retorsão**, que devolve os efeitos ruins para quem os jogou. | 1.20.1 (Fabric) |
| [**cassino**](cassino/) | **Cassino**: fichas (trocadas por ouro), **caça-níquel** de 5 rolos com tela própria e jackpot, **mesa de blackjack**, **roleta**, **dado** e **moeda** de cara ou coroa, com os resultados no chat para quem está perto. | 1.20.1 (Fabric) |
| [**transparencia**](transparencia/) | **Inventário Transparente**: o fundo do inventário (e de baús, fornalhas, mesas...) fica semitransparente para ver o mundo atrás; a tecla **I** troca entre 100%, 80%, 60%, 40% e 20%. Só visual: não precisa estar no servidor. | 1.20.1 (Fabric) |

## Baixar o .jar

O GitHub gera o `.jar` sozinho a cada alteração: abra a aba **Actions**, entre no último build do mod e baixe o artefato (por exemplo, `jblesk`). Também dá para gerar no PC: veja o README de cada mod.

## Estrutura

```
mods-udas/
├── jblesk/          o mod jblesk (código, recursos, README e licença)
├── batraquizar/     o mod Batraquizar (a poção que transforma em sapo e a de Retorsão)
├── cassino/         o mod Cassino (fichas, caça-níquel, dado e moeda)
├── transparencia/   o mod Inventário Transparente
└── .github/
    └── workflows/   um workflow por mod, que gera o .jar
```

Para adicionar outro mod: crie uma pasta nova com o projeto e um workflow em `.github/workflows/`.
