package com.edugcs.caixademusica.client;

import com.edugcs.caixademusica.CaixaDeMusica;
import com.edugcs.caixademusica.PlaybackState;
import com.edugcs.caixademusica.block.MusicBoxBlockEntity;
import com.edugcs.caixademusica.network.EntityActionPayload;
import com.edugcs.caixademusica.network.MusicBoxActionPayload;
import com.edugcs.caixademusica.network.PortableActionPayload;
import java.util.Locale;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

/**
 * Tela que abre ao clicar com o botão direito na caixa (bloco), ao usar a caixa portátil
 * (item na mão), com a tecla de atalho (caixa no inventário, ou a do baú do burro/barco por perto)
 * ou ao clicar no burro/barco com a Caixa de Música na mão: botões de controle, capa, título, tempo e barra de progresso
 * (clique para pular), link/nome da música e alcance (perto/longe) da caixa.
 */
public class MusicBoxScreen extends Screen {
    private static final int PANEL_WIDTH = 300;
    private static final int PANEL_HEIGHT = 152;

    private static final int PANEL_FILL = 0xFFC6C6C6;
    private static final int PANEL_LIGHT = 0xFFFFFFFF;
    private static final int PANEL_DARK = 0xFF555555;
    private static final int TITLE_COLOR = 0xFF404040;
    private static final int INFO_BG = 0xFFFFFFFF;
    private static final int INFO_BORDER = 0xFF373737;
    private static final int THUMB_BG = 0xFF2B2B2B;
    private static final int BLACK = 0xFF000000;
    private static final int GREEN_TEXT = 0xFF0A8A0A;
    private static final int GRAY_TEXT = 0xFF555555;
    private static final int RED_TEXT = 0xFFC00000;
    private static final int ORANGE_TEXT = 0xFFB86B00;
    private static final int BAR_GREEN = 0xFF3DDC3D;
    private static final int RANGE_GREEN = 0xFF2F9E44;

    private static final int THUMB_WIDTH = 64;
    private static final int THUMB_HEIGHT = 36;
    /** Quanto tempo mostrar o ponto escolhido ao pular, enquanto a música recarrega nesse ponto. */
    private static final long SEEK_PREVIEW_MS = 15_000;
    /** Quanto tempo um aviso da tela fica no lugar do título da música. */
    private static final long MESSAGE_MS = 4_000;

    // Esta tela controla uma de três coisas; as outras duas ficam com null / -1.
    /** O bloco da caixa de música. */
    private final BlockPos pos;
    /** O espaço do inventário com a caixa portátil (0 a 35, ou 40 = mão secundária). */
    private final int slot;
    /** A entidade (burro, mula, lhama ou barco) cuja caixa embutida no baú esta tela controla. */
    private final int entityId;

    private int panelX;
    private int panelY;
    private TextFieldWidget urlField;
    private BarWidget progressBar;
    private BarWidget nearBar;
    private BarWidget farBar;
    /** Aviso da própria tela (link vazio, Spotify, sem playlist...); some sozinho depois de {@link #MESSAGE_MS}. */
    private Text localMessage;
    private int localMessageColor;
    private long localMessageUntil;

    private long seekPreviewMs = -1;
    private long seekPreviewAt;
    private long seekFromPlayId;

    private MusicBoxScreen(Text title, BlockPos pos, int slot, int entityId) {
        super(title);
        this.pos = pos;
        this.slot = slot;
        this.entityId = entityId;
    }

    /** Tela da caixa de música (o bloco) em {@code pos}. */
    public static MusicBoxScreen forBlock(BlockPos pos) {
        return new MusicBoxScreen(Text.translatable("gui.caixademusica.title"), pos, -1, -1);
    }

    /** Tela da caixa portátil que está no espaço {@code slot} do inventário do jogador. */
    public static MusicBoxScreen forItem(int slot) {
        return new MusicBoxScreen(Text.translatable("gui.caixademusica.portable_title"), null, slot, -1);
    }

