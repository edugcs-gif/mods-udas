package com.cassino.mod.client;

import com.cassino.mod.Cassino;
import com.cassino.mod.machine.Roulette;
import com.cassino.mod.screen.RouletteScreenHandler;
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
import net.minecraft.util.math.RotationAxis;

/**
 * A tela da roleta: a roda à esquerda (com a faixa das últimas cores que saíram), a mesa com as três cores para
 * apostar (0, VERMELHO e PRETO) no meio, e GIRAR!, a aposta, os créditos, + e -, o espaço das fichas, DEPÓSITO e
 * RETIRADA à direita; o inventário embaixo.
 *
 * <p>Quem gira é o servidor. Quando o resultado chega, a roda gira e desacelera até a casa sorteada parar embaixo da
 * seta, com a bolinha correndo ao contrário; só então aparecem o prêmio e o saldo novo.
 */
public class RouletteScreen extends HandledScreen<RouletteScreenHandler> {
    private static final int BOX_BG = 0xFF8B8B8B;
    private static final int BOX_BG_DARK = 0xFF6A6A6A;
    private static final int WOOD = 0xFF6B4A2B;
    private static final int FELT = 0xFF2E7A3A;
    private static final int GREEN = 0xFF1F8F2F;
    private static final int RED = 0xFFC02020;
    private static final int BLACK = 0xFF1A1A1A;
    private static final int SELECTED = 0xFFF2D43A;
    private static final int SPOT = 0x60000000;
    private static final int INFO_BG = 0xFF3A3A3A;
    private static final int INFO_TEXT = 0xFFE8E8E8;
    private static final int WIN_TEXT = 0xFF1E7A1E;
    private static final int LOSE_TEXT = 0xFF555555;
    private static final int ERROR_TEXT = 0xFFB02020;
    private static final int GOLD_TEXT = 0xFFB07800;
    private static final int[] COLOR_FILL = {GREEN, RED, BLACK};

    // Posições a partir do canto da tela.
    private static final int WHEEL_BOX_X = 8;
    private static final int TOP_Y = 20;
    private static final int TOP_HEIGHT = 92;
    private static final int WHEEL_BOX_WIDTH = 72;
    private static final int WHEEL_CENTER_X = 44;
    private static final int WHEEL_CENTER_Y = 68;
    private static final int WHEEL_SIZE = 64;
    private static final int STRIP_X = 83;
    private static final int TABLE_X = 96;
    private static final int TABLE_WIDTH = 112;
    private static final int BOX_X = 100;
    private static final int BOX_Y = 26;
    private static final int BOX_WIDTH = 34;
    private static final int BOX_HEIGHT = 52;
    private static final int BOX_STEP = 36;
    private static final int MESSAGE_Y = 116;
    private static final int RIGHT_X = 214;
    private static final int RIGHT_WIDTH = 94;

    /** Quanto tempo a roda leva para parar. */
    private static final long SPIN_MS = 3_200;
    /** Voltas inteiras que a roda dá antes de parar. */
    private static final int TURNS = 4;
    private static final float POCKET_DEGREES = 360.0F / Roulette.POCKETS;
    /** Se o servidor não responder ao GIRAR! nesse tempo (sem créditos, por exemplo), o botão volta. */
    private static final long WAIT_MS = 1_500;

    private static final Identifier WHEEL_TEXTURE = Cassino.id("textures/gui/roulette_wheel.png");

    private ColorButton spinButton;
    private boolean initialized;
    private int seenSpins;
    /** A rotação da roda (graus, sentido horário) quando parada, e a do começo e do fim da girada em andamento. */
    private float wheelAngle;
    private float fromAngle;
    private float toAngle;
    private long spinStartedAt = -1;
    private int spinWin;
    private int lastTickPocket;
    private boolean waiting;
    private long waitingSince;
    private Text message;
    private int messageColor;

