package com.cassino.mod.client;

import com.cassino.mod.Cassino;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

public class CassinoClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HandledScreens.register(Cassino.SLOT_MACHINE_HANDLER, SlotMachineScreen::new);
    }
}
