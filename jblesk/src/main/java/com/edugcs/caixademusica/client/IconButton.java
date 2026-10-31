package com.edugcs.caixademusica.client;

import java.util.function.BooleanSupplier;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.text.Text;

/**
 * Botão escuro com um ícone verde de 9x9 pixels desenhado em código (sem texturas),
 * no estilo do player de música da referência.
 */
final class IconButton extends ClickableWidget {
    private static final int ICON_ON = 0xFF3DDC3D;
    private static final int ICON_OFF = 0xFF5E5E5E;
    private static final int BG = 0xFF2E2E2E;
    private static final int BG_HOVER = 0xFF474747;
    private static final int BG_TOGGLED = 0xFF5F5F5F;
    private static final int BG_DISABLED = 0xFF262626;
    private static final int BEVEL_LIGHT = 0xFF5C5C5C;
    private static final int BEVEL_DARK = 0xFF1A1A1A;

    // Ícones: "X" é pixel aceso. Todos 9x9.
    static final String[] PLAY = {
            ".........",
            "..X......",
            "..XX.....",
            "..XXX....",
            "..XXXX...",
            "..XXX....",
            "..XX.....",
            "..X......",
            "........."};
    static final String[] PAUSE = {
            ".........",
            "..XX.XX..",
            "..XX.XX..",
            "..XX.XX..",
            "..XX.XX..",
            "..XX.XX..",
            "..XX.XX..",
            "..XX.XX..",
            "........."};
    static final String[] STOP = {
            ".........",
            ".XXXXXXX.",
            ".XXXXXXX.",
            ".XXXXXXX.",
            ".XXXXXXX.",
            ".XXXXXXX.",
            ".XXXXXXX.",
            ".XXXXXXX.",
            "........."};
    static final String[] PREV = {
            ".........",
            ".X....X..",
            ".X...XX..",
            ".X..XXX..",
            ".X.XXXX..",
            ".X..XXX..",
            ".X...XX..",
            ".X....X..",
            "........."};
    static final String[] NEXT = {
            ".........",
            "..X....X.",
            "..XX...X.",
            "..XXX..X.",
            "..XXXX.X.",
            "..XXX..X.",
            "..XX...X.",
            "..X....X.",
            "........."};
    static final String[] VOLUME_DOWN = {
            ".........",
            "...X.....",
            "..XX.....",
            "XXXX.....",
            "XXXX.XXX.",
            "XXXX.....",
            "..XX.....",
            "...X.....",
            "........."};
    static final String[] VOLUME_UP = {
            ".........",
            "...X.....",
            "..XX..X..",
            "XXXX..X..",
            "XXXX.XXX.",
            "XXXX..X..",
            "..XX..X..",
            "...X.....",
            "........."};
    static final String[] MUTE = {
            ".........",
            "...X.....",
            "..XX.X.X.",
            "XXXX..X..",
            "XXXX..X..",
            "XXXX..X..",
            "..XX.X.X.",
            "...X.....",
            "........."};

    /** Os trechos acesos do ícone, calculados uma vez: (coluna, linha, comprimento) de cada um. */
    private final int[] runs;
    private final Runnable onPress;
    private final BooleanSupplier enabled;
    private final BooleanSupplier toggled;

    IconButton(int x, int y, int width, int height, String[] icon, Text tooltip, Runnable onPress,
               BooleanSupplier enabled, BooleanSupplier toggled) {
        super(x, y, width, height, tooltip);
        this.runs = compile(icon);
        this.onPress = onPress;
        this.enabled = enabled;
        this.toggled = toggled;
        setTooltip(Tooltip.of(tooltip));
    }

    @Override
    public void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean on = enabled.getAsBoolean();
        this.active = on;
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();

        int background = !on ? BG_DISABLED : toggled.getAsBoolean() ? BG_TOGGLED : isHovered() ? BG_HOVER : BG;
        context.fill(x, y, x + w, y + h, 0xFF000000);
        context.fill(x + 1, y + 1, x + w - 1, y + h - 1, background);
        if (on) {
            context.fill(x + 1, y + 1, x + w - 1, y + 2, BEVEL_LIGHT);
            context.fill(x + 1, y + 1, x + 2, y + h - 1, BEVEL_LIGHT);
            context.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, BEVEL_DARK);
            context.fill(x + w - 2, y + 1, x + w - 1, y + h - 1, BEVEL_DARK);
        }

        int color = on ? ICON_ON : ICON_OFF;
        int originX = x + (w - 9) / 2;
        int originY = y + (h - 9) / 2;
        for (int i = 0; i < runs.length; i += 3) {
            int left = originX + runs[i];
            int top = originY + runs[i + 1];
            context.fill(left, top, left + runs[i + 2], top + 1, color);
        }
    }

    /** Junta os pixels acesos vizinhos de cada linha em trechos, para desenhar um retângulo por trecho, não por pixel. */
    private static int[] compile(String[] icon) {
        int[] found = new int[icon.length * 5 * 3]; // no máximo 5 trechos por linha de 9 pixels
        int count = 0;
        for (int row = 0; row < icon.length; row++) {
            String line = icon[row];
            int col = 0;
            while (col < line.length()) {
                if (line.charAt(col) != 'X') {
                    col++;
                    continue;
                }
                int start = col;
                while (col < line.length() && line.charAt(col) == 'X') {
                    col++;
                }
                found[count++] = start;
                found[count++] = row;
                found[count++] = col - start;
            }
        }
        return java.util.Arrays.copyOf(found, count);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        onPress.run();
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }
}
