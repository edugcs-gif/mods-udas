package com.cassino.mod;

import com.cassino.mod.block.SlotMachineBlock;
import com.cassino.mod.item.CoinItem;
import com.cassino.mod.item.DiceItem;
import com.cassino.mod.screen.SlotMachineScreenHandler;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
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
 * Cassino: ficha (a "moeda" do cassino), caça-níquel, dado e moeda de cara ou coroa.
 */
public class Cassino implements ModInitializer {
    public static final String MOD_ID = "cassino";

    /** A ficha: vale por jogada no caça-níquel. 1 barra de ouro = 8 fichas (e 8 fichas = 1 barra de ouro). */
    public static final Item CHIP = new Item(new Item.Settings());
    public static final Item DIE = new DiceItem(new Item.Settings().maxCount(16));
    public static final Item COIN = new CoinItem(new Item.Settings().maxCount(16));

    public static final Block SLOT_MACHINE = new SlotMachineBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK));
    public static final Item SLOT_MACHINE_ITEM = new BlockItem(SLOT_MACHINE, new Item.Settings());

    /** A tela do caça-níquel (rolos, aposta, créditos e o espaço das fichas). */
    public static final ScreenHandlerType<SlotMachineScreenHandler> SLOT_MACHINE_HANDLER = Registry.register(
            Registries.SCREEN_HANDLER, id("slot_machine"),
            new ScreenHandlerType<>(SlotMachineScreenHandler::new, FeatureFlags.VANILLA_FEATURES));

    /** Tarefas para rodar daqui a alguns ticks (o aviso do jackpot espera os rolos pararem na tela). */
    private static final List<Later> LATER = new ArrayList<>();

    private record Later(int tick, Runnable task) {
    }

    public static final ItemGroup GROUP = FabricItemGroup.builder()
            .icon(() -> new ItemStack(CHIP))
            .displayName(Text.translatable("itemGroup.cassino"))
            .entries((context, entries) -> {
                entries.add(CHIP);
                entries.add(SLOT_MACHINE_ITEM);
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
        Registry.register(Registries.BLOCK, id("slot_machine"), SLOT_MACHINE);
        Registry.register(Registries.ITEM, id("slot_machine"), SLOT_MACHINE_ITEM);
        Registry.register(Registries.ITEM_GROUP, id("main"), GROUP);

        ServerTickEvents.END_SERVER_TICK.register(server -> {
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
