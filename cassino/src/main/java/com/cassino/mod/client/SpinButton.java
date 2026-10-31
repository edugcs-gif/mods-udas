package com.cassino.mod.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

/** O botão verde grande "GIRAR!". */
final class SpinButton extends ClickableWidget {
    private static final int OUTLINE = 0xFF1F3D17;
    private static final int FILL = 0xFF3C8E2C;
    private static final int FILL_HOVER = 0xFF4CA83A;
    private static final int FILL_OFF = 0xFF4A6B45;
    private static final int TOP = 0xFF7FD36A;
    private static final int TOP_OFF = 0xFF5E7A5A;
    private static final int BOTTOM = 0xFF235A19;

    private final Runnable onPress;

    SpinButton(int x, int y, int width, int height, Text message, Runnable onPress) {
        super(x, y, width, height, message);
        this.onPress = onPress;
    }

    @Override
    public void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();
        context.fill(x, y, x + w, y + h, OUTLINE);
        context.fill(x + 1, y + 1, x + w - 1, y + h - 1, !active ? FILL_OFF : isHovered() ? FILL_HOVER : FILL);
        context.fill(x + 1, y + 1, x + w - 1, y + 2, active ? TOP : TOP_OFF);
        context.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, BOTTOM);

        // O texto em tamanho 1,5x, como o letreiro de uma máquina.
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate(x + w / 2.0F, y + h / 2.0F - 6.0F, 0.0F);
        matrices.scale(1.5F, 1.5F, 1.0F);
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
