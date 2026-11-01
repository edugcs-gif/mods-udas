package com.cassino.mod.client;

import com.cassino.mod.Cassino;
import com.cassino.mod.machine.Blackjack;
import com.cassino.mod.screen.BlackjackScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * A tela da mesa de blackjack: as cartas do dealer e as suas (com o valor de cada mão) à esquerda; PEDIR (ou DAR
 * CARTAS, entre as mãos), PARAR, DOBRAR, a aposta, os créditos, o espaço das fichas com DEPÓSITO e RETIRADA à
 * direita; o inventário embaixo.
 *
 * <p>As cartas têm o valor (A, 2 a 10, J, Q, K) e o naipe como um item do jogo: copas = coração, ouros = barra de
 * ouro, espadas = espada de ferro e paus = graveto. A segunda carta do dealer fica virada até o jogador parar.
 */
public class BlackjackScreen extends HandledScreen<BlackjackScreenHandler> {
    private static final int TABLE = 0xFF8B8B8B;
    private static final int TABLE_DARK = 0xFF6A6A6A;
    private static final int TABLE_LIGHT = 0xFFA9A9A9;
    private static final int EMPTY_CARD = 0xFF7A7A7A;
    private static final int EMPTY_CARD_BORDER = 0xFF5E5E5E;
    private static final int CARD_FACE = 0xFFF4F4F4;
    private static final int CARD_BORDER = 0xFF4A4A4A;
    private static final int RED_SUIT = 0xFFC00000;
    private static final int BLACK_SUIT = 0xFF202020;
    private static final int BACK = 0xFFB02020;
    private static final int BACK_DARK = 0xFF5A0A0A;
    private static final int WIN_TEXT = 0xFF1E7A1E;
    private static final int LOSE_TEXT = 0xFF8A1A1A;
    private static final int PUSH_TEXT = 0xFF555555;
    private static final int GOLD_TEXT = 0xFFB07800;

    // Posições a partir do canto da tela.
    private static final int TABLE_X = 8;
    private static final int TABLE_Y = 20;
    private static final int TABLE_WIDTH = 140;
    private static final int TABLE_HEIGHT = 124;
    private static final int CARDS_X = 12;
    private static final int CARD_WIDTH = 24;
    private static final int CARD_HEIGHT = 28;
    private static final int CARD_STEP = 27;
    private static final int DEALER_LABEL_Y = 24;
    private static final int DEALER_CARDS_Y = 33;
    private static final int DEALER_VALUE_Y = 64;
    private static final int PLAYER_LABEL_Y = 76;
    private static final int PLAYER_CARDS_Y = 85;
    private static final int PLAYER_VALUE_Y = 116;
    private static final int MESSAGE_Y = 131;
    private static final int RIGHT_X = 154;
    private static final int RIGHT_WIDTH = 114;
    private static final int BET_Y = 83;
    private static final int CREDITS_Y = 97;
    /** Espaços de carta desenhados vazios na mesa (com mais cartas, elas se encaixam umas sobre as outras). */
    private static final int SHOWN_SPACES = 5;

    private static final Identifier HEART_TEXTURE = Cassino.id("textures/gui/symbol_heart.png");
    /** O naipe de cada carta como um item do jogo ({@code null} para copas, que usa o desenho do coração). */
    private static final ItemStack[] SUIT_ITEMS = {
            null, new ItemStack(Items.GOLD_INGOT), new ItemStack(Items.IRON_SWORD), new ItemStack(Items.STICK)
    };
    /** A carta virada: a cara do creeper, em quadradinhos de 2 pixels. */
    private static final String[] CREEPER = {
            "........",
            ".XX..XX.",
            ".XX..XX.",
            "...XX...",
            "..XXXX..",
            "..XXXX..",
            "..X..X..",
            "........"};

    private ColorButton mainButton;
    private ColorButton standButton;
    private ColorButton doubleButton;
    private ButtonWidget betUpButton;
    private ButtonWidget betDownButton;

    private int seenHands;
    /** A mão cujo resultado já foi mostrado (para mostrar uma vez só). */
    private int resultShownFor = -1;
    private Text message;
    private int messageColor;

