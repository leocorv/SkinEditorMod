package org.nerix.skineditor.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;

public class SkinEditorModClient implements ClientModInitializer {

    public static Identifier TEMP_SKIN_ID = null;
    public static SkinTextures.Model TEMP_SKIN_MODEL = SkinTextures.Model.WIDE;
    public static EditableSkin PERMANENT_SKIN = null;

    @Override
    public void onInitializeClient() {
        ClientNetworkHandler.registerClient();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("skineditor")
                    .executes(context -> {
                        MinecraftClient.getInstance().send(() ->
                                MinecraftClient.getInstance().setScreen(new SkinEditorScreen()));
                        return 1;
                    }));
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clearSessionState());
    }

    private static void clearSessionState() {
        if (PERMANENT_SKIN != null) {
            PERMANENT_SKIN.close();
            PERMANENT_SKIN = null;
        }

        TEMP_SKIN_ID = null;
        TEMP_SKIN_MODEL = SkinTextures.Model.WIDE;
        ClientNetworkHandler.CLIENT_ROOT_CATEGORY = null;
    }
}
