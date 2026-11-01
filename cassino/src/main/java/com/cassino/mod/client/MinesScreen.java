package com.cassino.mod.client;

import com.cassino.mod.machine.Mines;
import com.cassino.mod.screen.MinesScreenHandler;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * A tela do Campo Minado: a grade de blocos de pedra à esquerda (clique para cavar), as minas, o multiplicador e o
 * prêmio no meio, e JOGAR!/SACAR!, a aposta, os créditos, + e -, o espaço das fichas, DEPÓSITO e RETIRADA à direita;
 * o inventário embaixo.
 *
 * <p>Quem decide o que tem embaixo de cada bloco é o servidor: a tela só manda o clique e mostra o resultado (a pedra
 * preciosa, ou a mina explodindo, com a grade toda aparecendo no fim da rodada).
 */
public class MinesScreen extends HandledScreen<MinesScreenHandler> {
    private static final Identifier STONE = new Identifier("minecraft", "textures/block/stone.png");
    private static final ItemStack EMERALD = new ItemStack(Items.EMERALD);
    private static final ItemStack DIAMOND = new ItemStack(Items.DIAMOND);
    private static final ItemStack TNT = new ItemStack(Items.TNT);
    private static final ItemStack CREEPER = new ItemStack(Items.CREEPER_HEAD);

    private static final int TILE_EDGE = 0xFF2A2A2A;
    private static final int TILE_HOVER = 0x50FFFFFF;
    private static final int GEM_BG = 0xFF2A5A3A;
    private static final int MINE_BG = 0xFF5A2A2A;
    private static final int EXPLODED_BG = 0xFFD02020;
    private static final int SHOWN_BG = 0xFF3A3A3A;
    private static final int DIM = 0x90303030;
    private static final int INFO_BG = 0xFF3A3A3A;
    private static final int INFO_TEXT = 0xFFE8E8E8;
    private static final int GOLD_TEXT = 0xFFF2C230;
    private static final int LABEL_TEXT = 0xFFB0B0B0;
    private static final int WIN_TEXT = 0xFF1E7A1E;
    private static final int LOSE_TEXT = 0xFFB02020;
    private static final int HINT_TEXT = 0xFF555555;

    // Posições a partir do canto da tela.
    private static final int GRID_X = 8;
    private static final int GRID_Y = 22;
    private static final int TILE = 20;
    private static final int STEP = 22;
    private static final int MID_X = 122;
    private static final int MID_WIDTH = 86;
    private static final int INFO_Y = 49;
    private static final int INFO_HEIGHT = 60;
    private static final int MESSAGE_Y = 113;
    private static final int RIGHT_X = 214;
    private static final int RIGHT_WIDTH = 94;

    /** Quanto tempo a grade pisca quando uma mina explode. */
    private static final long FLASH_MS = 450;
    /** Se o servidor não responder a um clique nesse tempo, os botões voltam. */
    private static final long WAIT_MS = 1_500;

    private ColorButton playButton;
    private ColorButton cashButton;
    private ButtonWidget minesDown;
    private ButtonWidget minesUp;
    private ButtonWidget betUp;
    private ButtonWidget betDown;
    private boolean initialized;
    private int seenEvents;
    private int seenFound;
    private long flashAt = -1;
    private boolean waiting;
    private long waitingSince;
    private Text message;
    private int messageColor;

