package com.cassino.mod.client;

import com.cassino.mod.machine.Lottery;
import com.cassino.mod.screen.LotteryScreenHandler;
import java.util.Locale;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

/**
 * A tela da loteria de mobs: a tabela colorida com os 25 mobs à esquerda (clique num para escolher) e APOSTAR!, o
 * prêmio acumulado, os créditos, quanto falta para o sorteio, a sua aposta de hoje, o último mob sorteado, o espaço das
 * fichas e DEPÓSITO à direita; o magnata à esquerda do inventário e RETIRADA à direita dele.
 *
 * <p>O sorteio não acontece aqui: ele sai sozinho quando o dia do Minecraft acaba (veja {@link Lottery}).
 */
public class LotteryScreen extends HandledScreen<LotteryScreenHandler> {
    /** As cores das fileiras da tabela. */
    private static final int[] ROW_COLORS = {0xFFB8E0A8, 0xFFA8C8E8, 0xFFE8E0A0, 0xFFE8C0A0, 0xFFD0B8E4};
    private static final int CELL_OUTLINE = 0xFF404040;
    private static final int CELL_HOVER = 0x40FFFFFF;
    private static final int MY_BET = 0xFFF2D43A;
    private static final int SELECTED = 0xFFFFFFFF;
    private static final int LAST_DRAWN = 0xFF1E9A1E;
    private static final int INFO_BG = 0xFF3A3A3A;
    private static final int INFO_TEXT = 0xFFE8E8E8;
    private static final int GOLD_TEXT = 0xFFF2C230;
    private static final int LABEL_TEXT = 0xFFB0B0B0;
    private static final int WIN_TEXT = 0xFF1E7A1E;
    private static final int ERROR_TEXT = 0xFFB02020;
    private static final int HINT_TEXT = 0xFF555555;

    // Posições a partir do canto da tela.
    private static final int GRID_X = 8;
    private static final int GRID_Y = 20;
    private static final int COLUMNS = 5;
    private static final int CELL_WIDTH = 40;
    private static final int CELL_HEIGHT = 25;
    private static final int MESSAGE_Y = 147;
    private static final int RIGHT_X = 214;
    private static final int RIGHT_WIDTH = 94;
    private static final int INFO_Y = 46;
    private static final int MY_BET_Y = 93;
    private static final int LAST_Y = 117;
    private static final int BOX_HEIGHT = 22;

    /** Se o servidor não responder ao APOSTAR! nesse tempo, o botão volta. */
    private static final long WAIT_MS = 1_500;

    private ColorButton betButton;
    private boolean initialized;
    private int selected = -1;
    private int seenBet;
    private int seenLast;
    private boolean waiting;
    private long waitingSince;
    private Text message;
    private int messageColor;

