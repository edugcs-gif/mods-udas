package com.cassino.mod.client;

import com.cassino.mod.machine.Plinko;
import com.cassino.mod.screen.PlinkoScreenHandler;
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
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

/**
 * A tela do Plinko de Slime: o tabuleiro à esquerda (o funil em cima, a pirâmide de pinos e os buracos com os
 * multiplicadores embaixo) e LANÇAR!, a aposta, os créditos, + e -, o espaço das fichas, DEPÓSITO e RETIRADA à
 * direita; o inventário embaixo.
 *
 * <p>Quem sorteia o caminho é o servidor. Quando ele chega, a bola de slime sai do funil e quica de pino em pino (para
 * a esquerda ou para a direita, como o caminho manda) até cair no buraco; só então aparecem o prêmio e o saldo novo.
 */
public class PlinkoScreen extends HandledScreen<PlinkoScreenHandler> {
    private static final ItemStack BALL = new ItemStack(Items.SLIME_BALL);

    private static final int BOARD_BG = 0xFF8E8E8E;
    private static final int BOARD_OUTLINE = 0xFF555555;
    private static final int WALL = 0xFF3A3A3A;
    private static final int FUNNEL = 0xFF4A4A4A;
    private static final int FUNNEL_LIGHT = 0xFF8A8A8A;
    private static final int PEG = 0xFF3C3C3C;
    private static final int PEG_LIGHT = 0xFFC8C8C8;
    private static final int LANDED = 0xFFFFFFFF;
    private static final int INFO_BG = 0xFF3A3A3A;
    private static final int INFO_TEXT = 0xFFE8E8E8;
    private static final int WIN_TEXT = 0xFF1E7A1E;
    private static final int LOSE_TEXT = 0xFF555555;
    private static final int ERROR_TEXT = 0xFFB02020;
    private static final int GOLD_TEXT = 0xFFB07800;

    // Posições a partir do canto da tela.
    private static final int BOARD_X = 8;
    private static final int BOARD_Y = 20;
    private static final int BOARD_WIDTH = 200;
    private static final int BOARD_HEIGHT = 104;
    private static final int CENTER_X = BOARD_X + BOARD_WIDTH / 2;
    private static final int START_Y = 27;
    private static final int PEG_Y = 40;
    private static final int PEG_STEP_Y = 12;
    /** Meia distância entre dois pinos vizinhos: o quanto a bola anda para o lado a cada fileira. */
    private static final int HALF = 13;
    private static final int BUCKET_Y = 108;
    private static final int BUCKET_HEIGHT = 14;
    private static final int BUCKET_WIDTH = 2 * HALF;
    private static final int BUCKETS_X = CENTER_X - Plinko.BUCKETS * HALF;
    /** A bola em cima de um pino: o centro dela fica esse tanto acima do centro do pino. */
    private static final int REST = 7;
    private static final int MESSAGE_Y = 127;
    private static final int RIGHT_X = 214;
    private static final int RIGHT_WIDTH = 94;

    /** Quanto tempo a bola leva do funil ao primeiro pino, de um pino ao outro e do último pino ao buraco. */
    private static final long FIRST_MS = 180;
    private static final long BOUNCE_MS = 200;
    private static final long LAST_MS = 220;
    private static final long TOTAL_MS = FIRST_MS + (Plinko.ROWS - 1) * BOUNCE_MS + LAST_MS;
    /** Se o servidor não responder ao LANÇAR! nesse tempo (sem créditos, por exemplo), o botão volta. */
    private static final long WAIT_MS = 1_500;

    private ColorButton dropButton;
    private boolean initialized;
    private int seenDrops;
    private long dropStartedAt = -1;
    private int dropWin;
    private int lastSegment;
    private boolean waiting;
    private long waitingSince;
    private Text message;
    private int messageColor;

