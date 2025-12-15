package org.nerix.skineditor.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EditableSkin implements AutoCloseable {
    private final NativeImage image;
    private final NativeImageBackedTexture texture;
    private final Identifier textureId;

    // --- NOUVEAU : MÉMOIRE DES MODIFICATIONS ---
    private boolean showBaseSkin = true; // La case à cocher
    private final List<String> appliedPaths = new ArrayList<>(); // Liste des vêtements ajoutés
    private int currentBodyColor = -1; // -1 = Pas de couleur, sinon code couleur int

    public EditableSkin() {
        this.image = new NativeImage(NativeImage.Format.RGBA, 64, 64, false);
        this.texture = new NativeImageBackedTexture(this.image);

        String uniqueName = "temp_skin_" + System.currentTimeMillis();
        this.textureId = MinecraftClient.getInstance().getTextureManager()
                .registerDynamicTexture(uniqueName, this.texture);

        this.texture.setFilter(false, false);

        // Au démarrage, on construit l'image
        recompose();
    }

    public Identifier getTextureId() { return textureId; }

    // --- 1. LE MOTEUR DE RECOMPOSITION ---
    // Cette fonction redessine tout de zéro. C'est le secret pour la Checkbox.
    public void recompose() {
        // A. On efface tout (Transparent)
        fillPixels(0, 0, 64, 64, 0x00000000);

        // B. Si la case est cochée, on dessine le joueur
        if (showBaseSkin) {
            loadCurrentPlayerSkin();
        }

        // C. Si une couleur de peau est définie, on l'applique
        if (currentBodyColor != -1) {
            doPaintSkin(currentBodyColor);
        }

        // D. On ré-applique tous les vêtements de la liste
        for (String path : appliedPaths) {
            doMergeLayerFromDisk(path);
        }

        upload();
    }

    // --- 2. ACTIONS PUBLIQUES (Appelées par l'écran) ---

    public void setShowBaseSkin(boolean show) {
        this.showBaseSkin = show;
        recompose(); // On redessine tout instantanément
    }

    // On remplace "mergeLayer" par "addLayer" car on ajoute à la liste
    public void addLayer(String path) {
        this.appliedPaths.add(path);
        recompose();
    }

    public void setBodyColor(int color) {
        this.currentBodyColor = color;
        recompose();
    }

    // Pour le bouton Reset
    public void resetAll() {
        this.appliedPaths.clear();
        this.currentBodyColor = -1;
        this.showBaseSkin = true;
        recompose();
    }

//    // --- 3. SAUVEGARDE (EXPORT PNG) ---
//    public void saveSkinToDisk() {
//        try {
//            Path runDir = MinecraftClient.getInstance().runDirectory.toPath();
//            Path saveDir = runDir.resolve("saved_skins");
//
//            if (!Files.exists(saveDir)) Files.createDirectories(saveDir);
//
//            String filename = "skin_" + System.currentTimeMillis() + ".png";
//            Path target = saveDir.resolve(filename);
//
//            this.image.writeTo(target);
//            System.out.println("Skin sauvegardé ici : " + target.toAbsolutePath());
//
//            // Petit son ou feedback chat pourrait être ajouté ici
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    // --- 4. MÉTHODES INTERNES (LOGIQUE DESSIN) ---

    private void loadCurrentPlayerSkin() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        String skinUrl = client.player.getSkinTextures().textureUrl();
        if (skinUrl != null && !skinUrl.isEmpty()) {
            try {
                // CORRECTION ICI : new URL() est déprécié en Java 21, on utilise URI
                InputStream stream = URI.create(skinUrl).toURL().openStream();
                NativeImage skin = NativeImage.read(stream);
                this.image.copyFrom(skin);
                stream.close();
                return;
            } catch (Exception e) {
                // Ignore erreur réseau
            }
        }
        loadDefaultSkin();
    }

    private void loadDefaultSkin() {
        // Copie ici ta méthode loadDefaultSkin précédente
        // ... (pour abréger, je mets le standard)
        try {
            Identifier id = DefaultSkinHelper.getTexture();
            InputStream s = MinecraftClient.getInstance().getResourceManager().getResource(id).get().getInputStream();
            this.image.copyFrom(NativeImage.read(s));
        } catch (Exception e) {}
    }


    private void doMergeLayerFromDisk(String relativePath) {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        File file = configDir.resolve("skineditor/outfits/" + relativePath).toFile();

        if (file.exists()) {
            try (FileInputStream stream = new FileInputStream(file)) {
                NativeImage layer = NativeImage.read(stream);
                // Fusion
                for (int x = 0; x < layer.getWidth(); x++) {
                    for (int y = 0; y < layer.getHeight(); y++) {
                        int color = layer.getColor(x, y);
                        if ((color >> 24 & 0xFF) > 0) { // Si pas transparent
                            this.image.setColor(x, y, color);
                        }
                    }
                }
            } catch (IOException e) { e.printStackTrace(); }
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
                if (i >= 0 && i < 64 && j >= 0 && j < 64) this.image.setColor(i, j, color);
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