package com.edugcs.transparencia;

import com.mojang.blaze3d.systems.RenderSystem;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Inventário Transparente: o fundo das telas com inventário (inventário, baús, fornalhas, mesas...) fica
 * semitransparente, e o escurecimento atrás delas fica mais leve, para dar para ver o mundo. Os itens e os textos
 * continuam nítidos (são desenhados depois do fundo).
 *
 * <p>A tecla (I, por padrão) troca o quanto o fundo aparece: 100%, 80%, 60%, 40% e 20%. Funciona com o inventário
 * aberto, e a escolha fica salva em {@code config/transparencia.properties}. É só visual: só precisa estar no PC de
 * quem quer usar.
 */
public class Transparencia implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("Inventario Transparente");

    /** Quanto o fundo aparece, em %, na ordem em que a tecla troca. */
    private static final int[] LEVELS = {100, 80, 60, 40, 20};
    private static final int DEFAULT_OPACITY = 60;
    /** Quanto tempo o aviso "Fundo do inventário: 60%" fica no canto da tela. */
    private static final long HINT_MS = 1_500;

    private static int opacity = DEFAULT_OPACITY;
    private static long hintUntil;
    private static KeyBinding cycleKey;

    @Override
    public void onInitializeClient() {
        load();
        cycleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.transparencia.cycle",
                InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_I, "key.categories.inventory"));

        // Fora das telas: a tecla troca e avisa na barra de ação.
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (cycleKey.wasPressed()) {
                cycle();
                if (client.player != null) {
                    client.player.sendMessage(hint(), true);
                }
            }
        });

        // Com o inventário aberto: a tecla também troca (menos digitando numa caixa de texto), e o aviso aparece no canto.
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!(screen instanceof HandledScreen<?>)) {
                return;
            }
            ScreenKeyboardEvents.afterKeyPress(screen).register((current, key, scancode, modifiers) -> {
                if (cycleKey.matchesKey(key, scancode) && !(current.getFocused() instanceof TextFieldWidget)) {
                    cycle();
                    hintUntil = System.currentTimeMillis() + HINT_MS;
                }
            });
            ScreenEvents.afterRender(screen).register((current, context, mouseX, mouseY, tickDelta) -> {
                if (System.currentTimeMillis() < hintUntil) {
                    context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, hint(), 4, 4, 0xFFFFFF);
                }
            });
        });
    }

    private static Text hint() {
        return Text.translatable("message.transparencia.opacity", opacity);
    }

    /** Passa para o próximo nível (e volta para 100% depois do último). */
    private static void cycle() {
        int next = 0;
        for (int i = 0; i < LEVELS.length; i++) {
            if (LEVELS[i] == opacity) {
                next = (i + 1) % LEVELS.length;
                break;
            }
        }
        opacity = LEVELS[next];
        save();
    }

    // ---------------------------------------------------------------- usado pelos mixins

    /** Antes de desenhar o fundo de uma tela com inventário: tudo o que vier sai com a transparência escolhida. */
    public static void beginBackground(DrawContext context) {
        if (opacity >= 100) {
            return;
        }
        context.draw();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        context.setShaderColor(1.0F, 1.0F, 1.0F, opacity / 100.0F);
    }

    /** Depois do fundo: volta ao normal para os itens, os textos e os botões. */
    public static void endBackground(DrawContext context) {
        if (opacity >= 100) {
            return;
        }
        context.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    /** O escurecimento atrás de uma tela com inventário fica mais leve na mesma proporção. */
    public static int dimBackground(Screen screen, int color) {
        if (opacity >= 100 || !(screen instanceof HandledScreen<?>)) {
            return color;
        }
        int alpha = (color >>> 24) * opacity / 100;
        return (alpha << 24) | (color & 0xFFFFFF);
    }

    // ---------------------------------------------------------------- config

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("transparencia.properties");
    }

    private static void load() {
        Path file = file();
        if (!Files.isRegularFile(file)) {
            return;
        }
        try (InputStream in = Files.newInputStream(file)) {
            Properties props = new Properties();
            props.load(in);
            int value = Integer.parseInt(props.getProperty("opacidade", String.valueOf(DEFAULT_OPACITY)).trim());
            opacity = Math.max(5, Math.min(100, value));
        } catch (IOException | NumberFormatException e) {
            LOGGER.warn("Nao consegui ler {}: {}", file, e.getMessage());
        }
    }

    private static void save() {
        Path file = file();
        try (OutputStream out = Files.newOutputStream(file)) {
            Properties props = new Properties();
            props.setProperty("opacidade", Integer.toString(opacity));
            props.store(out, "Inventario Transparente: quanto o fundo do inventario aparece, em % (5 a 100)");
        } catch (IOException e) {
            LOGGER.warn("Nao consegui salvar {}: {}", file, e.getMessage());
        }
    }
}
