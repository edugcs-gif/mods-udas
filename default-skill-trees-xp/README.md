# Default Skill Trees com XP exponencial

Versão alterada do **Default Skill Trees 1.1** (árvores de Combate e Mineração do mod **Pufferfish's Skills**). O mod original é do Pufferfish e é de domínio público (licença Unlicense, em `mod/LICENSE`).

## O que mudou

Só a quantidade de XP necessária para subir de nível (o `experience_per_level` em `mod/data/puffish_skills/puffish_skills/categories/*/experience.json`). Agora **cada nível custa 5% a mais que o anterior**, sem teto:

| | Original | Agora |
|---|---|---|
| Combate | `min(level ^ 1.432 + 10, 200)` | `120 * 1.05 ^ level` |
| Mineração | 250 / 500 / 750 por faixa de 10 níveis | `1250 * 1.05 ^ level` |

| Nível | Combate | Mineração |
|---|---|---|
| 1 | 120 | 1.250 |
| 10 | 186 | 1.939 |
| 20 | 303 | 3.159 |
| 40 | 805 | 8.381 |
| 60 | 2.135 | 22.237 |

O XP que cada ação dá (matar mobs, minerar) e as habilidades continuam iguais ao original. Para mudar a curva, edite o `expression` nos dois `experience.json`.

## Instalar

No servidor, troque o `default_skill_trees-1.1.jar` original por este (são o mesmo mod: os dois juntos não deixam o servidor ligar). Precisa do Pufferfish's Skills e do Pufferfish's Attributes.

## Gerar o .jar

O GitHub gera sozinho (aba **Actions**, workflow `default-skill-trees-xp`): é só a pasta `mod/` compactada.
