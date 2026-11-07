package com.edugcs.tops;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/** Lê a pasta world/stats e soma as estatísticas por nick (juntando contas com dois UUIDs). */
final class Estatisticas {

    /** nick -> (categoria -> valor) */
    final Map<String, EnumMap<Categoria, Long>> porJogador = new HashMap<>();

    static Estatisticas ler(Path pasta, Function<UUID, String> nomes) throws Exception {
        Estatisticas e = new Estatisticas();
        try (DirectoryStream<Path> arquivos = Files.newDirectoryStream(pasta, "*.json")) {
            for (Path f : arquivos) {
                String fn = f.getFileName().toString();
                String id = fn.substring(0, fn.length() - 5);
                Map<String, Map<String, Long>> st;
                try {
                    st = converter(Json.parse(Files.readString(f)));
                } catch (Exception ex) {
                    continue; // arquivo corrompido ou vazio: ignora
                }
                String nome = null;
                try { nome = nomes.apply(UUID.fromString(id)); } catch (IllegalArgumentException ignored) { }
                if (nome == null) nome = id.length() > 8 ? id.substring(0, 8) : id;
                EnumMap<Categoria, Long> m = e.porJogador.computeIfAbsent(nome, k -> new EnumMap<>(Categoria.class));
                for (Categoria c : Categoria.values()) m.merge(c, c.calcular(st), c::combinar);
            }
        }
        e.calcularDerivadas();
        return e;
    }

    private static final double HORA = 72000.0; // ticks

    /** K/D, mortes por hora e diamantes por hora (guardados x100), já com as contas juntadas. */
    void calcularDerivadas() {
        for (EnumMap<Categoria, Long> m : porJogador.values()) {
            long pvp = m.getOrDefault(Categoria.PVP, 0L);
            long mortes = m.getOrDefault(Categoria.MORTES, 0L);
            long horas = m.getOrDefault(Categoria.HORAS, 0L);
            long dimas = m.getOrDefault(Categoria.DIAMANTES, 0L);
            m.put(Categoria.KD, pvp == 0 ? 0 : Math.round(pvp * 100.0 / Math.max(mortes, 1)));
            // só entra quem jogou pelo menos 1 hora, para não distorcer
            m.put(Categoria.AZARADO, horas < HORA ? 0 : Math.round(mortes * 100.0 * HORA / horas));
            m.put(Categoria.DIMAHORA, horas < HORA ? 0 : Math.round(dimas * 100.0 * HORA / horas));
        }
    }

    /** Coloca valores vindos de fora (ex.: XP contado pelo mod), somando por nick. */
    void definir(Categoria c, Map<String, Long> valores) {
        for (Map.Entry<String, Long> v : valores.entrySet()) {
            String nome = nomeExato(v.getKey());
            if (nome == null) nome = v.getKey();
            EnumMap<Categoria, Long> m = porJogador.computeIfAbsent(nome, k -> new EnumMap<>(Categoria.class));
            m.put(c, v.getValue());
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Map<String, Long>> converter(Object raiz) {
        Map<String, Map<String, Long>> out = new HashMap<>();
        Object stats = ((Map<String, Object>) raiz).get("stats");
        if (!(stats instanceof Map)) return out;
        for (Map.Entry<String, Object> cat : ((Map<String, Object>) stats).entrySet()) {
            if (!(cat.getValue() instanceof Map)) continue;
            Map<String, Long> vals = new HashMap<>();
            for (Map.Entry<String, Object> v : ((Map<String, Object>) cat.getValue()).entrySet()) {
                if (v.getValue() instanceof Number) vals.put(v.getKey(), ((Number) v.getValue()).longValue());
            }
            out.put(cat.getKey(), vals);
        }
        return out;
    }

    long valor(String nome, Categoria c) {
        EnumMap<Categoria, Long> m = porJogador.get(nome);
        return m == null ? 0 : m.getOrDefault(c, 0L);
    }

    /** Ranking ordenado do maior para o menor, sem quem está zerado. */
    List<Map.Entry<String, Long>> ranking(Categoria c) {
        List<Map.Entry<String, Long>> r = new ArrayList<>();
        for (Map.Entry<String, EnumMap<Categoria, Long>> e : porJogador.entrySet()) {
            long v = e.getValue().getOrDefault(c, 0L);
            if (!c.vazio(v)) r.add(Map.entry(e.getKey(), v));
        }
        r.sort((a, b) -> {
            int x = Long.compare(b.getValue(), a.getValue());
            return x != 0 ? x : a.getKey().compareToIgnoreCase(b.getKey());
        });
        return r;
    }

    /** Posição (1, 2, 3...) do jogador no ranking, ou 0 se não aparece. */
    int posicao(String nome, Categoria c) {
        List<Map.Entry<String, Long>> r = ranking(c);
        for (int i = 0; i < r.size(); i++) if (r.get(i).getKey().equalsIgnoreCase(nome)) return i + 1;
        return 0;
    }

    /** Nick exatamente como está salvo (para aceitar /status maikimbr). */
    String nomeExato(String nome) {
        for (String n : porJogador.keySet()) if (n.equalsIgnoreCase(nome)) return n;
        return null;
    }
}
