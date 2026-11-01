package com.cassino.mod.client;

import com.cassino.mod.Cassino;
import com.cassino.mod.machine.CoinFlip;
import com.cassino.mod.screen.CoinFlipScreenHandler;
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
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

/**
 * A tela do cara ou coroa: a moeda à esquerda (com o resultado e os últimos lançamentos embaixo), as duas apostas no
 * meio (CARA, com o rosto, e COROA; clique numa para escolher) e LANÇAR!, a aposta, os créditos, + e -, o espaço das
 * fichas, DEPÓSITO e RETIRADA à direita; o inventário embaixo.
 *
 * <p>Quem lança é o servidor. Quando o resultado chega, a moeda gira no ar (mostrando o rosto e a coroa) e cai no
 * lado sorteado, ou em pé; só então aparecem o prêmio e o saldo novo.
 */
public class CoinFlipScreen extends HandledScreen<CoinFlipScreenHandler> {
    private static final Identifier HEADS = Cassino.id("textures/gui/coin_heads.png");
    private static final Identifier TAILS = Cassino.id("textures/gui/coin_tails.png");
    private static final Identifier FACE = Cassino.id("textures/gui/coin_face.png");
    private static final Identifier CROWN = Cassino.id("textures/gui/crown.png");

    private static final int RESULT_BG = 0xFF2A2A2A;
    private static final int BOX_OUTLINE = 0xFF555555;
    private static final int BOX_FILL = 0xFFB4B4B4;
    private static final int HEADER_FILL = 0xFF8B8B8B;
    private static final int SELECTED = 0xFFF2D43A;
    private static final int COLUMN_SELECTED = 0x40F2D43A;
    private static final int COLUMN_HOVER = 0x30FFFFFF;
    private static final int INFO_BG = 0xFF3A3A3A;
    private static final int INFO_TEXT = 0xFFE8E8E8;
    private static final int ODDS_TEXT = 0xFF1E5A8A;
    private static final int WIN_TEXT = 0xFF1E7A1E;
    private static final int LOSE_TEXT = 0xFF555555;
    private static final int ERROR_TEXT = 0xFFB02020;
    private static final int GOLD_TEXT = 0xFFB07800;
    private static final int[] SIDE_TEXT = {0xFF2E7A2E, 0xFF9A6A00, 0xFF707070};
    private static final int COIN_EDGE = 0xFFF6C430;
    private static final int COIN_EDGE_DARK = 0xFF6E4806;

    // Posições a partir do canto da tela.
    private static final int LEFT_X = 8;
    private static final int LEFT_WIDTH = 72;
    private static final int RESULT_Y = 30;
    private static final int RESULT_HEIGHT = 66;
    private static final int COIN_CENTER_X = 44;
    private static final int COIN_CENTER_Y = 58;
    private static final int COIN_SIZE = 48;
    private static final int HISTORY_Y = 100;
    private static final int HISTORY_HEIGHT = 22;
    private static final int BETS_X = 86;
    private static final int BETS_Y = 20;
    private static final int BETS_WIDTH = 122;
    private static final int BETS_HEIGHT = 100;
    private static final int HEADER_HEIGHT = 12;
    private static final int COLUMN_WIDTH = BETS_WIDTH / 2;
    private static final int ICON_Y = 56;
    private static final int SPOT_Y = 96;
    private static final int MESSAGE_Y = 125;
    private static final int RIGHT_X = 214;
    private static final int RIGHT_WIDTH = 94;

    /** Quanto tempo a moeda fica no ar, e quantas voltas inteiras ela dá. */
    private static final long FLIP_MS = 1_500;
    private static final int TURNS = 5;
    /** Se o servidor não responder ao LANÇAR! nesse tempo (sem créditos, por exemplo), o botão volta. */
    private static final long WAIT_MS = 1_500;

    private ColorButton flipButton;
    private boolean initialized;
    private int seenFlips;
    /** O giro da moeda (radianos): 0 = rosto para cima, π = coroa, π/2 = em pé. */
    private float coinAngle;
    private float fromAngle;
    private float toAngle;
    private long flipStartedAt = -1;
    private int flipWin;
    private boolean waiting;
    private long waitingSince;
    private Text message;
    private int messageColor;

