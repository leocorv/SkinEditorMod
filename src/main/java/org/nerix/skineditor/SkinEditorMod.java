package org.nerix.skineditor;

import net.fabricmc.api.ModInitializer;
import org.nerix.skineditor.common.NetworkHandler;
import org.nerix.skineditor.server.OutfitLoader;

public class SkinEditorMod implements ModInitializer {

    @Override
    public void onInitialize() {
        NetworkHandler.registerPayloads();
        OutfitLoader.load();
        NetworkHandler.registerServer();
    }
}
