package com.edugcs.tops;

import java.util.Locale;
import java.util.Map;

/** Cada ranking do servidor e como o valor é calculado a partir das estatísticas. */
enum Categoria {
    // ----- Combate
    MOBS("mobs", "Mobs abatidos", "§c", "mob", "mobs", Tema.COMBATE, Tipo.CONTAGEM),
    PVP("pvp", "Jogadores abatidos", "§c", "abate", "abates", Tema.COMBATE, Tipo.CONTAGEM),
    KD("kd", "Relação K/D no PvP", "§c", "", "", Tema.COMBATE, Tipo.DECIMAL),
    CREEPERS("creepers", "Creepers mortos", "§a", "creeper", "creepers", Tema.COMBATE, Tipo.CONTAGEM),
    CAUSADO("causado", "Dano causado", "§c", "coração", "corações", Tema.COMBATE, Tipo.CORACOES),
    ESCUDO("escudo", "Dano bloqueado no escudo", "§9", "coração", "corações", Tema.COMBATE, Tipo.CORACOES),
    MORTES("mortes", "Quem mais morreu", "§4", "morte", "mortes", Tema.COMBATE, Tipo.CONTAGEM),
    AZARADO("azarado", "Mortes por hora de jogo", "§4", "morte/h", "mortes/h", Tema.COMBATE, Tipo.DECIMAL),
    DANO("dano", "Dano sofrido", "§4", "coração", "corações", Tema.COMBATE, Tipo.CORACOES),
    SOBREVIVENTE("sobrevivente", "Tempo sem morrer", "§6", "h", "h", Tema.COMBATE, Tipo.HORAS),
    // ----- Mineração
    BLOCOS("blocos", "Blocos quebrados", "§7", "bloco", "blocos", Tema.MINERACAO, Tipo.CONTAGEM),
    DIAMANTES("diamantes", "Diamantes minerados", "§b", "diamante", "diamantes", Tema.MINERACAO, Tipo.CONTAGEM),
    DIMAHORA("dimahora", "Diamantes por hora de jogo", "§b", "diamante/h", "diamantes/h", Tema.MINERACAO, Tipo.DECIMAL),
    ESMERALDAS("esmeraldas", "Esmeraldas mineradas", "§a", "esmeralda", "esmeraldas", Tema.MINERACAO, Tipo.CONTAGEM),
    FERRO("ferro", "Ferro minerado", "§f", "minério", "minérios", Tema.MINERACAO, Tipo.CONTAGEM),
    QUARTZO("quartzo", "Quartzo minerado", "§f", "minério", "minérios", Tema.MINERACAO, Tipo.CONTAGEM),
    NETHERITE("netherite", "Detritos antigos", "§5", "detrito", "detritos", Tema.MINERACAO, Tipo.CONTAGEM),
    FERRAMENTAS("ferramentas", "Ferramentas quebradas", "§8", "ferramenta", "ferramentas", Tema.MINERACAO, Tipo.CONTAGEM),
    // ----- Fazenda e Vila
    COLHEITA("colheita", "Plantações colhidas", "§a", "plantação", "plantações", Tema.FAZENDA, Tipo.CONTAGEM),
    CRIADOR("criador", "Animais criados", "§e", "animal", "animais", Tema.FAZENDA, Tipo.CONTAGEM),
    PEIXES("peixes", "Peixes pescados", "§b", "peixe", "peixes", Tema.FAZENDA, Tipo.CONTAGEM),
    TROCAS("trocas", "Trocas com aldeões", "§2", "troca", "trocas", Tema.FAZENDA, Tipo.CONTAGEM),
    CRAFTADOS("craftados", "Itens fabricados", "§6", "item", "itens", Tema.FAZENDA, Tipo.CONTAGEM),
    ENCANTAMENTOS("encantamentos", "Itens encantados", "§d", "item", "itens", Tema.FAZENDA, Tipo.CONTAGEM),
    // ----- Exploração e Vida
    HORAS("horas", "Tempo de jogo", "§6", "h", "h", Tema.EXPLORACAO, Tipo.HORAS),
    XP("xp", "XP ganho", "§a", "XP", "XP", Tema.EXPLORACAO, Tipo.CONTAGEM),
    DISTANCIA("distancia", "Distância percorrida", "§3", "km", "km", Tema.EXPLORACAO, Tipo.KM),
    NADO("nado", "Distância nadando", "§9", "km", "km", Tema.EXPLORACAO, Tipo.KM),
    CAVALO("cavalo", "Distância a cavalo", "§6", "km", "km", Tema.EXPLORACAO, Tipo.KM),
    QUEDA("queda", "Altura de queda somada", "§7", "bloco", "blocos", Tema.EXPLORACAO, Tipo.BLOCOS),
    PULOS("pulos", "Pulos", "§d", "pulo", "pulos", Tema.EXPLORACAO, Tipo.CONTAGEM),
    AGACHADO("agachado", "Tempo agachado", "§8", "h", "h", Tema.EXPLORACAO, Tipo.HORAS),
    BAUS("baus", "Baús abertos", "§6", "baú", "baús", Tema.EXPLORACAO, Tipo.CONTAGEM),
    DORMIR("dormir", "Noites dormidas", "§9", "noite", "noites", Tema.EXPLORACAO, Tipo.CONTAGEM),
    RAGEQUIT("ragequit", "Vezes que saiu do servidor", "§c", "saída", "saídas", Tema.EXPLORACAO, Tipo.CONTAGEM),
    LIXO("lixo", "Itens jogados no chão", "§8", "item", "itens", Tema.EXPLORACAO, Tipo.CONTAGEM);

