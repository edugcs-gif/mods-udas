# Cassino (Fabric 1.20.1)

Um mod de cassino para o Minecraft 1.20.1, com Fabric. É um mod original: o código, as texturas e os modelos foram feitos do zero.

## O que tem

| Item | O que faz |
| --- | --- |
| **Ficha** | A "moeda" do cassino. Receita: 1 barra de ouro = 8 fichas (sem forma). E 8 fichas = 1 barra de ouro, então não dá para lucrar só trocando. |
| **Caça-níquel** (bloco) | Botão direito abre a máquina: 5 rolos, aposta de 1 a 50 fichas, créditos e prêmios de até 100x a aposta. |
| **Dado** | Botão direito rola de 1 a 6; quem está até 16 blocos vê o resultado no chat. |
| **Moeda** | Botão direito joga cara ou coroa; quem está até 16 blocos vê o resultado no chat. |

Tudo fica na aba **Cassino** do criativo.

## Receitas

- **Caça-níquel:** fileira de cima 3 barras de ferro; no meio ferro, **ficha**, ferro; embaixo ferro, redstone, ferro.
- **Dado:** 4 quartzos em quadrado (2x2).
- **Moeda:** em cruz, 4 pepitas de ferro (em cima, embaixo e nos lados) e 1 pepita de ouro no centro.

## Caça-níquel

Botão direito no caça-níquel abre a tela da máquina:

- **5 rolos** à esquerda, com 3 símbolos cada: 7, estrela, diamante, coração, esmeralda e pá. Só a **linha do meio** (a destacada em amarelo) paga.
- **MOEDAS:** ponha fichas no espaço ao lado e aperte MOEDAS; cada ficha vira 1 crédito. Shift + clique numa ficha do inventário já a coloca no espaço. Se apertar GIRAR! sem créditos, as fichas do espaço são trocadas sozinhas.
- **APOSTA:** os botões **+** e **-** escolhem quanto cada girada custa: 1, 2, 5, 10, 20 ou 50 créditos.
- **GIRAR!:** os rolos giram e param um a um, da esquerda para a direita. Passe o mouse no botão para ver a tabela de prêmios.
- **CARTÃO:** saca todos os créditos em fichas. Ao fechar a máquina, os créditos que sobraram e as fichas do espaço também voltam para você: nada se perde.

### Prêmios

Ganha quem tirar **3, 4 ou 5 símbolos iguais seguidos na linha do meio, a partir do primeiro rolo** (da esquerda). O prêmio é a aposta vezes:

| Símbolo | 3 iguais | 4 iguais | 5 iguais |
| --- | --- | --- | --- |
| 7 | 10x | 30x | **100x (JACKPOT)** |
| Estrela | 5x | 15x | 50x |
| Diamante | 4x | 8x | 25x |
| Coração | 3x | 6x | 15x |
| Esmeralda | 2x | 4x | 10x |
| Pá | 2x | 3x | 6x |

- **40% das giradas ganham e 60% perdem.** Quase todas as vitórias são 3 pás ou 3 esmeraldas (o dobro da aposta); os prêmios maiores são cada vez mais raros, e o jackpot sai uma vez a cada ~25.000 giradas (quem está até 32 blocos é avisado no chat).
- No total a máquina devolve uns **88%** do que é apostado: quem joga perde mais do que ganha, e o caça-níquel não vira máquina de ouro.
- Para mudar as chances ou os prêmios, edite `WIN_CHANCE`, `WINS` e `PAYOUT` em `machine/SlotMachine.java`.

## Instalação

O `.jar` precisa do **Fabric Loader** e do **Fabric API** para a 1.20.1. Coloque na pasta `mods`. Em servidor, instale no servidor e em todos os jogadores (só o servidor sorteia, mas a tela da máquina fica no jogo de cada um).

## Como gerar o .jar

### Opção 1: GitHub (sem instalar nada)

Este mod mora no repositório [mods-udas](https://github.com/edugcs-gif/mods-udas), na pasta `cassino/`.

1. No repositório, abra a aba **Actions** e escolha o workflow **cassino - gerar o .jar**. Ele roda sozinho a cada alteração em `cassino/` (uns 2 a 3 minutos); também dá para rodar à mão em **Run workflow**.
2. Clique no build que terminou e baixe o artefato **cassino**. Dentro do zip está o `cassino-1.1.0.jar`.

### Opção 2: no seu PC

Precisa do **JDK 17 ou 21**. Na pasta `cassino/` do projeto:

```bash
./gradlew build        # Git Bash / Linux / Mac
gradlew.bat build      # Prompt do Windows
```

O jar fica em `build/libs/cassino-1.1.0.jar`. Para testar sem montar o modpack: `./gradlew runClient`.

## Ideias para a próxima versão

Roleta, mesa de blackjack, conquistas, uma ficha com valores diferentes (azul, verde, preta) e um caixa (NPC ou bloco) que troca fichas por esmeraldas.

## Licença

MIT (veja `LICENSE`).
