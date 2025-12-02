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

    // --- LA MÉTHODE QUI MANQUAIT ---
    // Elle dessine les pixels en mémoire MAIS n'envoie pas à la carte graphique.
    // Ça permet de faire plein de dessins d'un coup (comme paintSkin) et d'envoyer une seule fois à la fin.
    private void fillPixels(int x, int y, int w, int h, int color) {
        for (int i = x; i < x + w; i++) {
            for (int j = y; j < y + h; j++) {
                if (i >= 0 && i < 64 && j >= 0 && j < 64) {
                    this.image.setColor(i, j, color);
                }
            }
        }
    }

    // Peint intelligemment la peau (Layer 1 seulement)
    // --- COLORIAGE PEAU COMPLET (Toutes les faces du Layer 1) ---
    public void paintSkin(int color) {
        // --- TÊTE ---
        fillPixels(8, 0, 8, 8, color);   // Haut
        fillPixels(16, 0, 8, 8, color);  // Bas
        fillPixels(0, 8, 8, 8, color);   // Droite
        fillPixels(8, 8, 8, 8, color);   // Face
        fillPixels(16, 8, 8, 8, color);  // Gauche
        fillPixels(24, 8, 8, 8, color);  // Arrière

        // --- TORSE ---
        fillPixels(20, 16, 8, 4, color); // Haut
        fillPixels(28, 16, 8, 4, color); // Bas
        fillPixels(16, 20, 4, 12, color);// Droite
        fillPixels(20, 20, 8, 12, color);// Face
        fillPixels(28, 20, 4, 12, color);// Gauche
        fillPixels(32, 20, 8, 12, color);// Arrière

        // --- BRAS DROIT ---
        fillPixels(44, 16, 4, 4, color); // Haut
        fillPixels(48, 16, 4, 4, color); // Bas
        fillPixels(40, 20, 4, 12, color);// Droite
        fillPixels(44, 20, 4, 12, color);// Face
        fillPixels(48, 20, 4, 12, color);// Gauche
        fillPixels(52, 20, 4, 12, color);// Arrière

        // --- BRAS GAUCHE ---
        fillPixels(36, 48, 4, 4, color); // Haut
        fillPixels(40, 48, 4, 4, color); // Bas
        fillPixels(32, 52, 4, 12, color);// Droite
        fillPixels(36, 52, 4, 12, color);// Face
        fillPixels(40, 52, 4, 12, color);// Gauche
        fillPixels(44, 52, 4, 12, color);// Arrière

        // --- JAMBE DROITE ---
        fillPixels(4, 16, 4, 4, color);  // Haut
        fillPixels(8, 16, 4, 4, color);  // Bas
        fillPixels(0, 20, 4, 12, color); // Droite
        fillPixels(4, 20, 4, 12, color); // Face
        fillPixels(8, 20, 4, 12, color); // Gauche
        fillPixels(12, 20, 4, 12, color);// Arrière

        // --- JAMBE GAUCHE ---
        fillPixels(20, 48, 4, 4, color); // Haut
        fillPixels(24, 48, 4, 4, color); // Bas
        fillPixels(16, 52, 4, 12, color);// Droite
        fillPixels(20, 52, 4, 12, color);// Face
        fillPixels(24, 52, 4, 12, color);// Gauche
        fillPixels(28, 52, 4, 12, color);// Arrière

        // Envoie le tout à la carte graphique
        upload();
    }
}