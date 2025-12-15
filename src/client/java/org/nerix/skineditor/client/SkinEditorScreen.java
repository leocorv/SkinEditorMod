package org.nerix.skineditor.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.nerix.skineditor.common.OutfitCategory;
import org.nerix.skineditor.common.OutfitItem;

public class SkinEditorScreen extends Screen {

    private int leftZoneWidth;
    private int rightZoneWidth;
    private float playerRotationY = 0f;
    private boolean isDraggingModel = false;

    // --- NAVIGATION DYNAMIQUE ---
    // Si null = Menu Principal (Racine virtuelle)
    // Si objet = On est dans ce dossier spécifique
    private OutfitCategory currentFolder = null;

    // Mode spécial pour le Color Picker (Corps)
    private boolean isBodyMode = false;

    // Sliders Peau
    private float skinRed = 1.0f, skinGreen = 0.8f, skinBlue = 0.6f;

    public SkinEditorScreen() {
        super(Text.literal("Skin Editor"));
    }

    // --- ANTI FLOU ---
    @Override public boolean shouldPause() { return false; }
    @Override public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {}

    @Override
    protected void init() {
        this.leftZoneWidth = this.width / 4;
        this.rightZoneWidth = this.width / 4;

        if (SkinEditorModClient.PERMANENT_SKIN == null) {
            try {
                SkinEditorModClient.PERMANENT_SKIN = new EditableSkin();
                updateGlobalSkin();
            } catch (Exception e) { e.printStackTrace(); }
        }
        rebuildUI();
    }

    private void rebuildUI() {
        this.clearChildren();

        // Bouton Fermer (Toujours là)
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Fermer"), button -> this.close())
                .dimensions(this.width - 120, this.height - 40, 100, 20).build());

        int btnW = rightZoneWidth - 20;
        int startX = this.width - rightZoneWidth + 10;
        int startY = 40;
        int gap = 25;
        int currentY = startY;

        // ============================================================
        // CAS 1 : MENU PRINCIPAL (Racine)
        // ============================================================
        if (!isBodyMode && currentFolder == null) {

            // Bouton vers la navigation dynamique (Racine Serveur)
            this.addDrawableChild(ButtonWidget.builder(Text.literal("> Vêtements & Accessoires"), button -> {
                // On récupère la racine envoyée par le serveur
                if (ClientNetworkHandler.CLIENT_ROOT_CATEGORY != null) {
                    this.currentFolder = ClientNetworkHandler.CLIENT_ROOT_CATEGORY;
                    rebuildUI();
                } else {
                    // Si le serveur n'a rien envoyé ou qu'on est en solo sans serveur dédié configuré
                    System.out.println("Erreur : Aucune donnée reçue du serveur !");
                }
            }).dimensions(startX, currentY, btnW, 20).build());
            currentY += gap;

            // Bouton vers le Color Picker
            this.addDrawableChild(ButtonWidget.builder(Text.literal("> Corps & Peau"), button -> {
                this.isBodyMode = true;
                rebuildUI();
            }).dimensions(startX, currentY, btnW, 20).build());
            currentY += gap;

            // Reset
            this.addDrawableChild(ButtonWidget.builder(Text.literal("Reset à Steve"), button -> {
                if(getSkin() != null) { getSkin().loadDefaultSteve(); updateGlobalSkin(); }
            }).dimensions(startX, currentY + gap * 2, btnW, 20).build());
        }

        // ============================================================
        // CAS 2 : NAVIGATION DANS LES DOSSIERS (Dynamique)
        // ============================================================
        else if (!isBodyMode && currentFolder != null) {

            // Titre du dossier actuel (Visuel)
            this.addDrawableChild(ButtonWidget.builder(Text.literal("[" + currentFolder.displayName + "]"), b -> {})
                    .dimensions(startX, currentY - gap/2, btnW, 15).build()).active = false;
            currentY += gap;

            // A. Afficher les Sous-Dossiers (Catégories)
            for (OutfitCategory sub : currentFolder.subCategories) {
                this.addDrawableChild(ButtonWidget.builder(Text.literal("> " + sub.displayName), button -> {
                    this.currentFolder = sub; // On descend dans le dossier
                    rebuildUI();
                }).dimensions(startX, currentY, btnW, 20).build());
                currentY += gap;
            }

            // B. Afficher les Items (Fichiers PNG)
            for (OutfitItem item : currentFolder.items) {
                this.addDrawableChild(ButtonWidget.builder(Text.literal("+ " + item.displayName), button -> {
                    if (getSkin() != null) {
                        // C'EST ICI LA CORRECTION :
                        // On appelle la méthode qui charge depuis le disque (dans EditableSkin)
                        getSkin().mergeLayerFromDisk(item.path);
                        updateGlobalSkin();
                    }
                }).dimensions(startX, currentY, btnW, 20).build());
                currentY += gap;
            }

            // Bouton Retour
            currentY += gap; // Espace
            this.addDrawableChild(ButtonWidget.builder(Text.literal("< Retour"), button -> {
                // Retour au menu principal (Simplifié)
                this.currentFolder = null;
                rebuildUI();
            }).dimensions(startX, currentY, btnW, 20).build());
        }