    public RouletteScreen(RouletteScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        backgroundWidth = RouletteScreenHandler.WIDTH;
        backgroundHeight = RouletteScreenHandler.HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        if (!initialized) {
            seenSpins = handler.spins();
            wheelAngle = restingAngle(handler.pocket());
            initialized = true;
        }
        spinButton = addDrawableChild(new ColorButton(x + RIGHT_X, y + TOP_Y, RIGHT_WIDTH, 22,
                Text.translatable("gui.cassino.slot.spin"), ColorButton.GREEN, 1.5F, this::spin));
        spinButton.setTooltip(Tooltip.of(Text.translatable("gui.cassino.roulette.tip.rules")));
        addDrawableChild(ButtonWidget.builder(Text.literal("+"),
                        button -> click(RouletteScreenHandler.BUTTON_BET_UP))
                .dimensions(x + RIGHT_X, y + 76, 45, 16)
                .tooltip(Tooltip.of(Text.translatable("gui.cassino.slot.tip.bet_up")))
                .build());
        addDrawableChild(ButtonWidget.builder(Text.literal("-"),
                        button -> click(RouletteScreenHandler.BUTTON_BET_DOWN))
                .dimensions(x + RIGHT_X + RIGHT_WIDTH - 45, y + 76, 45, 16)
                .tooltip(Tooltip.of(Text.translatable("gui.cassino.slot.tip.bet_down")))
                .build());
        int depositX = RouletteScreenHandler.COIN_SLOT_X + 21;
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.cassino.slot.deposit"),
                        button -> click(RouletteScreenHandler.BUTTON_DEPOSIT))
                .dimensions(x + depositX, y + RouletteScreenHandler.COIN_SLOT_Y - 1,
                        RIGHT_X + RIGHT_WIDTH - depositX, 20)
                .tooltip(Tooltip.of(Text.translatable("gui.cassino.slot.tip.deposit")))
                .build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.cassino.slot.withdraw"),
                        button -> click(RouletteScreenHandler.BUTTON_WITHDRAW))
                .dimensions(x + RIGHT_X, y + 118, RIGHT_WIDTH, 17)
                .tooltip(Tooltip.of(Text.translatable("gui.cassino.slot.tip.withdraw")))
                .build());
    }

    private void click(int button) {
        if (client != null && client.interactionManager != null) {
            client.interactionManager.clickButton(handler.syncId, button);
        }
    }

    private void spin() {
        if (spinStartedAt >= 0 || waiting) {
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
        click(RouletteScreenHandler.BUTTON_SPIN);
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

    /** Clicar numa das caixas da mesa escolhe a cor da aposta (com a roda parada). */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && spinStartedAt < 0) {
            for (int color = 0; color < Roulette.COLORS; color++) {
                int boxX = x + BOX_X + color * BOX_STEP;
                int boxY = y + BOX_Y;
                if (mouseX >= boxX && mouseX < boxX + BOX_WIDTH && mouseY >= boxY && mouseY < boxY + BOX_HEIGHT) {
                    click(RouletteScreenHandler.BUTTON_CHOOSE + color);
                    play(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 0.6F);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    // ---------------------------------------------------------------- animação

    /** A rotação que deixa a casa {@code pocket} embaixo da seta (no alto da roda). */
    private static float restingAngle(int pocket) {
        return MathHelper.wrapDegrees(-pocket * POCKET_DEGREES);
    }

    private float progress(long now) {
        return MathHelper.clamp((now - spinStartedAt) / (float) SPIN_MS, 0.0F, 1.0F);
    }

    private void update(long now) {
        if (handler.spins() != seenSpins) {
            seenSpins = handler.spins();
            spinStartedAt = now;
            spinWin = handler.lastWin();
            waiting = false;
            message = null;
            // Gira sempre no sentido horário: algumas voltas inteiras e o que falta para a casa sorteada chegar na seta.
            fromAngle = wheelAngle;
            float rest = MathHelper.floorMod(restingAngle(handler.pocket()) - fromAngle, 360.0F);
            toAngle = fromAngle + TURNS * 360.0F + rest;
            lastTickPocket = (int) (fromAngle / POCKET_DEGREES);
        }
        if (waiting && now - waitingSince > WAIT_MS) {
            waiting = false;
        }
        if (spinStartedAt < 0) {
            return;
        }
        float t = progress(now);
        float eased = 1.0F - (1.0F - t) * (1.0F - t) * (1.0F - t);
        wheelAngle = fromAngle + (toAngle - fromAngle) * eased;
        int tickPocket = (int) (wheelAngle / POCKET_DEGREES);
        if (tickPocket != lastTickPocket) {
            lastTickPocket = tickPocket;
            play(SoundEvents.BLOCK_WOODEN_BUTTON_CLICK_ON, 1.8F, 0.15F);
        }
        if (t >= 1.0F) {
            spinStartedAt = -1;
            wheelAngle = MathHelper.wrapDegrees(toAngle);
            showResult();
        }
    }

    private void showResult() {
        int color = Roulette.color(handler.pocket());
        Text name = Text.translatable("gui.cassino.roulette.color." + color);
        if (spinWin > 0) {
            showMessage(Text.translatable("gui.cassino.roulette.result.win", name, spinWin),
                    color == Roulette.GREEN ? GOLD_TEXT : WIN_TEXT);
            play(color == Roulette.GREEN ? SoundEvents.ENTITY_PLAYER_LEVELUP : SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                    1.0F, 0.8F);
        } else {
            showMessage(Text.translatable("gui.cassino.roulette.result.lose", name), LOSE_TEXT);
        }
    }

    /** O saldo na tela: durante a girada, ainda sem o prêmio (ele entra quando a roda para). */
    private int shownCredits() {
        return spinStartedAt >= 0 ? Math.max(0, handler.credits() - spinWin) : handler.credits();
    }

    // ---------------------------------------------------------------- desenho

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        update(System.currentTimeMillis());
        spinButton.active = spinStartedAt < 0 && !waiting;
        renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
        CassinoGui.drawHouseTooltip(context, textRenderer, x, y, mouseX, mouseY, handler.house());
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        CassinoGui.drawPanel(context, x, y, backgroundWidth, backgroundHeight);
        drawWheel(context);
        drawStrip(context);
        drawTable(context);

        context.fill(x + RIGHT_X, y + 46, x + RIGHT_X + RIGHT_WIDTH, y + 72, INFO_BG);
        CassinoGui.drawCoinSlot(context, x + RouletteScreenHandler.COIN_SLOT_X,
                y + RouletteScreenHandler.COIN_SLOT_Y, !handler.getSlot(0).hasStack());
        CassinoGui.drawInventorySlots(context, x + RouletteScreenHandler.INVENTORY_X,
                y + RouletteScreenHandler.INVENTORY_Y, y + RouletteScreenHandler.HOTBAR_Y);
    }

    private void drawWheel(DrawContext context) {
        int left = x + WHEEL_BOX_X;
        int top = y + TOP_Y;
        context.fill(left, top, left + WHEEL_BOX_WIDTH, top + TOP_HEIGHT, BOX_BG_DARK);
        context.fill(left + 1, top + 1, left + WHEEL_BOX_WIDTH - 1, top + TOP_HEIGHT - 1, BOX_BG);

        float centerX = x + WHEEL_CENTER_X;
        float centerY = y + WHEEL_CENTER_Y;
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate(centerX, centerY, 0.0F);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(wheelAngle));
        context.drawTexture(WHEEL_TEXTURE, -WHEEL_SIZE / 2, -WHEEL_SIZE / 2, 0.0F, 0.0F, WHEEL_SIZE, WHEEL_SIZE,
                WHEEL_SIZE, WHEEL_SIZE);
        matrices.pop();

        // A bolinha: corre ao contrário da roda e cai na casa do alto (embaixo da seta) quando ela para.
        float ballAngle = 0.0F;
        float radius = 21.0F;
        if (spinStartedAt >= 0) {
            float remaining = 1.0F - progress(System.currentTimeMillis());
            ballAngle = -900.0F * remaining * remaining;
            radius = 21.0F + 6.0F * remaining;
        }
        float radians = ballAngle * MathHelper.RADIANS_PER_DEGREE;
        int ballX = Math.round(centerX + MathHelper.sin(radians) * radius);
        int ballY = Math.round(centerY - MathHelper.cos(radians) * radius);
        context.fill(ballX - 2, ballY - 1, ballX + 2, ballY + 1, 0xFFFFFFFF);
        context.fill(ballX - 1, ballY - 2, ballX + 1, ballY + 2, 0xFFFFFFFF);

        // A seta em cima da roda.
        int arrowX = x + WHEEL_CENTER_X;
        int arrowY = y + WHEEL_CENTER_Y - WHEEL_SIZE / 2 - 5;
        for (int row = 0; row < 4; row++) {
            context.fill(arrowX - 3 + row, arrowY + row, arrowX + 4 - row, arrowY + row + 1, SELECTED);
        }
    }

    /** A faixa com as últimas cores que saíram (a mais nova em cima; a da girada em andamento só aparece no fim). */
    private void drawStrip(DrawContext context) {
        int left = x + STRIP_X;
        int top = y + TOP_Y;
        context.fill(left, top, left + 10, top + TOP_HEIGHT, BOX_BG_DARK);
        int[] history = handler.history();
        int skip = spinStartedAt >= 0 ? 1 : 0;
        for (int i = 0; i + skip < history.length; i++) {
            int color = history[i + skip];
            if (color < 0 || color >= Roulette.COLORS) {
                continue;
            }
            int cellY = top + 2 + i * 8;
            context.fill(left + 1, cellY, left + 9, cellY + 7, COLOR_FILL[color]);
        }
    }

    /** A mesa de feltro com as três caixas de cor (a escolhida com borda amarela e a ficha em cima). */
    private void drawTable(DrawContext context) {
        int left = x + TABLE_X;
        int top = y + TOP_Y;
        context.fill(left, top, left + TABLE_WIDTH, top + TOP_HEIGHT, WOOD);
        context.fill(left + 2, top + 2, left + TABLE_WIDTH - 2, top + TOP_HEIGHT - 2, FELT);
        for (int color = 0; color < Roulette.COLORS; color++) {
            int boxX = x + BOX_X + color * BOX_STEP;
            int boxY = y + BOX_Y;
            boolean selected = handler.choice() == color;
            context.fill(boxX, boxY, boxX + BOX_WIDTH, boxY + BOX_HEIGHT, selected ? SELECTED : 0xFF101010);
            context.fill(boxX + 1, boxY + 1, boxX + BOX_WIDTH - 1, boxY + BOX_HEIGHT - 1, COLOR_FILL[color]);
            int spotX = boxX + (BOX_WIDTH - 18) / 2;
            int spotY = boxY + 16;
            context.fill(spotX, spotY, spotX + 18, spotY + 18, SPOT);
            if (selected) {
                context.drawItem(CassinoGui.CHIP, spotX + 1, spotY + 1);
            }
        }
    }

    /** Os textos, a partir do canto da tela. */
    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        CassinoGui.drawTitle(context, textRenderer, title, backgroundWidth);
        CassinoGui.drawHouse(context, textRenderer, handler.house());

        for (int color = 0; color < Roulette.COLORS; color++) {
            int boxX = BOX_X + color * BOX_STEP;
            Text label = Text.translatable("gui.cassino.roulette.box." + color);
            drawFitted(context, label, boxX + BOX_WIDTH / 2, BOX_Y + 5, BOX_WIDTH - 4);
            Text pays = Text.literal(Roulette.multiplier(color) + "x");
            context.drawText(textRenderer, pays, boxX + (BOX_WIDTH - textRenderer.getWidth(pays)) / 2, BOX_Y + 39,
                    0xFFFFFFFF, true);
        }
        Text rule = Text.translatable("gui.cassino.roulette.rule");
        drawFitted(context, rule, TABLE_X + TABLE_WIDTH / 2, BOX_Y + BOX_HEIGHT + 10, TABLE_WIDTH - 8);

        if (message != null) {
            int width = RIGHT_X - 6 - WHEEL_BOX_X;
            context.drawText(textRenderer, message, WHEEL_BOX_X + (width - textRenderer.getWidth(message)) / 2,
                    MESSAGE_Y, messageColor, false);
        }

        CassinoGui.drawAmount(context, textRenderer, Text.translatable("gui.cassino.slot.bet", handler.bet()),
                RIGHT_X + 4, 50, INFO_TEXT);
        CassinoGui.drawAmount(context, textRenderer, Text.translatable("gui.cassino.slot.credits", shownCredits()),
                RIGHT_X + 4, 61, INFO_TEXT);
        CassinoGui.drawInventoryLabel(context, textRenderer, RouletteScreenHandler.INVENTORY_X,
                RouletteScreenHandler.INVENTORY_Y);
    }

    /** Texto branco centralizado em {@code centerX}, diminuído se não couber em {@code maxWidth}. */
    private void drawFitted(DrawContext context, Text text, int centerX, int textY, int maxWidth) {
        int width = textRenderer.getWidth(text);
        float scale = Math.min(1.0F, maxWidth / (float) width);
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate(centerX, textY, 0.0F);
        matrices.scale(scale, scale, 1.0F);
        context.drawText(textRenderer, text, -width / 2, 0, 0xFFFFFFFF, true);
        matrices.pop();
    }
}
