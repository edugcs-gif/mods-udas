# Cassino (Fabric 1.20.1)

Um mod de cassino para o Minecraft 1.20.1, com Fabric. É um mod original: o código, as texturas e os modelos foram feitos do zero.

## O que tem

| Item | O que faz |
| --- | --- |
| **Ficha** | A "moeda" do cassino, usada para apostar. 8 barras de ouro + 1 diamante = 4 fichas. |
| **Trocador de Fichas** (bloco) | Botão direito abre a troca: ouro e diamante viram fichas, e fichas voltam a ser ouro e diamante (4 fichas = 8 barras de ouro + 1 diamante). |
| **Caça-níquel** (bloco) | Botão direito abre a máquina: 5 rolos, aposta de 1 a 64 fichas, créditos e prêmios de até 100x a aposta. |
| **Mesa de Blackjack** (bloco) | Botão direito abre a mesa: blackjack (21) contra o dealer, com os mesmos créditos e o mesmo saldo da casa do caça-níquel. |
| **Roleta** (bloco) | Botão direito abre a roleta: aposte no vermelho, no preto ou no 0 verde e veja a roda girar. Mesmos créditos e mesmo saldo da casa. |
| **Corrida de Cavalos** (bloco) | Botão direito abre as apostas: escolha um dos 7 cavalos do dia e veja a corrida. Os cavalos e as chances mudam a cada dia do Minecraft. |
| **Mesa de Dados** (bloco) | Botão direito abre o jogo de dados: aposte em pares, ímpares, um número, menor ou maior e role dois dados. Mesmos créditos e mesmo saldo da casa. |
| **Mesa de Cara ou Coroa** (bloco) | Botão direito abre o cara ou coroa: aposte na CARA (o rosto) ou na COROA e lance a moeda. Mesmos créditos e mesmo saldo da casa. |
| **Plinko de Slime** (bloco) | Botão direito abre o Plinko: a bola de slime quica numa pirâmide de pinos até cair num buraco que multiplica a aposta (de 0,2x a 10x). Mesmos créditos e mesmo saldo da casa. |
| **Loteria de Mobs** (bloco) | O "jogo do bicho" do Minecraft: escolha um dos 25 mobs e aposte 32 fichas; no fim do dia sai um mob, e quem acertou leva o prêmio acumulado. Saldo separado dos outros jogos. |
| **Campo Minado** (bloco) | Estilo "Mines": cave os blocos de uma grade 5x5 atrás de diamantes e esmeraldas (o multiplicador sobe) e saque antes de achar uma TNT ou um Creeper. Mesmos créditos e mesmo saldo da casa. |
| **Dado** | Botão direito rola de 1 a 6; quem está até 16 blocos vê o resultado no chat. |
| **Moeda** | Botão direito joga cara ou coroa; quem está até 16 blocos vê o resultado no chat. |

Tudo fica na aba **Cassino** do criativo.

## Receitas

- **Ficha:** 8 barras de ouro em volta e 1 diamante no centro = **4 fichas**.
- **Trocador de Fichas:** fileira de cima 3 barras de ferro; no meio ouro, **ficha**, ouro; embaixo 3 barras de ferro.
- **Caça-níquel:** fileira de cima 3 barras de ferro; no meio ferro, **ficha**, ferro; embaixo ferro, redstone, ferro.
- **Mesa de Blackjack:** em cima 3 tapetes verdes; no meio tábua, **ficha**, tábua; embaixo um graveto em cada canto (o meio vazio).
- **Roleta:** em cima lã vermelha, lã preta, lã vermelha; no meio lã preta, **ficha**, lã preta; embaixo 3 tábuas.
- **Corrida de Cavalos:** em cima 3 fardos de feno; no meio couro, **ficha**, couro; embaixo 3 tábuas.
- **Mesa de Dados:** em cima dado, tapete verde, dado; no meio tábua, **ficha**, tábua; embaixo um graveto em cada canto (o meio vazio).
- **Mesa de Cara ou Coroa:** em cima moeda, tapete verde, moeda; no meio tábua, **ficha**, tábua; embaixo um graveto em cada canto (o meio vazio).
- **Plinko de Slime:** fileira de cima 3 barras de ferro; no meio bola de slime, **ficha**, bola de slime; embaixo 3 barras de ferro.
- **Loteria de Mobs:** fileira de cima 3 papéis; no meio tábua, **ficha**, tábua; embaixo 3 tábuas.
- **Campo Minado:** fileira de cima 3 pedras; no meio pólvora, **ficha**, pólvora; embaixo 3 pedras.
- **Dado:** 4 quartzos em quadrado (2x2).
- **Moeda:** em cruz, 4 pepitas de ferro (em cima, embaixo e nos lados) e 1 pepita de ouro no centro.

