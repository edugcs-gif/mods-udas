package com.cassino.mod.client;

import com.cassino.mod.machine.Dice;
import com.cassino.mod.screen.DiceScreenHandler;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.random.Random;

/**
 * A tela do jogo de dados: os dois dados no feltro à esquerda (com a soma e as últimas somas embaixo), a lista de
 * apostas no meio (clique numa fileira para marcar ou desmarcar; a ficha aparece nas marcadas) e ROLAR!, a aposta, os
 * créditos, + e -, o espaço das fichas, DEPÓSITO e RETIRADA à direita; o inventário embaixo.
 *
 * <p>Quem rola é o servidor. Quando o resultado chega, os dados chacoalham um pouco na tela e param na soma
 * sorteada; só então aparecem o prêmio e o saldo novo.
 */
public class DiceScreen extends HandledScreen<DiceScreenHandler> {
    private static final int WOOD = 0xFF6B4A2B;
    private static final int FELT = 0xFF2E7A3A;
    private static final int LIST_OUTLINE = 0xFF555555;
    private static final int LIST_FILL = 0xFFB4B4B4;
    private static final int ROW_HOVER = 0x40FFFFFF;
    private static final int ROW_MARKED = 0x40F2D43A;
    private static final int INFO_BG = 0xFF3A3A3A;
    private static final int INFO_TEXT = 0xFFE8E8E8;
    private static final int ODDS_TEXT = 0xFF1E5A8A;
    private static final int WIN_TEXT = 0xFF1E7A1E;
    private static final int LOSE_TEXT = 0xFF555555;
    private static final int ERROR_TEXT = 0xFFB02020;
    private static final int GOLD_TEXT = 0xFFB07800;
    private static final int DIE_OUTLINE = 0xFF4A0808;
    private static final int DIE_FILL = 0xFFC8201E;
    private static final int DIE_LIGHT = 0xFFE65050;
    private static final int DIE_DARK = 0xFF8A1210;
    private static final int PIP = 0xFFF5F5F5;

    // Posições a partir do canto da tela.
    private static final int LEFT_X = 8;
    private static final int LEFT_WIDTH = 72;
    private static final int FELT_Y = 30;
    private static final int FELT_HEIGHT = 66;
    private static final int HISTORY_Y = 100;
    private static final int HISTORY_HEIGHT = 34;
    private static final int DIE_SIZE = 24;
    private static final int DIE_1_X = 14;
    private static final int DIE_1_Y = 37;
    private static final int DIE_2_X = 44;
    private static final int DIE_2_Y = 45;
    private static final int LIST_X = 86;
    private static final int LIST_Y = 20;
    private static final int LIST_WIDTH = 122;
    private static final int LIST_HEIGHT = 100;
    private static final int ROW_Y = 23;
    private static final int ROW_STEP = 19;
    private static final int SPOT_X = 89;
    private static final int LABEL_X = 110;
    private static final int NUMBER_BOX_X = 152;
    private static final int ODDS_RIGHT = 205;
    private static final int MESSAGE_Y = 125;
    private static final int RIGHT_X = 214;
    private static final int RIGHT_WIDTH = 94;

    /** Quanto tempo os dados chacoalham antes de parar, e de quanto em quanto tempo trocam de face. */
    private static final long ROLL_MS = 1_100;
    private static final long FACE_MS = 80;
    /** Se o servidor não responder ao ROLAR! nesse tempo (sem créditos, por exemplo), o botão volta. */
    private static final long WAIT_MS = 1_500;

    /** Onde ficam as bolinhas de cada face (coluna e linha, de 0 a 2). */
    private static final int[][][] PIPS = {
            {{1, 1}},
            {{0, 0}, {2, 2}},
            {{0, 0}, {1, 1}, {2, 2}},
            {{0, 0}, {2, 0}, {0, 2}, {2, 2}},
            {{0, 0}, {2, 0}, {1, 1}, {0, 2}, {2, 2}},
            {{0, 0}, {2, 0}, {0, 1}, {2, 1}, {0, 2}, {2, 2}},
    };

    private final Random shake = Random.create();
    private ColorButton rollButton;
    private boolean initialized;
    private int seenRolls;
    private long rollStartedAt = -1;
    private long lastFaceAt;
    private int rollWin;
    private int face1;
    private int face2;
    private int jitter1;
    private int jitter2;
    private boolean waiting;
    private long waitingSince;
    private Text message;
    private int messageColor;

