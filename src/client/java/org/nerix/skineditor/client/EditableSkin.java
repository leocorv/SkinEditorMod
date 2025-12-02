package org.nerix.skineditor.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

public class EditableSkin implements AutoCloseable {
    private final NativeImage image;
    private final NativeImageBackedTexture texture;
    private final Identifier textureId;

    public EditableSkin() {
        // 1. Création image
        this.image = new NativeImage(NativeImage.Format.RGBA, 64, 64, false);
        this.texture = new NativeImageBackedTexture(this.image);

        // 2. Enregistrement
        String uniqueName = "temp_skin_" + System.currentTimeMillis();
        this.textureId = MinecraftClient.getInstance().getTextureManager()
                .registerDynamicTexture(uniqueName, this.texture);

        // 3. Charger le VRAI Steve par défaut
        loadDefaultSteve();
    }

    public Identifier getTextureId() {
        return textureId;
    }

    // --- CHARGE LE SKIN DE BASE DE MINECRAFT ---
    public void loadDefaultSteve() {
        try {
            Identifier steveId = Identifier.of("minecraft", "textures/entity/player/wide/steve.png");
            Optional<Resource> resource = MinecraftClient.getInstance().getResourceManager().getResource(steveId);

            if (resource.isPresent()) {
                InputStream stream = resource.get().getInputStream();
                NativeImage steveImage = NativeImage.read(stream);
                this.image.copyFrom(steveImage); // Copie exacte de Steve
                stream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            fillArea(0, 0, 64, 64, 0xFFFFFFFF); // Fallback blanc si erreur
        }
        upload();
    }

    // --- APPLIQUER UN TEMPLATE PNG (Vêtements) ---
    public void mergeLayer(Identifier layerId) {
        try {
            Optional<Resource> resource = MinecraftClient.getInstance().getResourceManager().getResource(layerId);
            if (resource.isPresent()) {
                InputStream stream = resource.get().getInputStream();
                NativeImage layerImage = NativeImage.read(stream);

                // Fusion intelligente (garde la transparence)
                for (int x = 0; x < layerImage.getWidth(); x++) {
                    for (int y = 0; y < layerImage.getHeight(); y++) {
                        int color = layerImage.getColor(x, y);
                        // Si le pixel n'est pas transparent, on le peint
                        if ((color >> 24 & 0xFF) > 0) {
                            this.image.setColor(x, y, color);
                        }
                    }
                }
                stream.close();
                upload();
            }
        } catch (IOException e) {
            System.out.println("Erreur chargement template: " + layerId);
        }
    }

    // Peindre une zone (pour le color picker ou debug)
    public void fillArea(int x, int y, int width, int height, int color) {
        for (int i = x; i < x + width; i++) {
            for (int j = y; j < y + height; j++) {
                if (i >= 0 && i < 64 && j >= 0 && j < 64) {
                    this.image.setColor(i, j, color);
                }
            }
        }
        upload();
    }

    // --- L'ANTI-FLOU ULTIME ---
    private void upload() {
        this.texture.bindTexture();
        // Force le mode "Pixel Art" (Nearest) au niveau OpenGL
        // 9728 = GL_NEAREST
        RenderSystem.texParameter(3553, 10241, 9728); // Min Filter
        RenderSystem.texParameter(3553, 10240, 9728); // Mag Filter

        this.texture.upload();
    }

    @Override
    public void close() {
        this.texture.close();
    }
}