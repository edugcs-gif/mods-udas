package com.edugcs.batraquizar;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * A Bênção do Príncipe: o jogador que volta de sapo (o efeito acabou, bebeu leite ou morreu sapo) ganha
 * a bênção por 10 minutos, e enquanto ela durar nada o transforma em sapo de novo.
 *
 * <p>A bênção só sai com leite ou quando o tempo acaba: ela continua depois de morrer (passa para o
 * jogador que renasce) e o totem da imortalidade, que limpa os efeitos, não a tira.
 */
public final class BencaoDoPrincipe {
    /** 10 minutos, em ticks. */
    public static final int DURATION = 10 * 60 * 20;

    /**
     * Jogadores que acabaram de voltar de sapo. A bênção é dada no fim do tick, porque o jogo avisa que o efeito
     * saiu no meio da lista de efeitos (e mexer nela ali dá erro).
     */
    private static final List<ServerPlayerEntity> PENDING = new ArrayList<>();

    private BencaoDoPrincipe() {
    }

    static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (PENDING.isEmpty()) {
                return;
            }
            for (ServerPlayerEntity player : PENDING) {
                if (player.isAlive() && !player.isRemoved()) {
                    bless(player, new StatusEffectInstance(Batraquizar.BENCAO_DO_PRINCIPE, DURATION));
                }
            }
            PENDING.clear();
        });

        // Morrer não tira a bênção, e quem morre sapo também volta abençoado.
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (alive) {
                return; // voltou do End vivo: o jogo já copia os efeitos
            }
            StatusEffectInstance blessing = oldPlayer.getStatusEffect(Batraquizar.BENCAO_DO_PRINCIPE);
            if (blessing != null) {
                bless(newPlayer, new StatusEffectInstance(blessing));
            } else if (oldPlayer.hasStatusEffect(Batraquizar.BATRAQUIZADO)) {
                bless(newPlayer, new StatusEffectInstance(Batraquizar.BENCAO_DO_PRINCIPE, DURATION));
            }
        });
    }

    private static void bless(LivingEntity entity, StatusEffectInstance blessing) {
        entity.addStatusEffect(blessing);
    }

    /** O efeito Batraquizado saiu de alguém (acabou o tempo, leite, totem...): se for um jogador, ganha a bênção. */
    public static void onEffectRemoved(LivingEntity entity, StatusEffectInstance effect) {
        if (entity instanceof ServerPlayerEntity player && effect.getEffectType().value() == Batraquizar.BATRAQUIZADO.value()
                && player.isAlive() && !PENDING.contains(player)) {
            PENDING.add(player);
        }
    }

    /** Quem tem a bênção não pode receber o efeito Batraquizado (a poção "escorrega"). */
    public static boolean blocks(LivingEntity entity, StatusEffectInstance effect) {
        if (effect.getEffectType().value() != Batraquizar.BATRAQUIZADO.value()
                || !entity.hasStatusEffect(Batraquizar.BENCAO_DO_PRINCIPE)) {
            return false;
        }
        if (entity instanceof ServerPlayerEntity player) {
            player.sendMessage(Text.translatable("message.batraquizar.protected"), true);
        }
        return true;
    }
}
