package org.nerix.skineditor.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

public class SkinEditorModClient implements ClientModInitializer {

    // On stocke l'ID pour le Mixin
    public static Identifier TEMP_SKIN_ID = null;

    // NOUVEAU : On stocke l'objet Skin entier pour ne pas qu'il soit supprimé par le Garbage Collector
    public static EditableSkin PERMANENT_SKIN = null;

    @Override
    public void onInitializeClient() {
        ClientNetworkHandler.registerClient();
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("skineditor")
                    .executes(context -> {
                        MinecraftClient.getInstance().send(() -> {
                            MinecraftClient.getInstance().setScreen(new SkinEditorScreen());
                        });
                        return 1;
                    }));
        });
    }
}