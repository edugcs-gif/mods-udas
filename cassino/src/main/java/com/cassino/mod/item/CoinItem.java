package com.cassino.mod.item;

import com.cassino.mod.Cassino;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/** A moeda: botão direito joga cara ou coroa e avisa no chat quem está até 16 blocos. */
public class CoinItem extends Item {
    private static final int COOLDOWN_TICKS = 10;

    public CoinItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!world.isClient) {
            boolean heads = world.random.nextBoolean();
            Cassino.broadcastNearby(world, user,
                    Text.translatable("message.cassino.coin", user.getDisplayName(),
                            Text.translatable(heads ? "message.cassino.coin.heads" : "message.cassino.coin.tails")),
                    16.0);
            world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                    SoundCategory.PLAYERS, 0.8F, 1.6F);
            user.getItemCooldownManager().set(this, COOLDOWN_TICKS);
        }
        return TypedActionResult.success(stack, world.isClient);
    }
}