    enum Tipo { CONTAGEM, HORAS, KM, CORACOES, BLOCOS, DECIMAL }

    private static final Locale BR = new Locale("pt", "BR");
    private static final String[] COLHEITAS = {"wheat", "carrots", "potatoes", "beetroots", "nether_wart",
            "cocoa", "melon", "pumpkin", "sweet_berry_bush", "sugar_cane"};
    private static final String[] DISTANCIAS = {"walk_one_cm", "sprint_one_cm", "crouch_one_cm", "swim_one_cm",
            "walk_on_water_one_cm", "walk_under_water_one_cm", "climb_one_cm", "fly_one_cm", "aviate_one_cm",
            "boat_one_cm", "horse_one_cm", "minecart_one_cm", "pig_one_cm", "strider_one_cm"};

    final String id, titulo, cor, singular, plural;
    final Tema tema;
    final Tipo tipo;

    Categoria(String id, String titulo, String cor, String singular, String plural, Tema tema, Tipo tipo) {
        this.id = id; this.titulo = titulo; this.cor = cor; this.singular = singular; this.plural = plural;
        this.tema = tema; this.tipo = tipo;
    }

    /** Valor bruto (inteiro) desta categoria para um arquivo de estatísticas. */
    long calcular(Map<String, Map<String, Long>> st) {
        Map<String, Long> custom = st.getOrDefault("minecraft:custom", Map.of());
        Map<String, Long> mined = st.getOrDefault("minecraft:mined", Map.of());
        Map<String, Long> killed = st.getOrDefault("minecraft:killed", Map.of());
        switch (this) {
            case MOBS: return c(custom, "mob_kills");
            case PVP: return c(custom, "player_kills");
            case CREEPERS: return c(killed, "creeper");
            case CAUSADO: return c(custom, "damage_dealt");
            case ESCUDO: return c(custom, "damage_blocked_by_shield");
            case MORTES: return c(custom, "deaths");
            case DANO: return c(custom, "damage_taken");
            case SOBREVIVENTE: return c(custom, "time_since_death");
            case BLOCOS: return soma(mined);
            case DIAMANTES: return c(mined, "diamond_ore") + c(mined, "deepslate_diamond_ore");
            case ESMERALDAS: return c(mined, "emerald_ore") + c(mined, "deepslate_emerald_ore");
            case FERRO: return c(mined, "iron_ore") + c(mined, "deepslate_iron_ore");
            case NETHERITE: return c(mined, "ancient_debris");
            case QUARTZO: return c(mined, "nether_quartz_ore");
            case FERRAMENTAS: return soma(st.getOrDefault("minecraft:broken", Map.of()));
            case COLHEITA: { long t = 0; for (String k : COLHEITAS) t += c(mined, k); return t; }
            case CRIADOR: return c(custom, "animals_bred");
            case PEIXES: return c(custom, "fish_caught");
            case TROCAS: return c(custom, "traded_with_villager");
            case CRAFTADOS: return soma(st.getOrDefault("minecraft:crafted", Map.of()));
            case ENCANTAMENTOS: return c(custom, "enchant_item");
            case HORAS: return c(custom, "play_time");
            case DISTANCIA: { long t = 0; for (String k : DISTANCIAS) t += c(custom, k); return t; }
            case NADO: return c(custom, "swim_one_cm") + c(custom, "walk_under_water_one_cm");
            case CAVALO: return c(custom, "horse_one_cm");
            case QUEDA: return c(custom, "fall_one_cm");
            case PULOS: return c(custom, "jump");
            case AGACHADO: return c(custom, "sneak_time");
            case BAUS: return c(custom, "open_chest") + c(custom, "open_barrel") + c(custom, "open_enderchest");
            case DORMIR: return c(custom, "sleep_in_bed");
            case RAGEQUIT: return c(custom, "leave_game");
            case LIXO: return c(custom, "drop");
            // KD, AZARADO e DIMAHORA são calculados depois de juntar as contas (Estatisticas);
            // XP vem do contador do próprio mod (Experiencia).
            default: return 0;
        }
    }