    public LotteryScreen(LotteryScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        backgroundWidth = LotteryScreenHandler.WIDTH;
        backgroundHeight = LotteryScreenHandler.HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        if (!initialized) {
            seenBet = handler.myBet();
            seenLast = handler.lastMob();
            initialized = true;
        }
        betButton = addDrawableChild(new ColorButton(x + RIGHT_X, y + 20, RIGHT_WIDTH, 22,
                Text.translatable("gui.cassino.lottery.bet"), ColorButton.GREEN, 1.5F, this::bet));
        betButton.setTooltip(Tooltip.of(Text.translatable("gui.cassino.lottery.tip.bet", Lottery.BET)));
        int depositX = LotteryScreenHandler.COIN_SLOT_X + 21;
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.cassino.slot.deposit"),
                        button -> click(LotteryScreenHandler.BUTTON_DEPOSIT))
                .dimensions(x + depositX, y + LotteryScreenHandler.COIN_SLOT_Y - 1,
                        RIGHT_X + RIGHT_WIDTH - depositX, 20)
                .tooltip(Tooltip.of(Text.translatable("gui.cassino.slot.tip.deposit")))
                .build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.cassino.slot.withdraw"),
                        button -> click(LotteryScreenHandler.BUTTON_WITHDRAW))
                .dimensions(x + LotteryScreenHandler.INVENTORY_X + 168, y + LotteryScreenHandler.INVENTORY_Y,
                        63, 18)
                .tooltip(Tooltip.of(Text.translatable("gui.cassino.slot.tip.withdraw")))
                .build());
    }

    private void click(int button) {
        if (client != null && client.interactionManager != null) {
            client.interactionManager.clickButton(handler.syncId, button);
        }
    }

    private void bet() {
        if (waiting) {
            return;
        }
        if (handler.myBet() >= 0) {
            showMessage(Text.translatable("gui.cassino.lottery.already"), ERROR_TEXT);
            return;
        }
        if (selected < 0) {
            showMessage(Text.translatable("gui.cassino.lottery.choose_first"), ERROR_TEXT);
            return;
        }
        if (handler.credits() + handler.coinsWaiting() < Lottery.BET) {
            showMessage(Text.translatable("gui.cassino.slot.no_credits"), ERROR_TEXT);
            return;
        }
        waiting = true;
        waitingSince = System.currentTimeMillis();
        message = null;
        click(LotteryScreenHandler.BUTTON_BET + selected);
    }

    private void showMessage(Text text, int color) {
        message = text;
        messageColor = color;
    }

    private void play(SoundEvent sound, float pitch, float volume) {
        if (client != null) {
            client.getSoundManager().play(PositionedSoundInstance.master(sound, pitch, volume));
        }
    }

    /** O mob da célula embaixo do mouse, ou -1. */
    private int cellAt(double mouseX, double mouseY) {
        int column = (int) Math.floor((mouseX - x - GRID_X) / CELL_WIDTH);
        int row = (int) Math.floor((mouseY - y - GRID_Y) / CELL_HEIGHT);
        if (column < 0 || column >= COLUMNS || row < 0 || row * COLUMNS + column >= Lottery.COUNT) {
            return -1;
        }
        return row * COLUMNS + column;
    }

    /** Clicar num mob escolhe ele (enquanto o jogador não apostou hoje). */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int mob = cellAt(mouseX, mouseY);
            if (mob >= 0) {
                if (handler.myBet() >= 0) {
                    showMessage(Text.translatable("gui.cassino.lottery.already"), ERROR_TEXT);
                } else {
                    selected = mob;
                    message = null;
                    play(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 0.6F);
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void update(long now) {
        if (handler.myBet() != seenBet) {
            seenBet = handler.myBet();
            waiting = false;
            if (seenBet >= 0) {
                showMessage(Text.translatable("gui.cassino.lottery.placed", mobName(seenBet)), WIN_TEXT);
                play(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.2F, 0.6F);
            } else {
                selected = -1; // o sorteio saiu: dá para apostar de novo
            }
        }
        if (handler.lastMob() != seenLast) {
            seenLast = handler.lastMob();
            if (seenLast >= 0) {
                showMessage(Text.translatable("gui.cassino.lottery.drawn", mobName(seenLast)), WIN_TEXT);
            }
        }
        if (waiting && now - waitingSince > WAIT_MS) {
            waiting = false;
        }
    }

    private static String mobName(int mob) {
        return Lottery.name(mob).getString().toUpperCase(Locale.ROOT);
    }

    // ---------------------------------------------------------------- desenho

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        update(System.currentTimeMillis());
        betButton.active = handler.myBet() < 0 && !waiting;
        renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
        CassinoGui.drawTycoonTooltip(context, textRenderer, handler, x, y, mouseX, mouseY,
                LotteryScreenHandler.INVENTORY_X, LotteryScreenHandler.INVENTORY_Y,
                LotteryScreenHandler.HOTBAR_Y);
        int mob = cellAt(mouseX, mouseY);
        if (mob >= 0) {
            context.drawTooltip(textRenderer, Text.translatable("gui.cassino.lottery.cell", Lottery.name(mob)),
                    mouseX, mouseY);
        }
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        CassinoGui.drawPanel(context, x, y, backgroundWidth, backgroundHeight);
        drawGrid(context, mouseX, mouseY);

        context.fill(x + RIGHT_X, y + INFO_Y, x + RIGHT_X + RIGHT_WIDTH, y + INFO_Y + 44, INFO_BG);
        drawMobBox(context, MY_BET_Y, handler.myBet() >= 0 ? handler.myBet() : selected);
        drawMobBox(context, LAST_Y, handler.lastMob());
        CassinoGui.drawCoinSlot(context, x + LotteryScreenHandler.COIN_SLOT_X, y + LotteryScreenHandler.COIN_SLOT_Y,
                !handler.getSlot(0).hasStack());
        CassinoGui.drawInventorySlots(context, x + LotteryScreenHandler.INVENTORY_X,
                y + LotteryScreenHandler.INVENTORY_Y, y + LotteryScreenHandler.HOTBAR_Y);
    }

    /** A tabela: cada fileira de uma cor, a aposta de hoje com borda amarela e a ficha, o último sorteado em verde. */
    private void drawGrid(DrawContext context, int mouseX, int mouseY) {
        int hovered = cellAt(mouseX, mouseY);
        int chosen = handler.myBet() >= 0 ? handler.myBet() : selected;
        for (int mob = 0; mob < Lottery.COUNT; mob++) {
            int cellX = x + GRID_X + (mob % COLUMNS) * CELL_WIDTH;
            int cellY = y + GRID_Y + (mob / COLUMNS) * CELL_HEIGHT;
            context.fill(cellX, cellY, cellX + CELL_WIDTH, cellY + CELL_HEIGHT, CELL_OUTLINE);
            context.fill(cellX + 1, cellY + 1, cellX + CELL_WIDTH - 1, cellY + CELL_HEIGHT - 1,
                    ROW_COLORS[mob / COLUMNS]);
            if (mob == handler.lastMob()) {
                context.drawBorder(cellX + 1, cellY + 1, CELL_WIDTH - 2, CELL_HEIGHT - 2, LAST_DRAWN);
            }
            if (mob == hovered) {
                context.fill(cellX + 1, cellY + 1, cellX + CELL_WIDTH - 1, cellY + CELL_HEIGHT - 1, CELL_HOVER);
            }
            if (mob == chosen) {
                int color = handler.myBet() >= 0 ? MY_BET : SELECTED;
                context.drawBorder(cellX, cellY, CELL_WIDTH, CELL_HEIGHT, color);
                context.drawBorder(cellX + 1, cellY + 1, CELL_WIDTH - 2, CELL_HEIGHT - 2, color);
            }
            MobIcons.draw(context, mob, cellX + 12, cellY + 1, 16);
        }
    }

    /** O quadro escuro de "SUA APOSTA" ou "ÚLTIMO SORTEIO": o rosto e o nome do mob. */
    private void drawMobBox(DrawContext context, int boxY, int mob) {
        int left = x + RIGHT_X;
        int top = y + boxY;
        context.fill(left, top, left + RIGHT_WIDTH, top + BOX_HEIGHT, INFO_BG);
        if (mob >= 0) {
            MobIcons.draw(context, mob, left + 3, top + 8, 12);
        }
    }

    /** Os textos, a partir do canto da tela. */
    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        CassinoGui.drawTitle(context, textRenderer, title, backgroundWidth);
        int chosen = handler.myBet() >= 0 ? handler.myBet() : selected;

        for (int mob = 0; mob < Lottery.COUNT; mob++) {
            int cellX = GRID_X + (mob % COLUMNS) * CELL_WIDTH;
            int cellY = GRID_Y + (mob / COLUMNS) * CELL_HEIGHT;
            drawScaled(context, Text.literal(Integer.toString(mob + 1)), cellX + 2, cellY + 2, 0.5F, 0xFF303030);
            Text name = Text.literal(mobName(mob));
            float scale = Math.min(0.6F, (CELL_WIDTH - 3) / (float) textRenderer.getWidth(name));
            drawScaled(context, name, cellX + CELL_WIDTH / 2.0F - textRenderer.getWidth(name) * scale / 2.0F,
                    cellY + 19, scale, mob == chosen ? 0xFF000000 : 0xFF303030);
            if (mob == handler.myBet()) {
                MatrixStack matrices = context.getMatrices();
                matrices.push();
                matrices.translate(cellX + CELL_WIDTH - 10, cellY + 2, 200.0F);
                matrices.scale(0.5F, 0.5F, 1.0F);
                context.drawItem(CassinoGui.CHIP, 0, 0);
                matrices.pop();
            }
        }

        Text line = message != null ? message : Text.translatable("gui.cassino.lottery.hint", Lottery.BET);
        int color = message != null ? messageColor : HINT_TEXT;
        CassinoGui.drawFittedText(context, textRenderer, line, GRID_X + COLUMNS * CELL_WIDTH / 2, MESSAGE_Y,
                COLUMNS * CELL_WIDTH, color);

        CassinoGui.drawAmount(context, textRenderer, Text.translatable("gui.cassino.lottery.pot", handler.pot()),
                RIGHT_X + 4, INFO_Y + 3, GOLD_TEXT);
        CassinoGui.drawAmount(context, textRenderer, Text.translatable("gui.cassino.slot.credits", handler.credits()),
                RIGHT_X + 4, INFO_Y + 13, INFO_TEXT);
        int seconds = handler.seconds();
        context.drawText(textRenderer, Text.translatable("gui.cassino.lottery.timer",
                String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60)), RIGHT_X + 4, INFO_Y + 23,
                INFO_TEXT, false);
        context.drawText(textRenderer, Text.translatable("gui.cassino.lottery.bets_today", handler.betsToday()),
                RIGHT_X + 4, INFO_Y + 33, INFO_TEXT, false);

        drawBoxText(context, MY_BET_Y, Text.translatable("gui.cassino.lottery.my_bet"),
                handler.myBet() >= 0 ? Text.literal(mobName(handler.myBet()))
                        : selected >= 0 ? Text.translatable("gui.cassino.lottery.not_yet", mobName(selected))
                        : Text.translatable("gui.cassino.lottery.none"));
        drawBoxText(context, LAST_Y, Text.translatable("gui.cassino.lottery.last"),
                handler.lastMob() >= 0 ? Text.literal(mobName(handler.lastMob()))
                        : Text.translatable("gui.cassino.lottery.none"));

        CassinoGui.drawInventoryLabel(context, textRenderer, LotteryScreenHandler.INVENTORY_X,
                LotteryScreenHandler.INVENTORY_Y);
        CassinoGui.drawTycoon(context, textRenderer, handler, LotteryScreenHandler.INVENTORY_X,
                LotteryScreenHandler.INVENTORY_Y, LotteryScreenHandler.HOTBAR_Y);
    }

    /** O título pequeno em cima e o nome do mob ao lado do rosto. */
    private void drawBoxText(DrawContext context, int boxY, Text label, Text value) {
        drawScaled(context, label, RIGHT_X + 3, boxY + 2, 0.75F, LABEL_TEXT);
        float scale = Math.min(1.0F, (RIGHT_WIDTH - 20) / (float) textRenderer.getWidth(value));
        drawScaled(context, value, RIGHT_X + 17, boxY + 11 + (1.0F - scale) * 4.0F, scale, INFO_TEXT);
    }

    private void drawScaled(DrawContext context, Text text, float textX, float textY, float scale, int color) {
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate(textX, textY, 0.0F);
        matrices.scale(scale, scale, 1.0F);
        context.drawText(textRenderer, text, 0, 0, color, false);
        matrices.pop();
    }
}
