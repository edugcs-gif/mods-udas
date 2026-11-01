package com.cassino.mod.client;

import com.cassino.mod.screen.ChipExchangerScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

/**
 * A tela do trocador de fichas. Em cima, a troca de ida: os espaços do ouro e do diamante, quantas fichas eles viram e
 * TROCAR. Embaixo, a troca de volta: o espaço das fichas, quanto ouro e diamante elas viram e TROCAR. Cada parte tem
 * a conta no alto ("8 ouro + 1 diamante » 4 fichas"), desenhada com os itens.
 */
public class ChipExchangerScreen extends HandledScreen<ChipExchangerScreenHandler> {
    private static final ItemStack GOLD = new ItemStack(Items.GOLD_INGOT);
    private static final ItemStack DIAMOND = new ItemStack(Items.DIAMOND);
    private static final int BOX_OUTLINE = 0xFF555555;
    private static final int BOX_FILL = 0xFFB4B4B4;
    private static final int READY_TEXT = 0xFF1E7A1E;
    private static final int EMPTY_TEXT = 0xFF707070;

    // Posições a partir do canto da tela.
    private static final int BOX_X = 6;
    private static final int BOX_WIDTH = ChipExchangerScreenHandler.WIDTH - 12;
    private static final int BOX_HEIGHT = 40;
    private static final int BUY_BOX_Y = 18;
    private static final int SELL_BOX_Y = 62;
    private static final int PREVIEW_X = 50;
    private static final int BUTTON_X = 122;
    private static final int BUTTON_WIDTH = 44;
    /** Um item pequeno ao lado dos números: 16 pixels × 0,75. */
    private static final float ICON_SCALE = 0.75F;
    private static final int ICON_SIZE = 12;

    private ColorButton buyButton;
    private ColorButton sellButton;

    public ChipExchangerScreen(ChipExchangerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        backgroundWidth = ChipExchangerScreenHandler.WIDTH;
        backgroundHeight = ChipExchangerScreenHandler.HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        buyButton = addDrawableChild(new ColorButton(x + BUTTON_X, y + ChipExchangerScreenHandler.BUY_Y - 1,
                BUTTON_WIDTH, 20, Text.translatable("gui.cassino.exchanger.trade"), ColorButton.GREEN, 1.0F,
                () -> click(ChipExchangerScreenHandler.BUTTON_MAKE_CHIPS)));
        buyButton.setTooltip(Tooltip.of(Text.translatable("gui.cassino.exchanger.tip.buy")));
        sellButton = addDrawableChild(new ColorButton(x + BUTTON_X, y + ChipExchangerScreenHandler.SELL_Y - 1,
                BUTTON_WIDTH, 20, Text.translatable("gui.cassino.exchanger.trade"), ColorButton.BLUE, 1.0F,
                () -> click(ChipExchangerScreenHandler.BUTTON_CASH_OUT)));
        sellButton.setTooltip(Tooltip.of(Text.translatable("gui.cassino.exchanger.tip.sell")));
    }

    private void click(int button) {
        if (client != null && client.interactionManager != null) {
            client.interactionManager.clickButton(handler.syncId, button);
        }
    }

