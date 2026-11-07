package com.edugcs.tops;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Monta as linhas de texto (com cores §) de cada comando. Não depende do Minecraft. */
final class Mensagens {
    static final int TOP = 10;

    private static String cabecalho(String titulo) {
        return "§8§m        §r §e§l" + titulo + " §8§m        ";
    }

    private static String medalha(int pos) {
        return pos == 1 ? "§6" : pos == 2 ? "§f" : pos == 3 ? "§e" : "§7";
    }

    /** /top e /top <página>: o líder de cada categoria de um tema. */
    static List<String> resumo(Estatisticas e, Tema tema) {
        List<String> l = new ArrayList<>();
        l.add("");
        l.add(cabecalho("Tops do Servidor"));
        l.add(tema.cor + "§l" + tema.titulo + " §8(página " + tema.pagina() + "/" + Tema.values().length + ")");
        for (Categoria c : Categoria.values()) {
            if (c.tema != tema) continue;
            List<Map.Entry<String, Long>> r = e.ranking(c);
            String lider = r.isEmpty() ? "§8ninguém ainda"
                    : "§f" + r.get(0).getKey() + " §8- " + c.cor + c.formatar(r.get(0).getValue());
            l.add("§7" + c.titulo + " §8(" + c.id + ")§7: " + lider);
        }
        StringBuilder pags = new StringBuilder("§8Páginas: ");
        for (Tema t : Tema.values()) {
            pags.append(t == tema ? "§e§l" : "§7").append("/top ").append(t.pagina()).append(" §8").append(t.titulo);
            if (t.ordinal() < Tema.values().length - 1) pags.append(" §8· ");
        }
        l.add(pags.toString());
        l.add("§8Use §7/top <categoria>§8 para ver o top 10.");
        return l;
    }

    /** /top <categoria>: top 10 e a posição de quem pediu. */
    static List<String> ranking(Estatisticas e, Categoria c, String eu) {
        List<String> l = new ArrayList<>();
        List<Map.Entry<String, Long>> r = e.ranking(c);
        l.add("");
        l.add(cabecalho(c.titulo));
        if (r.isEmpty()) {
            l.add("§7Ninguém tem pontos nessa categoria ainda.");
            return l;
        }
        int meu = 0;
        for (int i = 0; i < r.size(); i++) {
            Map.Entry<String, Long> x = r.get(i);
            boolean souEu = eu != null && x.getKey().equalsIgnoreCase(eu);
            if (souEu) meu = i + 1;
            if (i < TOP) {
                String nome = souEu ? "§a§l" + x.getKey() : "§f" + x.getKey();
                l.add(medalha(i + 1) + "§l" + (i + 1) + "º §r" + nome + " §8- " + c.cor + c.formatar(x.getValue()));
            }
        }
        if (eu != null) {
            l.add("");
            if (meu == 0) l.add("§7» Você ainda não aparece nesse ranking.");
            else {
                l.add("§7» Você: " + c.cor + "§l" + c.formatar(e.valor(e.nomeExato(eu), c)) + " §8(§e" + meu + "º lugar§8)");
                if (meu == 1) l.add("§6§l» Você é o número 1 do servidor!");
            }
        }
        return l;
    }

    /** /status [jogador]: todas as categorias de uma pessoa, duas por linha, separadas por tema. */
    static List<String> status(Estatisticas e, String nome, boolean proprio) {
        List<String> l = new ArrayList<>();
        l.add("");
        l.add(cabecalho(proprio ? "Suas estatísticas" : "Estatísticas de " + nome));
        for (Tema t : Tema.values()) {
            l.add(t.cor + "§l" + t.titulo);
            String pendente = null;
            for (Categoria c : Categoria.values()) {
                if (c.tema != t) continue;
                long v = e.valor(nome, c);
                int pos = e.posicao(nome, c);
                String item = "§7" + c.titulo + ": " + c.cor + c.formatar(v) + (pos > 0 ? " " + medalha(pos) + pos + "º" : "");
                if (pendente == null) pendente = item;
                else { l.add(" " + pendente + " §8| " + item); pendente = null; }
            }
            if (pendente != null) l.add(" " + pendente);
        }
        return l;
    }
}
