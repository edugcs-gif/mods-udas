package com.edugcs.tops;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Contador de XP ganho (parte que não depende do Minecraft, para poder testar).
 * O jogo guarda o XP total "desde a última morte"; aqui somamos todo aumento desse valor,
 * e quando ele cai (morte, /xp) só atualizamos a referência.
 */
final class Contador {
    static final class Registro { String nome; long ganho; long ultimo; }

    final Map<UUID, Registro> dados = new LinkedHashMap<>();
    boolean sujo;

    /** Chamado periodicamente com o XP total atual de um jogador online. */
    void registrar(UUID id, String nome, long atual) {
        Registro r = dados.get(id);
        if (r == null) {
            r = new Registro();
            r.ganho = atual;   // começa com o XP que a pessoa já tem desde a última morte
            r.ultimo = atual;
            dados.put(id, r);
            sujo = true;
        } else if (atual > r.ultimo) {
            r.ganho += atual - r.ultimo;
            r.ultimo = atual;
            sujo = true;
        } else if (atual < r.ultimo) {
            r.ultimo = atual;
            sujo = true;
        }
        if (nome != null && !nome.equals(r.nome)) { r.nome = nome; sujo = true; }
    }

    /** XP ganho somado por nick. */
    Map<String, Long> porNome() {
        Map<String, Long> m = new HashMap<>();
        for (Registro r : dados.values()) if (r.nome != null) m.merge(r.nome, r.ganho, Long::sum);
        return m;
    }

    @SuppressWarnings("unchecked")
    void carregar(Path arquivo) throws Exception {
        dados.clear();
        if (!Files.exists(arquivo)) return;
        Object raiz = Json.parse(Files.readString(arquivo));
        Object jog = ((Map<String, Object>) raiz).get("jogadores");
        if (!(jog instanceof Map)) return;
        for (Map.Entry<String, Object> e : ((Map<String, Object>) jog).entrySet()) {
            Map<String, Object> v = (Map<String, Object>) e.getValue();
            Registro r = new Registro();
            r.nome = (String) v.get("nome");
            r.ganho = ((Number) v.getOrDefault("ganho", 0.0)).longValue();
            r.ultimo = ((Number) v.getOrDefault("ultimo", 0.0)).longValue();
            dados.put(UUID.fromString(e.getKey()), r);
        }
        sujo = false;
    }

    void salvar(Path arquivo) throws Exception {
        StringBuilder b = new StringBuilder("{\n  \"jogadores\": {");
        boolean primeiro = true;
        for (Map.Entry<UUID, Registro> e : dados.entrySet()) {
            Registro r = e.getValue();
            b.append(primeiro ? "\n" : ",\n");
            primeiro = false;
            b.append("    \"").append(e.getKey()).append("\": {\"nome\": \"")
             .append(r.nome == null ? "" : r.nome.replace("\\", "\\\\").replace("\"", "\\\""))
             .append("\", \"ganho\": ").append(r.ganho).append(", \"ultimo\": ").append(r.ultimo).append("}");
        }
        b.append("\n  }\n}\n");
        Path tmp = arquivo.resolveSibling(arquivo.getFileName() + ".tmp");
        Files.writeString(tmp, b.toString());
        Files.move(tmp, arquivo, StandardCopyOption.REPLACE_EXISTING);
        sujo = false;
    }
}
