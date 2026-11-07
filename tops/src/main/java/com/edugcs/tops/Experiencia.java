package com.edugcs.tops;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.WorldSavePath;
import net.minecraft.server.MinecraftServer;

import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

/** Liga o Contador de XP ao servidor: lê o XP dos jogadores online e salva em world/tops_xp.json. */
final class Experiencia {
    static final Contador CONTADOR = new Contador();
    private static boolean carregado, desativado;
    private static int ticks;

    private static Path arquivo(MinecraftServer server) {
        return server.getSavePath(WorldSavePath.ROOT).resolve("tops_xp.json");
    }

    /** Fim de cada tick do servidor. */
    static void tick(MinecraftServer server) {
        if (desativado) return;
        try {
            if (!carregado) {
                CONTADOR.carregar(arquivo(server));
                carregado = true;
            }
            ticks++;
            if (ticks % 20 == 0) { // a cada segundo
                for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                    GameProfile gp = p.getGameProfile();
                    UUID id = gp.getId();
                    if (id != null) CONTADOR.registrar(id, gp.getName(), p.totalExperience);
                }
            }
            if (ticks % 1200 == 0 && CONTADOR.sujo) CONTADOR.salvar(arquivo(server)); // a cada minuto
        } catch (Throwable t) {
            // nunca derruba o servidor: se algo der errado, só para de contar XP
            desativado = true;
            System.err.println("[Tops] Contador de XP desativado: " + t);
        }
    }

    /** Ao desligar o servidor. */
    static void salvar(MinecraftServer server) {
        if (desativado || !carregado) return;
        try {
            CONTADOR.salvar(arquivo(server));
        } catch (Throwable t) {
            System.err.println("[Tops] Não consegui salvar o XP: " + t);
        }
    }

    static Map<String, Long> porNome() { return CONTADOR.porNome(); }
}