    public CoinFlipScreen(CoinFlipScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        backgroundWidth = CoinFlipScreenHandler.WIDTH;
        backgroundHeight = CoinFlipScreenHandler.HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        if (!initialized) {
            seenFlips = handler.flips();
            coinAngle = restingAngle(handler.result());
            initialized = true;
        }
        flipButton = addDrawableChild(new ColorButton(x + RIGHT_X, y + 20, RIGHT_WIDTH, 22,
                Text.translatable("gui.cassino.coin.flip"), ColorButton.GREEN, 1.5F, this::flip));
        flipButton.setTooltip(Tooltip.of(Text.translatable("gui.cassino.coin.tip.rules")));
        addDrawableChild(ButtonWidget.builder(Text.literal("+"),
                        button -> click(CoinFlipScreenHandler.BUTTON_BET_UP))
                .dimensions(x + RIGHT_X, y + 76, 45, 16)
                .tooltip(Tooltip.of(Text.translatable("gui.cassino.slot.tip.bet_up")))
                .build());
        addDrawableChild(ButtonWidget.builder(Text.literal("-"),
                        button -> click(CoinFlipScreenHandler.BUTTON_BET_DOWN))
                .dimensions(x + RIGHT_X + RIGHT_WIDTH - 45, y + 76, 45, 16)
                .tooltip(Tooltip.of(Text.translatable("gui.cassino.slot.tip.bet_down")))
                .build());
        int depositX = CoinFlipScreenHandler.COIN_SLOT_X + 21;
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.cassino.slot.deposit"),
                        button -> click(CoinFlipScreenHandler.BUTTON_DEPOSIT))
                .dimensions(x + depositX, y + CoinFlipScreenHandler.COIN_SLOT_Y - 1,
                        RIGHT_X + RIGHT_WIDTH - depositX, 20)
                .tooltip(Tooltip.of(Text.translatable("gui.cassino.slot.tip.deposit")))
                .build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.cassino.slot.withdraw"),
                        button -> click(CoinFlipScreenHandler.BUTTON_WITHDRAW))
                .dimensions(x + RIGHT_X, y + 118, RIGHT_WIDTH, 17)
                .tooltip(Tooltip.of(Text.translatable("gui.cassino.slot.tip.withdraw")))
                .build());
    }

    private void click(int button) {
        if (client != null && client.interactionManager != null) {
            client.interactionManager.clickButton(handler.syncId, button);
        }
    }

    private void flip() {
        if (flipping() || waiting) {
            return;
        }
        int bet = handler.bet();
        if (handler.credits() + handler.coinsWaiting() < bet) {
            showMessage(Text.translatable("gui.cassino.slot.no_credits"), ERROR_TEXT);
            return;
        }
        if (bet > handler.maxBet()) {
            showMessage(handler.maxBet() > 0
                    ? Text.translatable("gui.cassino.blackjack.table_limit", handler.maxBet())
                    : Text.translatable("gui.cassino.blackjack.table_closed"), ERROR_TEXT);
            return;
        }
        waiting = true;
        waitingSince = System.currentTimeMillis();
        message = null;
        click(CoinFlipScreenHandler.BUTTON_FLIP);
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

    /** A coluna de aposta (CARA ou COROA) embaixo do mouse, ou -1. */
    private int columnAt(double mouseX, double mouseY) {
        int top = y + BETS_Y + HEADER_HEIGHT;
        if (mouseY < top || mouseY >= y + BETS_Y + BETS_HEIGHT - 1) {
            return -1;
        }
        for (int side = 0; side < CoinFlip.SIDES; side++) {
            int left = x + BETS_X + side * COLUMN_WIDTH;
            if (mouseX >= left && mouseX < left + COLUMN_WIDTH) {
                return side;
            }
        }
        return -1;
    }

    /** Clicar em CARA ou COROA escolhe o lado da aposta (com a moeda parada). */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && !flipping()) {
            int side = columnAt(mouseX, mouseY);
            if (side >= 0) {
                click(CoinFlipScreenHandler.BUTTON_CHOOSE + side);
                play(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 0.6F);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    // ---------------------------------------------------------------- animação

    /** O giro da moeda parada neste resultado. */
    private static float restingAngle(int result) {
        return switch (result) {
            case CoinFlip.TAILS -> MathHelper.PI;
            case CoinFlip.EDGE -> MathHelper.HALF_PI;
            default -> 0.0F;
        };
    }

    private boolean flipping() {
        return flipStartedAt >= 0;
    }

    private void update(long now) {
        if (handler.flips() != seenFlips) {
            seenFlips = handler.flips();
            flipStartedAt = now;
            flipWin = handler.lastWin();
            waiting = false;
            message = null;
            fromAngle = coinAngle;
            float rest = MathHelper.floorMod(restingAngle(handler.result()) - fromAngle, MathHelper.TAU);
            toAngle = fromAngle + TURNS * MathHelper.TAU + rest;
        }
        if (waiting && now - waitingSince > WAIT_MS) {
            waiting = false;
        }
        if (!flipping()) {
            return;
        }
        float t = progress(now);
        float eased = 1.0F - (1.0F - t) * (1.0F - t) * (1.0F - t);
        coinAngle = fromAngle + (toAngle - fromAngle) * eased;
        if (t >= 1.0F) {
            flipStartedAt = -1;
            coinAngle = MathHelper.floorMod(toAngle, MathHelper.TAU);
            showResult();
        }
    }

    private float progress(long now) {
        return MathHelper.clamp((now - flipStartedAt) / (float) FLIP_MS, 0.0F, 1.0F);
    }

    private void showResult() {
        int result = handler.result();
        play(SoundEvents.BLOCK_CHAIN_PLACE, 1.4F, 0.7F);
        if (result == CoinFlip.EDGE) {
            showMessage(Text.translatable("gui.cassino.coin.result.edge"), GOLD_TEXT);
            play(SoundEvents.ENTITY_VILLAGER_NO, 1.0F, 0.6F);
            return;
        }
        Text side = Text.translatable("gui.cassino.coin.side." + result);
        if (flipWin > 0) {
            showMessage(Text.translatable("gui.cassino.coin.result.win", side, flipWin), WIN_TEXT);
            play(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 0.8F);
        } else {
            showMessage(Text.translatable("gui.cassino.coin.result.lose", side), LOSE_TEXT);
        }
    }

    /** O saldo na tela: com a moeda no ar, ainda sem o prêmio (ele entra quando ela cai). */
    private int shownCredits() {
        return flipping() ? Math.max(0, handler.credits() - flipWin) : handler.credits();
    }

    // ---------------------------------------------------------------- desenho

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        update(System.currentTimeMillis());
        flipButton.active = !flipping() && !waiting;
        renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
        CassinoGui.drawHouseTooltip(context, textRenderer, x, y, mouseX, mouseY, handler.house());
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        CassinoGui.drawPanel(context, x, y, backgroundWidth, backgroundHeight);
        drawResultBox(context);
        drawHistoryBox(context);
        drawBets(context, mouseX, mouseY);

        context.fill(x + RIGHT_X, y + 46, x + RIGHT_X + RIGHT_WIDTH, y + 72, INFO_BG);
        CassinoGui.drawCoinSlot(context, x + CoinFlipScreenHandler.COIN_SLOT_X,
                y + CoinFlipScreenHandler.COIN_SLOT_Y, !handler.getSlot(0).hasStack());
        CassinoGui.drawInventorySlots(context, x + CoinFlipScreenHandler.INVENTORY_X,
                y + CoinFlipScreenHandler.INVENTORY_Y, y + CoinFlipScreenHandler.HOTBAR_Y);
    }

    /** O quadro escuro com a moeda: girando no ar durante o lançamento, ou parada no resultado. */
    private void drawResultBox(DrawContext context) {
        int left = x + LEFT_X;
        int top = y + RESULT_Y;
        context.fill(left, top, left + LEFT_WIDTH, top + RESULT_HEIGHT, BOX_OUTLINE);
        context.fill(left + 1, top + 1, left + LEFT_WIDTH - 1, top + RESULT_HEIGHT - 1, RESULT_BG);

        float lift = flipping() ? MathHelper.sin(progress(System.currentTimeMillis()) * MathHelper.PI) * 4.0F : 0.0F;
        float centerX = x + COIN_CENTER_X;
        float centerY = y + COIN_CENTER_Y - lift;
        float squash = MathHelper.cos(coinAngle);
        int half = COIN_SIZE / 2;
        if (Math.abs(squash) < 0.08F) {
            // De lado: só a borda da moeda aparece (é assim que ela fica quando cai em pé).
            int edgeX = Math.round(centerX);
            int edgeY = Math.round(centerY);
            context.fill(edgeX - half, edgeY - 3, edgeX + half, edgeY + 3, COIN_EDGE_DARK);
            context.fill(edgeX - half + 1, edgeY - 2, edgeX + half - 1, edgeY + 2, COIN_EDGE);
            return;
        }
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate(centerX, centerY, 0.0F);
        matrices.scale(1.0F, Math.abs(squash), 1.0F);
        context.drawTexture(squash > 0 ? HEADS : TAILS, -half, -half, 0.0F, 0.0F, COIN_SIZE, COIN_SIZE, COIN_SIZE,
                COIN_SIZE);
        matrices.pop();
    }

    private void drawHistoryBox(DrawContext context) {
        int left = x + LEFT_X;
        int top = y + HISTORY_Y;
        context.fill(left, top, left + LEFT_WIDTH, top + HISTORY_HEIGHT, BOX_OUTLINE);
        context.fill(left + 1, top + 1, left + LEFT_WIDTH - 1, top + HISTORY_HEIGHT - 1, BOX_FILL);
    }

    /** As duas apostas: a escolhida com borda amarela e a ficha embaixo do desenho. */
    private void drawBets(DrawContext context, int mouseX, int mouseY) {
        int left = x + BETS_X;
        int top = y + BETS_Y;
        context.fill(left, top, left + BETS_WIDTH, top + BETS_HEIGHT, BOX_OUTLINE);
        context.fill(left + 1, top + 1, left + BETS_WIDTH - 1, top + BETS_HEIGHT - 1, BOX_FILL);
        context.fill(left + 1, top + 1, left + BETS_WIDTH - 1, top + HEADER_HEIGHT, HEADER_FILL);
        context.fill(left + 1, top + HEADER_HEIGHT, left + BETS_WIDTH - 1, top + HEADER_HEIGHT + 1, BOX_OUTLINE);
        context.fill(left + COLUMN_WIDTH, top + HEADER_HEIGHT, left + COLUMN_WIDTH + 1, top + BETS_HEIGHT - 1,
                BOX_OUTLINE);

        int hovered = flipping() ? -1 : columnAt(mouseX, mouseY);
        for (int side = 0; side < CoinFlip.SIDES; side++) {
            int columnLeft = left + side * COLUMN_WIDTH + (side == 0 ? 1 : 2);
            int columnRight = left + (side + 1) * COLUMN_WIDTH - (side == 0 ? 0 : 1);
            int columnTop = top + HEADER_HEIGHT + 1;
            int columnBottom = top + BETS_HEIGHT - 1;
            boolean selected = handler.choice() == side;
            if (selected) {
                context.fill(columnLeft, columnTop, columnRight, columnBottom, COLUMN_SELECTED);
                context.drawBorder(columnLeft, columnTop, columnRight - columnLeft, columnBottom - columnTop,
                        SELECTED);
            }
            if (side == hovered) {
                context.fill(columnLeft, columnTop, columnRight, columnBottom, COLUMN_HOVER);
            }
            int centerX = left + side * COLUMN_WIDTH + COLUMN_WIDTH / 2;
            Identifier icon = side == CoinFlip.HEADS ? FACE : CROWN;
            int size = side == CoinFlip.HEADS ? 8 : 16;
            context.drawTexture(icon, centerX - 16, y + ICON_Y, 32, 32, 0.0F, 0.0F, size, size, size, size);
            int spotX = centerX - 9;
            CassinoGui.drawSlot(context, spotX, y + SPOT_Y);
            if (selected) {
                context.drawItem(CassinoGui.CHIP, spotX + 1, y + SPOT_Y + 1);
            }
        }
    }

    /** Os textos, a partir do canto da tela. */
    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        CassinoGui.drawTitle(context, textRenderer, title, backgroundWidth);
        CassinoGui.drawHouse(context, textRenderer, handler.house());

        drawCentered(context, Text.translatable("gui.cassino.coin.result"), LEFT_X + LEFT_WIDTH / 2, 20,
                CassinoGui.TEXT, false);
        Text shown = flipping()
                ? Text.literal("...")
                : Text.translatable("gui.cassino.coin.shown." + Math.max(0, Math.min(2, handler.result())));
        drawCentered(context, shown, LEFT_X + LEFT_WIDTH / 2, RESULT_Y + RESULT_HEIGHT - 11, 0xFFFFFFFF, true);
        drawHistory(context);

        drawCentered(context, Text.translatable("gui.cassino.coin.bets"), BETS_X + BETS_WIDTH / 2, BETS_Y + 2,
                0xFFFFFFFF, true);
        for (int side = 0; side < CoinFlip.SIDES; side++) {
            int centerX = BETS_X + side * COLUMN_WIDTH + COLUMN_WIDTH / 2;
            drawCentered(context, Text.translatable("gui.cassino.coin.side." + side), centerX,
                    BETS_Y + HEADER_HEIGHT + 4, CassinoGui.TEXT, false);
            drawCentered(context, Text.literal("(x" + CoinFlip.MULTIPLIER + ",0)"), centerX,
                    BETS_Y + HEADER_HEIGHT + 14, ODDS_TEXT, false);
        }

        int messageWidth = BETS_X + BETS_WIDTH - LEFT_X;
        Text line = message != null ? message : Text.translatable("gui.cassino.coin.hint");
        int color = message != null ? messageColor : LOSE_TEXT;
        context.drawText(textRenderer, line, LEFT_X + (messageWidth - textRenderer.getWidth(line)) / 2, MESSAGE_Y,
                color, false);

        CassinoGui.drawAmount(context, textRenderer, Text.translatable("gui.cassino.slot.bet", handler.bet()),
                RIGHT_X + 4, 50, INFO_TEXT);
        CassinoGui.drawAmount(context, textRenderer, Text.translatable("gui.cassino.slot.credits", shownCredits()),
                RIGHT_X + 4, 61, INFO_TEXT);
        CassinoGui.drawInventoryLabel(context, textRenderer, CoinFlipScreenHandler.INVENTORY_X,
                CoinFlipScreenHandler.INVENTORY_Y);
    }

    private void drawCentered(DrawContext context, Text text, int centerX, int textY, int color, boolean shadow) {
        context.drawText(textRenderer, text, centerX - textRenderer.getWidth(text) / 2, textY, color, shadow);
    }

    /** "ÚLTIMOS:" e os 2 últimos resultados numa linha (o do lançamento em andamento aparece quando a moeda cai). */
    private void drawHistory(DrawContext context) {
        context.drawText(textRenderer, Text.translatable("gui.cassino.coin.history"), LEFT_X + 4, HISTORY_Y + 3,
                CassinoGui.TEXT, false);
        int[] history = handler.history();
        int textX = LEFT_X + 4;
        int shown = 0;
        for (int i = flipping() ? 1 : 0; i < history.length && shown < 2; i++) {
            int result = history[i];
            if (result < 0 || result > CoinFlip.EDGE) {
                continue;
            }
            if (shown > 0) {
                context.drawText(textRenderer, ", ", textX, HISTORY_Y + 12, CassinoGui.TEXT, false);
                textX += textRenderer.getWidth(", ");
            }
            Text side = Text.translatable("gui.cassino.coin.side." + result);
            context.drawText(textRenderer, side, textX, HISTORY_Y + 12, SIDE_TEXT[result], false);
            textX += textRenderer.getWidth(side);
            shown++;
        }
    }
}
