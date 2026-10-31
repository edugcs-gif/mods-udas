# Cassino (Fabric 1.20.1)

Um mod de cassino para o Minecraft 1.20.1, com Fabric. É um mod original: o código, as texturas e os modelos foram feitos do zero.

## O que tem

| Item | O que faz |
| --- | --- |
| **Ficha** | A "moeda" do cassino, usada para apostar. Custa caro (8 barras de ouro + 1 diamante) e não volta a ser ouro. |
| **Caça-níquel** (bloco) | Botão direito abre a máquina: 5 rolos, aposta de 1 a 64 fichas, créditos e prêmios de até 100x a aposta. |
| **Dado** | Botão direito rola de 1 a 6; quem está até 16 blocos vê o resultado no chat. |
| **Moeda** | Botão direito joga cara ou coroa; quem está até 16 blocos vê o resultado no chat. |

Tudo fica na aba **Cassino** do criativo.

## Receitas

- **Ficha:** 8 barras de ouro em volta e 1 diamante no centro = 1 ficha.
- **Caça-níquel:** fileira de cima 3 barras de ferro; no meio ferro, **ficha**, ferro; embaixo ferro, redstone, ferro.
- **Dado:** 4 quartzos em quadrado (2x2).
- **Moeda:** em cruz, 4 pepitas de ferro (em cima, embaixo e nos lados) e 1 pepita de ouro no centro.

## Caça-níquel

Botão direito no caça-níquel abre a tela da máquina:

- **5 rolos** à esquerda, com 3 símbolos cada: 7, estrela, diamante, coração, esmeralda e pá. Só a **linha do meio** (a destacada em amarelo) paga.
- **DEPÓSITO:** ponha fichas no espaço ao lado e aperte DEPÓSITO; cada ficha vira 1 crédito. Shift + clique numa ficha do inventário já a coloca no espaço. Se apertar GIRAR! sem créditos, as fichas do espaço são depositadas sozinhas.
- **Os créditos são só seus:** ficam guardados no servidor no seu nome, valem em qualquer caça-níquel e continuam lá depois de fechar a máquina. Outro jogador que abrir a mesma máquina vê só os créditos dele.
- **APOSTA:** os botões **+** e **-** escolhem quanto cada girada custa: 1, 2, 4, 8, 16, 32 ou 64 créditos.
- **GIRAR!:** os rolos giram e param um a um, da esquerda para a direita. Passe o mouse no botão para ver a tabela de prêmios.
- **RETIRADA:** saca todos os seus créditos em fichas. Fichas deixadas no espaço sem depositar voltam para você ao fechar a máquina.

### Prêmios

Ganha quem tirar **3, 4 ou 5 símbolos iguais seguidos na linha do meio, a partir do primeiro rolo** (da esquerda). O prêmio é a aposta vezes:

| Símbolo | 3 iguais | 4 iguais | 5 iguais |
| --- | --- | --- | --- |
| 7 | 10x | 30x | **100x + acumulado (JACKPOT)** |
| Estrela | 5x | 15x | 50x |
| Diamante | 4x | 8x | 25x |
| Coração | 3x | 6x | 15x |
| Esmeralda | 2x | 4x | 10x |
| Pá | 2x | 3x | 6x |

- **Jackpot acumulado:** além dos 100x da aposta, quem tira cinco 7 leva uma parte do **saldo positivo da casa** (o "acumulado", que aparece embaixo dos rolos). Com a aposta máxima (64) leva **50%** do que sobra depois de pagar os 100x; com apostas menores, uma parte proporcional (aposta 32 = 25%, aposta 4 = ~3%). Proporcional para que apostar 1 ficha não valha a pena só para disputar o acumulado. Como sai só do que sobra de positivo, o jackpot nunca deixa a casa negativa.
- **40% das giradas ganham e 60% perdem.** Quase todas as vitórias são 3 pás ou 3 esmeraldas (o dobro da aposta); os prêmios maiores são cada vez mais raros, e o jackpot sai uma vez a cada ~25.000 giradas (quem está até 32 blocos é avisado no chat).
- No total a máquina devolve uns **88%** do que é apostado: quem joga perde mais do que ganha, e o caça-níquel não vira máquina de ouro.
- Para mudar as chances ou os prêmios, edite `WIN_CHANCE_BY_DEBT`, `WIN_CHANCE_BY_PROFIT`, `WINS`, `PAYOUT` e `JACKPOT_SHARE` em `machine/SlotMachine.java`.

