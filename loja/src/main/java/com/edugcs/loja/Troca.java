package com.edugcs.loja;

import java.util.List;

/**
 * Regras da troca, sem depender do Minecraft (S = tipo da pilha de itens).
 * Tudo é feito em cópias das listas; só depois que tudo deu certo o resultado é gravado de volta.
 */
final class Troca {
    interface Ops<S> {
        boolean vazio(S s);
        int qtd(S s);
        int max(S s);
        boolean igual(S modelo, S s);
        /** Nova pilha igual ao modelo com a quantidade dada (ou vazia se q <= 0). */
        S comQtd(S modelo, int q);
    }

    enum Resultado { OK, SEM_ESTOQUE, SEM_PAGAMENTO, BAU_CHEIO }

    static <S> int contar(List<S> slots, S modelo, int ate, Ops<S> o) {
        int t = 0;
        for (int i = 0; i < Math.min(ate, slots.size()); i++) {
            S s = slots.get(i);
            if (!o.vazio(s) && o.igual(modelo, s)) t += o.qtd(s);
        }
        return t;
    }

    static <S> int contar(List<S> slots, S modelo, Ops<S> o) { return contar(slots, modelo, slots.size(), o); }

    /** Tira 'qtd' itens iguais ao modelo. Retorna false (sem mexer) se não tiver o suficiente. */
    static <S> boolean remover(List<S> slots, S modelo, int qtd, Ops<S> o) {
        if (contar(slots, modelo, o) < qtd) return false;
        int resta = qtd;
        for (int i = 0; i < slots.size() && resta > 0; i++) {
            S s = slots.get(i);
            if (o.vazio(s) || !o.igual(modelo, s)) continue;
            int tira = Math.min(resta, o.qtd(s));
            slots.set(i, o.comQtd(s, o.qtd(s) - tira));
            resta -= tira;
        }
        return true;
    }

    /** Coloca 'qtd' itens iguais ao modelo nos primeiros 'ate' slots. Retorna quantos sobraram. */
    static <S> int adicionar(List<S> slots, S modelo, int qtd, int ate, Ops<S> o) {
        int resta = qtd;
        int lim = Math.min(ate, slots.size());
        for (int i = 0; i < lim && resta > 0; i++) {         // completa pilhas que já existem
            S s = slots.get(i);
            if (o.vazio(s) || !o.igual(modelo, s)) continue;
            int cabe = o.max(s) - o.qtd(s);
            if (cabe <= 0) continue;
            int poe = Math.min(cabe, resta);
            slots.set(i, o.comQtd(s, o.qtd(s) + poe));
            resta -= poe;
        }
        int maxModelo = Math.max(1, o.max(modelo));
        for (int i = 0; i < lim && resta > 0; i++) {         // usa slots vazios
            if (!o.vazio(slots.get(i))) continue;
            int poe = Math.min(maxModelo, resta);
            slots.set(i, o.comQtd(modelo, poe));
            resta -= poe;
        }
        return resta;
    }

    static <S> int adicionar(List<S> slots, S modelo, int qtd, Ops<S> o) { return adicionar(slots, modelo, qtd, slots.size(), o); }

    /**
     * Uma troca: o comprador entrega 'recebeQtd' de 'recebe' e leva 'vendeQtd' de 'vende'.
     * bau e jogador são cópias; jogadorAte = slots do jogador que podem receber itens.
     * Em caso de OK, 'sobra[0]' recebe quantos itens vendidos não couberam no inventário (vão pro chão).
     */
    static <S> Resultado trocar(List<S> bau, List<S> jogador, int jogadorAte,
                                S vende, int vendeQtd, S recebe, int recebeQtd, Ops<S> o, int[] sobra) {
        if (contar(bau, vende, o) < vendeQtd) return Resultado.SEM_ESTOQUE;
        if (contar(jogador, recebe, o) < recebeQtd) return Resultado.SEM_PAGAMENTO;
        remover(bau, vende, vendeQtd, o);
        if (adicionar(bau, recebe, recebeQtd, o) > 0) return Resultado.BAU_CHEIO;
        remover(jogador, recebe, recebeQtd, o);
        sobra[0] = adicionar(jogador, vende, vendeQtd, jogadorAte, o);
        return Resultado.OK;
    }
}
