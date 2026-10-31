package com.cassino.mod.block;

import com.cassino.mod.Cassino;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/**
 * Caça-níquel. Botão direito com uma ficha na mão: gasta 1 ficha e sorteia três rolos.
 *
 * <ul>
 *   <li>3 iguais: paga conforme o símbolo (veja {@link #PAYOUT});</li>
 *   <li>2 iguais: devolve a ficha;</li>
 *   <li>nenhum igual: perdeu.</li>
 * </ul>
 *
 * <p>Os símbolos raros pagam mais. Com os pesos e prêmios abaixo, o retorno médio é de uns 83% das fichas
 * jogadas, então a casa sempre ganha no longo prazo (não dá para "minerar" ouro no caça-níquel).
 */
public class SlotMachineBlock extends HorizontalFacingBlock {
    public static final DirectionProperty FACING = HorizontalFacingBlock.FACING;

    private static final String[] SYMBOLS = {"7", "★", "♦", "♥", "♣", "♠"};
    private static final Formatting[] COLORS = {
            Formatting.RED, Formatting.GOLD, Formatting.AQUA, Formatting.LIGHT_PURPLE, Formatting.GREEN,
            Formatting.DARK_GRAY
    };
    /** Quantas "bolinhas" cada símbolo tem no sorteio de cada rolo (o 7 é o mais raro). */
    private static final int[] WEIGHTS = {1, 2, 3, 4, 5, 5};
    /** Fichas pagas por 3 símbolos iguais, na mesma ordem de {@link #SYMBOLS}. */
    public static final int[] PAYOUT = {100, 30, 16, 10, 6, 6};
    /** A tabela do sorteio: o índice do símbolo repetido conforme o peso dele. */
    private static final int[] TABLE = buildTable();

    private static int[] buildTable() {
        int total = 0;
        for (int weight : WEIGHTS) {
            total += weight;
        }
        int[] table = new int[total];
        int next = 0;
        for (int symbol = 0; symbol < WEIGHTS.length; symbol++) {
            for (int i = 0; i < WEIGHTS[symbol]; i++) {
                table[next++] = symbol;
            }
        }
        return table;
    }

    public SlotMachineBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** A frente (com os rolos) fica virada para quem colocou o bloco. */
    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        return getDefaultState().with(FACING, context.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand,
                              BlockHitResult hit) {
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }

        ItemStack chips = findChip(player);
        if (chips == null) {
            player.sendMessage(Text.translatable("message.cassino.slot.no_chip").formatted(Formatting.RED), true);
            world.playSound(null, pos, SoundEvents.BLOCK_DISPENSER_FAIL, SoundCategory.BLOCKS, 0.7F, 1.0F);
            return ActionResult.CONSUME;
        }
        if (!player.getAbilities().creativeMode) {
            chips.decrement(1);
        }

        int[] reels = new int[3];
        for (int i = 0; i < reels.length; i++) {
            reels[i] = TABLE[world.random.nextInt(TABLE.length)];
        }

        boolean triple = reels[0] == reels[1] && reels[1] == reels[2];
        boolean pair = reels[0] == reels[1] || reels[1] == reels[2] || reels[0] == reels[2];
        int prize = triple ? PAYOUT[reels[0]] : pair ? 1 : 0;

        MutableText message = reelsText(reels).append(Text.literal("   "));
        if (triple) {
            boolean jackpot = reels[0] == 0;
            message.append(Text.translatable(jackpot ? "message.cassino.slot.jackpot" : "message.cassino.slot.win",
                    prize).formatted(jackpot ? Formatting.RED : Formatting.GREEN, Formatting.BOLD));
            if (jackpot) {
                Cassino.broadcastNearby(world, player,
                        Text.translatable("message.cassino.slot.jackpot_broadcast", player.getDisplayName(), prize)
                                .formatted(Formatting.GOLD), 32.0);
            }
        } else if (pair) {
            message.append(Text.translatable("message.cassino.slot.pair").formatted(Formatting.YELLOW));
        } else {
            message.append(Text.translatable("message.cassino.slot.lose").formatted(Formatting.GRAY));
        }
        player.sendMessage(message, true);

        if (prize > 0) {
            giveChips(player, prize);
        }
        if (triple && reels[0] == 0) {
            world.playSound(null, pos, SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.BLOCKS, 1.0F, 1.0F);
        } else if (prize > 1) {
            world.playSound(null, pos, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.BLOCKS, 1.0F, 1.2F);
        } else {
            world.playSound(null, pos, SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.BLOCKS, 0.8F, 1.0F);
        }
        return ActionResult.SUCCESS;
    }

    /** A ficha na mão principal ou, se não houver, na outra mão; {@code null} se o jogador não tem nenhuma. */
    private static ItemStack findChip(PlayerEntity player) {
        ItemStack main = player.getStackInHand(Hand.MAIN_HAND);
        if (main.isOf(Cassino.CHIP)) {
            return main;
        }
        ItemStack off = player.getStackInHand(Hand.OFF_HAND);
        return off.isOf(Cassino.CHIP) ? off : null;
    }

    private static MutableText reelsText(int[] reels) {
        MutableText text = Text.literal("[ ").formatted(Formatting.GRAY);
        for (int i = 0; i < reels.length; i++) {
            text.append(Text.literal(SYMBOLS[reels[i]]).formatted(COLORS[reels[i]], Formatting.BOLD));
            if (i < reels.length - 1) {
                text.append(Text.literal(" | ").formatted(Formatting.GRAY));
            }
        }
        return text.append(Text.literal(" ]").formatted(Formatting.GRAY));
    }

    /** Entrega as fichas ao jogador (em pilhas de até 64); o que não couber no inventário cai no chão. */
    private static void giveChips(PlayerEntity player, int amount) {
        int left = amount;
        while (left > 0) {
            int count = Math.min(left, Cassino.CHIP.getMaxCount());
            player.getInventory().offerOrDrop(new ItemStack(Cassino.CHIP, count));
            left -= count;
        }
    }
}
