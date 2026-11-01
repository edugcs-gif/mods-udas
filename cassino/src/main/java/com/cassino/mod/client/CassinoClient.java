package com.cassino.mod.client;

import com.cassino.mod.Cassino;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

public class CassinoClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HandledScreens.register(Cassino.CHIP_EXCHANGER_HANDLER, ChipExchangerScreen::new);
        HandledScreens.register(Cassino.SLOT_MACHINE_HANDLER, SlotMachineScreen::new);
        HandledScreens.register(Cassino.BLACKJACK_HANDLER, BlackjackScreen::new);
        HandledScreens.register(Cassino.ROULETTE_HANDLER, RouletteScreen::new);
        HandledScreens.register(Cassino.HORSE_RACE_HANDLER, HorseRaceScreen::new);
    }
}