    public PlinkoScreen(PlinkoScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        backgroundWidth = PlinkoScreenHandler.WIDTH;
        backgroundHeight = PlinkoScreenHandler.HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        if (!initialized) {
            seenDrops = handler.drops();
            initialized = true;
        }
        dropButton = addDrawableChild(new ColorButton(x + RIGHT_X, y + 20, RIGHT_WIDTH, 22,
                Text.translatable("gui.cassino.plinko.drop"), ColorButton.GREEN, 1.5F, this::drop));
        dropButton.setTooltip(Tooltip.of(Text.translatable("gui.cassino.plinko.tip.rules")));
        addDrawableChild(ButtonWidget.builder(Text.literal("+"),
                        button -> click(PlinkoScreenHandler.BUTTON_BET_UP))
                .dimensions(x + RIGHT_X, y + 76, 45, 16)
                .tooltip(Tooltip.of(Text.translatable("gui.cassino.slot.tip.bet_up")))
                .build());
        addDrawableChild(ButtonWidget.builder(Text.literal("-"),
                        button -> click(PlinkoScreenHandler.BUTTON_BET_DOWN))
                .dimensions(x + RIGHT_X + RIGHT_WIDTH - 45, y + 76, 45, 16)
                .tooltip(Tooltip.of(Text.translatable("gui.cassino.slot.tip.bet_down")))
                .build());
        int depositX = PlinkoScreenHandler.COIN_SLOT_X + 21;
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.cassino.slot.deposit"),
                        button -> click(PlinkoScreenHandler.BUTTON_DEPOSIT))
                .dimensions(x + depositX, y + PlinkoScreenHandler.COIN_SLOT_Y - 1,
                        RIGHT_X + RIGHT_WIDTH - depositX, 20)
                .tooltip(Tooltip.of(Text.translatable("gui.cassino.slot.tip.deposit")))
                .build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.cassino.slot.withdraw"),
                        button -> click(PlinkoScreenHandler.BUTTON_WITHDRAW))
                .dimensions(x + RIGHT_X, y + 118, RIGHT_WIDTH, 17)
                .tooltip(Tooltip.of(Text.translatable("gui.cassino.slot.tip.withdraw")))
                .build());
    }

    private void click(int button) {
        if (client != null && client.interactionManager != null) {
            client.interactionManager.clickButton(handler.syncId, button);
        }
    }

    private void drop() {
        if (dropping() || waiting) {
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
        click(PlinkoScreenHandler.BUTTON_DROP);
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

    // ---------------------------------------------------------------- animação

    private boolean dropping() {
        return dropStartedAt >= 0;
    }

    private void update(long now) {
        if (handler.drops() != seenDrops) {
            seenDrops = handler.drops();
            dropStartedAt = now;
            dropWin = handler.lastWin();
            lastSegment = 0;
            waiting = false;
            message = null;
        }
        if (waiting && now - waitingSince > WAIT_MS) {
            waiting = false;
        }
        if (!dropping()) {
            return;
        }
        long elapsed = now - dropStartedAt;
        int segment = segmentAt(elapsed);
        while (lastSegment < segment) {
            // A bola chegou num pino (ou no buraco): o barulhinho de slime, mais agudo a cada fileira.
            lastSegment++;
            if (lastSegment <= Plinko.ROWS) {
                play(SoundEvents.ENTITY_SLIME_SQUISH_SMALL, 1.0F + lastSegment * 0.08F, 0.5F);
            }
        }
        if (elapsed >= TOTAL_MS) {
            dropStartedAt = -1;
            showResult();
        }
    }

    /** Em que trecho a bola está: 0 = do funil ao 1º pino, 1 a 5 = entre pinos, 6 = do último pino ao buraco. */
    private static int segmentAt(long elapsed) {
        if (elapsed < FIRST_MS) {
            return 0;
        }
        return (int) Math.min(Plinko.ROWS, 1 + (elapsed - FIRST_MS) / BOUNCE_MS);
    }

    private void showResult() {
        int bucket = Plinko.bucket(handler.path());
        int multiplier10 = Plinko.multiplier10(bucket);
        String label = multiplierLabel(multiplier10);
        play(SoundEvents.ENTITY_SLIME_SQUISH, 1.0F, 0.6F);
        if (multiplier10 >= 100) {
            showMessage(Text.translatable("gui.cassino.plinko.result.win", label, dropWin), GOLD_TEXT);
            play(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0F, 0.8F);
        } else if (multiplier10 > 10) {
            showMessage(Text.translatable("gui.cassino.plinko.result.win", label, dropWin), WIN_TEXT);
            play(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 0.8F);
        } else if (dropWin > 0) {
            showMessage(Text.translatable("gui.cassino.plinko.result.back", label, dropWin), LOSE_TEXT);
        } else {
            showMessage(Text.translatable("gui.cassino.plinko.result.lose", label), LOSE_TEXT);
        }
    }

    /** "10x", "2x", "0,5x"... */
    private static String multiplierLabel(int multiplier10) {
        return multiplier10 % 10 == 0
                ? multiplier10 / 10 + "x"
                : multiplier10 / 10 + "," + multiplier10 % 10 + "x";
    }

    /** O saldo na tela: com a bola caindo, ainda sem o prêmio (ele entra quando ela chega no buraco). */
    private int shownCredits() {
        return dropping() ? Math.max(0, handler.credits() - dropWin) : handler.credits();
    }

    /** Onde a bola está antes de bater na fileira {@code row}: o x do pino (a partir do canto da tela). */
    private static int pegColumnX(int path, int row) {
        int rights = Integer.bitCount(path & ((1 << row) - 1));
        return CENTER_X + (2 * rights - row) * HALF;
    }

    private static int pegY(int row) {
        return PEG_Y + row * PEG_STEP_Y;
    }

    /** O centro da bola agora (a partir do canto da tela), seguindo o caminho. */
    private float[] ballPosition(long elapsed) {
        int path = handler.path();
        int segment = segmentAt(elapsed);
        if (segment == 0) {
            float t = elapsed / (float) FIRST_MS;
            return new float[] {CENTER_X, MathHelper.lerp(t * t, START_Y, pegY(0) - REST)};
        }
        long start = FIRST_MS + (segment - 1) * BOUNCE_MS;
        long length = segment == Plinko.ROWS ? LAST_MS : BOUNCE_MS;
        float t = MathHelper.clamp((elapsed - start) / (float) length, 0.0F, 1.0F);
        float fromX = pegColumnX(path, segment - 1);
        float fromY = pegY(segment - 1) - REST;
        float toX;
        float toY;
        if (segment == Plinko.ROWS) {
            toX = BUCKETS_X + Plinko.bucket(path) * BUCKET_WIDTH + HALF;
            toY = BUCKET_Y + BUCKET_HEIGHT / 2.0F;
        } else {
            toX = pegColumnX(path, segment);
            toY = pegY(segment) - REST;
        }
        // Para o lado com uma quicada: sobe um pouco e cai no próximo pino.
        float hop = 5.0F * 4.0F * t * (1.0F - t);
        return new float[] {MathHelper.lerp(t, fromX, toX), MathHelper.lerp(t * t, fromY, toY) - hop};
    }

    // ---------------------------------------------------------------- desenho

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        update(System.currentTimeMillis());
        dropButton.active = !dropping() && !waiting;
        renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
        CassinoGui.drawHouseTooltip(context, textRenderer, x, y, mouseX, mouseY, handler.house());
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        CassinoGui.drawPanel(context, x, y, backgroundWidth, backgroundHeight);
        drawBoard(context);

        context.fill(x + RIGHT_X, y + 46, x + RIGHT_X + RIGHT_WIDTH, y + 72, INFO_BG);
        CassinoGui.drawCoinSlot(context, x + PlinkoScreenHandler.COIN_SLOT_X, y + PlinkoScreenHandler.COIN_SLOT_Y,
                !handler.getSlot(0).hasStack());
        CassinoGui.drawInventorySlots(context, x + PlinkoScreenHandler.INVENTORY_X,
                y + PlinkoScreenHandler.INVENTORY_Y, y + PlinkoScreenHandler.HOTBAR_Y);
    }

    /** O tabuleiro: as paredes, o funil, os pinos, os buracos e a bola. */
    private void drawBoard(DrawContext context) {
        int left = x + BOARD_X;
        int top = y + BOARD_Y;
        context.fill(left, top, left + BOARD_WIDTH, top + BOARD_HEIGHT, BOARD_OUTLINE);
        context.fill(left + 1, top + 1, left + BOARD_WIDTH - 1, top + BOARD_HEIGHT - 1, BOARD_BG);

        // As paredes da pirâmide, do funil até as pontas dos buracos.
        drawLine(context, x + CENTER_X - 14, y + 32, x + BUCKETS_X, y + BUCKET_Y - 2);
        drawLine(context, x + CENTER_X + 13, y + 32, x + BUCKETS_X + Plinko.BUCKETS * BUCKET_WIDTH - 1,
                y + BUCKET_Y - 2);

        // O funil.
        for (int row = 0; row < 8; row++) {
            int halfWidth = 14 - row;
            context.fill(x + CENTER_X - halfWidth, y + 22 + row, x + CENTER_X + halfWidth, y + 23 + row, FUNNEL);
        }
        context.fill(x + CENTER_X - 14, y + 22, x + CENTER_X + 14, y + 23, FUNNEL_LIGHT);
        context.fill(x + CENTER_X - 5, y + 30, x + CENTER_X + 5, y + 34, FUNNEL);

        // Os pinos: a fileira r tem r + 1 pinos.
        for (int row = 0; row < Plinko.ROWS; row++) {
            for (int peg = 0; peg <= row; peg++) {
                drawPeg(context, x + CENTER_X + (2 * peg - row) * HALF, y + pegY(row));
            }
        }

        // Os buracos, com a cor do quanto pagam; o da última bola com a borda branca.
        int landed = dropping() ? -1 : Plinko.bucket(handler.path());
        for (int bucket = 0; bucket < Plinko.BUCKETS; bucket++) {
            int bucketX = x + BUCKETS_X + bucket * BUCKET_WIDTH;
            int bucketY = y + BUCKET_Y;
            int color = bucketColor(Plinko.multiplier10(bucket));
            context.fill(bucketX, bucketY, bucketX + BUCKET_WIDTH, bucketY + BUCKET_HEIGHT, 0xFF202020);
            context.fill(bucketX + 1, bucketY + 1, bucketX + BUCKET_WIDTH - 1, bucketY + BUCKET_HEIGHT - 1, color);
            if (bucket == landed && handler.drops() > 0) {
                context.drawBorder(bucketX, bucketY, BUCKET_WIDTH, BUCKET_HEIGHT, LANDED);
            }
        }

        if (dropping()) {
            float[] ball = ballPosition(System.currentTimeMillis() - dropStartedAt);
            MatrixStack matrices = context.getMatrices();
            matrices.push();
            matrices.translate(x + ball[0] - 6.0F, y + ball[1] - 6.0F, 0.0F);
            matrices.scale(0.75F, 0.75F, 1.0F);
            context.drawItem(BALL, 0, 0);
            matrices.pop();
        }
    }

    private static int bucketColor(int multiplier10) {
        if (multiplier10 >= 100) {
            return 0xFFB02020;
        }
        if (multiplier10 > 10) {
            return 0xFFD06A10;
        }
        if (multiplier10 >= 5) {
            return 0xFFC8A818;
        }
        return 0xFF7A7A7A;
    }

    /** Um pino redondinho de 5×5, com um brilho em cima. */
    private static void drawPeg(DrawContext context, int centerX, int centerY) {
        context.fill(centerX - 2, centerY - 1, centerX + 3, centerY + 2, PEG);
        context.fill(centerX - 1, centerY - 2, centerX + 2, centerY + 3, PEG);
        context.fill(centerX - 1, centerY - 1, centerX, centerY, PEG_LIGHT);
    }

    /** Uma linha de 2 pixels de largura de (x0, y0) até (x1, y1), descendo. */
    private static void drawLine(DrawContext context, int x0, int y0, int x1, int y1) {
        for (int lineY = y0; lineY <= y1; lineY++) {
            int lineX = x0 + Math.round((x1 - x0) * (lineY - y0) / (float) (y1 - y0));
            context.fill(lineX, lineY, lineX + 2, lineY + 1, WALL);
        }
    }

    /** Os textos, a partir do canto da tela. */
    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        CassinoGui.drawTitle(context, textRenderer, title, backgroundWidth);
        CassinoGui.drawHouse(context, textRenderer, handler.house());

        for (int bucket = 0; bucket < Plinko.BUCKETS; bucket++) {
            String label = multiplierLabel(Plinko.multiplier10(bucket));
            int centerX = BUCKETS_X + bucket * BUCKET_WIDTH + HALF;
            context.drawText(textRenderer, label, centerX - textRenderer.getWidth(label) / 2, BUCKET_Y + 3,
                    0xFFFFFFFF, true);
        }

        Text line = message != null ? message : Text.translatable("gui.cassino.plinko.hint");
        int color = message != null ? messageColor : LOSE_TEXT;
        context.drawText(textRenderer, line, BOARD_X + (BOARD_WIDTH - textRenderer.getWidth(line)) / 2, MESSAGE_Y,
                color, false);

        CassinoGui.drawAmount(context, textRenderer, Text.translatable("gui.cassino.slot.bet", handler.bet()),
                RIGHT_X + 4, 50, INFO_TEXT);
        CassinoGui.drawAmount(context, textRenderer, Text.translatable("gui.cassino.slot.credits", shownCredits()),
                RIGHT_X + 4, 61, INFO_TEXT);
        CassinoGui.drawInventoryLabel(context, textRenderer, PlinkoScreenHandler.INVENTORY_X,
                PlinkoScreenHandler.INVENTORY_Y);
    }
}
