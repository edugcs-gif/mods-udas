package com.cassino.mod.machine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.EntityType;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * A loteria de mobs (o "jogo do bicho" do Minecraft).
 *
 * <ul>
 *   <li>Cada jogador escolhe um dos {@link #MOBS 25 mobs} e aposta {@value #BET} fichas (dos créditos do cassino). Uma
 *       aposta por jogador por dia do Minecraft.</li>
 *   <li>As apostas vão para o <b>prêmio da loteria</b>, que tem o saldo separado dos outros jogos (não mexe no saldo
 *       da casa).</li>
 *   <li>Quando o dia do Minecraft acaba (o relógio do mundo passa de um dia para o outro, também dormindo ou com
 *       {@code /time set}), sai um mob, todos com a mesma chance. Quem acertou divide o prêmio inteiro (em partes
 *       iguais) e o recebe nos créditos; se ninguém acertou, o prêmio acumula para o dia seguinte.</li>
 *   <li>O resultado é anunciado no chat para todo mundo, e quem ganhou vê "VOCÊ GANHOU!" na tela (ou quando entrar
 *       no jogo, se não estava).</li>
 * </ul>
 *
 * <p>A loteria só paga o que tem no prêmio, então ela nunca dá fichas a mais: não usa a sorte da casa dos outros
 * jogos.
 */
public final class Lottery {
    /** A aposta, sempre a mesma. */
    public static final int BET = 32;
    private static final int TICKS_PER_DAY = 24_000;

    /** Os mobs da loteria, na ordem da tabela. */
    public static final List<EntityType<?>> MOBS = List.of(
            EntityType.PIG, EntityType.COW, EntityType.SHEEP, EntityType.CHICKEN, EntityType.WOLF,
            EntityType.FOX, EntityType.BEE, EntityType.SQUID, EntityType.VILLAGER, EntityType.IRON_GOLEM,
            EntityType.ZOMBIE, EntityType.DROWNED, EntityType.SKELETON, EntityType.CREEPER, EntityType.SPIDER,
            EntityType.SLIME, EntityType.PILLAGER, EntityType.ENDERMAN, EntityType.BLAZE, EntityType.GHAST,
            EntityType.PIGLIN, EntityType.WITHER_SKELETON, EntityType.WARDEN, EntityType.WITHER,
            EntityType.ENDER_DRAGON);
    public static final int COUNT = 25;

    private Lottery() {
    }

    /** O nome do mob (traduzido no jogo de cada um). */
    public static MutableText name(int mob) {
        return MOBS.get(Math.floorMod(mob, COUNT)).getName().copy();
    }

    /** O dia do Minecraft agora. */
    public static long today(MinecraftServer server) {
        return HorseRace.day(server.getOverworld().getTimeOfDay());
    }

    /** Quantos segundos faltam para o dia acabar (e o sorteio sair). */
    public static int secondsLeft(MinecraftServer server) {
        long time = server.getOverworld().getTimeOfDay();
        return (int) ((TICKS_PER_DAY - Math.floorMod(time, TICKS_PER_DAY)) / 20);
    }

    /** Chamado a cada segundo: se o dia das apostas já acabou, sorteia. */
    public static void tick(MinecraftServer server) {
        CassinoState state = CassinoState.get(server);
        if (state.lotteryBets().isEmpty() || state.lotteryDay() < 0) {
            return;
        }
        if (today(server) != state.lotteryDay()) {
            draw(server, state);
        }
    }

    /**
     * Registra a aposta (os créditos já foram descontados por quem chamou): o dinheiro vai para o prêmio. Devolve
     * {@code false} se o jogador já apostou hoje.
     */
    public static boolean placeBet(MinecraftServer server, ServerPlayerEntity player, int mob) {
        tick(server); // se o dia virou e o sorteio ainda não saiu, ele sai antes
        CassinoState state = CassinoState.get(server);
        if (state.lotteryBet(player.getUuid()) >= 0) {
            return false;
        }
        if (state.lotteryBets().isEmpty()) {
            state.setLotteryDay(today(server));
        }
        state.placeLotteryBet(player.getUuid(), Math.floorMod(mob, COUNT), player.getGameProfile().getName());
        state.addToLotteryPot(BET);
        return true;
    }

    /** O sorteio do dia: sai um mob, quem acertou divide o prêmio, e todo mundo fica sabendo pelo chat. */
    private static void draw(MinecraftServer server, CassinoState state) {
        int mob = server.getOverworld().getRandom().nextInt(COUNT);
        List<UUID> winners = new ArrayList<>();
        List<String> names = new ArrayList<>();
        for (Map.Entry<UUID, CassinoState.LotteryBet> entry : state.lotteryBets().entrySet()) {
            if (entry.getValue().mob() == mob) {
                winners.add(entry.getKey());
                names.add(entry.getValue().name());
            }
        }

        Text prefix = Text.translatable("message.cassino.lottery.prefix").formatted(Formatting.GOLD);
        Text mobName = name(mob).formatted(Formatting.YELLOW);
        Text message;
        if (winners.isEmpty()) {
            message = Text.translatable("message.cassino.lottery.draw.none", prefix, mobName, state.lotteryPot());
        } else {
            long share = state.lotteryPot() / winners.size();
            long paid = 0;
            for (UUID winner : winners) {
                int balance = state.balance(winner);
                int prize = (int) Math.min(share, CassinoState.MAX_BALANCE - (long) balance);
                state.setBalance(winner, balance + prize);
                paid += prize;
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(winner);
                if (player != null) {
                    announceWin(player, prize);
                } else {
                    state.addLotteryNotice(winner, prize);
                }
            }
            state.addToLotteryPot(-paid);
            Text who = Text.literal(String.join(", ", names)).formatted(Formatting.GREEN);
            message = Text.translatable(winners.size() == 1
                    ? "message.cassino.lottery.draw.win" : "message.cassino.lottery.draw.win_many",
                    prefix, mobName, who, share);
        }
        server.getPlayerManager().broadcast(message, false);

        state.setLastLotteryMob(mob);
        state.clearLotteryBets();
        state.setLotteryDay(-1);
    }

    /** "VOCÊ GANHOU!" na tela do jogador, com o valor e um som de conquista. */
    public static void announceWin(ServerPlayerEntity player, long amount) {
        player.networkHandler.sendPacket(new TitleFadeS2CPacket(10, 80, 20));
        player.networkHandler.sendPacket(new TitleS2CPacket(
                Text.translatable("message.cassino.lottery.won.title").formatted(Formatting.GOLD, Formatting.BOLD)));
        player.networkHandler.sendPacket(new SubtitleS2CPacket(
                Text.translatable("message.cassino.lottery.won.subtitle", amount).formatted(Formatting.YELLOW)));
        player.sendMessage(Text.translatable("message.cassino.lottery.won.chat", amount).formatted(Formatting.GOLD),
                false);
        player.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.MASTER, 1.0F, 1.0F);
    }
}
