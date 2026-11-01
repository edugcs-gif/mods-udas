package com.cassino.mod.client;

import com.cassino.mod.machine.HorseRace;
import com.cassino.mod.screen.HorseRaceScreenHandler;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.HorseColor;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.joml.Quaternionf;

/**
 * A tela da corrida de cavalos: os 7 cavalos do dia, cada um numa fileira com o desenho do cavalo, o nome, quanto paga
 * e a raia dele (o número corre até a linha de chegada); à direita, CORRER!, a aposta, os créditos, o espaço das
 * fichas, DEPÓSITO e RETIRADA; o inventário embaixo.
 *
 * <p>Quem corre é o servidor. Quando o resultado chega, a tela mostra a corrida (com ultrapassagens no meio) e o
 * vencedor cruza a linha por último no tempo da animação; só então aparecem o prêmio e o saldo novo.
 */
public class HorseRaceScreen extends HandledScreen<HorseRaceScreenHandler> {
    private static final int BOARD = 0xFF3A3A3A;
    private static final int BOARD_BORDER = 0xFF1E1E1E;
    private static final int ROW_SELECTED = 0xFF4E4E4E;
    private static final int LANE = 0xFF5C4630;
    private static final int LANE_DARK = 0xFF4A3826;
    private static final int SELECTED = 0xFFF2D43A;
    private static final int LABEL = 0xFFBBBBBB;
    private static final int NAME = 0xFFF0F0F0;
    private static final int ODDS = 0xFFE8C040;
    private static final int INFO_BG = 0xFF3A3A3A;
    private static final int WIN_TEXT = 0xFF1E7A1E;
    private static final int LOSE_TEXT = 0xFF555555;
    private static final int ERROR_TEXT = 0xFFB02020;
    private static final int[] MARKERS = {
            0xFFFF5555, 0xFFFFAA00, 0xFFFFFF55, 0xFF55FF55, 0xFF55FFFF, 0xFF5599FF, 0xFFFF55FF
    };

    // Posições a partir do canto da tela.
    private static final int BOARD_X = 8;
    private static final int BOARD_Y = 18;
    private static final int BOARD_WIDTH = 200;
    private static final int BOARD_HEIGHT = 120;
    private static final int HEADER_Y = 21;
    private static final int ROWS_Y = 30;
    private static final int ROW_HEIGHT = 15;
    private static final int HORSE_X = 33;
    private static final int TEXT_X = 43;
    private static final int LANE_X = 110;
    private static final int LANE_END = 192;
    private static final int FINISH_X = 194;
    private static final int MESSAGE_Y = 143;
    private static final int RIGHT_X = 214;
    private static final int RIGHT_WIDTH = 94;

    /** Quanto tempo a corrida leva na tela (igual ao {@link HorseRaceScreenHandler#RACE_TICKS}). */
    private static final long RACE_MS = 5_000;
    /** Se o servidor não responder ao CORRER! nesse tempo (sem créditos, por exemplo), o botão volta. */
    private static final long WAIT_MS = 1_500;
    private static final long GALLOP_MS = 350;

    private final HorseEntity[] horses = new HorseEntity[HorseRace.HORSES];
    private ColorButton runButton;
    private boolean initialized;
    private int seenRaces;
    private long raceStartedAt = -1;
    private int raceWin;
    private long lastGallop;
    private boolean waiting;
    private long waitingSince;
    private Text message;
    private int messageColor;