    /** Tela da caixa embutida no baú do burro, mula, lhama ou barco {@code entityId}. */
    public static MusicBoxScreen forEntity(int entityId) {
        MinecraftClient client = MinecraftClient.getInstance();
        Entity entity = client.world == null ? null : client.world.getEntityById(entityId);
        Text name = entity == null ? Text.empty() : entity.getName();
        return new MusicBoxScreen(Text.translatable("gui.caixademusica.entity_title", name), null, -1, entityId);
    }

    // ---------------------------------------------------------------- montagem

    @Override
    protected void init() {
        panelX = (width - PANEL_WIDTH) / 2;
        panelY = (height - PANEL_HEIGHT) / 2;

        PlaybackState current = state();
        String previous = urlField != null ? urlField.getText() : current != null ? current.url() : "";

        // Fileira de botões: música anterior, tocar, pausar, parar, próxima | volume -, +, mudo.
        int rowY = panelY + 18;
        int left = panelX + 8;
        addDrawableChild(new IconButton(left, rowY, 22, 18, IconButton.PREV, tip("prev"), () -> jump(-1),
                this::isPlaying, () -> false));
        addDrawableChild(new IconButton(left + 24, rowY, 22, 18, IconButton.PLAY, tip("play"), this::play,
                () -> true, () -> false));
        addDrawableChild(new IconButton(left + 48, rowY, 22, 18, IconButton.PAUSE, tip("pause"), this::togglePause,
                this::isPlaying, this::isPaused));
        addDrawableChild(new IconButton(left + 72, rowY, 22, 18, IconButton.STOP, tip("stop"), this::stop,
                this::isPlaying, () -> false));
        addDrawableChild(new IconButton(left + 96, rowY, 22, 18, IconButton.NEXT, tip("next"), () -> jump(1),
                this::isPlaying, () -> false));

        int right = panelX + PANEL_WIDTH - 8 - 70;
        addDrawableChild(new IconButton(right, rowY, 22, 18, IconButton.VOLUME_DOWN, tip("volume_down"),
                () -> ClientSettings.changeVolume(-10), () -> true, () -> false));
        addDrawableChild(new IconButton(right + 24, rowY, 22, 18, IconButton.VOLUME_UP, tip("volume_up"),
                () -> ClientSettings.changeVolume(10), () -> true, () -> false));
        addDrawableChild(new IconButton(right + 48, rowY, 22, 18, IconButton.MUTE, tip("mute"),
                ClientSettings::toggleMute, () -> true, ClientSettings::muted));

        // Barra de progresso (dentro do painel branco): clique ou arraste para pular na música.
        int infoX = panelX + 8;
        int infoY = panelY + 40;
        progressBar = new BarWidget(infoX + 3, infoY + 43, PANEL_WIDTH - 16 - 6, 7, 0.0, 1.0,
                () -> {
                    Display d = display();
                    return d.durationMs > 0 ? Math.min(1.0, d.positionMs / (double) d.durationMs) : 0.0;
                },
                null, BAR_GREEN, true, this::canSeek, this::seekTo);
        addDrawableChild(progressBar);

        urlField = new TextFieldWidget(textRenderer, panelX + 8, panelY + 98, PANEL_WIDTH - 16, 20,
                Text.translatable("gui.caixademusica.url"));
        urlField.setMaxLength(CaixaDeMusica.MAX_URL_LENGTH);
        urlField.setText(previous);
        urlField.setPlaceholder(Text.translatable("gui.caixademusica.url_hint").formatted(Formatting.DARK_GRAY));
        addDrawableChild(urlField);
        setInitialFocus(urlField);

        // Alcance: até onde o volume é total (perto) e até onde ainda se ouve (longe).
        int halfWidth = (PANEL_WIDTH - 16 - 4) / 2;
        nearBar = new BarWidget(panelX + 8, panelY + 124, halfWidth, 20, 0.0, PlaybackState.MAX_FAR,
                () -> rangeState().near(),
                () -> Text.translatable("gui.caixademusica.near", Math.round(nearBar.displayValue())),
                RANGE_GREEN, false, () -> true, value -> sendRange((int) Math.round(value), rangeState().far()));
        farBar = new BarWidget(panelX + 8 + halfWidth + 4, panelY + 124, halfWidth, 20, 0.0, PlaybackState.MAX_FAR,
                () -> rangeState().far(),
                () -> Text.translatable("gui.caixademusica.far", Math.round(farBar.displayValue())),
                RANGE_GREEN, false, () -> true, value -> sendRange(rangeState().near(), (int) Math.round(value)));
        addDrawableChild(nearBar);
        addDrawableChild(farBar);
    }