### Limite de prêmios da casa

O servidor guarda o **saldo da casa**: ele começa em 0, **sobe** com cada aposta e **desce** com cada prêmio. Quando está negativo, quer dizer que o cassino já deu mais fichas do que recebeu.

- A casa pode ficar **no máximo em -640**: o cassino nunca cria mais de 640 fichas a mais do que recebeu, no servidor todo. Um prêmio que passaria disso vira um prêmio menor que caiba; se nem o menor couber, a girada perde.
- Quanto mais no negativo, menor a chance de ganhar, aos poucos; e quando a casa está muito no positivo, a chance sobe um pouco, para devolver mais aos jogadores:

| Saldo da casa | Chance de ganhar | A máquina devolve |
| --- | --- | --- |
| +896 ou mais | 45% | ~99% |
| +832 a +895 | 44% | ~97% |
| +768 a +831 | 43% | ~95% |
| +704 a +767 | 42% | ~92% |
| +640 a +703 | 41% | ~90% |
| -63 a +639 | 40% (o normal) | ~88% |
| -64 a -127 | 37% | ~81% |
| -128 a -191 | 34% | ~75% |
| -192 a -255 | 31% | ~68% |
| -256 a -319 | 28% | ~62% |
| -320 a -383 | 25% | ~55% |
| -384 a -447 | 22% | ~48% |
| -448 a -511 | 19% | ~42% |
| -512 a -575 | 16% | ~35% |
| -576 a -639 | 13% | ~29% |
| -640 | 10% (só prêmios que caibam) | menos ainda |

Com as apostas dos jogadores a casa volta a subir, e a chance volta ao normal. A chance para em 45% mesmo com a casa muito no positivo: a partir de uns 45,5% a máquina passaria a pagar mais do que recebe. O saldo da casa e os créditos de cada jogador ficam salvos no mundo (`data/cassino.dat`).

## Instalação

O `.jar` precisa do **Fabric Loader** e do **Fabric API** para a 1.20.1. Coloque na pasta `mods`. Em servidor, instale no servidor e em todos os jogadores (só o servidor sorteia, mas a tela da máquina fica no jogo de cada um).

## Como gerar o .jar

### Opção 1: GitHub (sem instalar nada)

Este mod mora no repositório [mods-udas](https://github.com/edugcs-gif/mods-udas), na pasta `cassino/`.

1. No repositório, abra a aba **Actions** e escolha o workflow **cassino - gerar o .jar**. Ele roda sozinho a cada alteração em `cassino/` (uns 2 a 3 minutos); também dá para rodar à mão em **Run workflow**.
2. Clique no build que terminou e baixe o artefato **cassino**. Dentro do zip está o `cassino-1.4.0.jar`.

### Opção 2: no seu PC

Precisa do **JDK 17 ou 21**. Na pasta `cassino/` do projeto:

```bash
./gradlew build        # Git Bash / Linux / Mac
gradlew.bat build      # Prompt do Windows
```

O jar fica em `build/libs/cassino-1.4.0.jar`. Para testar sem montar o modpack: `./gradlew runClient`.

## Ideias para a próxima versão

Roleta, mesa de blackjack, conquistas, uma ficha com valores diferentes (azul, verde, preta) e um caixa (NPC ou bloco) que troca fichas por esmeraldas.

## Licença

MIT (veja `LICENSE`).
