package com.cassino.mod;

import com.cassino.mod.block.BlackjackTableBlock;
import com.cassino.mod.block.ChipExchangerBlock;
import com.cassino.mod.block.CoinFlipBlock;
import com.cassino.mod.block.DiceTableBlock;
import com.cassino.mod.block.HorseRaceBlock;
import com.cassino.mod.block.LotteryBlock;
import com.cassino.mod.block.MinesBlock;
import com.cassino.mod.block.PlinkoBlock;
import com.cassino.mod.block.RouletteBlock;
import com.cassino.mod.block.SlotMachineBlock;
import com.cassino.mod.item.CoinItem;
import com.cassino.mod.item.DiceItem;
import com.cassino.mod.machine.CassinoState;
import com.cassino.mod.machine.Lottery;
import com.cassino.mod.screen.BlackjackScreenHandler;
import com.cassino.mod.screen.ChipExchangerScreenHandler;
import com.cassino.mod.screen.CoinFlipScreenHandler;
import com.cassino.mod.screen.DiceScreenHandler;
import com.cassino.mod.screen.HorseRaceScreenHandler;
import com.cassino.mod.screen.LotteryScreenHandler;
import com.cassino.mod.screen.MinesScreenHandler;
import com.cassino.mod.screen.PlinkoScreenHandler;
import com.cassino.mod.screen.RouletteScreenHandler;
import com.cassino.mod.screen.SlotMachineScreenHandler;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

/**
 * Cassino: ficha (a "moeda" do cassino), trocador de fichas, caça-níquel, blackjack, roleta, corrida de cavalos, jogo de
 * dados, cara ou coroa, Plinko de Slime, loteria
 * de mobs, Campo Minado, dado e moeda.
 */
public class Cassino implements ModInitializer {
    public static final String MOD_ID = "cassino";

    /**
     * A ficha: o dinheiro do cassino. 8 barras de ouro + 1 diamante = 4 fichas, na mesa de trabalho ou no trocador (que
     * também faz a troca de volta).
     */
    public static final Item CHIP = new Item(new Item.Settings());
    public static final Item DIE = new DiceItem(new Item.Settings().maxCount(16));
    public static final Item COIN = new CoinItem(new Item.Settings().maxCount(16));