    public MinesScreen(MinesScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        backgroundWidth = MinesScreenHandler.WIDTH;
        backgroundHeight = MinesScreenHandler.HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        if (!initialized) {
            seenEvents = handler.events();
            seenFound = handler.found();
            initialized = true;
        }
        playButton = addDrawableChild(new ColorButton(x + RIGHT_X, y + 20, RIGHT_WIDTH, 22,
                Text.translatable("gui.cassino.mines.play"), ColorButton.GREEN, 1.5F, this::play));
        playButton.setTooltip(Tooltip.of(Text.translatable("gui.cassino.mines.tip.rules")));
        cashButton = addDrawableChild(new ColorButton(x + RIGHT_X, y + 20, RIGHT_WIDTH, 22,
                Text.translatable("gui.cassino.mines.cash"), ColorButton.GOLD, 1.5F, this::cashOut));
        cashButton.setTooltip(Tooltip.of(Text.translatable("gui.cassino.mines.tip.cash")));
        betUp = addDrawableChild(ButtonWidget.builder(Text.literal("+"),
                        button -> click(MinesScreenHandler.BUTTON_BET_UP))
                .dimensions(x + RIGHT_X, y + 76, 45, 16)
                .tooltip(Tooltip.of(Text.translatable("gui.cassino.slot.tip.bet_up")))
                .build());
        betDown = addDrawableChild(ButtonWidget.builder(Text.literal("-"),
                        button -> click(MinesScreenHandler.BUTTON_BET_DOWN))
                .dimensions(x + RIGHT_X + RIGHT_WIDTH - 45, y + 76, 45, 16)
                .tooltip(Tooltip.of(Text.translatable("gui.cassino.slot.tip.bet_down")))
                .build());
        minesDown = addDrawableChild(ButtonWidget.builder(Text.literal("-"),
                        button -> click(MinesScreenHandler.BUTTON_MINES_DOWN))
                .dimensions(x + MID_X, y + 31, 18, 14)
                .tooltip(Tooltip.of(Text.translatable("gui.cassino.mines.tip.mines")))
                .build());
        minesUp = addDrawableChild(ButtonWidget.builder(Text.literal("+"),
                        button -> click(MinesScreenHandler.BUTTON_MINES_UP))
                .dimensions(x + MID_X + MID_WIDTH - 18, y + 31, 18, 14)
                .tooltip(Tooltip.of(Text.translatable("gui.cassino.mines.tip.mines")))
                .build());
        int depositX = MinesScreenHandler.COIN_SLOT_X + 21;
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.cassino.slot.deposit"),
                        button -> click(MinesScreenHandler.BUTTON_DEPOSIT))
                .dimensions(x + depositX, y + MinesScreenHandler.COIN_SLOT_Y - 1,
                        RIGHT_X + RIGHT_WIDTH - depositX, 20)
                .tooltip(Tooltip.of(Text.translatable("gui.cassino.slot.tip.deposit")))
                .build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.cassino.slot.withdraw"),
                        button -> click(MinesScreenHandler.BUTTON_WITHDRAW))
                .dimensions(x + RIGHT_X, y + 118, RIGHT_WIDTH, 17)
                .tooltip(Tooltip.of(Text.translatable("gui.cassino.slot.tip.withdraw")))
                .build());
    }

    private void click(int button) {
        if (client != null && client.interactionManager != null) {
            client.interactionManager.clickButton(handler.syncId, button);
        }
    }

    private boolean playing() {
        return handler.phase() == MinesScreenHandler.PLAYING;
    }

    private void play() {
        if (waiting || playing()) {
            return;
        }
        if (handler.credits() + handler.coinsWaiting() < handler.bet()) {
            showMessage(Text.translatable("gui.cassino.slot.no_credits"), LOSE_TEXT);
            return;
        }
        waiting = true;
        waitingSince = System.currentTimeMillis();
        message = null;
        click(MinesScreenHandler.BUTTON_PLAY);
    }

    private void cashOut() {
        if (waiting || !playing()) {
            return;
        }
        waiting = true;
        waitingSince = System.currentTimeMillis();
        click(MinesScreenHandler.BUTTON_PLAY);
    }

    private void showMessage(Text text, int color) {
        message = text;
        messageColor = color;
    }

    private void sound(SoundEvent event, float pitch, float volume) {
        if (client != null) {
            client.getSoundManager().play(PositionedSoundInstance.master(event, pitch, volume));
        }
    }

    /** O bloco embaixo do mouse, ou -1. */
    private int tileAt(double mouseX, double mouseY) {
        int column = (int) Math.floor((mouseX - x - GRID_X) / STEP);
        int row = (int) Math.floor((mouseY - y - GRID_Y) / STEP);
        if (column < 0 || column >= Mines.SIZE || row < 0 || row >= Mines.SIZE) {
            return -1;
        }
        int inX = (int) (mouseX - x - GRID_X) - column * STEP;
        int inY = (int) (mouseY - y - GRID_Y) - row * STEP;
        return inX < TILE && inY < TILE ? row * Mines.SIZE + column : -1;
    }

    /** Clicar num bloco coberto cava ele (com a rodada aberta). */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && playing()) {
            int tile = tileAt(mouseX, mouseY);
            if (tile >= 0 && handler.tile(tile) == Mines.HIDDEN) {
                if (!handler.canDig()) {
                    showMessage(Text.translatable("gui.cassino.mines.limit"), LOSE_TEXT);
                } else {
                    click(MinesScreenHandler.BUTTON_DIG + tile);
                    sound(SoundEvents.BLOCK_STONE_HIT, 1.0F, 0.6F);
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void update(long now) {
        if (handler.events() != seenEvents) {
            seenEvents = handler.events();
            waiting = false;
            switch (handler.phase()) {
                case MinesScreenHandler.PLAYING -> {
                    if (handler.found() > seenFound) {
                        sound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8F + handler.found() * 0.05F, 0.6F);
                        message = null;
                    } else {
                        showMessage(Text.translatable("gui.cassino.mines.dig"), HINT_TEXT);
                    }
                }
                case MinesScreenHandler.BUSTED -> {
                    flashAt = now;
                    showMessage(Text.translatable("gui.cassino.mines.boom", handler.stake()), LOSE_TEXT);
                }
                case MinesScreenHandler.CASHED -> {
                    if (handler.found() > 0) {
                        showMessage(Text.translatable("gui.cassino.mines.cashed", handler.lastWin()), WIN_TEXT);
                        sound(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.2F, 0.5F);
                    } else {
                        showMessage(Text.translatable("gui.cassino.mines.refund"), HINT_TEXT);
                    }
                }
                default -> {
                }
            }
            seenFound = handler.found();
        }
        if (waiting && now - waitingSince > WAIT_MS) {
            waiting = false;
        }
    }

    /** "x1,13" (ou "x2.208" quando passa de mil). */
    private static String multiplier(double value) {
        if (value >= 1000) {
            return "x" + (long) value;
        }
        return String.format(Locale.ROOT, "x%.2f", value).replace('.', ',');
    }

    // ---------------------------------------------------------------- desenho

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        long now = System.currentTimeMillis();
        update(now);
        boolean playing = playing();
        playButton.visible = !playing;
        cashButton.visible = playing;
        playButton.active = !waiting;
        cashButton.active = !waiting;
        minesDown.active = !playing && handler.mines() > Mines.MIN_MINES;
        minesUp.active = !playing && handler.mines() < Mines.MAX_MINES;
        betUp.active = !playing;
        betDown.active = !playing;
        renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
        CassinoGui.drawTycoonTooltip(context, textRenderer, handler, x, y, mouseX, mouseY,
                MinesScreenHandler.INVENTORY_X, MinesScreenHandler.INVENTORY_Y,
                MinesScreenHandler.HOTBAR_Y);
        CassinoGui.drawHouseTooltip(context, textRenderer, x, y, mouseX, mouseY, handler.house());
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        CassinoGui.drawPanel(context, x, y, backgroundWidth, backgroundHeight);
        drawGrid(context, mouseX, mouseY);
        context.fill(x + MID_X, y + INFO_Y, x + MID_X + MID_WIDTH, y + INFO_Y + INFO_HEIGHT, INFO_BG);
        context.fill(x + RIGHT_X, y + 46, x + RIGHT_X + RIGHT_WIDTH, y + 72, INFO_BG);
        CassinoGui.drawCoinSlot(context, x + MinesScreenHandler.COIN_SLOT_X, y + MinesScreenHandler.COIN_SLOT_Y,
                !handler.getSlot(0).hasStack());
        CassinoGui.drawInventorySlots(context, x + MinesScreenHandler.INVENTORY_X, y + MinesScreenHandler.INVENTORY_Y,
                y + MinesScreenHandler.HOTBAR_Y);
    }

    /** A grade: pedra nos blocos cobertos, e a pedra preciosa ou a mina nos abertos (as do fim, apagadas). */
    private void drawGrid(DrawContext context, int mouseX, int mouseY) {
        int hovered = playing() && handler.canDig() ? tileAt(mouseX, mouseY) : -1;
        MatrixStack matrices = context.getMatrices();
        for (int i = 0; i < Mines.CELLS; i++) {
            int tileX = x + GRID_X + (i % Mines.SIZE) * STEP;
            int tileY = y + GRID_Y + (i / Mines.SIZE) * STEP;
            int code = handler.tile(i);
            int look = code & 7;
            boolean shown = (code & Mines.SHOWN) != 0;
            boolean exploded = (code & Mines.EXPLODED) != 0;
            if (look == Mines.HIDDEN) {
                context.fill(tileX, tileY, tileX + TILE, tileY + TILE, TILE_EDGE);
                context.drawTexture(STONE, tileX + 2, tileY + 2, 0.0F, 0.0F, 16, 16, 16, 16);
                if (i == hovered && handler.tile(i) == Mines.HIDDEN) {
                    context.fill(tileX + 2, tileY + 2, tileX + 18, tileY + 18, TILE_HOVER);
                }
                continue;
            }
            boolean mine = look == Mines.TNT || look == Mines.CREEPER;
            int background = exploded ? EXPLODED_BG : shown ? (mine ? MINE_BG : SHOWN_BG) : GEM_BG;
            context.fill(tileX, tileY, tileX + TILE, tileY + TILE, TILE_EDGE);
            context.fill(tileX + 1, tileY + 1, tileX + TILE - 1, tileY + TILE - 1, background);
            ItemStack icon = switch (look) {
                case Mines.EMERALD -> EMERALD;
                case Mines.DIAMOND -> DIAMOND;
                case Mines.TNT -> TNT;
                default -> CREEPER;
            };
            context.drawItem(icon, tileX + 2, tileY + 2);
            if (shown) {
                matrices.push();
                matrices.translate(0.0F, 0.0F, 300.0F);
                context.fill(tileX + 1, tileY + 1, tileX + TILE - 1, tileY + TILE - 1, DIM);
                matrices.pop();
            }
        }
        // A explosão: a grade pisca em laranja e some aos poucos.
        if (flashAt >= 0) {
            long elapsed = System.currentTimeMillis() - flashAt;
            if (elapsed > FLASH_MS) {
                flashAt = -1;
            } else {
                int alpha = (int) (0xB0 * (1.0F - elapsed / (float) FLASH_MS));
                matrices.push();
                matrices.translate(0.0F, 0.0F, 310.0F);
                context.fill(x + GRID_X - 2, y + GRID_Y - 2, x + GRID_X + Mines.SIZE * STEP,
                        y + GRID_Y + Mines.SIZE * STEP, alpha << 24 | 0xFF8020);
                matrices.pop();
            }
        }
    }

    /** Os textos, a partir do canto da tela. */
    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        CassinoGui.drawTitle(context, textRenderer, title, backgroundWidth);
        CassinoGui.drawHouse(context, textRenderer, handler.house());

        int center = MID_X + MID_WIDTH / 2;
        Text minesLabel = Text.translatable("gui.cassino.mines.mines");
        context.drawText(textRenderer, minesLabel, center - textRenderer.getWidth(minesLabel) / 2, 22,
                CassinoGui.TEXT, false);
        String mines = Integer.toString(handler.mines());
        context.drawText(textRenderer, mines, center - textRenderer.getWidth(mines) / 2, 34, CassinoGui.TITLE,
                false);

        int found = playing() || handler.phase() != MinesScreenHandler.WAITING ? handler.found() : 0;
        int safe = Mines.CELLS - handler.mines();
        boolean open = playing();
        int lineX = MID_X + 4;
        int maxWidth = MID_WIDTH - 8;
        drawLine(context, Text.translatable("gui.cassino.mines.found", found, safe), lineX, INFO_Y + 3, maxWidth,
                INFO_TEXT);
        drawLine(context, Text.translatable("gui.cassino.mines.now",
                found > 0 ? multiplier(Mines.multiplier(handler.mines(), found)) : "-"), lineX, INFO_Y + 14,
                maxWidth, INFO_TEXT);
        boolean more = found < safe;
        drawLine(context, Text.translatable("gui.cassino.mines.next",
                more ? multiplier(Mines.multiplier(handler.mines(), found + 1)) : "-"), lineX, INFO_Y + 25,
                maxWidth, INFO_TEXT);
        if (open) {
            CassinoGui.drawAmount(context, textRenderer, Text.translatable("gui.cassino.mines.cash_value",
                    Mines.payout(handler.stake(), handler.mines(), found)), lineX, INFO_Y + 36, GOLD_TEXT);
        } else {
            drawLine(context, Text.translatable("gui.cassino.mines.cash_value", "-"), lineX, INFO_Y + 36,
                    maxWidth, LABEL_TEXT);
        }
        drawLine(context, Text.translatable("gui.cassino.mines.chance",
                more ? Math.round(Mines.safeChance(handler.mines(), found) * 100) + "%" : "-"), lineX, INFO_Y + 47,
                maxWidth, LABEL_TEXT);

        Text line = message;
        int color = messageColor;
        if (open && !handler.canDig() && found < safe) {
            line = Text.translatable("gui.cassino.mines.limit");
            color = LOSE_TEXT;
        } else if (line == null) {
            line = Text.translatable(open ? "gui.cassino.mines.dig" : "gui.cassino.mines.hint");
            color = HINT_TEXT;
        }
        List<OrderedText> lines = textRenderer.wrapLines(line, MID_WIDTH);
        for (int i = 0; i < Math.min(2, lines.size()); i++) {
            OrderedText part = lines.get(i);
            context.drawText(textRenderer, part, center - textRenderer.getWidth(part) / 2, MESSAGE_Y + i * 9, color,
                    false);
        }

        CassinoGui.drawAmount(context, textRenderer, Text.translatable("gui.cassino.slot.bet",
                open ? handler.stake() : handler.bet()), RIGHT_X + 4, 50, INFO_TEXT);
        CassinoGui.drawAmount(context, textRenderer, Text.translatable("gui.cassino.slot.credits", handler.credits()),
                RIGHT_X + 4, 61, INFO_TEXT);
        CassinoGui.drawInventoryLabel(context, textRenderer, MinesScreenHandler.INVENTORY_X,
                MinesScreenHandler.INVENTORY_Y);
        CassinoGui.drawTycoon(context, textRenderer, handler, MinesScreenHandler.INVENTORY_X,
                MinesScreenHandler.INVENTORY_Y, MinesScreenHandler.HOTBAR_Y);
    }

    /** Uma linha do quadro do meio, diminuída se não couber. */
    private void drawLine(DrawContext context, Text text, int textX, int textY, int maxWidth, int color) {
        float scale = Math.min(1.0F, maxWidth / (float) textRenderer.getWidth(text));
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate(textX, textY + (1.0F - scale) * 4.0F, 0.0F);
        matrices.scale(scale, scale, 1.0F);
        context.drawText(textRenderer, text, 0, 0, color, false);
        matrices.pop();
    }
}
