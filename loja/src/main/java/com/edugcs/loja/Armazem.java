package com.edugcs.loja;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Lista de lojas e o arquivo world/lojas.json (sem depender do Minecraft). */
final class Armazem {
    static final class Registro {
        String dim;
        int px, py, pz;      // placa
        int bx, by, bz;      // baú
        UUID dono;
        String nomeDono;
        String vendeNbt, recebeNbt;
        int vendeQtd, recebeQtd;
        Object vende, recebe; // pilhas já convertidas (preenchido pelo mod)
        String tipo = "loja";            // "loja" ou "privado"
        List<String> amigos = new ArrayList<>(); // só para "privado": quem mais pode abrir

        boolean privado() { return "privado".equals(tipo); }

        boolean amigo(String nome) {
            for (String a : amigos) if (a.equalsIgnoreCase(nome)) return true;
            return false;
        }

        boolean placaEm(String d, int x, int y, int z) { return dim.equals(d) && px == x && py == y && pz == z; }
        boolean bauEm(String d, int x, int y, int z) { return dim.equals(d) && bx == x && by == y && bz == z; }
    }

    final List<Registro> lojas = new ArrayList<>();

    Registro porPlaca(String d, int x, int y, int z) {
        for (Registro r : lojas) if (r.placaEm(d, x, y, z)) return r;
        return null;
    }

    /** Loja cujo baú está exatamente nessa posição. */
    Registro porBau(String d, int x, int y, int z) {
        for (Registro r : lojas) if (r.bauEm(d, x, y, z)) return r;
        return null;
    }

    List<Registro> todasDoBau(String d, int x, int y, int z) {
        List<Registro> l = new ArrayList<>();
        for (Registro r : lojas) if (r.bauEm(d, x, y, z)) l.add(r);
        return l;
    }

    @SuppressWarnings("unchecked")
    void carregar(Path arquivo) throws Exception {
        lojas.clear();
        if (!Files.exists(arquivo)) return;
        Map<String, Object> raiz = (Map<String, Object>) Json.parse(Files.readString(arquivo));
        Object lista = raiz.get("lojas");
        if (!(lista instanceof List)) return;
        for (Object o : (List<Object>) lista) {
            try {
                Map<String, Object> m = (Map<String, Object>) o;
                Registro r = new Registro();
                r.dim = (String) m.get("dim");
                List<Object> p = (List<Object>) m.get("placa"), b = (List<Object>) m.get("bau");
                r.px = num(p.get(0)); r.py = num(p.get(1)); r.pz = num(p.get(2));
                r.bx = num(b.get(0)); r.by = num(b.get(1)); r.bz = num(b.get(2));
                r.dono = UUID.fromString((String) m.get("dono"));
                r.nomeDono = (String) m.get("nomeDono");
                Object tipo = m.get("tipo");
                if (tipo instanceof String) r.tipo = (String) tipo;   // arquivos antigos não têm: é loja
                if (r.privado()) {
                    Object am = m.get("amigos");
                    if (am instanceof List) for (Object a : (List<Object>) am) if (a instanceof String) r.amigos.add((String) a);
                } else {
                    r.vendeNbt = (String) m.get("vende");
                    r.recebeNbt = (String) m.get("recebe");
                    r.vendeQtd = num(m.get("vendeQtd"));
                    r.recebeQtd = num(m.get("recebeQtd"));
                }
                lojas.add(r);
            } catch (Exception ignorado) {
                // registro estragado: pula
            }
        }
    }

    void salvar(Path arquivo) throws Exception {
        StringBuilder sb = new StringBuilder("{\n  \"lojas\": [");
        for (int i = 0; i < lojas.size(); i++) {
            Registro r = lojas.get(i);
            sb.append(i == 0 ? "\n" : ",\n").append("    {")
              .append("\"dim\": ").append(str(r.dim))
              .append(", \"placa\": [").append(r.px).append(", ").append(r.py).append(", ").append(r.pz).append("]")
              .append(", \"bau\": [").append(r.bx).append(", ").append(r.by).append(", ").append(r.bz).append("]")
              .append(", \"dono\": ").append(str(r.dono.toString()))
              .append(", \"nomeDono\": ").append(str(r.nomeDono))
              .append(", \"tipo\": ").append(str(r.tipo));
            if (r.privado()) {
                sb.append(", \"amigos\": [");
                for (int k = 0; k < r.amigos.size(); k++) sb.append(k == 0 ? "" : ", ").append(str(r.amigos.get(k)));
                sb.append("]");
            } else {
                sb.append(", \"vende\": ").append(str(r.vendeNbt))
                  .append(", \"vendeQtd\": ").append(r.vendeQtd)
                  .append(", \"recebe\": ").append(str(r.recebeNbt))
                  .append(", \"recebeQtd\": ").append(r.recebeQtd);
            }
            sb.append("}");
        }
        sb.append("\n  ]\n}\n");
        Path tmp = arquivo.resolveSibling(arquivo.getFileName() + ".tmp");
        Files.writeString(tmp, sb.toString());
        Files.move(tmp, arquivo, StandardCopyOption.REPLACE_EXISTING);
    }

    private static int num(Object o) { return ((Number) o).intValue(); }

    static String str(String s) {
        if (s == null) return "null";
        StringBuilder b = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"': b.append("\\\""); break;
                case '\\': b.append("\\\\"); break;
                case '\n': b.append("\\n"); break;
                case '\r': b.append("\\r"); break;
                case '\t': b.append("\\t"); break;
                default:
                    if (c < 0x20) b.append(String.format("\\u%04x", (int) c));
                    else b.append(c);
            }
        }
        return b.append('"').toString();
    }

    /** Lê um número inteiro de uma linha da placa (aceita "16", "16x", "x16"). 0 = não tem número. */
    static int numeroDaLinha(String linha) {
        if (linha == null) return 0;
        String d = linha.replaceAll("[^0-9]", "");
        if (d.isEmpty() || d.length() > 5) return 0;
        return Integer.parseInt(d);
    }
}