    private static Text tip(String key) {
        return Text.translatable("gui.caixademusica.tip." + key);
    }

    // ---------------------------------------------------------------- estado

    // Durante o desenho de um quadro, o estado e o painel são calculados uma vez só e reaproveitados pelos
    // botões e barras (cada um consultava tudo de novo, umas 20 vezes por quadro). Fora do desenho (cliques,
    // tick) sempre se calcula na hora.
    private boolean inFrame;
    private PlaybackState frameState;
    private Display frameDisplay;

    /** O que a caixa está tocando agora, ou {@code null} se ela sumiu (bloco quebrado, item guardado). */
    private PlaybackState state() {
        return inFrame ? frameState : computeState();
    }

    private PlaybackState computeState() {
        if (client == null || client.world == null || client.player == null) {
            return null;
        }
        if (pos != null) {
            return client.world.getBlockEntity(pos) instanceof MusicBoxBlockEntity box ? box.getState() : null;
        }
        if (entityId >= 0) {
            if (client.world.getEntityById(entityId) == null) {
                return null;
            }
            // Ainda sem nenhum aviso do servidor: a caixa do baú ainda não foi usada, está com os valores padrão.
            PlaybackState known = MusicBoxClientManager.sourceState(entityId);
            return known != null ? known : PlaybackState.ENTITY_DEFAULT;
        }
        PlayerInventory inventory = client.player.getInventory();
        if (slot < 0 || slot >= inventory.size()) {
            return null;
        }
        ItemStack stack = inventory.getStack(slot);
        if (!stack.isOf(CaixaDeMusica.PORTABLE_ITEM)) {
            return null;
        }
        return stack.getOrDefault(CaixaDeMusica.PORTABLE_STATE, PlaybackState.PORTABLE_DEFAULT);
    }

    private PlaybackState rangeState() {
        PlaybackState state = state();
        if (state != null) {
            return state;
        }
        return pos != null ? PlaybackState.BLOCK_DEFAULT
                : entityId >= 0 ? PlaybackState.ENTITY_DEFAULT : PlaybackState.PORTABLE_DEFAULT;
    }

    private boolean isPlaying() {
        PlaybackState state = state();
        return state != null && state.playing();
    }

    private boolean isPaused() {
        PlaybackState state = state();
        return state != null && state.playing() && state.paused();
    }

    /** Chave da sessão de áudio desta caixa no {@link MusicBoxClientManager}. */
    private Object sessionKey() {
        if (pos != null) {
            return pos;
        }
        if (entityId >= 0) {
            return entityId;
        }
        // A caixa que o jogador carrega toca "em nome dele": a chave é o id da entidade dele.
        return client == null || client.player == null ? null : client.player.getId();
    }

    private boolean canSeek() {
        PlaybackState state = state();
        if (state == null || !state.audible()) {
            return false;
        }
        MusicBoxSession session = MusicBoxClientManager.session(sessionKey());
        MusicBoxSession.Now now = MusicBoxClientManager.now(sessionKey());
        return session != null && session.state() == MusicBoxSession.State.PLAYING && now != null
                && now.durationMs() > 0;
    }

    // ---------------------------------------------------------------- ações

    private void send(int action, int index, int extra, String url) {
        if (pos != null) {
            ClientPlayNetworking.send(new MusicBoxActionPayload(pos, action, index, extra, url));
        } else if (entityId >= 0) {
            ClientPlayNetworking.send(new EntityActionPayload(entityId, action, index, extra, url));
        } else {
            ClientPlayNetworking.send(new PortableActionPayload(slot, action, index, extra, url));
        }
    }