## Trocador de Fichas

Botão direito no trocador abre a troca, sempre na mesma conta da receita: **8 barras de ouro + 1 diamante = 4 fichas**, nos dois sentidos.

- **Em cima (ouro e diamante » fichas):** coloque barras de ouro e diamantes nos dois espaços e aperte **TROCAR** (verde). Ele faz quantas trocas couberem (até 32 fichas de uma vez, com 64 barras de ouro e 8 diamantes) e o que sobrar fica no espaço.
- **Embaixo (fichas » ouro e diamante):** coloque fichas no espaço e aperte **TROCAR** (azul). Cada 4 fichas voltam como 8 barras de ouro + 1 diamante; se sobrarem menos de 4, elas ficam no espaço.
- Ao lado de cada espaço a tela mostra quanto aquilo vai virar, e o que sai da troca vai direto para o seu inventário (ou cai no chão, se não couber).
- **Shift + clique** manda o ouro, o diamante e as fichas cada um para o seu espaço. Ao fechar a tela, o que ficou nos espaços volta para você.
- O trocador não mexe nos créditos das máquinas: para jogar, deposite as fichas na máquina; para trocar o que ganhou, use **RETIRADA** e leve as fichas ao trocador.

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

### O magnata do servidor

Em todos os jogos, **à esquerda do inventário**, um quadro com coroa mostra o **MAGNATA** do servidor: quem tem **mais créditos no cassino** (as fichas depositadas nas máquinas, mais os prêmios), com o nome e quanto tem. Embaixo aparece **a sua posição** no ranking ("3º de 12"; "é você!" se você for o magnata). Ele é atualizado na hora: se alguém passar você enquanto a tela está aberta, o quadro muda. Passe o mouse em cima para ver a explicação. Só entram no ranking os jogadores com créditos (fichas no inventário não contam).

### Limite de prêmios da casa

O servidor guarda o **saldo da casa**: ele começa em 0, **sobe** com cada aposta e **desce** com cada prêmio, somando todos os jogos (caça-níquel, blackjack, roleta, corrida, dados, cara ou coroa, Plinko e Campo Minado). Quando está negativo, quer dizer que o cassino já deu mais fichas do que recebeu. Ele aparece no **canto de cima, à esquerda, de todas as telas** ("CASA: +1234", em verde); passe o mouse em cima para ver a explicação. Quando a casa está no negativo, a tela mostra **0** (o valor de verdade nem sai do servidor), então os jogadores não ficam sabendo que as chances estão contra eles.

- A casa pode ficar **no máximo em -5000**: o cassino nunca cria mais de 5000 fichas a mais do que recebeu, no servidor todo. Um prêmio que passaria disso vira um prêmio menor que caiba; se nem o menor couber, a girada perde.
- Quanto mais no negativo, menor a chance de ganhar, **um pouco a cada 64 fichas** até perto de -5000, quando a casa ganha todas; e a partir de **+640** a chance sobe um ponto a cada 64 fichas, para a casa devolver fichas em vez de acumular. No caça-níquel:

| Saldo da casa | Chance de ganhar | A máquina devolve |
| --- | --- | --- |
| +1216 ou mais | 50% | ~110% |
| +1152 a +1215 | 49% | ~108% |
| +1088 a +1151 | 48% | ~106% |
| +1024 a +1087 | 47% | ~104% |
| +960 a +1023 | 46% | ~101% |
| +896 a +959 | 45% | ~99% |
| +832 a +895 | 44% | ~97% |
| +768 a +831 | 43% | ~95% |
| +704 a +767 | 42% | ~92% |
| +640 a +703 | 41% | ~90% |
| -63 a +639 | 40% (o normal) | ~88% |
| -640 | ~35% | ~77% |
| -1280 | ~30% | ~66% |
| -2496 | 20% | ~44% |
| -3840 | ~9% | ~20% |
| -4992 a -5000 | 0% (a casa ganha todas) | 0% |

