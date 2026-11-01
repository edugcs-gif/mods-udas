package com.cassino.mod.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;

/**
 * Os rostos dos mobs da loteria, recortados das texturas do próprio Minecraft: a frente da cabeça de cada um (e o
 * focinho, o nariz, os olhos... quando são peças separadas), na ordem de {@link com.cassino.mod.machine.Lottery#MOBS}.
 * O dragão usa a cabeça de dragão (o item).
 */
final class MobIcons {
    /** Um pedaço da textura: o canto (u, v) e o tamanho, e onde ele fica no rosto (em pixels da textura). */
    private record Layer(Identifier texture, int u, int v, int width, int height, float x, float y) {
    }

    /** Um rosto: o tamanho da textura, o tamanho do rosto e os pedaços, de trás para a frente. */
    private record Face(int textureWidth, int textureHeight, int width, int height, Layer... layers) {
    }

    private static final ItemStack DRAGON_HEAD = new ItemStack(Items.DRAGON_HEAD);
    private static final Face[] FACES = new Face[25];

    static {
        Identifier pig = tex("pig/pig");
        FACES[0] = new Face(64, 32, 8, 8, layer(pig, 8, 8, 8, 8, 0, 0), layer(pig, 17, 17, 4, 3, 2, 4));
        FACES[1] = new Face(64, 32, 8, 8, layer(tex("cow/cow"), 6, 6, 8, 8, 0, 0));
        FACES[2] = new Face(64, 32, 6, 6, layer(tex("sheep/sheep"), 8, 8, 6, 6, 0, 0),
                layer(tex("sheep/sheep_fur"), 6, 6, 6, 6, 0, 0));
        Identifier chicken = tex("chicken");
        FACES[3] = new Face(64, 32, 4, 6, layer(chicken, 3, 3, 4, 6, 0, 0), layer(chicken, 16, 2, 4, 2, 0, 2),
                layer(chicken, 16, 6, 2, 2, 1, 4));
        Identifier wolf = tex("wolf/wolf");
        FACES[4] = new Face(64, 32, 6, 8, layer(wolf, 17, 15, 2, 2, 0, 0), layer(wolf, 17, 15, 2, 2, 4, 0),
                layer(wolf, 4, 4, 6, 6, 0, 2), layer(wolf, 4, 14, 3, 3, 1.5F, 5));
        Identifier fox = tex("fox/fox");
        FACES[5] = new Face(48, 32, 8, 8, layer(fox, 9, 2, 2, 2, 0, 0), layer(fox, 16, 2, 2, 2, 6, 0),
                layer(fox, 7, 11, 8, 6, 0, 2), layer(fox, 9, 21, 4, 2, 2, 6));
        FACES[6] = new Face(64, 64, 7, 7, layer(tex("bee/bee"), 10, 10, 7, 7, 0, 0));
        FACES[7] = new Face(64, 32, 12, 16, layer(tex("squid/squid"), 12, 12, 12, 16, 0, 0));
        FACES[8] = villagerLike(tex("villager/villager"), 64, 64);
        FACES[9] = villagerLike(tex("iron_golem/iron_golem"), 128, 128);
        FACES[10] = biped(tex("zombie/zombie"), 64);
        FACES[11] = biped(tex("zombie/drowned"), 64);
        FACES[12] = biped(tex("skeleton/skeleton"), 32);
        FACES[13] = new Face(64, 32, 8, 8, layer(tex("creeper/creeper"), 8, 8, 8, 8, 0, 0));
        FACES[14] = new Face(64, 32, 8, 8, layer(tex("spider/spider"), 40, 12, 8, 8, 0, 0),
                layer(tex("spider_eyes"), 40, 12, 8, 8, 0, 0));
        Identifier slime = tex("slime/slime");
        FACES[15] = new Face(64, 32, 8, 8, layer(slime, 6, 22, 6, 6, 1, 1), layer(slime, 34, 2, 2, 2, 0.75F, 2),
                layer(slime, 34, 6, 2, 2, 5.25F, 2), layer(slime, 33, 9, 1, 1, 4, 5),
                layer(slime, 8, 8, 8, 8, 0, 0));
        FACES[16] = villagerLike(tex("illager/pillager"), 64, 64);
        FACES[17] = new Face(64, 32, 8, 8, layer(tex("enderman/enderman"), 8, 8, 8, 8, 0, 0),
                layer(tex("enderman/enderman_eyes"), 8, 8, 8, 8, 0, 0));
        FACES[18] = new Face(64, 32, 8, 8, layer(tex("blaze"), 8, 8, 8, 8, 0, 0));
        FACES[19] = new Face(64, 32, 16, 16, layer(tex("ghast/ghast"), 16, 16, 16, 16, 0, 0));
        Identifier piglin = tex("piglin/piglin");
        FACES[20] = new Face(64, 64, 10, 8, layer(piglin, 8, 8, 10, 8, 0, 0), layer(piglin, 32, 2, 4, 4, 3, 4),
                layer(piglin, 3, 5, 1, 2, 7, 6), layer(piglin, 3, 1, 1, 2, 2, 6));
        FACES[21] = biped(tex("skeleton/wither_skeleton"), 32);
        FACES[22] = new Face(128, 128, 16, 16, layer(tex("warden/warden"), 10, 42, 16, 16, 0, 0));
        FACES[23] = new Face(64, 64, 8, 8, layer(tex("wither/wither"), 8, 8, 8, 8, 0, 0));
        FACES[24] = null; // o dragão: a cabeça de dragão (o item)
    }

