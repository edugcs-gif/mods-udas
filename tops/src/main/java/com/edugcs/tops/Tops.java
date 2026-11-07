package com.edugcs.tops;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.UserCache;
import net.minecraft.util.WorldSavePath;
import net.minecraft.server.MinecraftServer;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Tops do Servidor — comandos que funcionam só no servidor (ninguém precisa instalar nada):
 *   /top [1-4|tema]    líder de cada categoria, em 4 páginas por tema
 *   /top <categoria>   top 10 da categoria e a sua posição
 *   /status [jogador]  todas as estatísticas de uma pessoa
 *   /peixes            atalho para /top peixes
 * Os números vêm da pasta world/stats, então contam todo o histórico do servidor.
 */
public class Tops implements ModInitializer {

    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(Experiencia::tick);
        ServerLifecycleEvents.SERVER_STOPPING.register(Experiencia::salvar);

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            LiteralArgumentBuilder<ServerCommandSource> top = LiteralArgumentBuilder.<ServerCommandSource>literal("top")
                    .executes(ctx -> resumo(ctx.getSource(), Tema.COMBATE));
            for (Tema t : Tema.values()) {
                top.then(LiteralArgumentBuilder.<ServerCommandSource>literal(String.valueOf(t.pagina()))
                        .executes(ctx -> resumo(ctx.getSource(), t)));
                top.then(LiteralArgumentBuilder.<ServerCommandSource>literal(t.id)
                        .executes(ctx -> resumo(ctx.getSource(), t)));
            }
            for (Categoria c : Categoria.values()) {
                top.then(LiteralArgumentBuilder.<ServerCommandSource>literal(c.id)
                        .executes(ctx -> ranking(ctx.getSource(), c)));
            }
            dispatcher.register(top);

            dispatcher.register(LiteralArgumentBuilder.<ServerCommandSource>literal("peixes")
                    .executes(ctx -> ranking(ctx.getSource(), Categoria.PEIXES)));

            dispatcher.register(LiteralArgumentBuilder.<ServerCommandSource>literal("status")
                    .executes(ctx -> status(ctx.getSource(), null))
                    .then(RequiredArgumentBuilder.<ServerCommandSource, String>argument("jogador", StringArgumentType.word())
                            .suggests(Tops::sugerirNomes)
                            .executes(ctx -> status(ctx.getSource(), StringArgumentType.getString(ctx, "jogador")))));
        });
    }

    // ---------- comandos ----------

    private static int resumo(ServerCommandSource src, Tema tema) {
        Estatisticas e = carregar(src, true);
        if (e == null) return 0;
        enviar(src, Mensagens.resumo(e, tema));
        return 1;
    }

    private static int ranking(ServerCommandSource src, Categoria c) {
        Estatisticas e = carregar(src, true);
        if (e == null) return 0;
        enviar(src, Mensagens.ranking(e, c, meuNome(src)));
        return 1;
    }

    private static int status(ServerCommandSource src, String pedido) {
        Estatisticas e = carregar(src, true);
        if (e == null) return 0;
        String eu = meuNome(src);
        String nome = pedido != null ? pedido : eu;
        if (nome == null) {
            msg(src, "§cUse /status <jogador>");
            return 0;
        }
        String alvo = e.nomeExato(nome);
        boolean proprio = eu != null && nome.equalsIgnoreCase(eu);
        if (alvo == null) {
            if (!proprio) {
                msg(src, "§cNão achei estatísticas de " + nome + ".");
                return 0;
            }
            alvo = eu; // jogador novo, ainda sem arquivo: mostra tudo zerado
        }
        enviar(src, Mensagens.status(e, alvo, proprio));
        return 1;
    }

    private static CompletableFuture<Suggestions> sugerirNomes(CommandContext<ServerCommandSource> ctx, SuggestionsBuilder b) {
        try {
            Estatisticas e = ler(ctx.getSource().getServer(), false);
            String digitado = b.getRemainingLowerCase();
            for (String n : e.porJogador.keySet()) {
                if (n.toLowerCase().startsWith(digitado)) b.suggest(n);
            }
        } catch (Exception ignored) {
        }
        return b.buildFuture();
    }

    // ---------- leitura ----------

    private static Estatisticas carregar(ServerCommandSource src, boolean salvar) {
        try {
            return ler(src.getServer(), salvar);
        } catch (Exception ex) {
            msg(src, "§cNão consegui ler as estatísticas: " + ex.getMessage());
            return null;
        }
    }

    private static Estatisticas ler(MinecraftServer server, boolean salvar) throws Exception {
        List<ServerPlayerEntity> online = server.getPlayerManager().getPlayerList();
        if (salvar) {
            // grava as estatísticas de quem está online, para os números estarem atualizados
            for (ServerPlayerEntity p : online) p.getStatHandler().save();
        }
        Path pasta = server.getSavePath(WorldSavePath.ROOT).resolve("stats");
        UserCache cache = server.getUserCache();
        Estatisticas e = Estatisticas.ler(pasta, uuid -> {
            for (ServerPlayerEntity p : online) {
                GameProfile gp = p.getGameProfile();
                if (uuid.equals(gp.getId())) return gp.getName();
            }
            if (cache == null) return null;
            Optional<GameProfile> gp = cache.getByUuid(uuid);
            return gp.isPresent() ? gp.get().getName() : null;
        });
        e.definir(Categoria.XP, Experiencia.porNome());
        return e;
    }

    private static String meuNome(ServerCommandSource src) {
        ServerPlayerEntity p = src.getPlayer(); // null no console
        return p == null ? null : p.getGameProfile().getName();
    }

    // ---------- envio ----------

    private static void enviar(ServerCommandSource src, List<String> linhas) {
        for (String l : linhas) msg(src, l);
    }

    private static void msg(ServerCommandSource src, String texto) {
        src.sendFeedback(() -> Text.literal(texto), false);
    }
}
