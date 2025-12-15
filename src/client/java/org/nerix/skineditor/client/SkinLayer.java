package org.nerix.skineditor.client;

import net.minecraft.client.texture.NativeImage;

public class SkinLayer {
    public final String name;
    public final NativeImage pixels; // L'image stockée en RAM (plus besoin de relire le disque)
    public boolean isVisible = true;

    public SkinLayer(String name, NativeImage pixels) {
        this.name = name;
        this.pixels = pixels;
    }

    // Libère la mémoire quand on supprime le calque
    public void close() {
        if (pixels != null) pixels.close();
    }
}