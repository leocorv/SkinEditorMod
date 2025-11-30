package org.nerix.skineditor.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

public class SkinEditorModClient implements ClientModInitializer {

    // LA VARIABLE MAGIQUE : C'est elle qui stocke l'ID du skin custom
    public static Identifier TEMP_SKIN_ID = null;

    @Override
    public void onInitializeClient() {
        System.out.println(">>> [SkinEditor] Chargement...");

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("skineditor")
                    .executes(context -> {
                        // On ouvre l'écran sur le thread principal
                        MinecraftClient.getInstance().send(() -> {
                            MinecraftClient.getInstance().setScreen(new SkinEditorScreen());
                        });
                        return 1;
                    }));
        });
    }
}