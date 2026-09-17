package org.nerix.skineditor.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class EditableSkin implements AutoCloseable {
    private static final int SKIN_SIZE = 64;

    private final NativeImage bufferImage;
    private final NativeImageBackedTexture texture;
    private final Identifier textureId;

    // 1. Base Skin (Joueur)
    private NativeImage basePlayerPixels = null;
    private boolean showBaseSkin = true;

    // 2. Liste des Calques
    public final List<SkinLayer> layers = new ArrayList<>();

    public EditableSkin() {
        this.bufferImage = new NativeImage(NativeImage.Format.RGBA, SKIN_SIZE, SKIN_SIZE, false);
        this.texture = new NativeImageBackedTexture(this.bufferImage);

        String uniqueName = "temp_skin_" + System.currentTimeMillis();
        this.textureId = MinecraftClient.getInstance().getTextureManager()
                .registerDynamicTexture(uniqueName, this.texture);

        this.texture.setFilter(false, false);
        loadBaseSkinInMemory();
        recompose();
    }

    public Identifier getTextureId() { return textureId; }

    // --- MOTEUR DE DESSIN ---
    public void recompose() {
        clearImage();

        if (showBaseSkin && basePlayerPixels != null) {
            copyPixels(basePlayerPixels, this.bufferImage);
        }

        for (SkinLayer layer : layers) {
            if (layer.isVisible) {
                mergePixels(layer.pixels, this.bufferImage);
            }
        }
        upload();
    }

    // --- ACTIONS CALQUES ---

    public void addLayer(String path, String displayName) {
        NativeImage img = loadFromDisk(path);
        if (img == null) {
            System.err.println("[SkinEditor] Impossible de charger l'outfit: " + path);
            return;
        }

        this.layers.add(new SkinLayer(displayName, img));
        recompose();
    }

    public void updateBodyColorLayer(int color) {
        SkinLayer bodyLayer = null;
        for (SkinLayer layer : layers) {
            if (layer.name.equals("Peinture Corps")) {
                bodyLayer = layer;
                break;
            }
        }

        if (bodyLayer == null) {
            NativeImage img = new NativeImage(SKIN_SIZE, SKIN_SIZE, true);
            bodyLayer = new SkinLayer("Peinture Corps", img);
            this.layers.add(0, bodyLayer);
        }

        fillBodyParts(bodyLayer.pixels, color);
        recompose();
    }

    public void paintPixel(int x, int y, int color) {
        SkinLayer drawLayer = null;
        for (SkinLayer layer : layers) {
            if (layer.name.equals("Mon Dessin")) {
                drawLayer = layer;
                break;
            }
        }

        if (drawLayer == null) {
            NativeImage img = new NativeImage(SKIN_SIZE, SKIN_SIZE, true);
            drawLayer = new SkinLayer("Mon Dessin", img);
            this.layers.add(drawLayer);
        }

        if (x >= 0 && x < SKIN_SIZE && y >= 0 && y < SKIN_SIZE) {
            drawLayer.pixels.setColor(x, y, color);
            recompose();
        }
    }

    public void removeLayer(int index) {
        if (index >= 0 && index < layers.size()) {
            layers.remove(index).close();
            recompose();
        }
    }

    public void toggleLayer(int index) {
        if (index >= 0 && index < layers.size()) {
            SkinLayer layer = layers.get(index);
            layer.isVisible = !layer.isVisible;
            recompose();
        }
    }

    public void setShowBaseSkin(boolean show) {
        this.showBaseSkin = show;
        recompose();
    }

    public void resetAll() {
        for (SkinLayer layer : layers) {
            layer.close();
        }
        layers.clear();
        showBaseSkin = true;
        recompose();
    }

    // --- INTERNE ---

    private void fillBodyParts(NativeImage img, int color) {
        for (int x = 0; x < SKIN_SIZE; x++) {
            for (int y = 0; y < SKIN_SIZE; y++) {
                img.setColor(x, y, 0);
            }
        }

        // Tête
        fillPixels(img, 8, 0, 8, 8, color);
        fillPixels(img, 16, 0, 8, 8, color);
        fillPixels(img, 0, 8, 32, 8, color);
        // Torse
        fillPixels(img, 20, 16, 8, 4, color);
        fillPixels(img, 28, 16, 8, 4, color);
        fillPixels(img, 16, 20, 24, 12, color);
        fillPixels(img, 32, 20, 8, 12, color);
        // Bras
        fillPixels(img, 44, 16, 4, 4, color);
        fillPixels(img, 48, 16, 4, 4, color);
        fillPixels(img, 40, 20, 16, 12, color);
        fillPixels(img, 36, 48, 4, 4, color);
        fillPixels(img, 40, 48, 4, 4, color);
        fillPixels(img, 32, 52, 16, 12, color);
        // Jambes
        fillPixels(img, 4, 16, 4, 4, color);
        fillPixels(img, 8, 16, 4, 4, color);
        fillPixels(img, 0, 20, 16, 12, color);
        fillPixels(img, 20, 48, 4, 4, color);
        fillPixels(img, 24, 48, 4, 4, color);
        fillPixels(img, 16, 52, 16, 12, color);
    }

    private void fillPixels(NativeImage img, int x, int y, int w, int h, int color) {
        int maxX = Math.min(x + w, img.getWidth());
        int maxY = Math.min(y + h, img.getHeight());
        for (int px = Math.max(0, x); px < maxX; px++) {
            for (int py = Math.max(0, y); py < maxY; py++) {
                img.setColor(px, py, color);
            }
        }
    }

    private void loadBaseSkinInMemory() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        String textureUrl = client.player.getSkinTextures().textureUrl();
        if (textureUrl != null && !textureUrl.isBlank()) {
            try (InputStream stream = URI.create(textureUrl).toURL().openStream()) {
                this.basePlayerPixels = NativeImage.read(stream);
                return;
            } catch (Exception e) {
                System.err.println("[SkinEditor] Impossible de charger le skin joueur, fallback par défaut: " + e.getMessage());
            }
        }

        try (InputStream stream = client.getResourceManager()
                .getResource(DefaultSkinHelper.getTexture())
                .orElseThrow()
                .getInputStream()) {
            this.basePlayerPixels = NativeImage.read(stream);
        } catch (Exception e) {
            System.err.println("[SkinEditor] Impossible de charger le skin par défaut: " + e.getMessage());
        }
    }

    private NativeImage loadFromDisk(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) return null;

        try {
            Path outfitsRoot = FabricLoader.getInstance()
                    .getConfigDir()
                    .resolve("skineditor")
                    .resolve("outfits")
                    .toAbsolutePath()
                    .normalize();

            Path file = outfitsRoot.resolve(relativePath).normalize();

            // Le chemin provient d'un payload serveur. Il ne doit jamais pouvoir
            // sortir du dossier local réservé aux outfits du mod.
            if (!file.startsWith(outfitsRoot)) {
                System.err.println("[SkinEditor] Chemin d'outfit refusé: " + relativePath);
                return null;
            }

            if (!Files.isRegularFile(file)) return null;

            try (InputStream stream = Files.newInputStream(file)) {
                return NativeImage.read(stream);
            }
        } catch (Exception e) {
            System.err.println("[SkinEditor] Erreur de lecture de l'outfit '" + relativePath + "': " + e.getMessage());
            return null;
        }
    }

    private void clearImage() {
        for (int x = 0; x < bufferImage.getWidth(); x++) {
            for (int y = 0; y < bufferImage.getHeight(); y++) {
                bufferImage.setColor(x, y, 0);
            }
        }
    }

    private void copyPixels(NativeImage src, NativeImage dest) {
        int width = Math.min(src.getWidth(), dest.getWidth());
        int height = Math.min(src.getHeight(), dest.getHeight());

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                dest.setColor(x, y, src.getColor(x, y));
            }
        }
    }

    private void mergePixels(NativeImage src, NativeImage dest) {
        int width = Math.min(src.getWidth(), dest.getWidth());
        int height = Math.min(src.getHeight(), dest.getHeight());

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int color = src.getColor(x, y);
                if ((color >> 24 & 0xFF) > 0) {
                    dest.setColor(x, y, color);
                }
            }
        }
    }

    private void upload() {
        this.texture.bindTexture();
        RenderSystem.texParameter(3553, 10241, 9728);
        RenderSystem.texParameter(3553, 10240, 9728);
        this.texture.upload();
    }

    @Override
    public void close() {
        this.texture.close();
        if (basePlayerPixels != null) {
            basePlayerPixels.close();
        }
        for (SkinLayer layer : layers) {
            layer.close();
        }
        layers.clear();
    }
}