    public DiceScreen(DiceScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        backgroundWidth = DiceScreenHandler.WIDTH;
        backgroundHeight = DiceScreenHandler.HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        if (!initialized) {
            seenRolls = handler.rolls();
            initialized = true;
        }
        rollButton = addDrawableChild(new ColorButton(x + RIGHT_X, y + 20, RIGHT_WIDTH, 22,
                Text.translatable("gui.cassino.dice.roll"), ColorButton.GREEN, 1.5F, this::roll));
        rollButton.setTooltip(Tooltip.of(Text.translatable("gui.cassino.dice.tip.rules")));
        addDrawableChild(ButtonWidget.builder(Text.literal("+"),
                        button -> click(DiceScreenHandler.BUTTON_BET_UP))
                .dimensions(x + RIGHT_X, y + 76, 45, 16)
                .tooltip(Tooltip.of(Text.translatable("gui.cassino.slot.tip.bet_up")))
                .build());
        addDrawableChild(ButtonWidget.builder(Text.literal("-"),
                        button -> click(DiceScreenHandler.BUTTON_BET_DOWN))
                .dimensions(x + RIGHT_X + RIGHT_WIDTH - 45, y + 76, 45, 16)
                .tooltip(Tooltip.of(Text.translatable("gui.cassino.slot.tip.bet_down")))
                .build());
        int depositX = DiceScreenHandler.COIN_SLOT_X + 21;
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.cassino.slot.deposit"),
                        button -> click(DiceScreenHandler.BUTTON_DEPOSIT))
                .dimensions(x + depositX, y + DiceScreenHandler.COIN_SLOT_Y - 1, RIGHT_X + RIGHT_WIDTH - depositX,
                        20)
                .tooltip(Tooltip.of(Text.translatable("gui.cassino.slot.tip.deposit")))
                .build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.cassino.slot.withdraw"),
                        button -> click(DiceScreenHandler.BUTTON_WITHDRAW))
                .dimensions(x + RIGHT_X, y + 118, RIGHT_WIDTH, 17)
                .tooltip(Tooltip.of(Text.translatable("gui.cassino.slot.tip.withdraw")))
                .build());
    }

    private void click(int button) {
        if (client != null && client.interactionManager != null) {
            client.interactionManager.clickButton(handler.syncId, button);
        }
    }

    private void roll() {
        if (rollStartedAt >= 0 || waiting) {
            return;
        }
        if (handler.mask() == 0) {
            showMessage(Text.translatable("gui.cassino.dice.no_bet"), ERROR_TEXT);
            return;
        }
        if (handler.credits() + handler.coinsWaiting() < handler.cost()) {
            showMessage(Text.translatable("gui.cassino.slot.no_credits"), ERROR_TEXT);
            return;
        }
        if (handler.bet() > handler.maxBet()) {
            showMessage(handler.maxBet() > 0
                    ? Text.translatable("gui.cassino.blackjack.table_limit", handler.maxBet())
                    : Text.translatable("gui.cassino.blackjack.table_closed"), ERROR_TEXT);
            return;
        }
        waiting = true;
        waitingSince = System.currentTimeMillis();
        message = null;
        click(DiceScreenHandler.BUTTON_ROLL);
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

    // ---------------------------------------------------------------- cliques na mesa

    private boolean inNumberBox(double mouseX, double mouseY) {
        int boxX = x + NUMBER_BOX_X;
        int boxY = y + rowY(Dice.NUMBER);
        return mouseX >= boxX && mouseX < boxX + 18 && mouseY >= boxY && mouseY < boxY + 18;
    }

    /** A fileira de aposta embaixo do mouse, ou -1. */
    private int rowAt(double mouseX, double mouseY) {
        if (mouseX < x + LIST_X + 2 || mouseX >= x + LIST_X + LIST_WIDTH - 2) {
            return -1;
        }
        for (int type = 0; type < Dice.TYPES; type++) {
            int top = y + rowY(type) - 1;
            if (mouseY >= top && mouseY < top + ROW_STEP) {
                return type;
            }
        }
        return -1;
    }

    private static int rowY(int type) {
        return ROW_Y + type * ROW_STEP;
    }

    /** Clicar numa fileira marca ou desmarca a aposta; clicar no quadradinho do NÚMERO troca o número. */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (rollStartedAt < 0 && (button == 0 || button == 1)) {
            if (inNumberBox(mouseX, mouseY)) {
                click(button == 0 ? DiceScreenHandler.BUTTON_NUMBER_UP : DiceScreenHandler.BUTTON_NUMBER_DOWN);
                play(SoundEvents.UI_BUTTON_CLICK.value(), 1.4F, 0.5F);
                return true;
            }
            int row = rowAt(mouseX, mouseY);
            if (row >= 0 && button == 0) {
                click(DiceScreenHandler.BUTTON_TOGGLE + row);
                play(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 0.6F);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** A rodinha do mouse em cima do quadradinho do NÚMERO também troca o número. */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (rollStartedAt < 0 && amount != 0 && inNumberBox(mouseX, mouseY)) {
            click(amount > 0 ? DiceScreenHandler.BUTTON_NUMBER_UP : DiceScreenHandler.BUTTON_NUMBER_DOWN);
            play(SoundEvents.UI_BUTTON_CLICK.value(), 1.4F, 0.5F);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    // ---------------------------------------------------------------- animação

    private void update(long now) {
        if (handler.rolls() != seenRolls) {
            seenRolls = handler.rolls();
            rollStartedAt = now;
            lastFaceAt = 0;
            rollWin = handler.lastWin();
            waiting = false;
            message = null;
        }
        if (waiting && now - waitingSince > WAIT_MS) {
            waiting = false;
        }
        if (rollStartedAt < 0) {
            return;
        }
        if (now - lastFaceAt >= FACE_MS) {
            lastFaceAt = now;
            face1 = 1 + shake.nextInt(6);
            face2 = 1 + shake.nextInt(6);
            jitter1 = shake.nextInt(5) - 2;
            jitter2 = shake.nextInt(5) - 2;
            play(SoundEvents.BLOCK_WOODEN_BUTTON_CLICK_ON, 1.6F + shake.nextFloat() * 0.3F, 0.2F);
        }
        if (now - rollStartedAt >= ROLL_MS) {
            rollStartedAt = -1;
            showResult();
        }
    }

    private void showResult() {
        int total = handler.die1() + handler.die2();
        if (rollWin > 0) {
            boolean big = rollWin >= handler.cost() * 5;
            showMessage(Text.translatable("gui.cassino.dice.result.win", total, rollWin), big ? GOLD_TEXT : WIN_TEXT);
            play(big ? SoundEvents.ENTITY_PLAYER_LEVELUP : SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 0.8F);
        } else {
            showMessage(Text.translatable("gui.cassino.dice.result.lose", total), LOSE_TEXT);
        }
    }

    private boolean rolling() {
        return rollStartedAt >= 0;
    }

    /** O saldo na tela: com os dados chacoalhando, ainda sem o prêmio (ele entra quando eles param). */
    private int shownCredits() {
        return rolling() ? Math.max(0, handler.credits() - rollWin) : handler.credits();
    }

    // ---------------------------------------------------------------- desenho

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        update(System.currentTimeMillis());
        rollButton.active = !rolling() && !waiting;
        renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
        drawTableTooltip(context, mouseX, mouseY);
        CassinoGui.drawHouseTooltip(context, textRenderer, x, y, mouseX, mouseY, handler.house());
    }

    /** Com o mouse numa fileira (ou no quadradinho do NÚMERO): o que ela paga e quando ganha. */
    private void drawTableTooltip(DrawContext context, int mouseX, int mouseY) {
        Text tip;
        if (inNumberBox(mouseX, mouseY)) {
            tip = Text.translatable("gui.cassino.dice.tip.number_box");
        } else {
            int row = rowAt(mouseX, mouseY);
            if (row < 0) {
                return;
            }
            int number = handler.number();
            int ways = 0;
            for (int total = Dice.MIN_TOTAL; total <= Dice.MAX_TOTAL; total++) {
                if (Dice.wins(row, number, total)) {
                    ways += Dice.ways(total);
                }
            }
            tip = Text.translatable("gui.cassino.dice.tip." + row, number, ways,
                    odds(Dice.odds10(row, number)));
        }
        context.drawOrderedTooltip(textRenderer, textRenderer.wrapLines(tip, 200), mouseX, mouseY);
    }

    private static String odds(int odds10) {
        return "x" + odds10 / 10 + "," + odds10 % 10;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        CassinoGui.drawPanel(context, x, y, backgroundWidth, backgroundHeight);
        drawFelt(context);
        drawHistoryBox(context);
        drawList(context, mouseX, mouseY);

        context.fill(x + RIGHT_X, y + 46, x + RIGHT_X + RIGHT_WIDTH, y + 72, INFO_BG);
        CassinoGui.drawCoinSlot(context, x + DiceScreenHandler.COIN_SLOT_X, y + DiceScreenHandler.COIN_SLOT_Y,
                !handler.getSlot(0).hasStack());
        CassinoGui.drawInventorySlots(context, x + DiceScreenHandler.INVENTORY_X, y + DiceScreenHandler.INVENTORY_Y,
                y + DiceScreenHandler.HOTBAR_Y);
    }

    /** O feltro com os dois dados (chacoalhando enquanto rolam). */
    private void drawFelt(DrawContext context) {
        int left = x + LEFT_X;
        int top = y + FELT_Y;
        context.fill(left, top, left + LEFT_WIDTH, top + FELT_HEIGHT, WOOD);
        context.fill(left + 2, top + 2, left + LEFT_WIDTH - 2, top + FELT_HEIGHT - 2, FELT);
        if (rolling()) {
            drawDie(context, x + DIE_1_X + jitter1, y + DIE_1_Y - Math.abs(jitter2), face1);
            drawDie(context, x + DIE_2_X + jitter2, y + DIE_2_Y - Math.abs(jitter1), face2);
        } else {
            drawDie(context, x + DIE_1_X, y + DIE_1_Y, handler.die1());
            drawDie(context, x + DIE_2_X, y + DIE_2_Y, handler.die2());
        }
    }

    /** Um dado vermelho com as bolinhas brancas. */
    private static void drawDie(DrawContext context, int left, int top, int face) {
        int size = DIE_SIZE;
        context.fill(left, top, left + size, top + size, DIE_OUTLINE);
        context.fill(left + 1, top + 1, left + size - 1, top + size - 1, DIE_FILL);
        context.fill(left + 1, top + 1, left + size - 1, top + 3, DIE_LIGHT);
        context.fill(left + 1, top + 1, left + 3, top + size - 1, DIE_LIGHT);
        context.fill(left + 1, top + size - 3, left + size - 1, top + size - 1, DIE_DARK);
        context.fill(left + size - 3, top + 1, left + size - 1, top + size - 1, DIE_DARK);
        for (int[] pip : PIPS[Math.max(1, Math.min(6, face)) - 1]) {
            int pipX = left + 4 + pip[0] * 6;
            int pipY = top + 4 + pip[1] * 6;
            context.fill(pipX, pipY, pipX + 4, pipY + 4, PIP);
        }
    }

    private void drawHistoryBox(DrawContext context) {
        int left = x + LEFT_X;
        int top = y + HISTORY_Y;
        context.fill(left, top, left + LEFT_WIDTH, top + HISTORY_HEIGHT, LIST_OUTLINE);
        context.fill(left + 1, top + 1, left + LEFT_WIDTH - 1, top + HISTORY_HEIGHT - 1, LIST_FILL);
    }

    /** A lista de apostas: a ficha no quadradinho das marcadas, e a fileira embaixo do mouse mais clara. */
    private void drawList(DrawContext context, int mouseX, int mouseY) {
        int left = x + LIST_X;
        int top = y + LIST_Y;
        context.fill(left, top, left + LIST_WIDTH, top + LIST_HEIGHT, LIST_OUTLINE);
        context.fill(left + 1, top + 1, left + LIST_WIDTH - 1, top + LIST_HEIGHT - 1, LIST_FILL);
        int hovered = rolling() || inNumberBox(mouseX, mouseY) ? -1 : rowAt(mouseX, mouseY);
        for (int type = 0; type < Dice.TYPES; type++) {
            int rowTop = y + rowY(type) - 1;
            if (handler.marked(type)) {
                context.fill(left + 2, rowTop, left + LIST_WIDTH - 2, rowTop + ROW_STEP - 1, ROW_MARKED);
            }
            if (type == hovered) {
                context.fill(left + 2, rowTop, left + LIST_WIDTH - 2, rowTop + ROW_STEP - 1, ROW_HOVER);
            }
            int spotX = x + SPOT_X;
            int spotY = y + rowY(type);
            CassinoGui.drawSlot(context, spotX, spotY);
            if (handler.marked(type)) {
                context.drawItem(CassinoGui.CHIP, spotX + 1, spotY + 1);
            }
        }
        CassinoGui.drawSlot(context, x + NUMBER_BOX_X, y + rowY(Dice.NUMBER));
    }

    /** Os textos, a partir do canto da tela. */
    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        CassinoGui.drawTitle(context, textRenderer, title, backgroundWidth);
        CassinoGui.drawHouse(context, textRenderer, handler.house());

        Text dice = Text.translatable("gui.cassino.dice.dice");
        context.drawText(textRenderer, dice, LEFT_X + (LEFT_WIDTH - textRenderer.getWidth(dice)) / 2, 20,
                CassinoGui.TEXT, false);
        Text total = rolling()
                ? Text.translatable("gui.cassino.dice.total", "?")
                : Text.translatable("gui.cassino.dice.total", handler.die1() + handler.die2());
        context.drawText(textRenderer, total, LEFT_X + (LEFT_WIDTH - textRenderer.getWidth(total)) / 2,
                FELT_Y + FELT_HEIGHT - 13, 0xFFFFFFFF, true);
        drawHistory(context);

        int number = handler.number();
        for (int type = 0; type < Dice.TYPES; type++) {
            int rowY = rowY(type);
            Text label = Text.translatable("gui.cassino.dice.bet." + type);
            context.drawText(textRenderer, label, LABEL_X, rowY + 5, CassinoGui.TEXT, false);
            String pays = odds(Dice.odds10(type, number));
            context.drawText(textRenderer, pays, ODDS_RIGHT - textRenderer.getWidth(pays), rowY + 5, ODDS_TEXT, false);
        }
        String chosen = Integer.toString(number);
        context.drawText(textRenderer, chosen, NUMBER_BOX_X + 9 - textRenderer.getWidth(chosen) / 2,
                rowY(Dice.NUMBER) + 5, 0xFFFFFFFF, true);

        int messageWidth = LIST_X + LIST_WIDTH - LEFT_X;
        Text line = message;
        int color = messageColor;
        if (line == null) {
            line = Text.translatable("gui.cassino.dice.cost", handler.bet(), handler.cost());
            color = LOSE_TEXT;
        }
        context.drawText(textRenderer, line, LEFT_X + (messageWidth - textRenderer.getWidth(line)) / 2, MESSAGE_Y,
                color, false);

        CassinoGui.drawAmount(context, textRenderer, Text.translatable("gui.cassino.slot.bet", handler.bet()),
                RIGHT_X + 4, 50, INFO_TEXT);
        CassinoGui.drawAmount(context, textRenderer, Text.translatable("gui.cassino.slot.credits", shownCredits()),
                RIGHT_X + 4, 61, INFO_TEXT);
        CassinoGui.drawInventoryLabel(context, textRenderer, DiceScreenHandler.INVENTORY_X,
                DiceScreenHandler.INVENTORY_Y);
    }

    /** "ÚLTIMOS:" e as últimas somas, 3 por linha (a da jogada em andamento só aparece quando os dados param). */
    private void drawHistory(DrawContext context) {
        context.drawText(textRenderer, Text.translatable("gui.cassino.dice.history"), LEFT_X + 4, HISTORY_Y + 4,
                CassinoGui.TEXT, false);
        List<String> totals = new ArrayList<>();
        int[] history = handler.history();
        for (int i = rolling() ? 1 : 0; i < history.length; i++) {
            if (history[i] >= Dice.MIN_TOTAL) {
                totals.add(Integer.toString(history[i]));
            }
        }
        for (int i = 0; i < totals.size(); i++) {
            int cellX = LEFT_X + 6 + (i % 3) * 22;
            int cellY = HISTORY_Y + 14 + (i / 3) * 10;
            context.drawText(textRenderer, totals.get(i), cellX, cellY, i == 0 ? 0xFF202020 : 0xFF606060, false);
        }
    }
}
