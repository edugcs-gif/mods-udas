package com.cassino.mod.client;

import com.cassino.mod.Cassino;
import com.cassino.mod.machine.SlotMachine;
import com.cassino.mod.screen.SlotMachineScreenHandler;
import java.util.Random;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

/**
 * A tela do caça-níquel: os 5 rolos à esquerda (a linha do meio, em amarelo, é a que paga), o botão GIRAR!, a
 * aposta, os créditos do jogador, DEPÓSITO (as fichas do espaço ao lado viram créditos) e RETIRADA (saca os
 * créditos em fichas), e o inventário embaixo.
 *
 * <p>Quem sorteia é o servidor. Quando o resultado chega, a tela gira os rolos e os para um a um, da esquerda
 * para a direita; só então mostra o prêmio e o saldo novo.
 */
public class SlotMachineScreen extends HandledScreen<SlotMachineScreenHandler> {
    private static final int FRAME = 0xFF3A3A3A;
    private static final int FRAME_LIGHT = 0xFF5C5C5C;
    private static final int CELL_FILL = 0xFF8B8B8B;
    private static final int CELL_LIGHT = 0xFFA9A9A9;
    private static final int CELL_DARK = 0xFF6A6A6A;
    private static final int PAYLINE = 0xFFF2D43A;
    private static final int WIN_TEXT = 0xFF1E7A1E;
    private static final int JACKPOT_TEXT = 0xFFC00000;
    private static final int LOSE_TEXT = 0xFF555555;
    private static final int ERROR_TEXT = 0xFFB02020;
    private static final int POOL_TEXT = 0xFFB07800;

    // Posições a partir do canto da tela (as do espaço das fichas e do inventário estão no SlotMachineScreenHandler).
    private static final int REELS_X = 8;
    private static final int REELS_Y = 22;
    private static final int REELS_WIDTH = 138;
    private static final int REELS_HEIGHT = 82;
    private static final int CELL_X = 11;
    private static final int CELL_Y = 25;
    private static final int CELL = 24;
    private static final int CELL_STEP_X = 27;
    private static final int CELL_STEP_Y = 26;
    private static final int RIGHT_X = 154;
    private static final int RIGHT_WIDTH = 114;
    private static final int BET_Y = 52;
    private static final int CREDITS_Y = 70;
    private static final int WITHDRAW_Y = 105;
    private static final int MESSAGE_Y = 108;

    /** O primeiro rolo para depois disso; cada um dos outros, {@link #STOP_STEP_MS} depois do anterior. */
    private static final long FIRST_STOP_MS = 700;
    private static final long STOP_STEP_MS = 280;
    /** Velocidade dos rolos girando, em símbolos por milissegundo. */
    private static final double SCROLL_SPEED = 0.016;
    /** Se o servidor não responder ao GIRAR! nesse tempo (sem créditos, por exemplo), o botão volta. */
    private static final long WAIT_MS = 1_500;
    /** Quanto tempo um aviso fica embaixo dos rolos antes de voltar a mostrar o acumulado do jackpot. */
    private static final long MESSAGE_MS = 4_000;

    private static final Identifier SEVEN_TEXTURE = Cassino.id("textures/gui/symbol_seven.png");
    private static final Identifier HEART_TEXTURE = Cassino.id("textures/gui/symbol_heart.png");
    /** O desenho de cada símbolo: um item do jogo, ou {@code null} para os desenhados pelo mod (7 e coração). */
    private static final ItemStack[] SYMBOL_ITEMS = {
            null, new ItemStack(Items.NETHER_STAR), new ItemStack(Items.DIAMOND), null, new ItemStack(Items.EMERALD),
            new ItemStack(Items.DIAMOND_SHOVEL)
    };

    /** O que passa em cada rolo enquanto ele gira (só enfeite; o resultado vem do servidor). */
    private final int[][] strips = new int[SlotMachine.REELS][24];

    private ColorButton spinButton;
    private boolean initialized;
    private int seenSpins;
    /** Quando começou a girada que está sendo mostrada ({@code -1} = rolos parados). */
    private long spinStartedAt = -1;
    private int[] spinCells;
    private int spinWin;
    private int stoppedReels = SlotMachine.REELS;
    private boolean waiting;
    private long waitingSince;
    /** Quantos símbolos seguidos ganharam na última girada mostrada (para piscar), ou 0. */
    private int winningRun;
    private Text message;
    private int messageColor;
    private long messageUntil;

