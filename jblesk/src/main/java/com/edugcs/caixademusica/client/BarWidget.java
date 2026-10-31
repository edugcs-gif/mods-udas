package com.edugcs.caixademusica.client;

import java.util.function.BooleanSupplier;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

/**
 * Barra horizontal que mostra um valor e deixa arrastar para escolher outro: serve para a barra de
 * progresso da música e para os controles de alcance. O novo valor só é entregue ao soltar o mouse
 * ({@code onCommit}), para não mandar um pacote ao servidor a cada pixel arrastado.
 */
final class BarWidget extends ClickableWidget {
    private static final int TRACK = 0xFF2E2E2E;
    private static final int TRACK_OFF = 0xFF3A3A3A;
    private static final int FILL_OFF = 0xFF4A4A4A;
    private static final int KNOB = 0xFFE8E8E8;

    private final double min;
    private final double max;
    private final DoubleSupplier current;
    private final Supplier<Text> label;
    private final DoubleConsumer onCommit;
    private final BooleanSupplier enabled;
    private final int fillColor;
    private final boolean knob;

    private boolean dragging;
    private double dragValue;

    /**
     * @param label texto no meio da barra, ou {@code null} para nenhum
     * @param knob  desenha uma marca no ponto atual (a barra de progresso usa)
     */
    BarWidget(int x, int y, int width, int height, double min, double max, DoubleSupplier current, Supplier<Text> label,
              int fillColor, boolean knob, BooleanSupplier enabled, DoubleConsumer onCommit) {
        super(x, y, width, height, Text.empty());
        this.min = min;
        this.max = max;
        this.current = current;
        this.label = label;
        this.fillColor = fillColor;
        this.knob = knob;
        this.enabled = enabled;
        this.onCommit = onCommit;
    }

    /** O valor que a barra mostra: o que está sendo arrastado, ou o atual. */
    double displayValue() {
        return dragging ? dragValue : current.getAsDouble();
    }

    /** Entrega o valor arrastado, se estava arrastando (a tela chama isto ao soltar o mouse, mesmo fora da barra). */
    void finishDrag() {
        if (dragging) {
            dragging = false;
            onCommit.accept(dragValue);
        }
    }

    private double valueAt(double mouseX) {
        double ratio = (mouseX - (getX() + 1)) / Math.max(1, getWidth() - 2);
        ratio = Math.max(0.0, Math.min(1.0, ratio));
        return min + ratio * (max - min);
    }

    @Override
    public void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean on = enabled.getAsBoolean();
        this.active = on;
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();

        context.fill(x, y, x + w, y + h, 0xFF000000);
        context.fill(x + 1, y + 1, x + w - 1, y + h - 1, on ? TRACK : TRACK_OFF);

        double ratio = max > min ? (displayValue() - min) / (max - min) : 0.0;
        ratio = Math.max(0.0, Math.min(1.0, ratio));
        int fillWidth = (int) Math.round(ratio * (w - 2));
        if (fillWidth > 0) {
            context.fill(x + 1, y + 1, x + 1 + fillWidth, y + h - 1, on ? fillColor : FILL_OFF);
        }
        if (knob && on) {
            int center = x + 1 + fillWidth;
            int left = Math.max(x + 1, center - 1);
            int right = Math.min(x + w - 1, center + 1);
            context.fill(left, y - 1, right, y + h + 1, KNOB);
        }
        if (label != null) {
            Text text = label.get();
            context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, text, x + w / 2,
                    y + (h - 8) / 2, 0xFFFFFFFF);
        }
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        dragging = true;
        dragValue = valueAt(mouseX);
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (dragging) {
            dragValue = valueAt(mouseX);
        }
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }
}