    public BlackjackScreen(BlackjackScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        backgroundWidth = BlackjackScreenHandler.WIDTH;
        backgroundHeight = BlackjackScreenHandler.HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        mainButton = addDrawableChild(new ColorButton(x + RIGHT_X, y + 20, RIGHT_WIDTH, 20,
                Text.translatable("gui.cassino.blackjack.deal"), ColorButton.GREEN, 1.25F, this::dealOrHit));
        mainButton.setTooltip(Tooltip.of(Text.translatable("gui.cassino.blackjack.tip.rules")));
        standButton = addDrawableChild(new ColorButton(x + RIGHT_X, y + 42, RIGHT_WIDTH, 16,
                Text.translatable("gui.cassino.blackjack.stand"), ColorButton.RED, 1.0F,
                () -> click(BlackjackScreenHandler.BUTTON_STAND)));
        doubleButton = addDrawableChild(new ColorButton(x + RIGHT_X + 17, y + 60, RIGHT_WIDTH - 34, 16,
                Text.translatable("gui.cassino.blackjack.double"), ColorButton.BLUE, 1.0F,
                () -> click(BlackjackScreenHandler.BUTTON_DOUBLE)));
        doubleButton.setTooltip(Tooltip.of(Text.translatable("gui.cassino.blackjack.tip.double")));

        betUpButton = addDrawableChild(ButtonWidget.builder(Text.literal("+"),
                        button -> click(BlackjackScreenHandler.BUTTON_BET_UP))
                .dimensions(x + RIGHT_X + RIGHT_WIDTH - 42, y + BET_Y - 4, 20, 14)
                .tooltip(Tooltip.of(Text.translatable("gui.cassino.slot.tip.bet_up")))
                .build());
        betDownButton = addDrawableChild(ButtonWidget.builder(Text.literal("-"),
                        button -> click(BlackjackScreenHandler.BUTTON_BET_DOWN))
                .dimensions(x + RIGHT_X + RIGHT_WIDTH - 20, y + BET_Y - 4, 20, 14)
                .tooltip(Tooltip.of(Text.translatable("gui.cassino.slot.tip.bet_down")))
                .build());
        int depositX = BlackjackScreenHandler.COIN_SLOT_X + 21;
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.cassino.slot.deposit"),
                        button -> click(BlackjackScreenHandler.BUTTON_DEPOSIT))
                .dimensions(x + depositX, y + BlackjackScreenHandler.COIN_SLOT_Y - 1,
                        RIGHT_X + RIGHT_WIDTH - depositX, 20)
                .tooltip(Tooltip.of(Text.translatable("gui.cassino.slot.tip.deposit")))
                .build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.cassino.slot.withdraw"),
                        button -> click(BlackjackScreenHandler.BUTTON_WITHDRAW))
                .dimensions(x + RIGHT_X, y + 127, RIGHT_WIDTH, 17)
                .tooltip(Tooltip.of(Text.translatable("gui.cassino.slot.tip.withdraw")))
                .build());
    }

    private void click(int button) {
        if (client != null && client.interactionManager != null) {
            client.interactionManager.clickButton(handler.syncId, button);
        }
    }

    /** O botão verde: entre as mãos, dá as cartas (se der para apostar); durante a mão, pede mais uma. */
    private void dealOrHit() {
        if (!handler.playing()) {
            int bet = handler.bet();
            if (handler.credits() + handler.coinsWaiting() < bet) {
                showMessage(Text.translatable("gui.cassino.slot.no_credits"), LOSE_TEXT);
                return;
            }
            if (bet > handler.maxBet()) {
                showMessage(handler.maxBet() > 0
                        ? Text.translatable("gui.cassino.blackjack.table_limit", handler.maxBet())
                        : Text.translatable("gui.cassino.blackjack.table_closed"), LOSE_TEXT);
                return;
            }
            message = null;
        }
        click(BlackjackScreenHandler.BUTTON_DEAL_OR_HIT);
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

    /** Acompanha a mão: limpa o aviso quando começa uma nova e mostra o resultado (uma vez) quando ela termina. */
    private void update() {
        if (handler.hands() != seenHands) {
            seenHands = handler.hands();
            message = null;
        }
        if (!handler.playing() && handler.result() != BlackjackScreenHandler.RESULT_NONE
                && resultShownFor != handler.hands()) {
            resultShownFor = handler.hands();
            showResult();
        }
    }

    private void showResult() {
        int bet = handler.handBet();
        int payout = handler.payout();
        switch (handler.result()) {
            case BlackjackScreenHandler.RESULT_BLACKJACK -> {
                showMessage(Text.translatable("gui.cassino.blackjack.result.blackjack", payout - bet), GOLD_TEXT);
                play(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.2F, 0.6F);
            }
            case BlackjackScreenHandler.RESULT_WIN ->
                    showMessage(Text.translatable("gui.cassino.blackjack.result.win", payout - bet), WIN_TEXT);
            case BlackjackScreenHandler.RESULT_DEALER_BUST ->
                    showMessage(Text.translatable("gui.cassino.blackjack.result.dealer_bust", payout - bet), WIN_TEXT);
            case BlackjackScreenHandler.RESULT_PUSH ->
                    showMessage(Text.translatable("gui.cassino.blackjack.result.push"), PUSH_TEXT);
            case BlackjackScreenHandler.RESULT_BUST ->
                    showMessage(Text.translatable("gui.cassino.blackjack.result.bust", bet), LOSE_TEXT);
            case BlackjackScreenHandler.RESULT_DEALER_BLACKJACK ->
                    showMessage(Text.translatable("gui.cassino.blackjack.result.dealer_blackjack", bet), LOSE_TEXT);
            case BlackjackScreenHandler.RESULT_LOSE ->
                    showMessage(Text.translatable("gui.cassino.blackjack.result.lose", bet), LOSE_TEXT);
            default -> {
            }
        }
    }

    // ---------------------------------------------------------------- desenho

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        update();
        boolean playing = handler.playing();
        mainButton.setMessage(Text.translatable(playing ? "gui.cassino.blackjack.hit" : "gui.cassino.blackjack.deal"));
        standButton.active = playing;
        doubleButton.active = playing && handler.canDouble();
        betUpButton.active = !playing;
        betDownButton.active = !playing;
        renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
        CassinoGui.drawHouseTooltip(context, textRenderer, x, y, mouseX, mouseY, handler.house());
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        CassinoGui.drawPanel(context, x, y, backgroundWidth, backgroundHeight);

        // A mesa: um quadro cinza com as duas fileiras de cartas.
        int left = x + TABLE_X;
        int top = y + TABLE_Y;
        context.fill(left, top, left + TABLE_WIDTH, top + TABLE_HEIGHT, TABLE);
        context.fill(left, top, left + TABLE_WIDTH, top + 1, TABLE_DARK);
        context.fill(left, top, left + 1, top + TABLE_HEIGHT, TABLE_DARK);
        context.fill(left, top + TABLE_HEIGHT - 1, left + TABLE_WIDTH, top + TABLE_HEIGHT, TABLE_LIGHT);
        context.fill(left + TABLE_WIDTH - 1, top, left + TABLE_WIDTH, top + TABLE_HEIGHT, TABLE_LIGHT);
        drawHand(context, handler.dealerCards(), handler.dealerCount(), y + DEALER_CARDS_Y);
        drawHand(context, handler.playerCards(), handler.playerCount(), y + PLAYER_CARDS_Y);

        CassinoGui.drawCoinSlot(context, x + BlackjackScreenHandler.COIN_SLOT_X,
                y + BlackjackScreenHandler.COIN_SLOT_Y, !handler.getSlot(0).hasStack());
        CassinoGui.drawInventorySlots(context, x + BlackjackScreenHandler.INVENTORY_X,
                y + BlackjackScreenHandler.INVENTORY_Y, y + BlackjackScreenHandler.HOTBAR_Y);
    }

    /** Uma fileira: os espaços vazios e, por cima, as cartas (encaixadas umas sobre as outras se forem muitas). */
    private void drawHand(DrawContext context, int[] cards, int count, int cardY) {
        int startX = x + CARDS_X;
        for (int i = 0; i < SHOWN_SPACES; i++) {
            int cardX = startX + i * CARD_STEP;
            context.fill(cardX, cardY, cardX + CARD_WIDTH, cardY + CARD_HEIGHT, EMPTY_CARD_BORDER);
            context.fill(cardX + 1, cardY + 1, cardX + CARD_WIDTH - 1, cardY + CARD_HEIGHT - 1, EMPTY_CARD);
        }
        int space = (SHOWN_SPACES - 1) * CARD_STEP;
        int step = count <= SHOWN_SPACES ? CARD_STEP : space / (count - 1);
        for (int i = 0; i < count; i++) {
            drawCard(context, cards[i], startX + i * step, cardY);
        }
    }

    private void drawCard(DrawContext context, int card, int cardX, int cardY) {
        context.fill(cardX, cardY, cardX + CARD_WIDTH, cardY + CARD_HEIGHT, card == Blackjack.HIDDEN ? BACK_DARK
                : CARD_BORDER);
        if (card == Blackjack.HIDDEN || card < 0 || card > Blackjack.HIDDEN) {
            context.fill(cardX + 1, cardY + 1, cardX + CARD_WIDTH - 1, cardY + CARD_HEIGHT - 1, BACK);
            int faceX = cardX + 4;
            int faceY = cardY + 6;
            for (int row = 0; row < CREEPER.length; row++) {
                for (int column = 0; column < CREEPER[row].length(); column++) {
                    if (CREEPER[row].charAt(column) == 'X') {
                        context.fill(faceX + column * 2, faceY + row * 2, faceX + column * 2 + 2, faceY + row * 2 + 2,
                                BACK_DARK);
                    }
                }
            }
            return;
        }
        context.fill(cardX + 1, cardY + 1, cardX + CARD_WIDTH - 1, cardY + CARD_HEIGHT - 1, CARD_FACE);
        int suit = Blackjack.suit(card);
        boolean red = suit == Blackjack.COPAS || suit == Blackjack.OUROS;
        context.drawText(textRenderer, Blackjack.rankLabel(card), cardX + 2, cardY + 2, red ? RED_SUIT : BLACK_SUIT,
                false);
        // O naipe: um item do jogo no meio de baixo da carta.
        int iconX = cardX + (CARD_WIDTH - 16) / 2;
        int iconY = cardY + 10;
        if (suit == Blackjack.COPAS) {
            context.drawTexture(HEART_TEXTURE, iconX, iconY, 0.0F, 0.0F, 16, 16, 16, 16);
        } else {
            context.drawItem(SUIT_ITEMS[suit], iconX, iconY);
        }
    }

    /** Os textos, a partir do canto da tela. */
    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        CassinoGui.drawTitle(context, textRenderer, title, backgroundWidth);
        CassinoGui.drawHouse(context, textRenderer, handler.house());

        context.drawText(textRenderer, Text.translatable("gui.cassino.blackjack.dealer"), CARDS_X, DEALER_LABEL_Y,
                CassinoGui.TEXT, false);
        boolean hidden = false;
        for (int i = 0; i < handler.dealerCount(); i++) {
            hidden |= handler.dealerCards()[i] == Blackjack.HIDDEN;
        }
        Text dealerValue = handler.dealerCount() == 0 ? Text.literal("-")
                : hidden ? Text.literal("?")
                : Text.literal(Integer.toString(Blackjack.value(handler.dealerCards(), handler.dealerCount())));
        context.drawText(textRenderer, Text.translatable("gui.cassino.blackjack.value", dealerValue), CARDS_X,
                DEALER_VALUE_Y, CassinoGui.TEXT, false);

        context.drawText(textRenderer, Text.translatable("gui.cassino.blackjack.you"), CARDS_X, PLAYER_LABEL_Y,
                CassinoGui.TEXT, false);
        Text playerValue = handler.playerCount() == 0 ? Text.literal("-")
                : Text.literal(Integer.toString(Blackjack.value(handler.playerCards(), handler.playerCount())));
        context.drawText(textRenderer, Text.translatable("gui.cassino.blackjack.value", playerValue), CARDS_X,
                PLAYER_VALUE_Y, CassinoGui.TEXT, false);

        if (message != null) {
            context.drawText(textRenderer, message, TABLE_X + (TABLE_WIDTH - textRenderer.getWidth(message)) / 2,
                    MESSAGE_Y, messageColor, false);
        }

        int bet = handler.playing() ? handler.handBet() : handler.bet();
        CassinoGui.drawAmount(context, textRenderer, Text.translatable("gui.cassino.slot.bet", bet), RIGHT_X, BET_Y);
        CassinoGui.drawAmount(context, textRenderer, Text.translatable("gui.cassino.slot.credits", handler.credits()),
                RIGHT_X, CREDITS_Y);
        CassinoGui.drawInventoryLabel(context, textRenderer, BlackjackScreenHandler.INVENTORY_X,
                BlackjackScreenHandler.INVENTORY_Y);
    }
}
