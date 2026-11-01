# Inventário Transparente (Fabric 1.20.1)

Deixa o **fundo do inventário** semitransparente, para dar para ver o mundo atrás enquanto você mexe nos itens. Vale para o inventário e para as outras telas com inventário: baús, fornalhas, mesas de trabalho, bigornas, o caça-níquel do Cassino...

- Só o **fundo** (e os quadradinhos dos espaços) fica transparente: os **itens, os textos e os botões continuam nítidos**.
- O **escurecimento** atrás do inventário também fica mais leve, na mesma proporção. Telas sem inventário (menu de pausa, opções, chat) não mudam.

## Tecla

A tecla **I** troca o quanto o fundo aparece: **100% → 80% → 60% → 40% → 20%** e volta para 100%. O padrão é **60%**.

- Funciona **com o inventário aberto** (dá para ver a mudança na hora; o nível aparece no canto da tela) e fora dele (aparece na barra de ação).
- Digitando numa caixa de texto (a busca do criativo, por exemplo), a tecla não troca nada.
- Para usar outra tecla: Opções → Controles → Atalhos de teclado → Inventário → **Transparência do inventário**.
- A escolha fica salva em `config/transparencia.properties` (`opacidade=60`; dá para pôr qualquer valor de 5 a 100).

## Instalação

Precisa do **Fabric Loader** e do **Fabric API** para a 1.20.1. Coloque o `.jar` na pasta `mods`.

É **só visual**: precisa estar só no PC de quem quer usar, **não** no servidor.

## Gerar o .jar

O GitHub gera o `.jar` sozinho a cada alteração: no repositório [mods-udas](https://github.com/edugcs-gif/mods-udas), aba **Actions**, workflow **transparencia - gerar o .jar**, artefato **transparencia**. No PC, na pasta `transparencia/`:

```bash
./gradlew build
```

O jar fica em `build/libs/transparencia-1.0.0.jar`.