    private void play() {
        PlaybackState state = state();
        if (state == null) {
            return;
        }
        String text = urlField.getText().trim();
        if (state.playing() && state.paused() && (text.isEmpty() || text.equals(state.url()))) {
            localMessage = null;
            send(MusicBoxActionPayload.RESUME, 0, 0, "");
            return;
        }
        if (text.isEmpty()) {
            text = state.url(); // sem digitar nada: toca de novo o último link
        }
        if (text.isEmpty()) {
            showMessage(Text.translatable("gui.caixademusica.empty"), RED_TEXT);
            return;
        }
        if (AudioLibrary.isSpotify(text)) {
            showMessage(Text.translatable("gui.caixademusica.spotify"), RED_TEXT);
            return;
        }
        if (text.length() > CaixaDeMusica.MAX_URL_LENGTH) {
            text = text.substring(0, CaixaDeMusica.MAX_URL_LENGTH);
        }
        localMessage = null;
        seekPreviewMs = -1;
        send(MusicBoxActionPayload.PLAY, 0, 0, text);
    }

    private void togglePause() {
        PlaybackState state = state();
        if (state == null || !state.playing()) {
            return;
        }
        send(state.paused() ? MusicBoxActionPayload.RESUME : MusicBoxActionPayload.PAUSE, 0, 0, "");
    }

    private void stop() {
        localMessage = null;
        seekPreviewMs = -1;
        send(MusicBoxActionPayload.STOP, 0, 0, "");
    }

    private void jump(int delta) {
        PlaybackState state = state();
        if (state == null || !state.playing()) {
            return;
        }
        MusicBoxSession session = MusicBoxClientManager.session(sessionKey());
        int target;
        if (session != null && session.trackCount() > 0) {
            int count = session.trackCount();
            if (count <= 1) {
                // Um vídeo só (sem playlist nem Mix): não há anterior nem próxima. Avisa, em vez de não fazer nada.
                showMessage(Text.translatable("gui.caixademusica.single_track"), ORANGE_TEXT);
                return;
            }
            target = Math.floorMod(session.audibleIndex() + delta, count);
        } else {
            target = state.startIndex() + delta;
        }
        localMessage = null;
        seekPreviewMs = -1;
        send(MusicBoxActionPayload.JUMP, target, 0, "");
    }

    private void showMessage(Text text, int color) {
        localMessage = text;
        localMessageColor = color;
        localMessageUntil = System.currentTimeMillis() + MESSAGE_MS;
    }

