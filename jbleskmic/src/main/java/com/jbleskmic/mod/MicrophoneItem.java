package com.jbleskmic.mod;

import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * O microfone.
 *
 * <ul>
 *   <li>Botão direito numa jbleskão: conecta o microfone àquela caixa;</li>
 *   <li>Segurando o microfone conectado (em qualquer mão), a voz de quem fala no Simple Voice Chat sai pela
 *       caixa;</li>
 *   <li>Botão direito no ar: mostra a conexão; agachado, desconecta.</li>
 * </ul>
 */
public class MicrophoneItem extends Item {
    private static final String KEY_DIM = "LinkDim";
    private static final String KEY_X = "LinkX";
    private static final String KEY_Y = "LinkY";
    private static final String KEY_Z = "LinkZ";

    public MicrophoneItem(Settings settings) {
        super(settings);
    }

    /** A caixa a que o microfone está conectado, ou {@code null}. */
    public static MicLinks.Link readLink(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt == null || !nbt.contains(KEY_DIM) || !nbt.contains(KEY_X) || !nbt.contains(KEY_Y)
                || !nbt.contains(KEY_Z)) {
            return null;
        }
        Identifier dim = Identifier.tryParse(nbt.getString(KEY_DIM));
        if (dim == null) {
            return null;
        }
        return new MicLinks.Link(RegistryKey.of(RegistryKeys.WORLD, dim),
                new BlockPos(nbt.getInt(KEY_X), nbt.getInt(KEY_Y), nbt.getInt(KEY_Z)));
    }

    private static void writeLink(ItemStack stack, World world, BlockPos pos) {
        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.putString(KEY_DIM, world.getRegistryKey().getValue().toString());
        nbt.putInt(KEY_X, pos.getX());
        nbt.putInt(KEY_Y, pos.getY());
        nbt.putInt(KEY_Z, pos.getZ());
    }

    private static void clearLink(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt == null) {
            return;
        }
        nbt.remove(KEY_DIM);
        nbt.remove(KEY_X);
        nbt.remove(KEY_Y);
        nbt.remove(KEY_Z);
        if (nbt.isEmpty()) {
            stack.setNbt(null);
        }
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        PlayerEntity player = context.getPlayer();
        Block speaker = MicLinks.speakerBlock();
        if (player == null || speaker == null || !world.getBlockState(pos).isOf(speaker)) {
            return ActionResult.PASS;
        }
        if (!world.isClient) {
            writeLink(context.getStack(), world, pos);
            player.sendMessage(Text.translatable("message.jbleskmic.linked", pos.toShortString())
                    .formatted(Formatting.GREEN), true);
        }
        return ActionResult.success(world.isClient);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!world.isClient) {
            MicLinks.Link link = readLink(stack);
            if (user.isSneaking()) {
                clearLink(stack);
                user.sendMessage(Text.translatable("message.jbleskmic.unlinked").formatted(Formatting.YELLOW), true);
            } else if (link != null) {
                user.sendMessage(Text.translatable("message.jbleskmic.status", link.pos().toShortString())
                        .formatted(Formatting.GREEN), true);
            } else {
                user.sendMessage(Text.translatable("message.jbleskmic.not_linked").formatted(Formatting.GRAY), true);
            }
        }
        return TypedActionResult.success(stack, world.isClient);
    }

    /** O microfone brilha quando está conectado. */
    @Override
    public boolean hasGlint(ItemStack stack) {
        return readLink(stack) != null;
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        MicLinks.Link link = readLink(stack);
        if (link != null) {
            tooltip.add(Text.translatable("item.jbleskmic.microphone.linked", link.pos().toShortString())
                    .formatted(Formatting.GREEN));
        } else {
            tooltip.add(Text.translatable("item.jbleskmic.microphone.not_linked").formatted(Formatting.GRAY));
        }
        tooltip.add(Text.translatable("item.jbleskmic.microphone.hint").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("item.jbleskmic.microphone.hint2").formatted(Formatting.DARK_GRAY));
    }
}