    // ---------------------------------------------------------------- desenho

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        buyButton.active = handler.buyTrades() > 0;
        sellButton.active = handler.sellTrades() > 0;
        renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        CassinoGui.drawPanel(context, x, y, backgroundWidth, backgroundHeight);
        drawBox(context, BUY_BOX_Y);
        drawBox(context, SELL_BOX_Y);
        CassinoGui.drawGhostSlot(context, x + ChipExchangerScreenHandler.GOLD_SLOT_X,
                y + ChipExchangerScreenHandler.BUY_Y, GOLD, !slotHasStack(ChipExchangerScreenHandler.GOLD));
        CassinoGui.drawGhostSlot(context, x + ChipExchangerScreenHandler.DIAMOND_SLOT_X,
                y + ChipExchangerScreenHandler.BUY_Y, DIAMOND, !slotHasStack(ChipExchangerScreenHandler.DIAMOND));
        CassinoGui.drawGhostSlot(context, x + ChipExchangerScreenHandler.CHIP_SLOT_X,
                y + ChipExchangerScreenHandler.SELL_Y, CassinoGui.CHIP,
                !slotHasStack(ChipExchangerScreenHandler.CHIPS));
        CassinoGui.drawInventorySlots(context, x + ChipExchangerScreenHandler.INVENTORY_X,
                y + ChipExchangerScreenHandler.INVENTORY_Y, y + ChipExchangerScreenHandler.HOTBAR_Y);
    }

    private boolean slotHasStack(int slot) {
        return handler.getSlot(slot).hasStack();
    }

    private void drawBox(DrawContext context, int boxY) {
        int left = x + BOX_X;
        int top = y + boxY;
        context.fill(left, top, left + BOX_WIDTH, top + BOX_HEIGHT, BOX_OUTLINE);
        context.fill(left + 1, top + 1, left + BOX_WIDTH - 1, top + BOX_HEIGHT - 1, BOX_FILL);
    }

    /** Os textos, a partir do canto da tela. */
    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        CassinoGui.drawTitle(context, textRenderer, title, backgroundWidth);
        int center = BOX_X + BOX_WIDTH / 2;
        int gold = ChipExchangerScreenHandler.GOLD_PER_TRADE;
        int diamonds = ChipExchangerScreenHandler.DIAMONDS_PER_TRADE;
        int chips = ChipExchangerScreenHandler.CHIPS_PER_TRADE;

        // Em cima: "8 ouro + 1 diamante » 4 fichas", e quantas fichas o que está nos espaços vira.
        int buyLineX = center - (countWidth(gold) + signWidth("+") + countWidth(diamonds) + signWidth("»")
                + countWidth(chips)) / 2;
        int lineX = drawCount(context, gold, GOLD, buyLineX, BUY_BOX_Y + 5, CassinoGui.TEXT);
        lineX = drawSign(context, "+", lineX, BUY_BOX_Y + 5);
        lineX = drawCount(context, diamonds, DIAMOND, lineX, BUY_BOX_Y + 5, CassinoGui.TEXT);
        lineX = drawSign(context, "»", lineX, BUY_BOX_Y + 5);
        drawCount(context, chips, CassinoGui.CHIP, lineX, BUY_BOX_Y + 5, CassinoGui.TEXT);

        int buy = handler.buyTrades();
        int previewY = ChipExchangerScreenHandler.BUY_Y + 5;
        int previewX = drawSign(context, "»", PREVIEW_X, previewY);
        drawCount(context, buy * chips, CassinoGui.CHIP, previewX, previewY, buy > 0 ? READY_TEXT : EMPTY_TEXT);

        // Embaixo: "4 fichas » 8 ouro + 1 diamante", e quanto ouro e diamante as fichas do espaço viram.
        int sellLineX = center - (countWidth(chips) + signWidth("»") + countWidth(gold) + signWidth("+")
                + countWidth(diamonds)) / 2;
        lineX = drawCount(context, chips, CassinoGui.CHIP, sellLineX, SELL_BOX_Y + 5, CassinoGui.TEXT);
        lineX = drawSign(context, "»", lineX, SELL_BOX_Y + 5);
        lineX = drawCount(context, gold, GOLD, lineX, SELL_BOX_Y + 5, CassinoGui.TEXT);
        lineX = drawSign(context, "+", lineX, SELL_BOX_Y + 5);
        drawCount(context, diamonds, DIAMOND, lineX, SELL_BOX_Y + 5, CassinoGui.TEXT);

        int sell = handler.sellTrades();
        int color = sell > 0 ? READY_TEXT : EMPTY_TEXT;
        previewY = ChipExchangerScreenHandler.SELL_Y + 5;
        previewX = drawSign(context, "»", PREVIEW_X, previewY);
        previewX = drawCount(context, sell * gold, GOLD, previewX, previewY, color);
        drawCount(context, sell * diamonds, DIAMOND, previewX, previewY, color);

        CassinoGui.drawInventoryLabel(context, textRenderer, ChipExchangerScreenHandler.INVENTORY_X,
                ChipExchangerScreenHandler.INVENTORY_Y);
    }

    /** Desenha "16" com o item pequeno do lado; devolve onde a próxima parte começa. */
    private int drawCount(DrawContext context, int count, ItemStack icon, int textX, int textY, int color) {
        Text text = Text.literal(Integer.toString(count));
        context.drawText(textRenderer, text, textX, textY, color, false);
        int iconX = textX + textRenderer.getWidth(text) + 1;
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate(iconX, textY - 3, 0.0F);
        matrices.scale(ICON_SCALE, ICON_SCALE, 1.0F);
        context.drawItem(icon, 0, 0);
        matrices.pop();
        return iconX + ICON_SIZE + 3;
    }

    private int countWidth(int count) {
        return textRenderer.getWidth(Integer.toString(count)) + 1 + ICON_SIZE + 3;
    }

    /** Desenha um sinal ("+" ou "»"); devolve onde a próxima parte começa. */
    private int drawSign(DrawContext context, String sign, int textX, int textY) {
        context.drawText(textRenderer, sign, textX, textY, CassinoGui.TEXT, false);
        return textX + signWidth(sign);
    }

    private int signWidth(String sign) {
        return textRenderer.getWidth(sign) + 4;
    }
}
