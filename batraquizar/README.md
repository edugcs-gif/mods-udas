# Batraquizar (Fabric 1.21.1)

O **Batraquizador** é uma poção que **transforma em sapo** quem for atingido por ela. Jogue o **Batraquizador Arremessável** num amigo e veja ele virar um sapinho pulando por aí.

## Como fazer a poção

No suporte de poções:

1. **Garrafa de água** + verruga do Nether = Poção Estranha
2. **Poção Estranha** + **froglight** (qualquer cor: ocre, verdejante ou perolada) = **Batraquizador** (1 minuto)
3. *(opcional)* + pó de redstone = Batraquizador prolongado (3 minutos)
4. **+ pólvora** = **Batraquizador Arremessável** (esse é o de jogar nos outros)
5. *(opcional)* + bafo do dragão = Batraquizador Persistente (deixa uma nuvem no chão)

A froglight sai quando um sapo come um cubo de magma pequeno (a cor depende do tipo de sapo).

Também dá para fazer a **Flecha do Batraquizador** com o Batraquizador Persistente, como as outras flechas com efeito. No criativo, ele aparece nas abas de poções junto com as outras poções.

## O que acontece com quem vira sapo (efeito **Batraquizado**)

- **Vira sapo de verdade** para todo mundo: o jogador (ou qualquer criatura atingida) é desenhado como um sapo, que anda e pula junto com ele e **coaxa** de vez em quando. O nome do jogador continua aparecendo em cima do sapo.
- **Fica pequeno** (30% do tamanho, uns 0,5 bloco de altura): passa por buracos de 1 bloco e a câmera fica lá embaixo.
- **Pula mais alto** (uns 2 blocos) e cai de até 3 blocos a mais sem se machucar.
- **Não ataca nem quebra blocos** (sapo não tem mão: em primeira pessoa o braço some).
- Ainda pode **usar itens**: comer, abrir portas e **beber leite**, que desfaz o feitiço na hora.
- Quando o efeito acaba, sai uma nuvem de fumaça e a pessoa volta ao normal.

Cuidado: a poção arremessável pega em todo mundo perto de onde ela quebra, inclusive em quem jogou.

## Instalar

1. Instale o [Fabric Loader](https://fabricmc.net/use/) para o Minecraft 1.21.1 e o [Fabric API](https://modrinth.com/mod/fabric-api).
2. Coloque o `batraquizar-*.jar` na pasta `mods`.
3. Num servidor, o mod precisa estar **no servidor e em todos os jogadores**.

## Gerar o .jar no PC

```
./gradlew build
```

O arquivo sai em `build/libs/batraquizar-<versão>.jar`. O GitHub também gera o `.jar` sozinho a cada alteração (aba **Actions**, build "batraquizar", artefato `batraquizar`).
