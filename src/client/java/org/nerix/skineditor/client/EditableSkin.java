package org.nerix.skineditor.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.loader.api.FabricLoader; // IMPORTANT pour le chemin config
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Optional;

public class EditableSkin implements AutoCloseable {
    private final NativeImage image;
    private final NativeImageBackedTexture texture;
    private final Identifier textureId;

    public EditableSkin() {
        // 1. Image RGBA
        this.image = new NativeImage(NativeImage.Format.RGBA, 64, 64, false);
        this.texture = new NativeImageBackedTexture(this.image);

        // 2. Enregistrement
        String uniqueName = "temp_skin_" + System.currentTimeMillis();
        this.textureId = MinecraftClient.getInstance().getTextureManager()
                .registerDynamicTexture(uniqueName, this.texture);

        // 3. Anti-Flou initial
        this.texture.setFilter(false, false);

        // 4. Charger Steve par défaut
        loadDefaultSteve();
    }

    public Identifier getTextureId() { return textureId; }

    // Charge le skin de Steve depuis les assets internes
    public void loadDefaultSteve() {
        try {
            Identifier steveId = Identifier.of("minecraft", "textures/entity/player/wide/steve.png");
            Optional<Resource> resource = MinecraftClient.getInstance().getResourceManager().getResource(steveId);
            if (resource.isPresent()) {
                try (InputStream stream = resource.get().getInputStream()) {
                    NativeImage steveImage = NativeImage.read(stream);
                    this.image.copyFrom(steveImage);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            fillArea(0, 0, 64, 64, 0xFFFFFFFF);
        }
        upload();
    }

    // --- LA MÉTHODE QUI POSAIT PROBLÈME (Corrigée avec les bons imports) ---
    public void mergeLayerFromDisk(String relativePath) {
        // On récupère le dossier de config via Fabric
        Path configDir = FabricLoader.getInstance().getConfigDir();
        // On construit le chemin complet : .minecraft/config/skineditor/outfits/TonFichier.png
        File file = configDir.resolve("skineditor/outfits/" + relativePath).toFile();

        if (!file.exists()) {
            System.out.println("Erreur : Fichier introuvable -> " + file.getAbsolutePath());
            return;
        }

        try (FileInputStream stream = new FileInputStream(file)) {
            NativeImage layerImage = NativeImage.read(stream);

            // Fusion des pixels (Gestion transparence)
            for (int x = 0; x < layerImage.getWidth(); x++) {
                for (int y = 0; y < layerImage.getHeight(); y++) {
                    int color = layerImage.getColor(x, y);
                    // Si le pixel n'est pas transparent (Alpha > 0)
                    if ((color >> 24 & 0xFF) > 0) {
                        this.image.setColor(x, y, color);
                    }
                }
            }
            upload(); // On envoie au GPU
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // --- COLORIAGE PEAU (Sliders) ---
    public void paintSkin(int color) {
        // Tête
        fillPixels(8, 0, 8, 8, color); fillPixels(16, 0, 8, 8, color); // Haut/Bas
        fillPixels(0, 8, 32, 8, color); // Faces

        // Torse
        fillPixels(20, 16, 8, 4, color); fillPixels(28, 16, 8, 4, color); // Haut/Bas
        fillPixels(16, 20, 24, 12, color); // Faces
        fillPixels(32, 20, 8, 12, color); // Dos

        // Bras
        fillPixels(44, 16, 4, 4, color); fillPixels(48, 16, 4, 4, color); // Haut/Bas Droit
        fillPixels(40, 20, 16, 12, color); // Faces Droit
        fillPixels(36, 48, 4, 4, color); fillPixels(40, 48, 4, 4, color); // Haut/Bas Gauche
        fillPixels(32, 52, 16, 12, color); // Faces Gauche

        // Jambes
        fillPixels(4, 16, 4, 4, color); fillPixels(8, 16, 4, 4, color); // Haut/Bas Droit
        fillPixels(0, 20, 16, 12, color); // Faces Droit
        fillPixels(20, 48, 4, 4, color); fillPixels(24, 48, 4, 4, color); // Haut/Bas Gauche
        fillPixels(16, 52, 16, 12, color); // Faces Gauche

        upload();
    }

    public void fillArea(int x, int y, int width, int height, int color) {
        fillPixels(x, y, width, height, color);
        upload();
    }

    public void resetToSteveBase() {
        fillArea(0, 0, 64, 64, 0x00000000);
        loadDefaultSteve();
    }

    private void fillPixels(int x, int y, int w, int h, int color) {
        for (int i = x; i < x + w; i++) {
            for (int j = y; j < y + h; j++) {
                if (i >= 0 && i < 64 && j >= 0 && j < 64) {
                    this.image.setColor(i, j, color);
                }
            }
        }
    }

    private void upload() {
        this.texture.bindTexture();
        // Force le pixel art (Anti-Flou)
        RenderSystem.texParameter(3553, 10241, 9728);
        RenderSystem.texParameter(3553, 10240, 9728);
        this.texture.upload();
    }

    @Override
    public void close() {
        this.texture.close();
    }
}