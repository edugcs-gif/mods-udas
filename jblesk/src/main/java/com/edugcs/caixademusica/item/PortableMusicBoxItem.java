package com.edugcs.caixademusica.item;

import com.edugcs.caixademusica.CaixaDeMusica;
import com.edugcs.caixademusica.PlaybackState;
import java.util.List;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * A caixa de som portátil: toca de onde estiver guardada (mão, inventário, baú de burro, barco com
 * baú) e o som acompanha quem a carrega. Clique com o botão direito (no ar) abre a mesma tela da
 * caixa de música; guardada no inventário, a tecla de atalho abre a tela.
 */
public class PortableMusicBoxItem extends Item {
    private static final int TOOLTIP_URL_LENGTH = 48;

    public PortableMusicBoxItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient) {
            int slot = hand == Hand.OFF_HAND ? PlayerInventory.OFF_HAND_SLOT : user.getInventory().selectedSlot;
            CaixaDeMusica.bridge.openPortableScreen(slot);
        }
        return TypedActionResult.success(stack, world.isClient);
    }

    /** Brilha quando está tocando (pausada não brilha). */
    @Override
    public boolean hasGlint(ItemStack stack) {
        PlaybackState state = PlaybackState.ofStack(stack);
        return state != null && state.audible();
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        PlaybackState state = PlaybackState.ofStack(stack);
        if (state != null && state.playing() && !state.url().isBlank()) {
            String url = state.url();
            if (url.length() > TOOLTIP_URL_LENGTH) {
                url = url.substring(0, TOOLTIP_URL_LENGTH - 3) + "...";
            }
            String key = state.paused()
                    ? "item.caixademusica.portable_music_box.paused"
                    : "item.caixademusica.portable_music_box.playing";
            tooltip.add(Text.translatable(key, url).formatted(Formatting.GRAY));
        } else {
            tooltip.add(Text.translatable("item.caixademusica.portable_music_box.hint").formatted(Formatting.GRAY));
            tooltip.add(Text.translatable("item.caixademusica.portable_music_box.hint2").formatted(Formatting.GRAY));
        }
    }
}
