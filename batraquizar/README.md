# Batraquizar (Fabric 1.20.1)

Duas poções: o **Batraquizador** e a **Poção de Retorsão**.

O **Batraquizador** é uma poção que **transforma em sapo** quem for atingido por ela. Jogue o **Batraquizador Arremessável** num amigo e veja ele virar um sapinho pulando por aí.

## Como fazer a poção

No suporte de poções:

1. **Garrafa de água** + verruga do Nether = Poção Estranha
2. **Poção Estranha** + **froglight** (qualquer cor: ocre, verdejante ou perolada) = **Batraquizador** (30 segundos)
3. *(opcional)* + pó de redstone = Batraquizador prolongado (1 minuto e 30 segundos)
4. **+ pólvora** = **Batraquizador Arremessável** (esse é o de jogar nos outros)
5. *(opcional)* + bafo do dragão = Batraquizador Persistente (deixa uma nuvem no chão)

A froglight sai quando um sapo come um cubo de magma pequeno (a cor depende do tipo de sapo).

Também dá para fazer a **Flecha do Batraquizador** com o Batraquizador Persistente, como as outras flechas com efeito. No criativo, ele aparece nas abas de poções junto com as outras poções.

## O que acontece com quem vira sapo (efeito **Batraquizado**)

- **Vira sapo de verdade** para todo mundo: o jogador (ou qualquer criatura atingida) é desenhado como um sapo, que anda e pula junto com ele e **coaxa** de vez em quando. O nome do jogador continua aparecendo em cima do sapo.
- **Fica pequeno** (30% do tamanho, uns 0,5 bloco de altura): passa por buracos de 1 bloco e a câmera fica lá embaixo.
- **Fica 40% mais rápido** (como a poção de Velocidade II).
- **Pula mais alto** (uns 2 blocos) e **não toma dano de queda**.
- **Não ataca nem quebra blocos** (sapo não tem mão: em primeira pessoa o braço some).
- Ainda pode **usar itens**: comer, abrir portas e **beber leite**, que desfaz o feitiço na hora.
- Quando o efeito acaba, sai uma nuvem de fumaça e a pessoa volta ao normal.

Como em todas as poções do jogo, a persistente e a flecha duram menos que a poção normal.

## Bênção do Príncipe

Quem volta de sapo (o efeito acabou, bebeu leite ou morreu sapo) ganha o efeito **Bênção do Príncipe** por **10 minutos**:

- Enquanto durar, **ninguém consegue te transformar em sapo de novo**: a poção não pega e aparece o aviso "A Bênção do Príncipe te protegeu de virar sapo!".
- A bênção **não sai quando você morre** (nem com o totem da imortalidade): só sai com **leite** ou quando o **tempo acaba**.

Cuidado: a poção arremessável pega em todo mundo perto de onde ela quebra, inclusive em quem jogou.

## Poção de Retorsão

Uma poção que **devolve os feitiços**: enquanto durar, todo efeito ruim que alguém tentar colocar em você volta para quem jogou.

No suporte de poções:

1. **Poção Estranha** + **fragmento de ametista** = **Poção de Retorsão** (1 minuto)
2. *(buffada)* + pó de redstone = Poção de Retorsão prolongada (**3 minutos**)
3. Pólvora, bafo do dragão e flecha funcionam como nas outras poções (dá para jogar Retorsão nos amigos).

Como funciona:

- Qualquer **efeito ruim** que outra criatura tente aplicar em você (Batraquizador, veneno, lentidão, fraqueza, dano instantâneo, flechas com efeito, a nuvem da persistente, a poção da bruxa...) **volta para quem aplicou**.
- **Cada efeito devolvido custa 1 minuto** da Retorsão. Então a normal devolve **1** efeito e a buffada devolve **no máximo 3**.
- Se **os dois** estiverem com Retorsão, o efeito fica indo e voltando (**ping-pong**) até a Retorsão de alguém acabar: aí ele pega nessa pessoa.
- Toda vez que um efeito é devolvido, toca um **som de espelho** e um **raio de partículas azuis** voa de quem foi atingido até quem atacou. No ping-pong dá para ver o raio indo e voltando.
- Efeitos ruins que não vieram de ninguém (de um dispensador, por exemplo) e efeitos que não pegariam em você de qualquer jeito (como o Batraquizador com a Bênção do Príncipe) não são devolvidos e não gastam a Retorsão.

## Instalar

1. Instale o [Fabric Loader](https://fabricmc.net/use/) para o Minecraft 1.20.1 e o [Fabric API](https://modrinth.com/mod/fabric-api).
2. Coloque o `batraquizar-*.jar` na pasta `mods`.
3. Num servidor, o mod precisa estar **no servidor e em todos os jogadores**.

## Gerar o .jar no PC

```
./gradlew build
```

O arquivo sai em `build/libs/batraquizar-<versão>.jar`. O GitHub também gera o `.jar` sozinho a cada alteração (aba **Actions**, build "batraquizar", artefato `batraquizar`).
