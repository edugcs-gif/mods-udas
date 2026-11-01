package com.cassino.mod.screen;

import com.cassino.mod.Cassino;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.slot.Slot;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

/**
 * O trocador de fichas: troca ouro e diamante por fichas, e fichas de volta por ouro e diamante, sempre na mesma
 * conta da receita: {@value #GOLD_PER_TRADE} barras de ouro + {@value #DIAMONDS_PER_TRADE} diamante =
 * {@value #CHIPS_PER_TRADE} fichas.
 *
 * <p>Em cima ficam os espaços do ouro e do diamante, com TROCAR (vira fichas); embaixo, o espaço das fichas, com
 * TROCAR (vira ouro e diamante). Cada TROCAR faz quantas trocas couberem no que está no espaço; o que sobra (menos de
 * 8 barras de ouro, ou menos de 4 fichas) fica lá. O que sai da troca vai para o inventário (ou cai no chão, se não
 * couber), e o que ficou nos espaços volta para o jogador quando a tela fecha.
 */
public class ChipExchangerScreenHandler extends ScreenHandler {
    public static final int BUTTON_MAKE_CHIPS = 0;
    public static final int BUTTON_CASH_OUT = 1;

    public static final int GOLD_PER_TRADE = 8;
    public static final int DIAMONDS_PER_TRADE = 1;
    public static final int CHIPS_PER_TRADE = 4;

    public static final int GOLD = 0;
    public static final int DIAMOND = 1;
    public static final int CHIPS = 2;

    // ---------------------------------------------------------------- desenho (em pixels, a partir do canto da tela)

    public static final int WIDTH = 176;
    public static final int HEIGHT = 202;
    /** O alto dos espaços da troca de ida (ouro e diamante) e da de volta (fichas). */
    public static final int BUY_Y = 34;
    public static final int SELL_Y = 78;
    public static final int GOLD_SLOT_X = 10;
    public static final int DIAMOND_SLOT_X = 28;
    public static final int CHIP_SLOT_X = 10;
    public static final int INVENTORY_X = 7;
    public static final int INVENTORY_Y = 120;
    public static final int HOTBAR_Y = 178;

    private final ScreenHandlerContext context;
    private final Inventory input = new SimpleInventory(3);

    /** Do lado do cliente (a tela). */
    public ChipExchangerScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, ScreenHandlerContext.EMPTY);
    }

    public ChipExchangerScreenHandler(int syncId, PlayerInventory playerInventory, ScreenHandlerContext context) {
        super(Cassino.CHIP_EXCHANGER_HANDLER, syncId);
        this.context = context;

        addSlot(new OnlySlot(input, GOLD, GOLD_SLOT_X + 1, BUY_Y + 1, Items.GOLD_INGOT));
        addSlot(new OnlySlot(input, DIAMOND, DIAMOND_SLOT_X + 1, BUY_Y + 1, Items.DIAMOND));
        addSlot(new OnlySlot(input, CHIPS, CHIP_SLOT_X + 1, SELL_Y + 1, Cassino.CHIP));
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, INVENTORY_X + 1 + column * 18,
                        INVENTORY_Y + 1 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, INVENTORY_X + 1 + column * 18, HOTBAR_Y + 1));
        }
    }

    /** Um espaço que só aceita um item. */
    private static final class OnlySlot extends Slot {
        private final Item item;

        OnlySlot(Inventory inventory, int index, int x, int y, Item item) {
            super(inventory, index, x, y);
            this.item = item;
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return stack.isOf(item);
        }
    }

    // ---------------------------------------------------------------- as contas (a tela usa para mostrar a prévia)

    /** Quantas trocas de ida cabem no ouro e no diamante dos espaços. */
    public int buyTrades() {
        return Math.min(count(GOLD) / GOLD_PER_TRADE, count(DIAMOND) / DIAMONDS_PER_TRADE);
    }

    /** Quantas trocas de volta cabem nas fichas do espaço. */
    public int sellTrades() {
        return count(CHIPS) / CHIPS_PER_TRADE;
    }

    private int count(int slot) {
        return input.getStack(slot).getCount();
    }

    // ---------------------------------------------------------------- botões (só no servidor)

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (id == BUTTON_MAKE_CHIPS) {
            int trades = buyTrades();
            if (trades == 0) {
                return false;
            }
            input.getStack(GOLD).decrement(trades * GOLD_PER_TRADE);
            input.getStack(DIAMOND).decrement(trades * DIAMONDS_PER_TRADE);
            give(player, Cassino.CHIP, trades * CHIPS_PER_TRADE);
        } else if (id == BUTTON_CASH_OUT) {
            int trades = sellTrades();
            if (trades == 0) {
                return false;
            }
            input.getStack(CHIPS).decrement(trades * CHIPS_PER_TRADE);
            give(player, Items.GOLD_INGOT, trades * GOLD_PER_TRADE);
            give(player, Items.DIAMOND, trades * DIAMONDS_PER_TRADE);
        } else {
            return false;
        }
        input.markDirty();
        context.run((world, pos) -> world.playSound(null, pos, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                SoundCategory.BLOCKS, 0.6F, 1.3F));
        return true;
    }

    /** Põe no inventário, em pilhas do tamanho certo; o que não couber cai no chão. */
    private static void give(PlayerEntity player, Item item, int count) {
        while (count > 0) {
            int amount = Math.min(count, item.getMaxCount());
            player.getInventory().offerOrDrop(new ItemStack(item, amount));
            count -= amount;
        }
    }

    // ---------------------------------------------------------------- tela

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        // O que ficou nos espaços volta para o jogador (como na mesa de trabalho).
        context.run((world, pos) -> dropInventory(player, input));
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return canUse(context, player, Cassino.CHIP_EXCHANGER);
    }

    /** Shift + clique: ouro, diamante e fichas vão cada um para o seu espaço; o que está nos espaços volta. */
    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasStack()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getStack();
        ItemStack original = stack.copy();
        if (index <= CHIPS) {
            if (!insertItem(stack, CHIPS + 1, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            int target = stack.isOf(Items.GOLD_INGOT) ? GOLD
                    : stack.isOf(Items.DIAMOND) ? DIAMOND
                    : stack.isOf(Cassino.CHIP) ? CHIPS : -1;
            if (target < 0 || !insertItem(stack, target, target + 1, false)) {
                return ItemStack.EMPTY;
            }
        }
        if (stack.isEmpty()) {
            slot.setStack(ItemStack.EMPTY);
        } else {
            slot.markDirty();
        }
        return original;
    }
}