Com as apostas dos jogadores a casa volta a subir, e a chance volta ao normal. De uns 45,5% para cima a máquina paga mais do que recebe: é de propósito, para os jogadores ganharem de volta o que a casa acumulou, até ela voltar para perto de +640. O saldo da casa e os créditos de cada jogador ficam salvos no mundo (`data/cassino.dat`).

**A mesma regra vale em todos os jogos** (a conta está em `machine/Luck.java`): de -63 a +639 tudo é normal; no negativo, a casa tem cada vez mais sorte (a cada 64 fichas no negativo, uns 1,3% a mais das rodadas que você ganharia viram derrota; perto de -5000, todas); e de **+640** para cima a chance dos jogadores sobe a cada 64 fichas, até **+1216**, quando todos os jogos devolvem uns **110%**:

| Saldo da casa | Caça-níquel | Roleta (vermelho/preto) | Roleta (verde) | Corrida (seu cavalo) | Blackjack (jogando perfeito) |
| --- | --- | --- | --- | --- | --- |
| até +639 | 40% (~88%) | 48,6% (~97%) | 2,7% (~97%) | a chance do dia (~87%) | ~99% |
| +896 | 45% (~99%) | 51,8% (~104%) | 2,9% (~104%) | chance x1,125 (~98%) | ~104% |
| +1216 ou mais | 50% (~110%) | 55,0% (~110%) | 3,05% (~110%) | chance x1,25 (~109%) | ~110% |

No negativo, quanto cada jogo devolve (com a mesma estratégia de sempre):

| Saldo da casa | Sorte da casa | Caça-níquel | Roleta (verm./preto) | Cara ou coroa | Dados (pares) | Plinko | Campo Minado | Blackjack (perfeito) |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| -640 | 13% | ~77% | ~85% | ~84% | ~87% | ~91% | ~84% | ~85% |
| -1280 | 26% | ~66% | ~72% | ~71% | ~74% | ~83% | ~71% | ~72% |
| -2496 | 50% | ~44% | ~49% | ~48% | ~50% | ~68% | ~48% | ~47% |
| -3840 | 77% | ~20% | ~22% | ~22% | ~23% | ~52% | ~22% | ~21% |
| -4992 a -5000 | 100% | 0% | 0% | 0% | 0% | ~38% | 0% | 0% |

Perto de -5000 **a casa ganha todas**: ninguém acerta nada (no Plinko a bola só cai nos buracos de 0,5x e 0,2x, que devolvem menos que a aposta).

**Sem teto para cima:** o saldo da casa é um valor só, compartilhado por todos os jogos, e pode subir quanto for; a partir de +640 a chance dos jogadores sobe (até +1216), e é isso que faz a casa devolver fichas em vez de acumular.

## Mesa de Blackjack

Botão direito na mesa abre o blackjack (21) contra o dealer. Os **créditos são os mesmos** do caça-níquel (o seu saldo vale nas duas), e o **espaço das fichas, DEPÓSITO e RETIRADA** funcionam igual.

- **DAR CARTAS** (o botão verde) gasta a aposta e dá duas cartas para você e duas para o dealer (a segunda dele fica virada). Durante a mão, o mesmo botão vira **PEDIR** (mais uma carta).
- **PARAR**: você para e o dealer compra até ter 17 ou mais.
- **DOBRAR**: só com as duas primeiras cartas; dobra a aposta, você recebe só mais uma carta e para.
- **APOSTA** com **+** e **-**: 1, 2, 4, 8, 16, 32 ou 64 (só entre as mãos).
- **Pagamento:** blackjack (Ás + uma carta de 10 nas duas primeiras) paga **3:2**; vitória paga **1:1**; empate devolve a aposta. O Ás vale 11 ou 1, e as figuras valem 10.
- **As cartas:** o valor (A, 2 a 10, J, Q, K) e o naipe como um item do jogo: **copas = coração**, **ouros = barra de ouro**, **espadas = espada de ferro** e **paus = graveto**. Copas e ouros com o número em vermelho.
- Se você fechar a mesa (ou sair do jogo) no meio de uma mão, você para e a mão termina normalmente.

**Ligada ao caça-níquel:** o blackjack usa o **mesmo saldo da casa**:

