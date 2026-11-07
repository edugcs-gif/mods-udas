package com.edugcs.tops;

/** Páginas do /top. */
enum Tema {
    COMBATE("combate", "Combate", "§c"),
    MINERACAO("mineracao", "Mineração", "§7"),
    FAZENDA("fazenda", "Fazenda e Vila", "§a"),
    EXPLORACAO("exploracao", "Exploração e Vida", "§3");

    final String id, titulo, cor;

    Tema(String id, String titulo, String cor) { this.id = id; this.titulo = titulo; this.cor = cor; }

    int pagina() { return ordinal() + 1; }
}
