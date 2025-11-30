package org.nerix.skineditor.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

public class EditableSkin implements AutoCloseable {
    private final NativeImage image;
    private final NativeImageBackedTexture texture;
    private final Identifier textureId;

    public EditableSkin() {
        // 1. Création image
        this.image = new NativeImage(NativeImage.Format.RGBA, 64, 64, false);

        // 2. Remplissage BLANC
        int whiteColor = 0xFFFFFFFF;
        for (int x = 0; x < 64; x++) {
            for (int y = 0; y < 64; y++) {
                this.image.setColor(x, y, whiteColor);
            }
        }

        // 3. Création de l'objet Texture
        this.texture = new NativeImageBackedTexture(this.image);

        // 4. Enregistrement (C'est là qu'on obtient l'ID)
        String uniqueName = "temp_skin_" + System.currentTimeMillis();
        this.textureId = MinecraftClient.getInstance().getTextureManager()
                .registerDynamicTexture(uniqueName, this.texture);

        // --- CORRECTION FLOU ---
        // On applique le filtre APRES l'enregistrement pour être sûr qu'il est pris en compte
        // (false, false) = Nearest Neighbor (Pixelisé)
        this.texture.setFilter(false, false);

        // 5. Envoi des données au GPU
        this.texture.upload();
    }

    public Identifier getTextureId() {
        return textureId;
    }

    public void fillArea(int x, int y, int width, int height, int color) {
        for (int i = x; i < x + width; i++) {
            for (int j = y; j < y + height; j++) {
                if (i >= 0 && i < 64 && j >= 0 && j < 64) {
                    this.image.setColor(i, j, color);
                }
            }
        }
        this.texture.upload();
    }

    public void clear() {
        // Reset en blanc
        fillArea(0, 0, 64, 64, 0xFFFFFFFF);
    }

    @Override
    public void close() {
        this.texture.close();
    }
}