- O limite de **-5000** vale para tudo junto. Ao dar as cartas (e ao dobrar), a mesa **reserva** o pior caso para a casa (você tirar blackjack, ou ganhar a aposta dobrada) e só aceita a aposta se ele couber, contando também as mãos de outros jogadores em andamento. Se a aposta for alta demais, a tela mostra a aposta máxima do momento.
- A **sorte da mesa** segue o saldo da casa: numa parte das cartas a mesa tira duas e dá a melhor para um lado e a pior para o outro. Com a casa no negativo, numa parte das cartas a mesa escolhe a dedo a melhor para o dealer e a pior para você (cada vez mais a cada 64 fichas; perto de -5000, todas, e o dealer sempre ganha); de 0 a +639 o jogo é **limpo**; de **+640** para cima, a melhor vai para você (1,5% a cada 64, até 15% em +1216).

Quanto a mesa devolve (simulação com as regras do mod, 150 mil mãos ou mais por linha):

| Saldo da casa | Jogando perfeito | Pedindo até 17, sem dobrar |
| --- | --- | --- |
| +1216 ou mais | ~110% | ~105% |
| +896 | ~104% | ~100% |
| +640 | ~100% | ~96% |
| 0 a +639 (limpo) | ~99% | ~94% |
| -640 | ~85% | ~81% |
| -1280 | ~72% | ~69% |
| -2496 | ~47% | ~44% |
| -3840 | ~21% | ~20% |
| -4992 a -5000 | 0% | 0% |

## Roleta

Botão direito na roleta abre a mesa da roleta (só com as cores, sem os números). Os **créditos**, o **espaço das fichas**, **DEPÓSITO** e **RETIRADA** são os mesmos das outras máquinas.

- A roda tem **37 casas**, como a roleta europeia: **18 vermelhas**, **18 pretas** e o **0 verde**.
- **Clique numa das cores da mesa** (0, VERMELHO ou PRETO) para escolher onde apostar: a ficha aparece na cor escolhida.
- **APOSTA** com **+** e **-** (1 a 64) e **GIRAR!**: a roda gira, desacelera e para com a casa sorteada embaixo da seta; a bolinha corre ao contrário e cai nela.
- **Vermelho ou preto devolve 2x** a aposta; **o verde devolve 36x**. Nos dois casos a roleta devolve em média **~97%**: a casa ganha com o 0 (quem aposta numa cor perde quando sai o verde).
- A faixa ao lado da roda mostra as **últimas cores** que saíram (a mais nova em cima).

**Ligada ao resto do cassino:** mesmo **saldo da casa** e mesmo limite de **-5000**. Uma girada só é aceita se o pior caso (a sua cor sair) couber no limite: com a casa perto de 0, a aposta no verde fica limitada (a tela mostra o máximo). Com a casa no negativo vale a mesma **sorte da casa** do blackjack (numa parte das giradas que você ganharia, a bolinha cai numa casa de outra cor a cada 64 fichas no negativo, uns 1,3% a mais das rodadas que você ganharia viram derrota; perto de -5000, todas). De 0 a +639 a roleta é limpa; de **+640** para cima, numa parte das giradas que você perderia a roda gira de novo: a chance da sua cor sobe aos poucos até **55%** no vermelho/preto e **3,05%** no verde em +1216, quando a roleta devolve uns **110%**.

## Corrida de Cavalos

Botão direito no bloco abre as apostas da corrida. Os **créditos**, o **espaço das fichas**, **DEPÓSITO** e **RETIRADA** são os mesmos das outras máquinas.

- Correm **7 cavalos**, um de cada pelagem do jogo, e **a cada dia do Minecraft muda tudo**: quais nomes correm (Rapidinha, Cruz Credo, Mimimi, Arrogante, Pinga Fogo, Velho Ranzinza, Pé de Valsa, Ventania, Trovão Manco, Bigodudo, Pangaré Veloz, Sossegado, Furacão de Botas, Tô Atrasado, Pé Quente, Chá de Cadeira...) e a força de cada um. Todo mundo no servidor vê a mesma escalação, e ela muda quando vira o dia (também com `/time set`). O dia aparece na tela.
- Embaixo de cada nome está **quanto ele paga** (x1,5 para o favorito, até x30 para o azarão). Passe o mouse num cavalo para ver a **chance de ele vencer hoje**.
- **Clique na fileira de um cavalo** para apostar nele (a ficha aparece na caixinha), escolha a **APOSTA** com **-** e **+** e aperte **CORRER!**: os números correm nas raias até a linha de chegada, com ultrapassagens no meio; se o seu cavalo vencer, a aposta volta multiplicada.
- Em cada cavalo, uns **90%** do que é apostado volta no longo prazo (um pouco menos, pelo arredondamento). Quando um azarão de **x10 ou mais** ganha, quem está perto é avisado no chat.