        // ============================================================
        // CAS 3 : COLOR PICKER (Corps)
        // ============================================================
        else if (isBodyMode) {
            this.addDrawableChild(new ColorSlider(startX, currentY, btnW, 20, Text.literal("Rouge"), skinRed) {
                @Override protected void updateMessage() { this.setMessage(Text.literal("Rouge: " + (int)(value * 255))); }
                @Override protected void applyValue() { skinRed = (float)this.value; updateSkinColor(); }
            }); currentY += gap;

            this.addDrawableChild(new ColorSlider(startX, currentY, btnW, 20, Text.literal("Vert"), skinGreen) {
                @Override protected void updateMessage() { this.setMessage(Text.literal("Vert: " + (int)(value * 255))); }
                @Override protected void applyValue() { skinGreen = (float)this.value; updateSkinColor(); }
            }); currentY += gap;

            this.addDrawableChild(new ColorSlider(startX, currentY, btnW, 20, Text.literal("Bleu"), skinBlue) {
                @Override protected void updateMessage() { this.setMessage(Text.literal("Bleu: " + (int)(value * 255))); }
                @Override protected void applyValue() { skinBlue = (float)this.value; updateSkinColor(); }
            }); currentY += gap * 2;

            this.addDrawableChild(ButtonWidget.builder(Text.literal("< Retour"), b -> { isBodyMode = false; rebuildUI(); }).dimensions(startX, currentY, btnW, 20).build());
        }
    }

    // --- LOGIQUE INTERNE ---

    private void updateSkinColor() {
        if (getSkin() != null) {
            int r = (int)(skinRed * 255); int g = (int)(skinGreen * 255); int b = (int)(skinBlue * 255);
            int color = (0xFF << 24) | (b << 16) | (g << 8) | r;
            getSkin().paintSkin(color);
            updateGlobalSkin();
        }
    }

    private EditableSkin getSkin() { return SkinEditorModClient.PERMANENT_SKIN; }
    private void updateGlobalSkin() { if (getSkin() != null) SkinEditorModClient.TEMP_SKIN_ID = getSkin().getTextureId(); }

    private abstract static class ColorSlider extends SliderWidget {
        public ColorSlider(int x, int y, int width, int height, Text text, double value) {
            super(x, y, width, height, text, value);
            this.updateMessage();
        }
    }

    // --- RENDU (AVEC ANTI-FLOU) ---

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xFF202020);

        // Cadres
        context.fill(leftZoneWidth, 0, leftZoneWidth + 1, this.height, 0xFF404040);
        context.fill(this.width - rightZoneWidth, 0, this.width - rightZoneWidth + 1, this.height, 0xFF404040);

        // Titres Dynamiques
        String catTitle = "Menu";
        if(isBodyMode) catTitle = "Couleur Peau";
        else if(currentFolder != null) catTitle = currentFolder.displayName;
        else catTitle = "Catégories";

        context.drawCenteredTextWithShadow(this.textRenderer, "Aperçu", leftZoneWidth / 2, 10, 0xFFAAAAAA);
        context.drawCenteredTextWithShadow(this.textRenderer, "Éditeur", this.width / 2, 10, 0xFFAAAAAA);
        context.drawCenteredTextWithShadow(this.textRenderer, catTitle, this.width - (rightZoneWidth / 2), 10, 0xFFAAAAAA);

        // Skin Central
        if (getSkin() != null) {
            int centerX = this.width / 2; int centerY = this.height / 2; int size = 128;
            context.getMatrices().push(); context.getMatrices().translate(0, 0, 10);
            context.fill(centerX - size/2 - 2, centerY - size/2 - 2, centerX + size/2 + 2, centerY + size/2 + 2, 0xFF888888);
            RenderSystem.texParameter(3553, 10241, 9728); RenderSystem.texParameter(3553, 10240, 9728);
            context.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            context.drawTexture(getSkin().getTextureId(), centerX - size/2, centerY - size/2, size, size, 0, 0, 64, 64, 64, 64);
            context.getMatrices().pop();
        }
        renderPlayerDoll(context);
        super.render(context, mouseX, mouseY, delta);
    }

    private void renderPlayerDoll(DrawContext context) {
        if (this.client == null || this.client.player == null) return;
        int x = leftZoneWidth / 2; int y = this.height / 2 + 60;
        Quaternionf q = new Quaternionf().rotateZ((float)Math.PI).mul(new Quaternionf().rotateY((float)Math.toRadians(-playerRotationY)));
        context.getMatrices().push(); context.getMatrices().translate(0, 0, 50);
        InventoryScreen.drawEntity(context, x, y, 70, new Vector3f(0,0,0), q, new Quaternionf(), this.client.player);
        context.getMatrices().pop();

        // Zone de Nettoyage Anti-Flou
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) { if (button == 0 && mouseX < leftZoneWidth) { this.isDraggingModel = true; return true; } return super.mouseClicked(mouseX, mouseY, button); }
    @Override public boolean mouseReleased(double mouseX, double mouseY, int button) { if (button == 0) isDraggingModel = false; return super.mouseReleased(mouseX, mouseY, button); }
    @Override public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) { if (this.isDraggingModel) { this.playerRotationY += (float) deltaX * 2.0f; return true; } return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY); }
}