    public HorseRaceScreen(HorseRaceScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        backgroundWidth = HorseRaceScreenHandler.WIDTH;
        backgroundHeight = HorseRaceScreenHandler.HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        if (!initialized) {
            seenRaces = handler.races();
            // Um cavalo de cada pelagem do jogo, só para desenhar na lista.
            HorseColor[] colors = HorseColor.values();
            for (int i = 0; i < horses.length && client != null && client.world != null; i++) {
                horses[i] = EntityType.HORSE.create(client.world);
                if (horses[i] != null) {
                    horses[i].setVariant(colors[i % colors.length]);
                }
            }
            initialized = true;
        }
        runButton = addDrawableChild(new ColorButton(x + RIGHT_X, y + BOARD_Y, RIGHT_WIDTH, 22,
                Text.translatable("gui.cassino.race.run"), ColorButton.GREEN, 1.5F, this::run));
        runButton.setTooltip(Tooltip.of(Text.translatable("gui.cassino.race.tip.rules")));
        addDrawableChild(ButtonWidget.builder(Text.literal("-"),
                        button -> click(HorseRaceScreenHandler.BUTTON_BET_DOWN))
                .dimensions(x + RIGHT_X, y + 56, 24, 18)
                .tooltip(Tooltip.of(Text.translatable("gui.cassino.slot.tip.bet_down")))
                .build());
        addDrawableChild(ButtonWidget.builder(Text.literal("+"),
                        button -> click(HorseRaceScreenHandler.BUTTON_BET_UP))
                .dimensions(x + RIGHT_X + RIGHT_WIDTH - 24, y + 56, 24, 18)
                .tooltip(Tooltip.of(Text.translatable("gui.cassino.slot.tip.bet_up")))
                .build());
        int depositX = HorseRaceScreenHandler.COIN_SLOT_X + 21;
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.cassino.slot.deposit"),
                        button -> click(HorseRaceScreenHandler.BUTTON_DEPOSIT))
                .dimensions(x + depositX, y + HorseRaceScreenHandler.COIN_SLOT_Y - 1,
                        RIGHT_X + RIGHT_WIDTH - depositX, 20)
                .tooltip(Tooltip.of(Text.translatable("gui.cassino.slot.tip.deposit")))
                .build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.cassino.slot.withdraw"),
                        button -> click(HorseRaceScreenHandler.BUTTON_WITHDRAW))
                .dimensions(x + RIGHT_X, y + 118, RIGHT_WIDTH, 17)
                .tooltip(Tooltip.of(Text.translatable("gui.cassino.slot.tip.withdraw")))
                .build());
    }

    private void click(int button) {
        if (client != null && client.interactionManager != null) {
            client.interactionManager.clickButton(handler.syncId, button);
        }
    }

    private void run() {
        if (raceStartedAt >= 0 || waiting) {
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
        click(HorseRaceScreenHandler.BUTTON_RUN);
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

    /** "3. Cruz Credo": o número e o nome do cavalo. */
    private Text horseName(int horse) {
        return Text.literal((horse + 1) + ". " + HorseRace.NAMES[Math.floorMod(handler.name(horse),
                HorseRace.NAMES.length)]);
    }

    private static String odds(int odds10) {
        return "x" + odds10 / 10 + "," + odds10 % 10;
    }

    private int rowAt(double mouseX, double mouseY) {
        if (mouseX < x + BOARD_X || mouseX >= x + BOARD_X + BOARD_WIDTH) {
            return -1;
        }
        int row = (int) Math.floor((mouseY - (y + ROWS_Y)) / ROW_HEIGHT);
        return row >= 0 && row < HorseRace.HORSES ? row : -1;
    }

    /** Clicar na fileira de um cavalo escolhe a aposta (com a corrida parada). */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int row = rowAt(mouseX, mouseY);
        if (button == 0 && raceStartedAt < 0 && row >= 0) {
            click(HorseRaceScreenHandler.BUTTON_CHOOSE + row);
            play(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 0.6F);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    // ---------------------------------------------------------------- animação

    private void update(long now) {
        if (handler.races() != seenRaces) {
            seenRaces = handler.races();
            raceStartedAt = now;
            raceWin = handler.lastWin();
            waiting = false;
            message = null;
        }
        if (waiting && now - waitingSince > WAIT_MS) {
            waiting = false;
        }
        if (raceStartedAt < 0) {
            return;
        }
        if (now - lastGallop > GALLOP_MS) {
            lastGallop = now;
            play(SoundEvents.ENTITY_HORSE_GALLOP, 1.0F, 0.25F);
        }
        if (now - raceStartedAt >= RACE_MS) {
            raceStartedAt = -1;
            int winner = handler.order()[0];
            if (winner >= 0) {
                if (raceWin > 0) {
                    showMessage(Text.translatable("gui.cassino.race.result.win", horseName(winner), raceWin),
                            WIN_TEXT);
                    play(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.2F, 0.6F);
                } else {
                    showMessage(Text.translatable("gui.cassino.race.result.lose", horseName(winner)), LOSE_TEXT);
                }
            }
        }
    }

    /**
     * Onde o cavalo está na raia (0 = largada, 1 = chegada). Durante a corrida, todos avançam até a posição final
     * (o vencedor na chegada, os outros um pouco atrás, pela colocação) com um balanço que permite ultrapassagens; no
     * fim ficam parados na posição final até a próxima corrida.
     */
    private float lanePosition(int horse) {
        int[] order = handler.order();
        int place = -1;
        for (int i = 0; i < order.length; i++) {
            if (order[i] == horse) {
                place = i;
            }
        }
        if (place < 0) {
            return 0.0F;
        }
        float end = 1.0F - 0.045F * place - (place > 0 ? 0.02F : 0.0F);
        if (raceStartedAt < 0) {
            return end;
        }
        float t = MathHelper.clamp((System.currentTimeMillis() - raceStartedAt) / (float) RACE_MS, 0.0F, 1.0F);
        float wobble = 0.07F * MathHelper.sin(MathHelper.PI * t)
                * MathHelper.sin(MathHelper.PI * 3.0F * t + horse * 1.3F);
        return MathHelper.clamp(end * t + wobble, 0.0F, 1.0F);
    }

    /** O saldo na tela: durante a corrida, ainda sem o prêmio (ele entra quando ela termina). */
    private int shownCredits() {
        return raceStartedAt >= 0 ? Math.max(0, handler.credits() - raceWin) : handler.credits();
    }

    // ---------------------------------------------------------------- desenho

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        update(System.currentTimeMillis());
        runButton.active = raceStartedAt < 0 && !waiting;
        renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
        CassinoGui.drawTycoonTooltip(context, textRenderer, handler, x, y, mouseX, mouseY,
                HorseRaceScreenHandler.INVENTORY_X, HorseRaceScreenHandler.INVENTORY_Y,
                HorseRaceScreenHandler.HOTBAR_Y);
        CassinoGui.drawHouseTooltip(context, textRenderer, x, y, mouseX, mouseY, handler.house());
        int row = rowAt(mouseX, mouseY);
        if (row >= 0 && raceStartedAt < 0) {
            context.drawTooltip(textRenderer, List.of(horseName(row),
                    Text.translatable("gui.cassino.race.tip.chance", handler.chance(row) / 10.0),
                    Text.translatable("gui.cassino.race.tip.pays", odds(handler.odds(row)))), mouseX, mouseY);
        }
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        CassinoGui.drawPanel(context, x, y, backgroundWidth, backgroundHeight);

        int left = x + BOARD_X;
        int top = y + BOARD_Y;
        context.fill(left, top, left + BOARD_WIDTH, top + BOARD_HEIGHT, BOARD_BORDER);
        context.fill(left + 1, top + 1, left + BOARD_WIDTH - 1, top + BOARD_HEIGHT - 1, BOARD);
        boolean finished = raceStartedAt < 0 && handler.order()[0] >= 0;
        for (int horse = 0; horse < HorseRace.HORSES; horse++) {
            int rowY = y + ROWS_Y + horse * ROW_HEIGHT;
            if (handler.choice() == horse) {
                context.fill(left + 1, rowY, left + BOARD_WIDTH - 1, rowY + ROW_HEIGHT, ROW_SELECTED);
            }
            if (finished && handler.order()[0] == horse) {
                context.drawBorder(left + 1, rowY, BOARD_WIDTH - 2, ROW_HEIGHT, SELECTED);
            }
            // A caixinha da aposta (com a ficha no cavalo escolhido).
            int boxX = x + BOARD_X + 3;
            context.fill(boxX, rowY + 1, boxX + 13, rowY + 14, handler.choice() == horse ? SELECTED : BOARD_BORDER);
            context.fill(boxX + 1, rowY + 2, boxX + 12, rowY + 13, 0xFF2A2A2A);
            if (handler.choice() == horse) {
                MatrixStack matrices = context.getMatrices();
                matrices.push();
                matrices.translate(boxX + 1, rowY + 2, 0.0F);
                matrices.scale(0.6875F, 0.6875F, 1.0F);
                context.drawItem(CassinoGui.CHIP, 0, 0);
                matrices.pop();
            }
            drawHorse(context, horse, x + HORSE_X, rowY + ROW_HEIGHT - 1);
            // A raia.
            context.fill(x + LANE_X, rowY + 3, x + FINISH_X, rowY + 12, horse % 2 == 0 ? LANE : LANE_DARK);
        }
        // A linha de chegada quadriculada.
        for (int cellY = y + ROWS_Y; cellY < y + ROWS_Y + HorseRace.HORSES * ROW_HEIGHT; cellY += 2) {
            for (int column = 0; column < 3; column++) {
                boolean white = ((cellY - y) / 2 + column) % 2 == 0;
                int cellX = x + FINISH_X + column * 2;
                context.fill(cellX, cellY, cellX + 2, cellY + 2, white ? 0xFFFFFFFF : 0xFF000000);
            }
        }

        context.fill(x + RIGHT_X + 26, y + 56, x + RIGHT_X + RIGHT_WIDTH - 26, y + 74, INFO_BG);
        CassinoGui.drawCoinSlot(context, x + HorseRaceScreenHandler.COIN_SLOT_X,
                y + HorseRaceScreenHandler.COIN_SLOT_Y, !handler.getSlot(0).hasStack());
        CassinoGui.drawInventorySlots(context, x + HorseRaceScreenHandler.INVENTORY_X,
                y + HorseRaceScreenHandler.INVENTORY_Y, y + HorseRaceScreenHandler.HOTBAR_Y);
    }

    /** O cavalo do jogo, de lado (virado para a chegada), com os pés em ({@code feetX}, {@code feetY}). */
    private void drawHorse(DrawContext context, int horse, int feetX, int feetY) {
        HorseEntity entity = horses[horse];
        if (entity == null) {
            return;
        }
        entity.bodyYaw = -90.0F;
        entity.prevBodyYaw = -90.0F;
        entity.setYaw(-90.0F);
        entity.headYaw = -90.0F;
        entity.prevHeadYaw = -90.0F;
        entity.setPitch(0.0F);
        InventoryScreen.drawEntity(context, feetX, feetY, 7, new Quaternionf().rotateZ(MathHelper.PI), null, entity);
    }

    /** Os textos, a partir do canto da tela. */
    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        CassinoGui.drawTitle(context, textRenderer, title, backgroundWidth);
        CassinoGui.drawHouse(context, textRenderer, handler.house());

        drawSmall(context, Text.translatable("gui.cassino.race.horse"), TEXT_X, HEADER_Y, LABEL);
        drawSmall(context, Text.translatable("gui.cassino.race.track"), LANE_X, HEADER_Y, LABEL);
        for (int horse = 0; horse < HorseRace.HORSES; horse++) {
            int rowY = ROWS_Y + horse * ROW_HEIGHT;
            drawSmall(context, horseName(horse), TEXT_X, rowY + 2, LANE_X - TEXT_X - 3, NAME);
            drawSmall(context, Text.literal(odds(handler.odds(horse))), TEXT_X, rowY + 9, ODDS);
            // O número do cavalo correndo na raia.
            int markerX = LANE_X + 2 + Math.round(lanePosition(horse) * (LANE_END - LANE_X - 2));
            context.drawText(textRenderer, Integer.toString(horse + 1), markerX, rowY + 4, MARKERS[horse], true);
        }

        if (message != null) {
            context.drawText(textRenderer, message, BOARD_X + (BOARD_WIDTH - textRenderer.getWidth(message)) / 2,
                    MESSAGE_Y, messageColor, false);
        }

        Text betLabel = Text.translatable("gui.cassino.race.bet");
        context.drawText(textRenderer, betLabel, RIGHT_X + (RIGHT_WIDTH - textRenderer.getWidth(betLabel)) / 2, 45,
                CassinoGui.TEXT, false);
        Text bet = Text.literal(Integer.toString(handler.bet()));
        context.drawText(textRenderer, bet, RIGHT_X + (RIGHT_WIDTH - textRenderer.getWidth(bet)) / 2, 61, 0xFFFFFFFF,
                false);
        CassinoGui.drawAmount(context, textRenderer, Text.translatable("gui.cassino.slot.credits", shownCredits()),
                RIGHT_X, 82);
        Text day = Text.translatable("gui.cassino.race.day", handler.day());
        context.drawText(textRenderer, day, RIGHT_X + (RIGHT_WIDTH - textRenderer.getWidth(day)) / 2, 140,
                CassinoGui.TEXT, false);
        CassinoGui.drawInventoryLabel(context, textRenderer, HorseRaceScreenHandler.INVENTORY_X,
                HorseRaceScreenHandler.INVENTORY_Y);
        CassinoGui.drawTycoon(context, textRenderer, handler, HorseRaceScreenHandler.INVENTORY_X,
                HorseRaceScreenHandler.INVENTORY_Y, HorseRaceScreenHandler.HOTBAR_Y);
    }

    /** Texto em 3/4 do tamanho (para caber na fileira). */
    private void drawSmall(DrawContext context, Text text, int textX, int textY, int color) {
        drawSmall(context, text, textX, textY, Integer.MAX_VALUE, color);
    }

    /** Texto pequeno (3/4 do tamanho) que diminui mais se passar de {@code maxWidth} (os nomes compridos). */
    private void drawSmall(DrawContext context, Text text, int textX, int textY, int maxWidth, int color) {
        float scale = Math.min(0.75F, maxWidth / (float) textRenderer.getWidth(text));
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate(textX, textY + (0.75F - scale) * 4.0F, 0.0F);
        matrices.scale(scale, scale, 1.0F);
        context.drawText(textRenderer, text, 0, 0, color, false);
        matrices.pop();
    }
}