    /** Pular na música: o servidor recebe só quantos ms avançar ou voltar a partir do ponto atual. */
    private void seekTo(double ratio) {
        MusicBoxSession.Now now = MusicBoxClientManager.now(sessionKey());
        MusicBoxSession session = MusicBoxClientManager.session(sessionKey());
        if (now == null || session == null || now.durationMs() <= 0) {
            return;
        }
        long target = Math.max(0L, Math.min(now.durationMs() - 1000L, Math.round(ratio * now.durationMs())));
        long delta = target - now.positionMs();
        seekPreviewMs = target;
        seekPreviewAt = System.currentTimeMillis();
        seekFromPlayId = session.playId;
        send(MusicBoxActionPayload.SEEK, (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, delta)), 0, "");
    }

    private void sendRange(int near, int far) {
        send(MusicBoxActionPayload.RANGE, near, far, "");
    }

    // ---------------------------------------------------------------- teclado e mouse

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) && urlField.isFocused()) {
            play();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        // Entrega o valor arrastado mesmo que o mouse seja solto fora da barra.
        if (progressBar != null) {
            progressBar.finishDrag();
        }
        if (nearBar != null) {
            nearBar.finishDrag();
        }
        if (farBar != null) {
            farBar.finishDrag();
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void tick() {
        super.tick();
        // Fecha sozinha se a caixa sumir (bloco quebrado, item guardado, animal ou barco longe) ou se o
        // jogador se afastar do bloco ou do animal/barco.
        if (client == null || client.player == null || state() == null
                || (pos != null && client.player.getPos().squaredDistanceTo(Vec3d.ofCenter(pos)) > 12 * 12)
                || (entityId >= 0 && client.player.squaredDistanceTo(client.world.getEntityById(entityId)) > 12 * 12)) {
            close();
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    // ---------------------------------------------------------------- desenho

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        frameState = computeState();
        frameDisplay = null;
        inFrame = true;
        try {
            super.render(context, mouseX, mouseY, delta);
        } finally {
            inFrame = false;
            frameState = null;
            frameDisplay = null;
        }
    }

    /** Tudo o que é desenhado e fica atrás dos botões e barras (painel, capa, título, tempo). */
    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);
        drawPanel(context);
        drawInfo(context);
    }

    private void drawPanel(DrawContext context) {
        int x = panelX;
        int y = panelY;
        context.fill(x, y, x + PANEL_WIDTH, y + PANEL_HEIGHT, BLACK);
        context.fill(x + 1, y + 1, x + PANEL_WIDTH - 1, y + PANEL_HEIGHT - 1, PANEL_FILL);
        context.fill(x + 1, y + 1, x + PANEL_WIDTH - 1, y + 2, PANEL_LIGHT);
        context.fill(x + 1, y + 1, x + 2, y + PANEL_HEIGHT - 1, PANEL_LIGHT);
        context.fill(x + 1, y + PANEL_HEIGHT - 2, x + PANEL_WIDTH - 1, y + PANEL_HEIGHT - 1, PANEL_DARK);
        context.fill(x + PANEL_WIDTH - 2, y + 1, x + PANEL_WIDTH - 1, y + PANEL_HEIGHT - 1, PANEL_DARK);
        context.drawText(textRenderer, title, x + 8, y + 6, TITLE_COLOR, false);
    }

    private void drawInfo(DrawContext context) {
        Display display = display();
        int x = panelX + 8;
        int y = panelY + 40;
        int w = PANEL_WIDTH - 16;
        int h = 52;
        context.fill(x, y, x + w, y + h, INFO_BG);
        context.drawBorder(x, y, w, h, INFO_BORDER);

        // Capa (ou um ♪ enquanto não tem)
        int thumbX = x + 3;
        int thumbY = y + 3;
        context.fill(thumbX, thumbY, thumbX + THUMB_WIDTH, thumbY + THUMB_HEIGHT, THUMB_BG);
        ArtworkCache.Art art = ArtworkCache.get(display.artworkUrl);
        if (art != null) {
            // Recorta no meio para caber em 16:9, sem esticar.
            int sourceHeight = Math.min(art.height(), art.width() * 9 / 16);
            float v = (art.height() - sourceHeight) / 2.0F;
            context.drawTexture(art.id(), thumbX, thumbY, THUMB_WIDTH, THUMB_HEIGHT, 0.0F, v, art.width(), sourceHeight,
                    art.width(), art.height());
        } else {
            context.drawCenteredTextWithShadow(textRenderer, "♪", thumbX + THUMB_WIDTH / 2, thumbY + 14, BAR_GREEN);
        }

        // Título, centralizado na área à direita da capa
        int areaX = x + THUMB_WIDTH + 9;
        int areaWidth = w - THUMB_WIDTH - 13;
        String title = trim(display.title, areaWidth);
        context.drawText(textRenderer, title, areaX + (areaWidth - textRenderer.getWidth(title)) / 2, y + 6,
                display.titleColor, false);

        // Tempo (esquerda), posição na playlist ou "Pausada" (meio), volume pessoal (direita)
        int rowY = y + 22;
        String total = display.durationMs > 0 ? time(display.durationMs)
                : display.live ? Text.translatable("gui.caixademusica.live").getString() : "--:--";
        context.drawText(textRenderer, time(display.positionMs) + "/" + total, areaX, rowY, GREEN_TEXT, false);
        if (!display.note.isEmpty()) {
            context.drawText(textRenderer, display.note, areaX + (areaWidth - textRenderer.getWidth(display.note)) / 2,
                    rowY, display.noteColor, false);
        }
        String volume = ClientSettings.muted()
                ? Text.translatable("gui.caixademusica.muted").getString()
                : Text.translatable("gui.caixademusica.volume", ClientSettings.volume()).getString();
        context.drawText(textRenderer, volume, x + w - 4 - textRenderer.getWidth(volume), rowY,
                ClientSettings.muted() ? RED_TEXT : GREEN_TEXT, false);
    }

    /** O que mostrar no painel branco agora. */
    private Display display() {
        if (!inFrame) {
            return computeDisplay();
        }
        if (frameDisplay == null) {
            frameDisplay = computeDisplay();
        }
        return frameDisplay;
    }

    private Display computeDisplay() {
        if (localMessage != null && System.currentTimeMillis() > localMessageUntil) {
            localMessage = null;
        }
        if (localMessage != null) {
            return new Display(localMessage.getString(), localMessageColor, "", 0, 0, 0, null, false);
        }
        PlaybackState state = state();
        if (state == null || !state.playing()) {
            return new Display(Text.translatable("gui.caixademusica.status.stopped").getString(), GRAY_TEXT, "", 0, 0, 0,
                    null, false);
        }

        MusicBoxSession.Now now = MusicBoxClientManager.now(sessionKey());
        long position = now != null ? now.positionMs() : 0;
        long duration = now != null ? now.durationMs() : 0;
        String artwork = now != null ? now.artworkUrl() : null;
        String title = now != null ? now.title() : "";
        String note = now != null && now.count() > 1 ? (now.index() + 1) + "/" + now.count() : "";

        if (state.paused()) {
            String paused = Text.translatable("gui.caixademusica.status.paused").getString();
            return new Display(title.isEmpty() ? paused : title, BLACK, paused, ORANGE_TEXT, position, duration, artwork,
                    false);
        }
        if (client != null && MusicBoxClientManager.isMuted(client)) {
            return new Display(Text.translatable("gui.caixademusica.status.muted").getString(), RED_TEXT, "", 0, 0, 0,
                    null, false);
        }

        MusicBoxSession session = MusicBoxClientManager.session(sessionKey());
        // Depois de pular, mostra o ponto escolhido até a música recarregar nele.
        if (seekPreviewMs >= 0) {
            boolean reloaded = session != null && session.state() == MusicBoxSession.State.PLAYING
                    && session.playId != seekFromPlayId;
            if (reloaded || System.currentTimeMillis() - seekPreviewAt > SEEK_PREVIEW_MS) {
                seekPreviewMs = -1;
            } else {
                position = seekPreviewMs;
            }
        }
        if (session == null) {
            return new Display(Text.translatable("gui.caixademusica.status.loading").getString(), GRAY_TEXT, note,
                    GRAY_TEXT, position, duration, artwork, false);
        }
        return switch (session.state()) {
            case LOADING -> new Display(Text.translatable("gui.caixademusica.status.loading").getString(), GRAY_TEXT, note,
                    GRAY_TEXT, position, duration, artwork, false);
            case BUFFERING -> new Display(Text.translatable("gui.caixademusica.status.buffering").getString(), GRAY_TEXT,
                    note, GRAY_TEXT, position, duration, artwork, false);
            case ERROR -> new Display(Text.translatable("gui.caixademusica.status.error").getString() + " "
                    + session.error(), RED_TEXT, "", 0, 0, 0, null, false);
            case PLAYING -> new Display(title, BLACK, note, GRAY_TEXT, position, duration, artwork, duration <= 0);
        };
    }

    private String trim(String text, int maxWidth) {
        if (textRenderer.getWidth(text) <= maxWidth) {
            return text;
        }
        return textRenderer.trimToWidth(text, maxWidth - textRenderer.getWidth("...")) + "...";
    }

    private static String time(long ms) {
        long seconds = Math.max(0L, ms / 1000L);
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return hours > 0
                ? String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, secs)
                : String.format(Locale.ROOT, "%02d:%02d", minutes, secs);
    }

    private record Display(String title, int titleColor, String note, int noteColor, long positionMs, long durationMs,
                           String artworkUrl, boolean live) {
    }
}