**Ligada ao resto do cassino:** mesmo **saldo da casa** e mesmo limite de **-5000**: a corrida só é aceita se o pior caso (o seu cavalo vencer) couber, então apostas altas em azarões ficam limitadas com a casa perto de 0 (a tela mostra o máximo). Com a casa no negativo vale a mesma **sorte da casa** das outras mesas (numa parte das corridas que o seu cavalo ganharia, vence outro a cada 64 fichas no negativo, uns 1,3% a mais das rodadas que você ganharia viram derrota; perto de -5000, todas). De 0 a +639 a corrida é limpa; de **+640** para cima, numa parte das corridas que o seu cavalo perderia o vencedor é sorteado de novo: a chance dele sobe aos poucos até **1,25 vez** a do dia em +1216 (20% vira 25%), quando cada cavalo devolve uns **110%**.

## Jogo de Dados

Botão direito na mesa de dados abre o jogo. Os **créditos**, o **espaço das fichas**, **DEPÓSITO** e **RETIRADA** são os mesmos das outras máquinas.

- Rolam **dois dados**, e as apostas são sobre a **soma** (de 2 a 12).
- **Clique numa fileira** da lista para escolher a aposta (a ficha aparece na escolhida). É **uma aposta por jogada**.
- **APOSTA** com **+** e **-** (1 a 64) e **ROLAR!**: os dados chacoalham no feltro e param; a soma aparece embaixo deles, e as últimas somas no quadro "ÚLTIMOS".
- Passe o mouse numa fileira para ver quando ela ganha e quantas vezes em 36 ela sai.

| Aposta | Ganha quando a soma é | Sai (em 36) | Paga |
| --- | --- | --- | --- |
| PARES | par | 18 | x2,0 |
| ÍMPARES | ímpar | 18 | x2,0 |
| NÚMERO | o número escolhido (2 a 12) | 1 a 6 | x5,7 no 7; x6,8 no 6 e no 8; x8,5 no 5 e no 9; x11,4 no 4 e no 10; x17,1 no 3 e no 11; x34,2 no 2 e no 12 |
| MENOR | 2 a 6 | 15 | x2,2 |
| MAIOR | 8 a 12 | 15 | x2,2 |

- O **número do NÚMERO** fica no quadradinho da fileira: clique para aumentar, botão direito para diminuir, ou use a rodinha do mouse.
- O prêmio é a aposta vezes o multiplicador, arredondado. PARES e ÍMPARES são jogo limpo (devolvem **100%** no longo prazo); NÚMERO, MENOR e MAIOR devolvem uns **92 a 95%**, e a casa fica com a diferença. O 7 não é nem MENOR nem MAIOR.

**Ligado ao resto do cassino:** mesmo **saldo da casa** e mesmo limite de **-5000**: a jogada só é aceita se o pior caso (a sua aposta acertar) couber, então apostas altas no NÚMERO 2 ou 12 ficam limitadas com a casa perto de 0 (a tela mostra o máximo). Com a casa no negativo vale a mesma **sorte da casa** das outras mesas (numa parte das jogadas que você ganharia, os dados mostram outra soma a cada 64 fichas no negativo, uns 1,3% a mais das rodadas que você ganharia viram derrota; perto de -5000, todas); de 0 a +639 os dados são limpos; de **+640** para cima, numa parte das jogadas que você perderia os dados rolam de novo, e a chance de acertar sobe aos poucos até +1216.

## Cara ou Coroa

Botão direito na mesa de cara ou coroa abre o jogo. Os **créditos**, o **espaço das fichas**, **DEPÓSITO** e **RETIRADA** são os mesmos das outras máquinas.

- **Clique em CARA ou COROA** para escolher o lado (a ficha aparece no escolhido). A cara da moeda é o rosto da skin do MasterChief; a coroa, uma coroa dourada.
- **APOSTA** com **+** e **-** (1 a 64) e **LANÇAR!**: a moeda gira no ar, mostrando o rosto e a coroa, e cai. O resultado aparece embaixo dela, e os últimos no quadro "ÚLTIMOS".
- **Acertar devolve 2x** a aposta. A casa ganha porque, **uma vez em 25**, a moeda **cai em pé**, e aí ninguém acerta: cada lado sai em 48% das vezes, e o jogo devolve uns **96%**.

