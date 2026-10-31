package com.edugcs.batraquizar;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.registry.FabricBrewingRecipeRegistry;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.Items;
import net.minecraft.potion.Potion;
import net.minecraft.potion.Potions;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Batraquizar: a poção Batraquizador. Quem bebe, é atingido pela poção arremessável (ou persistente, ou pela flecha) vira
 * sapo até o efeito acabar. Leite desfaz, como qualquer efeito. Quem volta ao normal ganha a Bênção do
 * Príncipe, que impede de virar sapo de novo por 10 minutos.
 *
 * <p>Ser sapo é o efeito Batraquizado ({@link BatraquizadoEffect}): ele deixa mais rápido (modificador de
 * velocidade), e os mixins encolhem a pessoa, deixam pular mais alto e tiram o dano de queda. O desenho de sapo
 * é só no cliente ({@code BatraquizarClient}); ele sabe quem é sapo pelo modificador de velocidade, que o jogo
 * já manda para todos os jogadores por perto.
 */
public class Batraquizar implements ModInitializer {
    public static final String MOD_ID = "batraquizar";
    public static final Logger LOGGER = LoggerFactory.getLogger("Batraquizar");

    /** Duração da poção normal (30 segundos) e da prolongada com redstone (1 minuto e meio), em ticks. */
    private static final int DURATION = 30 * 20;
    private static final int LONG_DURATION = 90 * 20;

    public static final StatusEffect BATRAQUIZADO =
            Registry.register(Registries.STATUS_EFFECT, id("batraquizado"), new BatraquizadoEffect());
    /** Ganha quem volta de sapo: por um tempo, ninguém consegue transformar de novo (veja {@link BencaoDoPrincipe}). */
    public static final StatusEffect BENCAO_DO_PRINCIPE =
            Registry.register(Registries.STATUS_EFFECT, id("bencao_do_principe"), new BencaoDoPrincipeEffect());

    // As duas usam o nome "batraquizador", então as traduções são as mesmas ("Batraquizador").
    public static final Potion BATRAQUIZADOR = Registry.register(Registries.POTION, id("batraquizador"),
            new Potion("batraquizador", new StatusEffectInstance(BATRAQUIZADO, DURATION)));
    public static final Potion LONG_BATRAQUIZADOR = Registry.register(Registries.POTION, id("long_batraquizador"),
            new Potion("batraquizador", new StatusEffectInstance(BATRAQUIZADO, LONG_DURATION)));

    /** Devolve os efeitos ruins para quem os aplicou (veja {@link Retorsao}). */
    public static final StatusEffect RETORSAO =
            Registry.register(Registries.STATUS_EFFECT, id("retorsao"), new RetorsaoEffect());
    // Normal: 1 minuto (devolve 1 efeito). Prolongada com redstone: 3 minutos (devolve até 3).
    public static final Potion RETORSAO_POTION = Registry.register(Registries.POTION, id("retorsao"),
            new Potion("retorsao", new StatusEffectInstance(RETORSAO, Retorsao.COST)));
    public static final Potion LONG_RETORSAO_POTION = Registry.register(Registries.POTION, id("long_retorsao"),
            new Potion("retorsao", new StatusEffectInstance(RETORSAO, 3 * Retorsao.COST)));

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }

    /** Esta criatura está transformada em sapo? Funciona no servidor e no cliente (para qualquer criatura por perto). */
    public static boolean isFrog(LivingEntity entity) {
        // Os atributos ainda não existem enquanto a criatura está sendo criada (o jogo já pergunta o tamanho dela).
        if (entity.getAttributes() == null) {
            return false;
        }
        EntityAttributeInstance speed = entity.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        return speed != null && speed.getModifier(BatraquizadoEffect.SPEED_ID) != null;
    }

    @Override
    public void onInitialize() {
        // Poção estranha + froglight (qualquer uma das três cores) = Batraquizador; + redstone = dura mais.
        // A arremessável (pólvora), a persistente (bafo do dragão) e a flecha saem pelas receitas normais do jogo.
        FabricBrewingRecipeRegistry.registerPotionRecipe(Potions.AWKWARD,
                Ingredient.ofItems(Items.OCHRE_FROGLIGHT, Items.VERDANT_FROGLIGHT, Items.PEARLESCENT_FROGLIGHT),
                BATRAQUIZADOR);
        FabricBrewingRecipeRegistry.registerPotionRecipe(BATRAQUIZADOR, Ingredient.ofItems(Items.REDSTONE),
                LONG_BATRAQUIZADOR);
        // Poção estranha + fragmento de ametista = Retorsão; + redstone = Retorsão prolongada.
        FabricBrewingRecipeRegistry.registerPotionRecipe(Potions.AWKWARD, Ingredient.ofItems(Items.AMETHYST_SHARD),
                RETORSAO_POTION);
        FabricBrewingRecipeRegistry.registerPotionRecipe(RETORSAO_POTION, Ingredient.ofItems(Items.REDSTONE),
                LONG_RETORSAO_POTION);

        BencaoDoPrincipe.init();
        Retorsao.init();

        // Sapo não ataca nem quebra blocos. Usar itens continua valendo (comer, abrir portas e beber leite para desvirar).
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) ->
                isFrog(player) && !player.isSpectator() ? ActionResult.FAIL : ActionResult.PASS);
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) ->
                isFrog(player) && !player.isSpectator() ? ActionResult.FAIL : ActionResult.PASS);
    }
}
