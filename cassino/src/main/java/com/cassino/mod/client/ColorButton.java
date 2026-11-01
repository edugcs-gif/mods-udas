package com.cassino.mod.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

/** Um botão colorido com o texto grande, como o letreiro de uma máquina: GIRAR!, PEDIR, PARAR, DOBRAR. */
final class ColorButton extends ClickableWidget {
    /** As cores de um botão: contorno, fundo, fundo com o mouse em cima, fundo desligado, brilho de cima e sombra. */
    record Colors(int outline, int fill, int hover, int off, int top, int bottom) {
    }

    static final Colors GREEN = new Colors(0xFF1F3D17, 0xFF3C8E2C, 0xFF4CA83A, 0xFF4A6B45, 0xFF7FD36A, 0xFF235A19);
    static final Colors RED = new Colors(0xFF3D1414, 0xFFB52A2A, 0xFFCC3A3A, 0xFF6B4545, 0xFFE07070, 0xFF6E1515);
    static final Colors BLUE = new Colors(0xFF14243D, 0xFF3E5F8E, 0xFF4C72A8, 0xFF4A5566, 0xFF8AA6D6, 0xFF223A5E);
    static final Colors GOLD = new Colors(0xFF4A3608, 0xFFC08A10, 0xFFD89E1C, 0xFF6B5A35, 0xFFF2D06A, 0xFF7A5606);

    private final Colors colors;
    private final float textScale;
    private final Runnable onPress;

    ColorButton(int x, int y, int width, int height, Text message, Colors colors, float textScale, Runnable onPress) {
        super(x, y, width, height, message);
        this.colors = colors;
        this.textScale = textScale;
        this.onPress = onPress;
    }

    @Override
    public void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();
        context.fill(x, y, x + w, y + h, colors.outline());
        context.fill(x + 1, y + 1, x + w - 1, y + h - 1,
                !active ? colors.off() : isHovered() ? colors.hover() : colors.fill());
        context.fill(x + 1, y + 1, x + w - 1, y + 2, active ? colors.top() : colors.off());
        context.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, colors.bottom());

        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate(x + w / 2.0F, y + h / 2.0F - 4.0F * textScale, 0.0F);
        matrices.scale(textScale, textScale, 1.0F);
        context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, getMessage(), 0, 0,
                active ? 0xFFFFFF : 0xA0A0A0);
        matrices.pop();
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
