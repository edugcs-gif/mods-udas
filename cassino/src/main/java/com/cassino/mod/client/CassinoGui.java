package com.cassino.mod.client;

import com.cassino.mod.Cassino;
import java.util.Locale;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** O desenho comum às telas do cassino: o painel, o título grande, os espaços, os valores com ficha e o inventário. */
final class CassinoGui {
    static final int PANEL = 0xFFC6C6C6;
    static final int PANEL_LIGHT = 0xFFFFFFFF;
    static final int PANEL_DARK = 0xFF555555;
    static final int OUTLINE = 0xFF000000;
    static final int TEXT = 0xFF3F3F3F;
    static final int TITLE = 0xFF202020;
    static final int SLOT_DARK = 0xFF373737;
    static final int SLOT_FILL = 0xFF8B8B8B;

    static final ItemStack CHIP = new ItemStack(Cassino.CHIP);

    private CassinoGui() {
    }

    /** O painel cinza com borda, como as telas do jogo. */
    static void drawPanel(DrawContext context, int x, int y, int width, int height) {
        context.fill(x, y, x + width, y + height, OUTLINE);
        context.fill(x + 1, y + 1, x + width - 1, y + height - 1, PANEL);
        context.fill(x + 1, y + 1, x + width - 1, y + 3, PANEL_LIGHT);
        context.fill(x + 1, y + 1, x + 3, y + height - 1, PANEL_LIGHT);
        context.fill(x + 1, y + height - 3, x + width - 1, y + height - 1, PANEL_DARK);
        context.fill(x + width - 3, y + 1, x + width - 1, y + height - 1, PANEL_DARK);
    }

    /** O título em letras grandes, no meio do alto da tela (coordenadas a partir do canto da tela). */
    static void drawTitle(DrawContext context, TextRenderer textRenderer, Text title, int panelWidth) {
        Text bigTitle = Text.literal(title.getString().toUpperCase(Locale.ROOT)).formatted(Formatting.BOLD);
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate(panelWidth / 2.0F, 6.0F, 0.0F);
        matrices.scale(1.5F, 1.5F, 1.0F);
        context.drawText(textRenderer, bigTitle, -textRenderer.getWidth(bigTitle) / 2, 0, TITLE, false);
        matrices.pop();
    }

    // ---------------------------------------------------------------- saldo da casa (no canto de cima, à esquerda)

    private static final int HOUSE_X = 8;
    private static final int HOUSE_Y = 8;
    private static final float HOUSE_SCALE = 0.75F;
    private static final int HOUSE_POSITIVE = 0xFF1E7A1E;

    /** O servidor manda 0 quando a casa está no negativo, então aqui ela nunca é menor que 0. */
    private static Text houseText(int house) {
        return Text.translatable("gui.cassino.house", house > 0 ? "+" + house : "0");
    }

    /** "CASA: +1234" no canto de cima (coordenadas a partir do canto da tela): verde no lucro, cinza no 0. */
    static void drawHouse(DrawContext context, TextRenderer textRenderer, int house) {
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate(HOUSE_X, HOUSE_Y, 0.0F);
        matrices.scale(HOUSE_SCALE, HOUSE_SCALE, 1.0F);
        context.drawText(textRenderer, houseText(house), 0, 0,
                house > 0 ? HOUSE_POSITIVE : TEXT, false);
        matrices.pop();
    }

    /** Com o mouse em cima do saldo da casa: a explicação. */
    static void drawHouseTooltip(DrawContext context, TextRenderer textRenderer, int panelX, int panelY, int mouseX,
                                 int mouseY, int house) {
        int left = panelX + HOUSE_X;
        int top = panelY + HOUSE_Y;
        int width = Math.round(textRenderer.getWidth(houseText(house)) * HOUSE_SCALE);
        if (mouseX >= left && mouseX < left + width && mouseY >= top && mouseY < top + 7) {
            context.drawOrderedTooltip(textRenderer,
                    textRenderer.wrapLines(Text.translatable("gui.cassino.house.tip"), 200), mouseX, mouseY);
        }
    }

    static void drawSlot(DrawContext context, int slotX, int slotY) {
        context.fill(slotX, slotY, slotX + 18, slotY + 18, PANEL_LIGHT);
        context.fill(slotX, slotY, slotX + 17, slotY + 17, SLOT_DARK);
        context.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 17, SLOT_FILL);
    }

    /** O espaço das fichas, com uma ficha "apagada" quando está vazio. */
    static void drawCoinSlot(DrawContext context, int slotX, int slotY, boolean empty) {
        drawGhostSlot(context, slotX, slotY, CHIP, empty);
    }

    /** Um espaço que mostra, "apagado", o item que vai nele enquanto está vazio. */
    static void drawGhostSlot(DrawContext context, int slotX, int slotY, ItemStack ghost, boolean empty) {
        drawSlot(context, slotX, slotY);
        if (empty) {
            context.drawItem(ghost, slotX + 1, slotY + 1);
            MatrixStack matrices = context.getMatrices();
            matrices.push();
            matrices.translate(0.0F, 0.0F, 300.0F);
            context.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 17, 0xA08B8B8B);
            matrices.pop();
        }
    }

    /** Os espaços do inventário (3 fileiras) e da barra de baixo. */
    static void drawInventorySlots(DrawContext context, int inventoryX, int inventoryY, int hotbarY) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                drawSlot(context, inventoryX + column * 18, inventoryY + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            drawSlot(context, inventoryX + column * 18, hotbarY);
        }
    }

    /** "INVENTÁRIO" em cima dos espaços (coordenadas a partir do canto da tela). */
    static void drawInventoryLabel(DrawContext context, TextRenderer textRenderer, int inventoryX, int inventoryY) {
        Text inventory = Text.literal(Text.translatable("container.inventory").getString().toUpperCase(Locale.ROOT));
        context.drawText(textRenderer, inventory, inventoryX, inventoryY - 10, TEXT, false);
    }

    /** "APOSTA: 5" ou "CRÉDITOS: 150", com uma ficha pequena do lado. */
    static void drawAmount(DrawContext context, TextRenderer textRenderer, Text text, int textX, int textY) {
        drawAmount(context, textRenderer, text, textX, textY, TEXT);
    }

    static void drawAmount(DrawContext context, TextRenderer textRenderer, Text text, int textX, int textY,
                           int color) {
        context.drawText(textRenderer, text, textX, textY, color, false);
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate(textX + textRenderer.getWidth(text) + 2, textY - 2, 0.0F);
        matrices.scale(0.75F, 0.75F, 1.0F);
        context.drawItem(CHIP, 0, 0);
        matrices.pop();
    }
}
