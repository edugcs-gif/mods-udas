# Cassino (Fabric 1.20.1)

Um mod de cassino para o Minecraft 1.20.1, com Fabric. É um mod original: o código, as texturas e os modelos foram feitos do zero.

## O que tem

| Item | O que faz |
| --- | --- |
| **Ficha** | A "moeda" do cassino. Receita: 1 barra de ouro = 8 fichas (sem forma). E 8 fichas = 1 barra de ouro, então não dá para lucrar só trocando. |
| **Caça-níquel** (bloco) | Botão direito com uma ficha na mão: gasta 1 ficha e gira os 3 rolos. |
| **Dado** | Botão direito rola de 1 a 6; quem está até 16 blocos vê o resultado no chat. |
| **Moeda** | Botão direito joga cara ou coroa; quem está até 16 blocos vê o resultado no chat. |

Tudo fica na aba **Cassino** do criativo.

## Receitas

- **Caça-níquel:** fileira de cima 3 barras de ferro; no meio ferro, **ficha**, ferro; embaixo ferro, redstone, ferro.
- **Dado:** 4 quartzos em quadrado (2x2).
- **Moeda:** em cruz, 4 pepitas de ferro (em cima, embaixo e nos lados) e 1 pepita de ouro no centro.

## Prêmios do caça-níquel

- **3 iguais:** 7 → 100 fichas (JACKPOT, avisa quem está até 32 blocos), ★ → 30, ♦ → 16, ♥ → 10, ♣ → 6, ♠ → 6.
- **2 iguais:** a ficha volta.
- **Nenhum igual:** perdeu a ficha.

O retorno médio é de uns **83%** das fichas jogadas, então a casa ganha no longo prazo e o caça-níquel não vira máquina de ouro. Para mudar os prêmios ou as chances, edite `PAYOUT` e `WEIGHTS` em `SlotMachineBlock.java`.

## Instalação

O `.jar` precisa do **Fabric Loader** e do **Fabric API** para a 1.20.1. Coloque na pasta `mods`. Em servidor, instale também no servidor (só o servidor sorteia, mas os itens e o bloco precisam existir nos dois lados).

## Como gerar o .jar

### Opção 1: GitHub (sem instalar nada)

Este mod mora no repositório [mods-udas](https://github.com/edugcs-gif/mods-udas), na pasta `cassino/`.

1. No repositório, abra a aba **Actions** e escolha o workflow **cassino - gerar o .jar**. Ele roda sozinho a cada alteração em `cassino/` (uns 2 a 3 minutos); também dá para rodar à mão em **Run workflow**.
2. Clique no build que terminou e baixe o artefato **cassino**. Dentro do zip está o `cassino-1.0.0.jar`.

### Opção 2: no seu PC

Precisa do **JDK 17 ou 21**. Na pasta `cassino/` do projeto:

```bash
./gradlew build        # Git Bash / Linux / Mac
gradlew.bat build      # Prompt do Windows
```

O jar fica em `build/libs/cassino-1.0.0.jar`. Para testar sem montar o modpack: `./gradlew runClient`.

## Ideias para a próxima versão

Roleta, mesa de blackjack, sons e animação dos rolos, conquistas, uma ficha com valores diferentes (azul, verde, preta) e um caixa (NPC ou bloco) que troca fichas por esmeraldas.

## Licença

MIT (veja `LICENSE`).
