# Loja de Baú (Fabric 1.20.1)

Lojas de troca com **baú e placa**, e **baús trancados**. **Só precisa estar no servidor**: quem entra não instala nada.

## Criar uma loja

1. Coloque um baú (ou barril) e uma placa nele (na frente ou em cima).
2. Escreva `[loja]` na 1ª linha.
3. Segure o item que vai **vender** na mão principal e o que quer **receber** na outra mão (tecla **F** troca de mão).
4. Clique com o botão direito na placa.

As quantidades são as das pilhas que você está segurando, ou escreva na placa antes: **linha 2** = quantos vende, **linha 3** = quantos recebe. A placa é reescrita e travada:

```
[Loja]
16x Barra de Ferro
por 1x Diamante
Edugcs
```

Dá para pôr várias placas no mesmo baú, cada uma vendendo uma coisa.

## Comprar

Clique na placa: sai o pagamento do inventário e entram os itens (o que não couber cai no chão). **Agachado**, faz até 10 trocas seguidas. O item precisa ser exatamente o pedido (encantamentos e nome contam). O dono recebe um aviso se estiver online.

O dono clica na própria placa para ver quantas trocas o estoque aguenta e quanto pagamento tem guardado. Quebrar a placa desfaz a loja.

## Baú privado

Placa com `[privado]` (ou `[trancado]`, `[private]`) na 1ª linha, num baú, barril, fornalha etc. Nas **linhas 2 a 4**, nicks de amigos que também podem abrir (opcional). Clique na placa para trancar; quebrar a placa destranca.

## Proteção

- Só o dono (e, no privado, os amigos) abre o baú; só o dono quebra o baú e a placa. **OPs** podem tudo.
- Baú duplo fica protegido inteiro.
- Ninguém coloca funil ou carrinho com funil embaixo de um baú protegido.
- Explosões ainda podem quebrar o baú, como qualquer baú normal.

As lojas e os baús trancados ficam em `world/lojas.json`.

## Gerar o .jar

O GitHub gera sozinho (aba **Actions**, workflow `loja`). No PC: `./gradlew build` dentro desta pasta; o jar fica em `build/libs/`.