    public static final Block CHIP_EXCHANGER = new ChipExchangerBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK));
    public static final Item CHIP_EXCHANGER_ITEM = new BlockItem(CHIP_EXCHANGER, new Item.Settings());
    public static final Block SLOT_MACHINE = new SlotMachineBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK));
    public static final Item SLOT_MACHINE_ITEM = new BlockItem(SLOT_MACHINE, new Item.Settings());
    public static final Block BLACKJACK_TABLE =
            new BlackjackTableBlock(AbstractBlock.Settings.copy(Blocks.OAK_PLANKS).strength(2.0F));
    public static final Item BLACKJACK_TABLE_ITEM = new BlockItem(BLACKJACK_TABLE, new Item.Settings());
    public static final Block ROULETTE =
            new RouletteBlock(AbstractBlock.Settings.copy(Blocks.OAK_PLANKS).strength(2.0F));
    public static final Item ROULETTE_ITEM = new BlockItem(ROULETTE, new Item.Settings());
    public static final Block HORSE_RACE =
            new HorseRaceBlock(AbstractBlock.Settings.copy(Blocks.OAK_PLANKS).strength(2.0F));
    public static final Item HORSE_RACE_ITEM = new BlockItem(HORSE_RACE, new Item.Settings());
    public static final Block DICE_TABLE =
            new DiceTableBlock(AbstractBlock.Settings.copy(Blocks.OAK_PLANKS).strength(2.0F));
    public static final Item DICE_TABLE_ITEM = new BlockItem(DICE_TABLE, new Item.Settings());
    public static final Block COIN_FLIP =
            new CoinFlipBlock(AbstractBlock.Settings.copy(Blocks.OAK_PLANKS).strength(2.0F));
    public static final Item COIN_FLIP_ITEM = new BlockItem(COIN_FLIP, new Item.Settings());
    public static final Block PLINKO = new PlinkoBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK));
    public static final Item PLINKO_ITEM = new BlockItem(PLINKO, new Item.Settings());
    public static final Block LOTTERY =
            new LotteryBlock(AbstractBlock.Settings.copy(Blocks.OAK_PLANKS).strength(2.0F));
    public static final Item LOTTERY_ITEM = new BlockItem(LOTTERY, new Item.Settings());
    public static final Block MINES = new MinesBlock(AbstractBlock.Settings.copy(Blocks.STONE));
    public static final Item MINES_ITEM = new BlockItem(MINES, new Item.Settings());

    /** A tela do trocador de fichas (ouro e diamante viram fichas, e fichas viram ouro e diamante). */
    public static final ScreenHandlerType<ChipExchangerScreenHandler> CHIP_EXCHANGER_HANDLER = Registry.register(
            Registries.SCREEN_HANDLER, id("chip_exchanger"),
            new ScreenHandlerType<>(ChipExchangerScreenHandler::new, FeatureFlags.VANILLA_FEATURES));
    /** A tela do caça-níquel (rolos, aposta, créditos e o espaço das fichas). */
    public static final ScreenHandlerType<SlotMachineScreenHandler> SLOT_MACHINE_HANDLER = Registry.register(
            Registries.SCREEN_HANDLER, id("slot_machine"),
            new ScreenHandlerType<>(SlotMachineScreenHandler::new, FeatureFlags.VANILLA_FEATURES));
    /** A tela da mesa de blackjack (as cartas, a aposta, os créditos e o espaço das fichas). */
    public static final ScreenHandlerType<BlackjackScreenHandler> BLACKJACK_HANDLER = Registry.register(
            Registries.SCREEN_HANDLER, id("blackjack"),
            new ScreenHandlerType<>(BlackjackScreenHandler::new, FeatureFlags.VANILLA_FEATURES));
    /** A tela da roleta (a roda, as cores para apostar, a aposta, os créditos e o espaço das fichas). */
    public static final ScreenHandlerType<RouletteScreenHandler> ROULETTE_HANDLER = Registry.register(
            Registries.SCREEN_HANDLER, id("roulette"),
            new ScreenHandlerType<>(RouletteScreenHandler::new, FeatureFlags.VANILLA_FEATURES));
    /** A tela da corrida de cavalos (os cavalos do dia, as raias, a aposta, os créditos e o espaço das fichas). */
    public static final ScreenHandlerType<HorseRaceScreenHandler> HORSE_RACE_HANDLER = Registry.register(
            Registries.SCREEN_HANDLER, id("horse_race"),
            new ScreenHandlerType<>(HorseRaceScreenHandler::new, FeatureFlags.VANILLA_FEATURES));

    /** A tela do jogo de dados (os dados, as apostas, a aposta, os créditos e o espaço das fichas). */
    public static final ScreenHandlerType<DiceScreenHandler> DICE_HANDLER = Registry.register(
            Registries.SCREEN_HANDLER, id("dice"),
            new ScreenHandlerType<>(DiceScreenHandler::new, FeatureFlags.VANILLA_FEATURES));

    /** A tela do cara ou coroa (a moeda, os dois lados, a aposta, os créditos e o espaço das fichas). */
    public static final ScreenHandlerType<CoinFlipScreenHandler> COIN_FLIP_HANDLER = Registry.register(
            Registries.SCREEN_HANDLER, id("coin_flip"),
            new ScreenHandlerType<>(CoinFlipScreenHandler::new, FeatureFlags.VANILLA_FEATURES));

    /** A tela do Plinko de Slime (o tabuleiro de pinos, a aposta, os créditos e o espaço das fichas). */
    public static final ScreenHandlerType<PlinkoScreenHandler> PLINKO_HANDLER = Registry.register(
            Registries.SCREEN_HANDLER, id("plinko"),
            new ScreenHandlerType<>(PlinkoScreenHandler::new, FeatureFlags.VANILLA_FEATURES));

    /** A tela da loteria de mobs (a tabela dos mobs, o prêmio, a aposta do dia e o espaço das fichas). */
    public static final ScreenHandlerType<LotteryScreenHandler> LOTTERY_HANDLER = Registry.register(
            Registries.SCREEN_HANDLER, id("lottery"),
            new ScreenHandlerType<>(LotteryScreenHandler::new, FeatureFlags.VANILLA_FEATURES));

    /** A tela do Campo Minado (a grade, as minas, o multiplicador, a aposta e o espaço das fichas). */
    public static final ScreenHandlerType<MinesScreenHandler> MINES_HANDLER = Registry.register(
            Registries.SCREEN_HANDLER, id("mines"),
            new ScreenHandlerType<>(MinesScreenHandler::new, FeatureFlags.VANILLA_FEATURES));

    /** Tarefas para rodar daqui a alguns ticks (o aviso do jackpot espera os rolos pararem na tela). */
    private static final List<Later> LATER = new ArrayList<>();

    private record Later(int tick, Runnable task) {
    }

    public static final ItemGroup GROUP = FabricItemGroup.builder()
            .icon(() -> new ItemStack(CHIP))
            .displayName(Text.translatable("itemGroup.cassino"))
            .entries((context, entries) -> {
                entries.add(CHIP);
                entries.add(CHIP_EXCHANGER_ITEM);
                entries.add(SLOT_MACHINE_ITEM);
                entries.add(BLACKJACK_TABLE_ITEM);
                entries.add(ROULETTE_ITEM);
                entries.add(HORSE_RACE_ITEM);
                entries.add(DICE_TABLE_ITEM);
                entries.add(COIN_FLIP_ITEM);
                entries.add(PLINKO_ITEM);
                entries.add(LOTTERY_ITEM);
                entries.add(MINES_ITEM);
                entries.add(DIE);
                entries.add(COIN);
            })
            .build();

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        Registry.register(Registries.ITEM, id("chip"), CHIP);
        Registry.register(Registries.ITEM, id("die"), DIE);
        Registry.register(Registries.ITEM, id("coin"), COIN);
        Registry.register(Registries.BLOCK, id("chip_exchanger"), CHIP_EXCHANGER);
        Registry.register(Registries.ITEM, id("chip_exchanger"), CHIP_EXCHANGER_ITEM);
        Registry.register(Registries.BLOCK, id("slot_machine"), SLOT_MACHINE);
        Registry.register(Registries.ITEM, id("slot_machine"), SLOT_MACHINE_ITEM);
        Registry.register(Registries.BLOCK, id("blackjack_table"), BLACKJACK_TABLE);
        Registry.register(Registries.ITEM, id("blackjack_table"), BLACKJACK_TABLE_ITEM);
        Registry.register(Registries.BLOCK, id("roulette"), ROULETTE);
        Registry.register(Registries.ITEM, id("roulette"), ROULETTE_ITEM);
        Registry.register(Registries.BLOCK, id("horse_race"), HORSE_RACE);
        Registry.register(Registries.ITEM, id("horse_race"), HORSE_RACE_ITEM);
        Registry.register(Registries.BLOCK, id("dice_table"), DICE_TABLE);
        Registry.register(Registries.ITEM, id("dice_table"), DICE_TABLE_ITEM);
        Registry.register(Registries.BLOCK, id("coin_flip"), COIN_FLIP);
        Registry.register(Registries.ITEM, id("coin_flip"), COIN_FLIP_ITEM);
        Registry.register(Registries.BLOCK, id("plinko"), PLINKO);
        Registry.register(Registries.ITEM, id("plinko"), PLINKO_ITEM);
        Registry.register(Registries.BLOCK, id("lottery"), LOTTERY);
        Registry.register(Registries.ITEM, id("lottery"), LOTTERY_ITEM);
        Registry.register(Registries.BLOCK, id("mines"), MINES);
        Registry.register(Registries.ITEM, id("mines"), MINES_ITEM);
        Registry.register(Registries.ITEM_GROUP, id("main"), GROUP);

        // Quem ganhou na loteria sem estar no jogo fica sabendo quando entrar.
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            Long prize = CassinoState.get(server).takeLotteryNotice(handler.player.getUuid());
            if (prize != null) {
                Lottery.announceWin(handler.player, prize);
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % 20 == 0) {
                Lottery.tick(server); // o sorteio da loteria sai quando o dia do Minecraft acaba
            }
            if (LATER.isEmpty()) {
                return;
            }
            int now = server.getTicks();
            List<Runnable> due = new ArrayList<>();
            for (Iterator<Later> it = LATER.iterator(); it.hasNext(); ) {
                Later later = it.next();
                if (now >= later.tick()) {
                    due.add(later.task());
                    it.remove();
                }
            }
            due.forEach(Runnable::run);
        });
    }

    /** Roda {@code task} daqui a {@code ticks} ticks, na thread do servidor. */
    public static void later(MinecraftServer server, int ticks, Runnable task) {
        LATER.add(new Later(server.getTicks() + ticks, task));
    }

    /** Manda a mensagem (no chat) a todos os jogadores até {@code range} blocos de {@code source}. */
    public static void broadcastNearby(World world, Entity source, Text message, double range) {
        double max = range * range;
        for (PlayerEntity player : world.getPlayers()) {
            if (player.squaredDistanceTo(source) <= max) {
                player.sendMessage(message, false);
            }
        }
    }
}
