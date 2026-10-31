package com.edugcs.caixademusica;

import com.edugcs.caixademusica.block.MusicBoxBlock;
import com.edugcs.caixademusica.block.MusicBoxBlockEntity;
import com.edugcs.caixademusica.item.PortableMusicBoxItem;
import com.edugcs.caixademusica.network.ClockSyncPayload;
import com.edugcs.caixademusica.network.EntityActionPayload;
import com.edugcs.caixademusica.network.MusicBoxActionPayload;
import com.edugcs.caixademusica.network.PortableActionPayload;
import com.edugcs.caixademusica.network.PortableSourcesPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CaixaDeMusica implements ModInitializer {
    public static final String MOD_ID = "caixademusica";
    public static final Logger LOGGER = LoggerFactory.getLogger("Caixa de Musica");

    /** Tamanho máximo do link/texto aceito pelo servidor. */
    public static final int MAX_URL_LENGTH = 512;

    public static final Block MUSIC_BOX = new MusicBoxBlock(AbstractBlock.Settings.copy(Blocks.JUKEBOX));
    public static final Item MUSIC_BOX_ITEM = new BlockItem(MUSIC_BOX, new Item.Settings());
    public static final BlockEntityType<MusicBoxBlockEntity> MUSIC_BOX_BLOCK_ENTITY =
            FabricBlockEntityTypeBuilder.create(MusicBoxBlockEntity::new, MUSIC_BOX).build();

    /** A caixa portátil. O que ela está tocando fica guardado no NBT do item ({@link PlaybackState#ofStack}). */
    public static final Item PORTABLE_ITEM = new PortableMusicBoxItem(new Item.Settings().maxCount(1));

    /**
     * Ponte para o código que só existe no cliente (tela, áudio). No servidor dedicado
     * fica a implementação vazia, então nada do lado do cliente é carregado lá.
     */
    public static ClientBridge bridge = new ClientBridge() {
    };

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        Registry.register(Registries.BLOCK, id("music_box"), MUSIC_BOX);
        Registry.register(Registries.ITEM, id("music_box"), MUSIC_BOX_ITEM);
        Registry.register(Registries.BLOCK_ENTITY_TYPE, id("music_box"), MUSIC_BOX_BLOCK_ENTITY);
        Registry.register(Registries.ITEM, id("portable_music_box"), PORTABLE_ITEM);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(entries -> {
            entries.add(MUSIC_BOX_ITEM);
            entries.add(PORTABLE_ITEM);
        });

        ServerPlayNetworking.registerGlobalReceiver(MusicBoxActionPayload.TYPE, CaixaDeMusica::handleAction);
        ServerPlayNetworking.registerGlobalReceiver(PortableActionPayload.TYPE, CaixaDeMusica::handlePortableAction);

        // A caixa portátil toca de onde estiver (inventário, baú de burro, barco com baú): o servidor
        // procura quais estão tocando e avisa os jogadores por perto.
        PortableTracker.init();
        SpeakerGroups.init();

        // O baú do burro/mula/lhama e do barco é uma caixa de som: ações da tela dela.
        ServerPlayNetworking.registerGlobalReceiver(EntityActionPayload.TYPE, CaixaDeMusica::handleEntityAction);

        // Botão direito no animal/barco com a Caixa de Música (bloco) na mão: abre a tela da caixa do baú.
        // O clique normal continua montando, e o agachado continua abrindo o baú.
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (player.isSpectator() || !player.getStackInHand(hand).isOf(MUSIC_BOX_ITEM)
                    || !PortableTracker.isSpeakerCarrier(entity)) {
                return ActionResult.PASS;
            }
            if (world.isClient) {
                bridge.openEntityScreen(entity.getId());
            }
            return ActionResult.SUCCESS;
        });

        // O relógio do servidor, para os clientes entrarem na música da caixa portátil no ponto certo.
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                sender.sendPacket(new ClockSyncPayload(System.currentTimeMillis())));
    }

    /** Aplica ao estado o que o jogador apertou na tela; devolve o mesmo estado se não for nada válido. */
    private static PlaybackState applyAction(PlaybackState state, int action, int index, int extra, String url) {
        long now = System.currentTimeMillis();
        return switch (action) {
            case MusicBoxActionPayload.PLAY -> {
                String link = url.trim();
                if (link.length() > MAX_URL_LENGTH) {
                    link = link.substring(0, MAX_URL_LENGTH);
                }
                yield link.isEmpty() ? state : state.play(link, now);
            }
            case MusicBoxActionPayload.STOP -> state.stop(now);
            case MusicBoxActionPayload.JUMP -> state.jump(index, now);
            case MusicBoxActionPayload.PAUSE -> state.pause(now);
            case MusicBoxActionPayload.RESUME -> state.resume(now);
            case MusicBoxActionPayload.SEEK -> state.seek(index, now);
            case MusicBoxActionPayload.RANGE -> state.range(index, extra);
            case MusicBoxActionPayload.GROUP -> SpeakerGroups.join(state, index);
            default -> state;
        };
    }

    /** Roda na thread do servidor quando alguém aperta um botão na tela da caixa. */
    private static void handleAction(MusicBoxActionPayload payload, ServerPlayerEntity player, PacketSender sender) {
        World world = player.getWorld();
        BlockPos pos = payload.pos();

        if (!world.isChunkLoaded(pos) || player.squaredDistanceTo(Vec3d.ofCenter(pos)) > 8.0 * 8.0) {
            return;
        }
        if (!(world.getBlockEntity(pos) instanceof MusicBoxBlockEntity box)) {
            return;
        }

        PlaybackState next = applyAction(box.getState(), payload.action(), payload.index(), payload.extra(), payload.url());
        if (payload.action() == MusicBoxActionPayload.PLAY && next != box.getState()) {
            LOGGER.info("{} colocou \"{}\" na caixa de musica em {}",
                    player.getName().getString(), next.url(), pos.toShortString());
        }
        box.setState(next);
        // Se a caixa está conectada, as outras do grupo passam a tocar o mesmo (o alcance não é copiado).
        if (payload.action() != MusicBoxActionPayload.RANGE) {
            SpeakerGroups.publish(next, player.getServer());
        }
    }

    /** Roda na thread do servidor quando alguém aperta um botão na tela da caixa portátil. */
    private static void handlePortableAction(PortableActionPayload payload, ServerPlayerEntity player,
                                             PacketSender sender) {
        PlayerInventory inventory = player.getInventory();
        int slot = payload.slot();
        if (slot < 0 || slot >= inventory.size()) {
            return;
        }
        ItemStack stack = inventory.getStack(slot);
        if (!stack.isOf(PORTABLE_ITEM)) {
            return;
        }
        PlaybackState current = PlaybackState.ofStack(stack);
        if (current == null) {
            current = PlaybackState.PORTABLE_DEFAULT;
        }
        PlaybackState next = applyAction(current, payload.action(), payload.index(), payload.extra(), payload.url());
        if (next.equals(current)) {
            return;
        }
        if (payload.action() == MusicBoxActionPayload.PLAY) {
            LOGGER.info("{} colocou \"{}\" na caixa de som portatil", player.getName().getString(), next.url());
        }
        next.writeTo(stack);
        if (payload.action() != MusicBoxActionPayload.RANGE) {
            SpeakerGroups.publish(next, player.getServer());
        }
        // Avisa quem está por perto agora, sem esperar a próxima varredura.
        PortableTracker.update(player.getServerWorld());
    }

    /** Roda na thread do servidor quando alguém aperta um botão na tela da caixa do baú de um animal ou barco. */
    private static void handleEntityAction(EntityActionPayload payload, ServerPlayerEntity player, PacketSender sender) {
        Entity entity = player.getServerWorld().getEntityById(payload.entityId());
        if (entity == null || !PortableTracker.isSpeakerCarrier(entity) || player.squaredDistanceTo(entity) > 12.0 * 12.0) {
            return;
        }
        PlaybackState current = ((BoxHolder) entity).caixademusica$getBox();
        if (current == null) {
            current = PlaybackState.ENTITY_DEFAULT;
        }
        PlaybackState next = applyAction(current, payload.action(), payload.index(), payload.extra(), payload.url());
        if (next.equals(current)) {
            return;
        }
        if (payload.action() == MusicBoxActionPayload.PLAY) {
            LOGGER.info("{} colocou \"{}\" na caixa de som de {}", player.getName().getString(), next.url(),
                    entity.getName().getString());
        }
        ((BoxHolder) entity).caixademusica$setBox(next);
        if (payload.action() != MusicBoxActionPayload.RANGE) {
            SpeakerGroups.publish(next, player.getServer());
        }
        // Avisa quem está por perto agora, sem esperar a próxima varredura.
        PortableTracker.update(player.getServerWorld());
    }

    public interface ClientBridge {
        default void openScreen(BlockPos pos) {
        }

        /** Abre a tela da caixa portátil que está no espaço {@code slot} do inventário. */
        default void openPortableScreen(int slot) {
        }

        /** Abre a tela da caixa embutida no baú do burro, mula, lhama ou barco {@code entityId}. */
        default void openEntityScreen(int entityId) {
        }

        default void clientTick(MusicBoxBlockEntity box) {
        }

        default void removed(BlockPos pos) {
        }
    }
}
