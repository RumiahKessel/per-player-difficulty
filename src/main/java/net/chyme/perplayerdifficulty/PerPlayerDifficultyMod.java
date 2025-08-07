
package net.chyme.perplayerdifficulty;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PerPlayerDifficultyMod implements DedicatedServerModInitializer {
    public static final String MOD_ID = "perplayerdifficulty";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeServer() {
        LOGGER.info("Initializing Per Player Difficulty Mod");

        // Register command
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            DifficultyCommand.register(dispatcher);
        });

        // Initialize data manager on server start
        ServerLifecycleEvents.SERVER_STARTED.register((server) -> {
            PlayerDifficultyManager.initialize(server);
        });

        // Save data on server stop
        ServerLifecycleEvents.SERVER_STOPPING.register((server) -> {
            PlayerDifficultyManager.saveData();
        });

        // Initialize data for new worlds
        ServerWorldEvents.LOAD.register((server, world) -> {
            PlayerDifficultyManager.initialize(server);
        });
    }
}