    /** Como juntar duas contas do mesmo nick (original + pirata). */
    long combinar(long a, long b) {
        return this == SOBREVIVENTE ? Math.max(a, b) : a + b;
    }

    private static long c(Map<String, Long> m, String k) { return m.getOrDefault("minecraft:" + k, 0L); }

    private static long soma(Map<String, Long> m) { long t = 0; for (long v : m.values()) t += v; return t; }

    /** Valor já formatado com a unidade, ex.: "1.531 mobs", "27,7 h", "304,4 km". */
    String formatar(long v) {
        switch (tipo) {
            case HORAS: return String.format(BR, "%,.1f h", v / 72000.0);          // ticks
            case KM: return String.format(BR, "%,.1f km", v / 100000.0);           // cm
            case CORACOES: { long h = Math.round(v / 20.0); return String.format(BR, "%,d %s", h, h == 1 ? singular : plural); } // décimos de vida
            case BLOCOS: { long b = Math.round(v / 100.0); return String.format(BR, "%,d %s", b, b == 1 ? singular : plural); }   // cm
            case DECIMAL: {   // guardado multiplicado por 100
                String n = String.format(BR, "%,.2f", v / 100.0);
                return plural.isEmpty() ? n : n + " " + (v == 100 ? singular : plural);
            }
            default: return String.format(BR, "%,d %s", v, v == 1 ? singular : plural);
        }
    }

    /** Categorias que não vêm direto de um número dos arquivos de estatísticas. */
    boolean derivada() { return this == KD || this == AZARADO || this == DIMAHORA || this == XP; }

    /** Se o valor aparece como zero depois de formatado (para não listar quem não tem nada). */
    boolean vazio(long v) {
        switch (tipo) {
            case HORAS: return v < 3600;      // menos de 3 minutos
            case KM: return v < 10000;        // menos de 100 m
            case CORACOES: return v < 10;
            case BLOCOS: return v < 50;
            default: return v <= 0;
        }
    }
}