    private MobIcons() {
    }

    private static Identifier tex(String path) {
        return new Identifier("minecraft", "textures/entity/" + path + ".png");
    }

    private static Layer layer(Identifier texture, int u, int v, int width, int height, float x, float y) {
        return new Layer(texture, u, v, width, height, x, y);
    }

    /** Zumbi, esqueleto e companhia: a cabeça 8×8 e a camada de cima (o "chapéu"). */
    private static Face biped(Identifier texture, int textureHeight) {
        return new Face(64, textureHeight, 8, 8, layer(texture, 8, 8, 8, 8, 0, 0), layer(texture, 40, 8, 8, 8, 0, 0));
    }

    /** Aldeão, golem e saqueador: a cabeça 8×10 e o narigão por cima, saindo um pouco para baixo. */
    private static Face villagerLike(Identifier texture, int textureWidth, int textureHeight) {
        return new Face(textureWidth, textureHeight, 8, 11, layer(texture, 8, 8, 8, 10, 0, 0),
                layer(texture, 26, 2, 2, 4, 3, 7));
    }

    /** Desenha o rosto do mob {@code mob} num quadrado de {@code size} pixels com o canto em (x, y). */
    static void draw(DrawContext context, int mob, int x, int y, int size) {
        Face face = mob >= 0 && mob < FACES.length ? FACES[mob] : null;
        MatrixStack matrices = context.getMatrices();
        if (face == null) {
            matrices.push();
            matrices.translate(x, y, 0.0F);
            matrices.scale(size / 16.0F, size / 16.0F, 1.0F);
            context.drawItem(DRAGON_HEAD, 0, 0);
            matrices.pop();
            return;
        }
        float scale = size / (float) Math.max(face.width(), face.height());
        matrices.push();
        matrices.translate(x + (size - face.width() * scale) / 2.0F, y + (size - face.height() * scale) / 2.0F, 0.0F);
        matrices.scale(scale, scale, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        for (Layer layer : face.layers()) {
            matrices.push();
            matrices.translate(layer.x(), layer.y(), 0.0F);
            context.drawTexture(layer.texture(), 0, 0, layer.width(), layer.height(), layer.u(), layer.v(),
                    layer.width(), layer.height(), face.textureWidth(), face.textureHeight());
            matrices.pop();
        }
        RenderSystem.disableBlend();
        matrices.pop();
    }
}
