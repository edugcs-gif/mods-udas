package com.jbleskmic.mod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * Microfone para o jblesk: conectado a uma jbleskão, a voz de quem o segura (Simple Voice Chat) sai pela
 * caixa de som, para todo mundo que estiver perto dela.
 */
public class JbleskMic implements ModInitializer {
    public static final String MOD_ID = "jbleskmic";

    public static final Item MICROPHONE = new MicrophoneItem(new Item.Settings().maxCount(1));

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        Registry.register(Registries.ITEM, id("microphone"), MICROPHONE);
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> entries.add(MICROPHONE));
        MicLinks.init();
    }
}
