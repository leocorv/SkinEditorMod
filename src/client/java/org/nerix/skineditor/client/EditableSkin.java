package org.nerix.skineditor.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.util.Identifier;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class EditableSkin implements AutoCloseable {
    private final NativeImage bufferImage; // L'image finale
    private final NativeImageBackedTexture texture;
    private final Identifier textureId;

    // --- ARCHITECTURE CALQUES ---
    // 1. Le skin de base (Joueur) est maintenant stocké à part
    private NativeImage basePlayerPixels = null;
    private boolean showBaseSkin = true;

    // 2. La liste des vêtements (Objets Layer)
    public final List<SkinLayer> layers = new ArrayList<>();

    // 3. Couleur corps
    private int currentBodyColor = -1;

    public EditableSkin() {
        this.bufferImage = new NativeImage(NativeImage.Format.RGBA, 64, 64, false);
        this.texture = new NativeImageBackedTexture(this.bufferImage);

        String uniqueName = "temp_skin_" + System.currentTimeMillis();
        this.textureId = MinecraftClient.getInstance().getTextureManager()
                .registerDynamicTexture(uniqueName, this.texture);

        this.texture.setFilter(false, false);

        // On charge le skin du joueur en mémoire une bonne fois pour toutes
        loadBaseSkinInMemory();
        recompose();
    }

    public Identifier getTextureId() { return textureId; }

    // --- MOTEUR DE DESSIN OPTIMISÉ ---
    public void recompose() {
        // 1. Reset (Transparent)
        clearImage();

        // 2. Base Skin (copie depuis la mémoire)
        if (showBaseSkin && basePlayerPixels != null) {
            copyPixels(basePlayerPixels, this.bufferImage);
        }

        // 3. Couleur Peau
        if (currentBodyColor != -1) {
            doPaintSkin(currentBodyColor);
        }

        // 4. Vêtements (On empile les calques visibles)
        for (SkinLayer layer : layers) {
            if (layer.isVisible) {
                mergePixels(layer.pixels, this.bufferImage);
            }
        }

        upload();
    }

    // --- GESTION DES CALQUES (Actions) ---

    public void addLayer(String path, String displayName) {
        // On charge l'image disque -> RAM
        NativeImage img = loadFromDisk(path);
        if (img != null) {
            // On crée le calque et on l'ajoute
            this.layers.add(new SkinLayer(displayName, img));
            recompose();
        }
    }

    public void removeLayer(int index) {
        if (index >= 0 && index < layers.size()) {
            SkinLayer l = layers.remove(index);
            l.close(); // Important : libérer la mémoire
            recompose();
        }
    }

    public void toggleLayer(int index) {
        if (index >= 0 && index < layers.size()) {
            SkinLayer l = layers.get(index);
            l.isVisible = !l.isVisible;
            recompose();
        }
    }

    public void moveLayerUp(int index) {
        if (index < layers.size() - 1) {
            SkinLayer l = layers.remove(index);
            layers.add(index + 1, l);
            recompose();
        }
    }

    public void setShowBaseSkin(boolean show) {
        this.showBaseSkin = show;
        recompose();
    }

    public void setBodyColor(int color) {
        this.currentBodyColor = color;
        recompose();
    }

    public void resetAll() {
        for(SkinLayer l : layers) l.close();
        layers.clear();
        currentBodyColor = -1;
        showBaseSkin = true;
        recompose();
    }

    // --- OUTILS INTERNES ---

    private void loadBaseSkinInMemory() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        try {
            String url = client.player.getSkinTextures().textureUrl();
            InputStream stream;
            if (url != null && !url.isEmpty()) {
                stream = URI.create(url).toURL().openStream();
            } else {
                Identifier id = DefaultSkinHelper.getTexture();
                stream = client.getResourceManager().getResource(id).get().getInputStream();
            }

            this.basePlayerPixels = NativeImage.read(stream);
            stream.close();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private NativeImage loadFromDisk(String relativePath) {
        try {
            Path configDir = FabricLoader.getInstance().getConfigDir();
            File file = configDir.resolve("skineditor/outfits/" + relativePath).toFile();
            if (file.exists()) {
                return NativeImage.read(new FileInputStream(file));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    private void clearImage() {
        for (int x = 0; x < 64; x++)
            for (int y = 0; y < 64; y++)
                this.bufferImage.setColor(x, y, 0x00000000);
    }

    private void copyPixels(NativeImage src, NativeImage dest) {
        for (int x = 0; x < 64; x++) {
            for (int y = 0; y < 64; y++) {
                dest.setColor(x, y, src.getColor(x, y));
            }
        }
    }

    private void mergePixels(NativeImage src, NativeImage dest) {
        for (int x = 0; x < src.getWidth(); x++) {
            for (int y = 0; y < src.getHeight(); y++) {
                int color = src.getColor(x, y);
                if ((color >> 24 & 0xFF) > 0) { // Si pas transparent
                    dest.setColor(x, y, color);
                }
            }
        }
    }

    private void doPaintSkin(int color) {
        // Tête
        fillPixels(8, 0, 8, 8, color); fillPixels(16, 0, 8, 8, color); fillPixels(0, 8, 32, 8, color);
        // Torse
        fillPixels(20, 16, 8, 4, color); fillPixels(28, 16, 8, 4, color); fillPixels(16, 20, 24, 12, color); fillPixels(32, 20, 8, 12, color);
        // Bras
        fillPixels(44, 16, 4, 4, color); fillPixels(48, 16, 4, 4, color); fillPixels(40, 20, 16, 12, color);
        fillPixels(36, 48, 4, 4, color); fillPixels(40, 48, 4, 4, color); fillPixels(32, 52, 16, 12, color);
        // Jambes
        fillPixels(4, 16, 4, 4, color); fillPixels(8, 16, 4, 4, color); fillPixels(0, 20, 16, 12, color);
        fillPixels(20, 48, 4, 4, color); fillPixels(24, 48, 4, 4, color); fillPixels(16, 52, 16, 12, color);
    }

    private void fillPixels(int x, int y, int w, int h, int color) {
        for (int i = x; i < x + w; i++) {
            for (int j = y; j < y + h; j++) {
                if (i >= 0 && i < 64 && j >= 0 && j < 64) this.bufferImage.setColor(i, j, color);
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
    }
}