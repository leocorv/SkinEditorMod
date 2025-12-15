package org.nerix.skineditor.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.loader.api.FabricLoader; // IMPORTANT pour le chemin config
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class EditableSkin implements AutoCloseable {
    private final NativeImage image;
    private final NativeImage baseImage;
    private final NativeImage overlayImage;
    private final NativeImageBackedTexture texture;
    private final Identifier textureId;
    private boolean showBaseSkin = true;

    public EditableSkin() {
        this.image = new NativeImage(NativeImage.Format.RGBA, 64, 64, false);
        this.baseImage = new NativeImage(NativeImage.Format.RGBA, 64, 64, false);
        this.overlayImage = new NativeImage(NativeImage.Format.RGBA, 64, 64, false);
        this.texture = new NativeImageBackedTexture(this.image);

        String uniqueName = "temp_skin_" + System.currentTimeMillis();
        this.textureId = MinecraftClient.getInstance().getTextureManager()
                .registerDynamicTexture(uniqueName, this.texture);

        this.texture.setFilter(false, false);
        loadDefaultSteve();
    }

    public Identifier getTextureId() { return textureId; }

    public boolean isBaseSkinVisible() { return showBaseSkin; }

    public void setBaseSkinVisible(boolean visible) {
        this.showBaseSkin = visible;
        composeAndUpload();
    }

    // Charge le skin de Steve depuis les assets internes
    public void loadDefaultSteve() {
        try {
            Identifier steveId = Identifier.of("minecraft", "textures/entity/player/wide/steve.png");
            Optional<Resource> resource = MinecraftClient.getInstance().getResourceManager().getResource(steveId);
            if (resource.isPresent()) {
                try (InputStream stream = resource.get().getInputStream()) {
                    NativeImage steveImage = NativeImage.read(stream);
                    this.baseImage.copyFrom(steveImage);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            fillArea(0, 0, 64, 64, 0xFFFFFFFF);
        }
        clearImage(overlayImage);
        composeAndUpload();
    }

    public void loadPlayerSkin(AbstractClientPlayerEntity player) {
        if (player == null) return;

        SkinTextures textures = player.getSkinTextures();
        NativeImage playerImage = tryExtractTexture(textures.texture());

        if (playerImage == null && textures.textureUrl() != null && !textures.textureUrl().isBlank()) {
            try (InputStream stream = new URL(textures.textureUrl()).openStream()) {
                playerImage = NativeImage.read(stream);
            } catch (IOException ignored) { }
        }

        if (playerImage != null) {
            this.baseImage.copyFrom(playerImage);
            composeAndUpload();
        }
    }

    private NativeImage tryExtractTexture(Identifier id) {
        AbstractTexture texture = MinecraftClient.getInstance().getTextureManager().getTexture(id);
        if (texture instanceof NativeImageBackedTexture nativeImageBackedTexture) {
            NativeImage img = nativeImageBackedTexture.getImage();
            if (img != null) {
                NativeImage copy = new NativeImage(img.getFormat(), img.getWidth(), img.getHeight(), false);
                copy.copyFrom(img);
                return copy;
            }
        }
        return null;
    }

    // --- LA MÉTHODE QUI POSAIT PROBLÈME (Corrigée avec les bons imports) ---
    public void mergeLayerFromDisk(String relativePath) {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        File file = configDir.resolve("skineditor/outfits/" + relativePath).toFile();

        if (!file.exists()) {
            System.out.println("Erreur : Fichier introuvable -> " + file.getAbsolutePath());
            return;
        }

        try (FileInputStream stream = new FileInputStream(file)) {
            NativeImage layerImage = NativeImage.read(stream);

            for (int x = 0; x < layerImage.getWidth(); x++) {
                for (int y = 0; y < layerImage.getHeight(); y++) {
                    int color = layerImage.getColor(x, y);
                    if ((color >> 24 & 0xFF) > 0) {
                        this.overlayImage.setColor(x, y, color);
                    }
                }
            }
            composeAndUpload();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // --- COLORIAGE PEAU (Sliders) ---
    public void paintSkin(int color) {
        fillPixels(baseImage,8, 0, 8, 8, color); fillPixels(baseImage,16, 0, 8, 8, color);
        fillPixels(baseImage,0, 8, 32, 8, color);

        fillPixels(baseImage,20, 16, 8, 4, color); fillPixels(baseImage,28, 16, 8, 4, color);
        fillPixels(baseImage,16, 20, 24, 12, color);
        fillPixels(baseImage,32, 20, 8, 12, color);

        fillPixels(baseImage,44, 16, 4, 4, color); fillPixels(baseImage,48, 16, 4, 4, color);
        fillPixels(baseImage,40, 20, 16, 12, color);
        fillPixels(baseImage,36, 48, 4, 4, color); fillPixels(baseImage,40, 48, 4, 4, color);
        fillPixels(baseImage,32, 52, 16, 12, color);

        fillPixels(baseImage,4, 16, 4, 4, color); fillPixels(baseImage,8, 16, 4, 4, color);
        fillPixels(baseImage,0, 20, 16, 12, color);
        fillPixels(baseImage,20, 48, 4, 4, color); fillPixels(baseImage,24, 48, 4, 4, color);
        fillPixels(baseImage,16, 52, 16, 12, color);

        composeAndUpload();
    }

    public void fillArea(int x, int y, int width, int height, int color) {
        fillPixels(baseImage,x, y, width, height, color);
        composeAndUpload();
    }

    public void resetToSteveBase() {
        fillArea(0, 0, 64, 64, 0x00000000);
        clearImage(overlayImage);
        loadDefaultSteve();
    }

    public Path exportSkin() throws IOException {
        Path exportDir = FabricLoader.getInstance().getConfigDir().resolve("skineditor/exports");
        Files.createDirectories(exportDir);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        Path exportFile = exportDir.resolve("skin_" + timestamp + ".png");
        try (var output = Files.newOutputStream(exportFile)) {
            this.image.writeTo(output);
        }
        return exportFile;
    }

    private void fillPixels(NativeImage target,int x, int y, int w, int h, int color) {
        for (int i = x; i < x + w; i++) {
            for (int j = y; j < y + h; j++) {
                if (i >= 0 && i < 64 && j >= 0 && j < 64) {
                    target.setColor(i, j, color);
                }
            }
        }
    }

    private void composeAndUpload() {
        clearImage(this.image);

        if (showBaseSkin) {
            this.image.copyFrom(this.baseImage);
        }

        for (int x = 0; x < overlayImage.getWidth(); x++) {
            for (int y = 0; y < overlayImage.getHeight(); y++) {
                int color = overlayImage.getColor(x, y);
                if ((color >> 24 & 0xFF) > 0) {
                    this.image.setColor(x, y, color);
                }
            }
        }

        upload();
    }

    private void upload() {
        this.texture.bindTexture();
        RenderSystem.texParameter(3553, 10241, 9728);
        RenderSystem.texParameter(3553, 10240, 9728);
        this.texture.upload();
    }

    private void clearImage(NativeImage target) {
        for (int x = 0; x < target.getWidth(); x++) {
            for (int y = 0; y < target.getHeight(); y++) {
                target.setColor(x, y, 0x00000000);
            }
        }
    }

    @Override
    public void close() {
        this.texture.close();
    }
}