**Ligado ao resto do cassino:** mesmo **saldo da casa** e mesmo limite de **-5000** (o pior caso é o seu lado sair). Com a casa no negativo vale a mesma **sorte da casa** das outras mesas (numa parte dos lançamentos que você ganharia, a moeda cai do outro lado a cada 64 fichas no negativo, uns 1,3% a mais das rodadas que você ganharia viram derrota; perto de -5000, todas); de 0 a +639 a moeda é limpa; de **+640** para cima, numa parte dos lançamentos que você perderia a moeda é lançada de novo, até o jogo devolver uns **110%** em +1216.

## Plinko de Slime

Botão direito no Plinko abre o jogo. Os **créditos**, o **espaço das fichas**, **DEPÓSITO** e **RETIRADA** são os mesmos das outras máquinas.

- Escolha a **APOSTA** com **+** e **-** (1 a 64) e aperte **LANÇAR!**: a **bola de slime** sai do funil e quica nas **6 fileiras de pinos**, cada vez para a esquerda ou para a direita (meio a meio), até cair num dos **7 buracos** de baixo. O buraco onde ela caiu fica com a borda branca.
- Cada buraco multiplica a aposta. Os do meio são os mais fáceis; as pontas, as mais difíceis:

| Buraco | 10x | 2x | 0,5x | 0,2x | 0,5x | 2x | 10x |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Chance | 1,6% | 9,4% | 23,4% | 31,2% | 23,4% | 9,4% | 1,6% |

- No total o Plinko devolve uns **98,4%** do que é apostado. Quando a conta não dá um número inteiro de fichas (0,5x de 1 ficha, por exemplo), a parte quebrada vira a chance de 1 ficha a mais (metade das vezes volta 1), então os multiplicadores valem igual para qualquer aposta.

**Ligado ao resto do cassino:** mesmo **saldo da casa** e mesmo limite de **-5000** (o pior caso é a bola cair numa ponta, 10x; com a casa perto de 0 a aposta fica limitada e a tela mostra o máximo). Com a casa no negativo vale a mesma **sorte da casa** das outras mesas (numa parte das quedas nos buracos de 2x e 10x, a bola cai num buraco de 0,5x ou 0,2x a cada 64 fichas no negativo, uns 1,3% a mais das rodadas que você ganharia viram derrota; perto de -5000, todas); de 0 a +639 o Plinko é limpo; de **+640** para cima, numa parte das quedas nos buracos de 0,5x e 0,2x a bola cai de novo, até o jogo devolver uns **110%** em +1216.

## Loteria de Mobs

O "jogo do bicho" do Minecraft. Botão direito no bloco abre a tabela com **25 mobs**: Porco, Vaca, Ovelha, Galinha, Lobo, Raposa, Abelha, Lula, Aldeão, Golem de Ferro, Zumbi, Afogado, Esqueleto, Creeper, Aranha, Slime, Saqueador, Enderman, Blaze, Ghast, Piglin, Esqueleto Wither, Warden, Wither e Dragão (o rosto de cada um sai da textura do próprio Minecraft).

- **Clique num mob** para escolher e aperte **APOSTAR!**: a aposta é sempre **32 fichas**, tiradas dos seus créditos (os mesmos das outras máquinas). **Uma aposta por dia**, num mob só.
- Quando o **dia do Minecraft acaba** (o relógio passa para o dia seguinte, também quando todo mundo dorme ou com `/time set`), **sai um mob**, todos com a mesma chance (1 em 25). A tela mostra quanto falta para o sorteio.
- Quem acertou **leva o prêmio inteiro** (se mais de um acertou, divide em partes iguais), direto nos créditos. Se **ninguém acertou, o prêmio acumula** para o dia seguinte.
- O resultado é **anunciado no chat** para todo mundo, e quem ganhou vê **"VOCÊ GANHOU!"** na tela com o valor. Quem ganhou sem estar no jogo vê o aviso quando entrar.
- A tela mostra o **prêmio acumulado**, os seus créditos, quantas apostas entraram hoje, a **sua aposta** (com borda amarela e a ficha na tabela) e o **último mob sorteado** (com borda verde).

