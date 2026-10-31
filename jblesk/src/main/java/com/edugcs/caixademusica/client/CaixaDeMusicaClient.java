package com.edugcs.caixademusica.client;

import com.edugcs.caixademusica.CaixaDeMusica;
import com.edugcs.caixademusica.PlaybackState;
import com.edugcs.caixademusica.PortableTracker;
import com.edugcs.caixademusica.block.MusicBoxBlockEntity;
import com.edugcs.caixademusica.network.ClockSyncPayload;
import com.edugcs.caixademusica.network.PortableSourcesPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

public class CaixaDeMusicaClient implements ClientModInitializer {
    /** Até que distância (blocos) a tecla procura um burro ou barco com baú. */
    private static final double SPEAKER_REACH = 4.0;

    private static KeyBinding openKey;

    @Override
    public void onInitializeClient() {
        CaixaDeMusica.bridge = new CaixaDeMusica.ClientBridge() {
            @Override
            public void openScreen(BlockPos pos) {
                MinecraftClient.getInstance().setScreen(MusicBoxScreen.forBlock(pos));
            }

            @Override
            public void openPortableScreen(int slot) {
                MinecraftClient.getInstance().setScreen(MusicBoxScreen.forItem(slot));
            }

            @Override
            public void openEntityScreen(int entityId) {
                MinecraftClient.getInstance().setScreen(MusicBoxScreen.forEntity(entityId));
            }

            @Override
            public void clientTick(MusicBoxBlockEntity box) {
                MusicBoxClientManager.report(box);
            }

            @Override
            public void removed(BlockPos pos) {
                MusicBoxClientManager.removed(pos);
            }
        };

        ClientPlayNetworking.registerGlobalReceiver(ClockSyncPayload.TYPE,
                (payload, player, sender) -> MusicBoxClientManager.setServerClock(payload.serverMillis()));
        ClientPlayNetworking.registerGlobalReceiver(PortableSourcesPayload.TYPE,
                (payload, player, sender) -> MusicBoxClientManager.setPortableSources(payload.entries()));

        // Tecla para abrir a tela da caixa do burro/barco por perto, ou da caixa portátil do inventário.
        openKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.caixademusica.open", GLFW.GLFW_KEY_B, "key.categories.misc"));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.wasPressed()) {
                openNearbyOrCarried(client);
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(MusicBoxClientManager::tick);
    }

    /**
     * Abre a tela de uma caixa: primeiro a do baú do burro, mula, lhama ou barco em que o jogador está
     * montado ou que está por perto; se não houver, a caixa portátil do inventário (a que está tocando,
     * ou a primeira que houver).
     */
    private static void openNearbyOrCarried(MinecraftClient client) {
        if (client.player == null || client.world == null || client.currentScreen != null) {
            return;
        }

        Entity speaker = nearestSpeaker(client);
        if (speaker != null) {
            client.setScreen(MusicBoxScreen.forEntity(speaker.getId()));
            return;
        }

        PlayerInventory inventory = client.player.getInventory();
        int found = -1;
        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack stack = inventory.getStack(slot);
            if (!stack.isOf(CaixaDeMusica.PORTABLE_ITEM)) {
                continue;
            }
            PlaybackState state = PlaybackState.ofStack(stack);
            if (state != null && state.audible()) {
                found = slot;
                break;
            }
            if (found < 0) {
                found = slot;
            }
        }
        if (found < 0) {
            client.player.sendMessage(Text.translatable("gui.caixademusica.no_target"), true);
            return;
        }
        client.setScreen(MusicBoxScreen.forItem(found));
    }

    /** O burro/mula/lhama/barco com baú em que o jogador está montado, ou o mais perto dele (até {@link #SPEAKER_REACH}). */
    private static Entity nearestSpeaker(MinecraftClient client) {
        Entity vehicle = client.player.getVehicle();
        if (vehicle != null && PortableTracker.isSpeakerCarrier(vehicle)) {
            return vehicle;
        }
        Entity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Entity entity : client.world.getOtherEntities(client.player,
                client.player.getBoundingBox().expand(SPEAKER_REACH), PortableTracker::isSpeakerCarrier)) {
            double distance = client.player.squaredDistanceTo(entity);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = entity;
            }
        }
        return best;
    }
}
