package com.cassino.mod.block;

import com.cassino.mod.screen.DiceScreenHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Mesa de dados. Botão direito abre o jogo de dados (veja {@link DiceScreenHandler}). */
public class DiceTableBlock extends Block {
    public DiceTableBlock(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand,
                              BlockHitResult hit) {
        if (!world.isClient) {
            player.openHandledScreen(state.createScreenHandlerFactory(world, pos));
        }
        return ActionResult.success(world.isClient);
    }

    @Override
    public NamedScreenHandlerFactory createScreenHandlerFactory(BlockState state, World world, BlockPos pos) {
        return new SimpleNamedScreenHandlerFactory((syncId, inventory, player) ->
                new DiceScreenHandler(syncId, inventory, ScreenHandlerContext.create(world, pos)),
                Text.translatable("gui.cassino.dice.title"));
    }
}