    public SlotMachineScreen(SlotMachineScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        backgroundWidth = SlotMachineScreenHandler.WIDTH;
        backgroundHeight = SlotMachineScreenHandler.HEIGHT;
        Random random = new Random(1234);
        for (int[] strip : strips) {
            for (int i = 0; i < strip.length; i++) {
                strip[i] = random.nextInt(SlotMachine.SYMBOLS);
            }
        }
    }

    @Override
    protected void init() {
        super.init();
        if (!initialized) {
            // Abriu agora: não anima o que já estava nos rolos.
            seenSpins = handler.spins();
            initialized = true;
        }
        spinButton = addDrawableChild(new ColorButton(x + RIGHT_X, y + REELS_Y, RIGHT_WIDTH, 22,
                Text.translatable("gui.cassino.slot.spin"), ColorButton.GREEN, 1.5F, this::spin));
        spinButton.setTooltip(Tooltip.of(payTable()));

        // + e - na linha da aposta; o espaço das fichas, DEPÓSITO e, embaixo, RETIRADA.
        addDrawableChild(ButtonWidget.builder(Text.literal("+"),
                        button -> click(SlotMachineScreenHandler.BUTTON_BET_UP))
                .dimensions(x + RIGHT_X + RIGHT_WIDTH - 42, y + BET_Y - 4, 20, 16)
                .tooltip(Tooltip.of(Text.translatable("gui.cassino.slot.tip.bet_up")))
                .build());
        addDrawableChild(ButtonWidget.builder(Text.literal("-"),
                        button -> click(SlotMachineScreenHandler.BUTTON_BET_DOWN))
                .dimensions(x + RIGHT_X + RIGHT_WIDTH - 20, y + BET_Y - 4, 20, 16)
                .tooltip(Tooltip.of(Text.translatable("gui.cassino.slot.tip.bet_down")))
                .build());
        int depositX = SlotMachineScreenHandler.COIN_SLOT_X + 21;
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.cassino.slot.deposit"),
                        button -> click(SlotMachineScreenHandler.BUTTON_DEPOSIT))
                .dimensions(x + depositX, y + SlotMachineScreenHandler.COIN_SLOT_Y - 1,
                        RIGHT_X + RIGHT_WIDTH - depositX, 20)
                .tooltip(Tooltip.of(Text.translatable("gui.cassino.slot.tip.deposit")))
                .build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.cassino.slot.withdraw"),
                        button -> click(SlotMachineScreenHandler.BUTTON_WITHDRAW))
                .dimensions(x + RIGHT_X, y + WITHDRAW_Y, RIGHT_WIDTH, 20)
                .tooltip(Tooltip.of(Text.translatable("gui.cassino.slot.tip.withdraw")))
                .build());
    }

    /** A tabela de prêmios (aparece ao passar o mouse no GIRAR!). */
    private static Text payTable() {
        MutableText text = Text.translatable("gui.cassino.slot.paytable");
        for (int symbol = 0; symbol < SlotMachine.SYMBOLS; symbol++) {
            int[] pay = SlotMachine.PAYOUT[symbol];
            text.append("\n").append(Text.translatable("gui.cassino.slot.paytable.line",
                    Text.translatable("gui.cassino.slot.symbol." + symbol), pay[0], pay[1], pay[2]));
        }
        text.append("\n").append(Text.translatable("gui.cassino.slot.paytable.jackpot",
                (int) Math.round(SlotMachine.JACKPOT_SHARE * 100), SlotMachine.maxBet()));
        return text;
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
        if (handler.credits() + handler.coinsWaiting() < handler.bet()) {
            showMessage(Text.translatable("gui.cassino.slot.no_credits"), ERROR_TEXT);
            return;
        }
        waiting = true;
        waitingSince = System.currentTimeMillis();
        message = null;
        winningRun = 0;
        click(SlotMachineScreenHandler.BUTTON_SPIN);
    }

    private void showMessage(Text text, int color) {
        message = text;
        messageColor = color;
        messageUntil = System.currentTimeMillis() + MESSAGE_MS;
    }

    private void play(net.minecraft.sound.SoundEvent sound, float pitch, float volume) {
        if (client != null) {
            client.getSoundManager().play(PositionedSoundInstance.master(sound, pitch, volume));
        }
    }

    // ---------------------------------------------------------------- animação

    private long stopTime(int reel) {
        return FIRST_STOP_MS + reel * STOP_STEP_MS;
    }

    /** Começa a animação quando chega uma girada nova, para os rolos na hora certa e mostra o prêmio no fim. */
    private void updateSpin(long now) {
        if (handler.spins() != seenSpins) {
            seenSpins = handler.spins();
            spinStartedAt = now;
            spinCells = handler.cells().clone();
            spinWin = handler.lastWin();
            stoppedReels = 0;
            waiting = false;
            message = null;
            winningRun = 0;
        }
        if (waiting && now - waitingSince > WAIT_MS) {
            waiting = false;
        }
        if (spinStartedAt < 0) {
            return;
        }
        long elapsed = now - spinStartedAt;
        while (stoppedReels < SlotMachine.REELS && elapsed >= stopTime(stoppedReels)) {
            stoppedReels++;
            play(SoundEvents.BLOCK_LEVER_CLICK, 1.2F + stoppedReels * 0.1F, 0.5F);
        }
        if (stoppedReels == SlotMachine.REELS) {
            spinStartedAt = -1;
            if (spinWin > 0) {
                winningRun = SlotMachine.runLength(spinCells);
                boolean jackpot = SlotMachine.isJackpot(spinCells);
                showMessage(Text.translatable(jackpot ? "gui.cassino.slot.jackpot" : "gui.cassino.slot.win", spinWin),
                        jackpot ? JACKPOT_TEXT : WIN_TEXT);
                if (jackpot) {
                    play(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
                } else {
                    play(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 0.8F);
                }
            } else {
                showMessage(Text.translatable("gui.cassino.slot.lose"), LOSE_TEXT);
            }
        }
    }

    /** O saldo na tela: durante a animação, ainda sem o prêmio (ele entra quando os rolos param). */
    private int shownCredits() {
        return spinStartedAt >= 0 ? Math.max(0, handler.credits() - spinWin) : handler.credits();
    }

    // ---------------------------------------------------------------- desenho

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        long now = System.currentTimeMillis();
        updateSpin(now);
        spinButton.active = spinStartedAt < 0 && !waiting;
        renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
        CassinoGui.drawHouseTooltip(context, textRenderer, x, y, mouseX, mouseY, handler.house());
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        CassinoGui.drawPanel(context, x, y, backgroundWidth, backgroundHeight);

        drawReels(context);

        CassinoGui.drawCoinSlot(context, x + SlotMachineScreenHandler.COIN_SLOT_X,
                y + SlotMachineScreenHandler.COIN_SLOT_Y, !handler.getSlot(0).hasStack());
        CassinoGui.drawInventorySlots(context, x + SlotMachineScreenHandler.INVENTORY_X,
                y + SlotMachineScreenHandler.INVENTORY_Y, y + SlotMachineScreenHandler.HOTBAR_Y);
    }

    private void drawReels(DrawContext context) {
        int left = x + REELS_X;
        int top = y + REELS_Y;
        context.fill(left, top, left + REELS_WIDTH, top + REELS_HEIGHT, FRAME);
        context.drawBorder(left, top, REELS_WIDTH, REELS_HEIGHT, FRAME_LIGHT);

        long elapsed = spinStartedAt >= 0 ? System.currentTimeMillis() - spinStartedAt : 0;
        int[] cells = spinStartedAt >= 0 ? spinCells : handler.cells();
        // Os símbolos vencedores piscam devagar.
        int glow = 0x50 + (int) (0x40 * (1 + MathHelper.sin(System.currentTimeMillis() / 150.0F)) / 2);

        for (int reel = 0; reel < SlotMachine.REELS; reel++) {
            int cellX = x + CELL_X + reel * CELL_STEP_X;
            for (int row = 0; row < SlotMachine.ROWS; row++) {
                int cellY = y + CELL_Y + row * CELL_STEP_Y;
                context.fill(cellX, cellY, cellX + CELL, cellY + CELL, CELL_FILL);
                context.fill(cellX, cellY, cellX + CELL, cellY + 1, CELL_LIGHT);
                context.fill(cellX, cellY, cellX + 1, cellY + CELL, CELL_LIGHT);
                context.fill(cellX, cellY + CELL - 1, cellX + CELL, cellY + CELL, CELL_DARK);
                context.fill(cellX + CELL - 1, cellY, cellX + CELL, cellY + CELL, CELL_DARK);
                if (row == SlotMachine.PAYLINE_ROW) {
                    if (reel < winningRun) {
                        context.fill(cellX + 1, cellY + 1, cellX + CELL - 1, cellY + CELL - 1,
                                (glow << 24) | (PAYLINE & 0xFFFFFF));
                    }
                    context.drawBorder(cellX - 1, cellY - 1, CELL + 2, CELL + 2, PAYLINE);
                }
            }

            int columnTop = y + CELL_Y;
            int columnBottom = columnTop + (SlotMachine.ROWS - 1) * CELL_STEP_Y + CELL;
            context.enableScissor(cellX, columnTop, cellX + CELL, columnBottom);
            if (spinStartedAt >= 0 && reel >= stoppedReels) {
                // Girando: os símbolos da fita descem e somem embaixo.
                double position = elapsed * SCROLL_SPEED + reel * 0.37;
                int base = MathHelper.floor(position);
                float offset = (float) (position - base) * CELL_STEP_Y;
                int[] strip = strips[reel];
                for (int row = -1; row < SlotMachine.ROWS; row++) {
                    int symbol = strip[Math.floorMod(row - base, strip.length)];
                    drawSymbol(context, symbol, cellX + 2, columnTop + row * CELL_STEP_Y + offset + 2);
                }
            } else {
                for (int row = 0; row < SlotMachine.ROWS; row++) {
                    drawSymbol(context, cells[SlotMachine.cell(reel, row)], cellX + 2,
                            columnTop + row * CELL_STEP_Y + 2);
                }
            }
            context.disableScissor();
        }
    }

    /** Um símbolo em 20x20 (o desenho de 16x16 ampliado), com o canto de cima à esquerda em (px, py). */
    private void drawSymbol(DrawContext context, int symbol, float px, float py) {
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate(px, py, 0.0F);
        matrices.scale(1.25F, 1.25F, 1.0F);
        if (symbol == SlotMachine.SEVEN) {
            context.drawTexture(SEVEN_TEXTURE, 0, 0, 0.0F, 0.0F, 16, 16, 16, 16);
        } else if (symbol == SlotMachine.HEART) {
            context.drawTexture(HEART_TEXTURE, 0, 0, 0.0F, 0.0F, 16, 16, 16, 16);
        } else if (symbol >= 0 && symbol < SYMBOL_ITEMS.length && SYMBOL_ITEMS[symbol] != null) {
            context.drawItem(SYMBOL_ITEMS[symbol], 0, 0);
        }
        matrices.pop();
    }

    /** Os textos, a partir do canto da tela: título, aposta, créditos, aviso e "INVENTÁRIO". */
    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        CassinoGui.drawTitle(context, textRenderer, title, backgroundWidth);
        CassinoGui.drawHouse(context, textRenderer, handler.house());
        CassinoGui.drawAmount(context, textRenderer, Text.translatable("gui.cassino.slot.bet", handler.bet()), RIGHT_X,
                BET_Y);
        CassinoGui.drawAmount(context, textRenderer, Text.translatable("gui.cassino.slot.credits", shownCredits()),
                RIGHT_X, CREDITS_Y);

        // Embaixo dos rolos: o aviso da última girada por uns segundos; senão, o acumulado do jackpot.
        Text below = message;
        int belowColor = messageColor;
        if (message == null || (spinStartedAt < 0 && System.currentTimeMillis() > messageUntil)) {
            below = Text.translatable("gui.cassino.slot.pool", handler.pool());
            belowColor = POOL_TEXT;
        }
        context.drawText(textRenderer, below, REELS_X + (REELS_WIDTH - textRenderer.getWidth(below)) / 2,
                MESSAGE_Y, belowColor, false);

        CassinoGui.drawInventoryLabel(context, textRenderer, SlotMachineScreenHandler.INVENTORY_X,
                SlotMachineScreenHandler.INVENTORY_Y);
    }
}
