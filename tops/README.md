# Tops do Servidor (Fabric 1.20.1)

Rankings do servidor tirados das **estatísticas do próprio jogo** (a pasta `world/stats`), então contam todo o histórico, não só a partir de quando o mod foi instalado. **Só precisa estar no servidor**: quem entra não instala nada.

## Comandos (qualquer jogador)

| Comando | O que mostra |
|---|---|
| `/top` ou `/top 1`…`/top 4` | O 1º lugar de cada ranking, separado em 4 páginas por tema (também dá `/top combate`, `/top mineracao`, `/top fazenda`, `/top exploracao`) |
| `/top <categoria>` | O top 10 da categoria e a sua posição |
| `/status [jogador]` | Todas as estatísticas de uma pessoa, com a colocação em cada uma |
| `/peixes` | Atalho para `/top peixes` |

## Rankings (35)

- **Combate:** `mobs`, `pvp`, `kd` (relação K/D), `creepers`, `causado` (dano causado), `escudo` (dano bloqueado), `mortes`, `azarado` (mortes por hora), `dano` (dano sofrido), `sobrevivente` (tempo sem morrer)
- **Mineração:** `blocos`, `diamantes`, `dimahora` (diamantes por hora), `esmeraldas`, `ferro`, `netherite`, `ferramentas` (quebradas)
- **Fazenda e Vila:** `colheita`, `criador`, `peixes`, `trocas`, `craftados`, `encantamentos`
- **Exploração e Vida:** `horas`, `xp`, `distancia`, `nado`, `cavalo`, `queda`, `pulos`, `agachado`, `baus`, `dormir`, `ragequit` (vezes que saiu), `lixo` (itens jogados no chão)

Detalhes:

- Contas que aparecem com dois UUIDs (modo original e pirata) são somadas pelo nick.
- `azarado` e `dimahora` só contam quem jogou pelo menos 1 hora.
- **XP ganho** não existe nas estatísticas do jogo: o mod conta a partir de quando foi instalado (começando com o XP que cada um tem desde a última morte) e salva em `world/tops_xp.json`, a cada minuto e ao desligar o servidor. Se o contador der algum erro, ele só para de contar, sem derrubar o servidor.

## Gerar o .jar

O GitHub gera sozinho (aba **Actions**, workflow `tops`). No PC: `./gradlew build` dentro desta pasta; o jar fica em `build/libs/`.
