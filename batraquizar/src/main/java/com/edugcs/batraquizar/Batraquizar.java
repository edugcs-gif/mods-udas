package com.edugcs.batraquizar;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.registry.FabricBrewingRecipeRegistryBuilder;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.Items;
import net.minecraft.potion.Potion;
import net.minecraft.potion.Potions;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Batraquizar: a poção Batraquizador. Quem bebe, é atingido pela poção arremessável (ou persistente, ou pela flecha) vira
 * sapo até o efeito acabar. Leite desfaz, como qualquer efeito.
 *
 * <p>Ser sapo é o efeito Batraquizado ({@link BatraquizadoEffect}): ele encolhe a pessoa (atributo de
 * tamanho), deixa pular mais alto e cair sem se machucar de alturas pequenas. O desenho de sapo é só no cliente
 * ({@code BatraquizarClient}); ele sabe quem é sapo pelo modificador de tamanho, que o jogo já manda para
 * todos os jogadores por perto.
 */
public class Batraquizar implements ModInitializer {
    public static final String MOD_ID = "batraquizar";
    public static final Logger LOGGER = LoggerFactory.getLogger("Batraquizar");

    /** Duração da poção normal (1 minuto) e da prolongada com redstone (3 minutos), em ticks. */
    private static final int DURATION = 60 * 20;
    private static final int LONG_DURATION = 3 * 60 * 20;

    public static final RegistryEntry<StatusEffect> BATRAQUIZADO =
            Registry.registerReference(Registries.STATUS_EFFECT, id("batraquizado"), new BatraquizadoEffect());

    // As duas usam o nome "batraquizador", então as traduções são as mesmas ("Batraquizador").
    public static final RegistryEntry<Potion> BATRAQUIZADOR = Registry.registerReference(Registries.POTION,
            id("batraquizador"), new Potion("batraquizador", new StatusEffectInstance(BATRAQUIZADO, DURATION)));
    public static final RegistryEntry<Potion> LONG_BATRAQUIZADOR = Registry.registerReference(Registries.POTION,
            id("long_batraquizador"),
            new Potion("batraquizador", new StatusEffectInstance(BATRAQUIZADO, LONG_DURATION)));

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    /** Esta criatura está transformada em sapo? Funciona no servidor e no cliente (para qualquer criatura por perto). */
    public static boolean isFrog(LivingEntity entity) {
        EntityAttributeInstance scale = entity.getAttributeInstance(EntityAttributes.GENERIC_SCALE);
        return scale != null && scale.hasModifier(BatraquizadoEffect.SCALE_ID);
    }

    @Override
    public void onInitialize() {
        // Poção estranha + bola de slime = Batraquizador; + redstone = dura mais. A arremessável (pólvora),
        // a persistente (bafo do dragão) e a flecha saem pelas receitas normais do jogo.
        FabricBrewingRecipeRegistryBuilder.BUILD.register(builder -> {
            builder.registerPotionRecipe(Potions.AWKWARD, Items.SLIME_BALL, BATRAQUIZADOR);
            builder.registerPotionRecipe(BATRAQUIZADOR, Items.REDSTONE, LONG_BATRAQUIZADOR);
        });

        // Sapo não ataca nem quebra blocos. Usar itens continua valendo (comer, abrir portas e beber leite para desvirar).
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) ->
                isFrog(player) && !player.isSpectator() ? ActionResult.FAIL : ActionResult.PASS);
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) ->
                isFrog(player) && !player.isSpectator() ? ActionResult.FAIL : ActionResult.PASS);
    }
}