**Saldo separado:** a loteria tem o próprio saldo (o prêmio, que só junta as apostas) e não mexe no saldo da casa dos outros jogos; ela só paga o que tem, então nunca cria fichas.

## Campo Minado

Estilo "Mines". Botão direito no bloco abre uma **grade de 5x5 blocos de pedra**. Os **créditos**, o **espaço das fichas**, **DEPÓSITO** e **RETIRADA** são os mesmos das outras máquinas.

- Escolha **quantas minas** tem na grade (**MINAS**, de 1 a 24, com - e +) e a **APOSTA**, e aperte **JOGAR!**: a aposta sai dos créditos e a grade fica coberta.
- **Clique nos blocos para cavar.** Um **diamante** ou uma **esmeralda** aumenta o multiplicador; uma **TNT** ou um **Creeper** explode e você perde a aposta.
- Aperte **SACAR!** (o botão dourado) a qualquer momento para levar a aposta vezes o multiplicador. Antes de achar a primeira pedra, SACAR! só devolve a aposta. Achar todas as pedras saca sozinho, e fechar a tela no meio da rodada também.
- No fim da rodada a grade toda aparece (apagada): onde estavam as outras minas e pedras.
- O quadro do meio mostra quantas pedras você achou, o multiplicador **atual**, o da **próxima** pedra, quanto você leva se **sacar** agora e a **chance** de o próximo bloco ser uma pedra.

Mais minas, mais difícil, e o multiplicador sobe mais rápido. Alguns exemplos:

| Minas | 1 pedra | 3 pedras | 5 pedras | 10 pedras |
| --- | --- | --- | --- | --- |
| 1 | x1,00 | x1,09 | x1,20 | x1,60 |
| 3 | x1,09 | x1,43 | x1,94 | x4,85 |
| 5 | x1,20 | x1,94 | x3,29 | x16,98 |
| 10 | x1,60 | x4,85 | x16,98 | x1044 |
| 24 | x24,00 | - | - | - |

O multiplicador é o justo (pela chance de chegar até ali) vezes **0,96**: seja qual for a estratégia, o jogo devolve uns **96%** no longo prazo. O prêmio é arredondado para baixo.

**Ligado ao resto do cassino:** mesmo **saldo da casa** e mesmo limite de **-5000**. Enquanto a rodada está aberta, a casa reserva o que pagaria se você sacasse, e só deixa cavar se o prêmio depois da próxima pedra ainda couber no limite: perto do limite, a tela avisa **"Limite da casa: hora de sacar!"**. Com a casa no negativo vale a mesma **sorte da casa** das outras mesas (uma parte das rodadas já começa condenada, e o primeiro bloco cavado nela é mina a cada 64 fichas no negativo, uns 1,3% a mais das rodadas que você ganharia viram derrota; perto de -5000, todas); de +640 para cima, a primeira mina de cada rodada pode virar pedra preciosa.

## Instalação

O `.jar` precisa do **Fabric Loader** e do **Fabric API** para a 1.20.1. Coloque na pasta `mods`. Em servidor, instale no servidor e em todos os jogadores (só o servidor sorteia, mas a tela da máquina fica no jogo de cada um).

## Como gerar o .jar

### Opção 1: GitHub (sem instalar nada)

Este mod mora no repositório [mods-udas](https://github.com/edugcs-gif/mods-udas), na pasta `cassino/`.

1. No repositório, abra a aba **Actions** e escolha o workflow **cassino - gerar o .jar**. Ele roda sozinho a cada alteração em `cassino/` (uns 2 a 3 minutos); também dá para rodar à mão em **Run workflow**.
2. Clique no build que terminou e baixe o artefato **cassino**. Dentro do zip está o `cassino-1.19.0.jar`.

### Opção 2: no seu PC

Precisa do **JDK 17 ou 21**. Na pasta `cassino/` do projeto:

```bash
./gradlew build        # Git Bash / Linux / Mac
gradlew.bat build      # Prompt do Windows
```

O jar fica em `build/libs/cassino-1.19.0.jar`. Para testar sem montar o modpack: `./gradlew runClient`.

## Ideias para a próxima versão

Números na roleta, divisão de pares no blackjack, conquistas, uma ficha com valores diferentes (azul, verde, preta) e um caixa (NPC) que troca fichas por esmeraldas.

## Licença

MIT (veja `LICENSE`).
