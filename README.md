# mods-udas

Mods de Minecraft (Fabric) do edugcs. Cada mod fica na sua própria pasta, com README, licença e projeto Gradle próprios.

| Mod | O que faz | Minecraft |
|---|---|---|
| [**jblesk**](jblesk/) | **jbleskão** (bloco) e **jblesk** (caixa de som portátil): tocam músicas e playlists do YouTube, SoundCloud ou links diretos de áudio para todo mundo que estiver perto. Pausa, pular na barra, volume pessoal, alcance perto/longe, e a caixa também toca do inventário, do baú do burro e do barco com baú. | 1.21.1 (Fabric) |

## Baixar o .jar

O GitHub gera o `.jar` sozinho a cada alteração: abra a aba **Actions**, entre no último build do mod e baixe o artefato (por exemplo, `jblesk`). Também dá para gerar no PC: veja o README de cada mod.

## Estrutura

```
mods-udas/
├── jblesk/          o mod jblesk (código, recursos, README e licença)
└── .github/
    └── workflows/   um workflow por mod, que gera o .jar
```

Para adicionar outro mod: crie uma pasta nova com o projeto e um workflow em `.github/workflows/`.
