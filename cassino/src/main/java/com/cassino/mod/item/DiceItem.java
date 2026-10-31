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

/** O dado: botão direito rola de 1 a 6 e avisa no chat quem está até 16 blocos. */
public class DiceItem extends Item {
    private static final int COOLDOWN_TICKS = 10;

    public DiceItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!world.isClient) {
            int roll = world.random.nextInt(6) + 1;
            Cassino.broadcastNearby(world, user,
                    Text.translatable("message.cassino.die", user.getDisplayName(), roll), 16.0);
            world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.BLOCK_WOOD_HIT,
                    SoundCategory.PLAYERS, 0.8F, 1.0F);
            user.getItemCooldownManager().set(this, COOLDOWN_TICKS);
        }
        return TypedActionResult.success(stack, world.isClient);
    }
}
