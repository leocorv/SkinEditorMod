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
    private final NativeImage bufferImage;
    private final NativeImageBackedTexture texture;
    private final Identifier textureId;

    // 1. Base Skin (Joueur)
    private NativeImage basePlayerPixels = null;
    private boolean showBaseSkin = true;

    // 2. Liste des Calques
    public final List<SkinLayer> layers = new ArrayList<>();

    public EditableSkin() {
        this.bufferImage = new NativeImage(NativeImage.Format.RGBA, 64, 64, false);
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

        // 1. Base
        if (showBaseSkin && basePlayerPixels != null) {
            copyPixels(basePlayerPixels, this.bufferImage);
        }

        // 2. Calques (Inclus désormais la Couleur du corps et le Dessin)
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
        if (img != null) {
            this.layers.add(new SkinLayer(displayName, img));
            recompose();
        }
    }

    // NOUVEAU : La couleur du corps devient un calque !
    public void updateBodyColorLayer(int color) {
        // On cherche si le calque existe déjà
        SkinLayer bodyLayer = null;
        for (SkinLayer l : layers) {
            if (l.name.equals("Peinture Corps")) {
                bodyLayer = l;
                break;
            }
        }

        // Si non, on le crée
        if (bodyLayer == null) {
            NativeImage img = new NativeImage(64, 64, true);
            bodyLayer = new SkinLayer("Peinture Corps", img);
            this.layers.add(0, bodyLayer); // On l'ajoute au début (en bas de la pile)
        }

        // On remplit les zones du corps sur ce calque
        fillBodyParts(bodyLayer.pixels, color);
        recompose();
    }

    // NOUVEAU : Dessiner un pixel (Pinceau)
    public void paintPixel(int x, int y, int color) {
        // On cherche un calque "Dessin", sinon on le crée
        SkinLayer drawLayer = null;
        for (SkinLayer l : layers) {
            if (l.name.equals("Mon Dessin")) {
                drawLayer = l;
                break;
            }
        }

        if (drawLayer == null) {
            NativeImage img = new NativeImage(64, 64, true); // true = clear (transparent)
            drawLayer = new SkinLayer("Mon Dessin", img);
            this.layers.add(drawLayer); // Ajouté à la fin (par dessus tout)
        }

        // On dessine le pixel
        if (x >= 0 && x < 64 && y >= 0 && y < 64) {
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
            SkinLayer l = layers.get(index);
            l.isVisible = !l.isVisible;
            recompose();
        }
    }

    public void setShowBaseSkin(boolean show) {
        this.showBaseSkin = show;
        recompose();
    }

    public void resetAll() {
        for(SkinLayer l : layers) l.close();
        layers.clear();
        showBaseSkin = true;
        recompose();
    }

    // --- INTERNE ---

    private void fillBodyParts(NativeImage img, int color) {
        // On nettoie d'abord l'image du calque
        for(int x=0; x<64; x++) for(int y=0; y<64; y++) img.setColor(x, y, 0);

        // Tête
        fillPixels(img, 8, 0, 8, 8, color); fillPixels(img, 16, 0, 8, 8, color); fillPixels(img, 0, 8, 32, 8, color);
        // Torse
        fillPixels(img, 20, 16, 8, 4, color); fillPixels(img, 28, 16, 8, 4, color); fillPixels(img, 16, 20, 24, 12, color); fillPixels(img, 32, 20, 8, 12, color);
        // Bras
        fillPixels(img, 44, 16, 4, 4, color); fillPixels(img, 48, 16, 4, 4, color); fillPixels(img, 40, 20, 16, 12, color);
        fillPixels(img, 36, 48, 4, 4, color); fillPixels(img, 40, 48, 4, 4, color); fillPixels(img, 32, 52, 16, 12, color);
        // Jambes
        fillPixels(img, 4, 16, 4, 4, color); fillPixels(img, 8, 16, 4, 4, color); fillPixels(img, 0, 20, 16, 12, color);
        fillPixels(img, 20, 48, 4, 4, color); fillPixels(img, 24, 48, 4, 4, color); fillPixels(img, 16, 52, 16, 12, color);
    }

    private void fillPixels(NativeImage img, int x, int y, int w, int h, int color) {
        for (int i = x; i < x + w; i++) {
            for (int j = y; j < y + h; j++) {
                if (i < 64 && j < 64) img.setColor(i, j, color);
            }
        }
    }

    private void loadBaseSkinInMemory() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        try {
            String url = client.player.getSkinTextures().textureUrl();
            InputStream stream;
            if (url != null && !url.isEmpty()) stream = URI.create(url).toURL().openStream();
            else stream = client.getResourceManager().getResource(DefaultSkinHelper.getTexture()).get().getInputStream();
            this.basePlayerPixels = NativeImage.read(stream);
            stream.close();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private NativeImage loadFromDisk(String relativePath) {
        try {
            Path configDir = FabricLoader.getInstance().getConfigDir();
            File file = configDir.resolve("skineditor/outfits/" + relativePath).toFile();
            if (file.exists()) return NativeImage.read(new FileInputStream(file));
        } catch (Exception e) {}
        return null;
    }

    private void clearImage() {
        for (int x = 0; x < 64; x++) for (int y = 0; y < 64; y++) this.bufferImage.setColor(x, y, 0);
    }

    private void copyPixels(NativeImage src, NativeImage dest) {
        for (int x = 0; x < 64; x++) for (int y = 0; y < 64; y++) dest.setColor(x, y, src.getColor(x, y));
    }

    private void mergePixels(NativeImage src, NativeImage dest) {
        for (int x = 0; x < src.getWidth(); x++) {
            for (int y = 0; y < src.getHeight(); y++) {
                int color = src.getColor(x, y);
                if ((color >> 24 & 0xFF) > 0) dest.setColor(x, y, color);
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
        if (basePlayerPixels != null) basePlayerPixels.close();
        for(SkinLayer l : layers) l.close();
    }
}