package org.nerix.skineditor.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
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

    private OutfitCategory currentFolder = null;
    private boolean isBodyMode = false;
    private float skinRed = 1.0f, skinGreen = 0.8f, skinBlue = 0.6f;
    private boolean baseSkinEnabled = true;

    public SkinEditorScreen() { super(Text.literal("Skin Editor")); }
    @Override public boolean shouldPause() { return false; }
    @Override public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {}

    @Override
    protected void init() {
        this.leftZoneWidth = this.width / 4;
        this.rightZoneWidth = this.width / 4;

        if (SkinEditorModClient.PERMANENT_SKIN == null) {
            SkinEditorModClient.PERMANENT_SKIN = new EditableSkin();
            updateGlobalSkin();
        }
        rebuildUI();
    }

    private void rebuildUI() {
        this.clearChildren();

        // --- ZONE GAUCHE : LISTE DES CALQUES ---
        int layerY = 40;
        int layerX = 10;
        int layerW = leftZoneWidth - 20;

        // Titre
        this.addDrawableChild(ButtonWidget.builder(Text.literal("--- CALQUES ---"), b->{}).dimensions(layerX, 10, layerW, 20).build()).active = false;

        if (getSkin() != null) {
            // On parcourt la liste des calques (en sens inverse pour afficher le dernier ajouté en haut ?)
            // Ou ordre normal : 0 est en dessous.
            // Affichons de haut en bas (dernier ajouté = en haut de la liste visuelle)
            for (int i = getSkin().layers.size() - 1; i >= 0; i--) {
                SkinLayer layer = getSkin().layers.get(i);
                final int index = i;

                // 1. Bouton Visibilité [O] ou [-]
                String eyeText = layer.isVisible ? "[O]" : "[-]";
                this.addDrawableChild(ButtonWidget.builder(Text.literal(eyeText), b -> {
                    getSkin().toggleLayer(index);
                    rebuildUI(); // On rafraichit pour changer le texte du bouton
                }).dimensions(layerX, layerY, 20, 20).build());

                // 2. Nom du calque (Bouton inactif ou sélectionnable)
                this.addDrawableChild(ButtonWidget.builder(Text.literal(layer.name), b -> {
                    // Futur : Sélectionner le calque pour le modifier
                }).dimensions(layerX + 22, layerY, layerW - 44, 20).build());

                // 3. Bouton Supprimer [X]
                this.addDrawableChild(ButtonWidget.builder(Text.literal("X"), b -> {
                    getSkin().removeLayer(index);
                    rebuildUI(); // On rafraichit car la liste a changé
                }).dimensions(layerX + layerW - 20, layerY, 20, 20).build());

                layerY += 22;
            }
        }

        // --- ZONE CENTRE ---
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        CheckboxWidget baseToggle = CheckboxWidget.builder(Text.literal("Base Skin"), this.textRenderer)
                .pos(centerX - 40, centerY + 80)
                .checked(this.baseSkinEnabled)
                .callback((checkbox, checked) -> {
                    this.baseSkinEnabled = checked;
                    if(getSkin() != null) {
                        getSkin().setShowBaseSkin(checked);
                        updateGlobalSkin();
                    }
                }).build();
        this.addDrawableChild(baseToggle);

        // --- ZONE DROITE : CATALOGUE ---
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Fermer"), button -> this.close())
                .dimensions(this.width - 120, this.height - 40, 100, 20).build());

        int btnW = rightZoneWidth - 20;
        int startX = this.width - rightZoneWidth + 10;
        int startY = 40;
        int gap = 25;
        int currentY = startY;

        if (!isBodyMode && currentFolder == null) {
            this.addDrawableChild(ButtonWidget.builder(Text.literal("> Vêtements"), button -> {
                if (ClientNetworkHandler.CLIENT_ROOT_CATEGORY != null) {
                    this.currentFolder = ClientNetworkHandler.CLIENT_ROOT_CATEGORY;
                    rebuildUI();
                }
            }).dimensions(startX, currentY, btnW, 20).build());
            currentY += gap;

            this.addDrawableChild(ButtonWidget.builder(Text.literal("> Corps & Peau"), button -> {
                this.isBodyMode = true; rebuildUI();
            }).dimensions(startX, currentY, btnW, 20).build());
            currentY += gap;

            this.addDrawableChild(ButtonWidget.builder(Text.literal("Tout Effacer"), button -> {
                if(getSkin() != null) { getSkin().resetAll(); rebuildUI(); updateGlobalSkin(); }
            }).dimensions(startX, currentY + gap * 2, btnW, 20).build());
        }
        else if (!isBodyMode && currentFolder != null) {
            // Navigation Dossiers
            this.addDrawableChild(ButtonWidget.builder(Text.literal("[" + currentFolder.displayName + "]"), b -> {}).dimensions(startX, currentY - gap/2, btnW, 15).build()).active = false;
            currentY += gap;

            for (OutfitCategory sub : currentFolder.subCategories) {
                this.addDrawableChild(ButtonWidget.builder(Text.literal("> " + sub.displayName), button -> {
                    this.currentFolder = sub; rebuildUI();
                }).dimensions(startX, currentY, btnW, 20).build());
                currentY += gap;
            }

            for (OutfitItem item : currentFolder.items) {
                this.addDrawableChild(ButtonWidget.builder(Text.literal("+ " + item.displayName), button -> {
                    if (getSkin() != null) {
                        // AJOUT DU CALQUE
                        getSkin().addLayer(item.path, item.displayName);
                        rebuildUI(); // On recharge l'UI pour voir le nouveau calque à gauche !
                        updateGlobalSkin();
                    }
                }).dimensions(startX, currentY, btnW, 20).build());
                currentY += gap;
            }

            currentY += gap;
            this.addDrawableChild(ButtonWidget.builder(Text.literal("< Retour"), button -> {
                this.currentFolder = null; rebuildUI();
            }).dimensions(startX, currentY, btnW, 20).build());
        }

        // 3. COLOR PICKER
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

    private void updateSkinColor() {
        if (getSkin() != null) {
            int r = (int)(skinRed * 255); int g = (int)(skinGreen * 255); int b = (int)(skinBlue * 255);
            int color = (0xFF << 24) | (b << 16) | (g << 8) | r;
            getSkin().setBodyColor(color);
            updateGlobalSkin();
        }
    }
    private EditableSkin getSkin() { return SkinEditorModClient.PERMANENT_SKIN; }
    private void updateGlobalSkin() { if (getSkin() != null) SkinEditorModClient.TEMP_SKIN_ID = getSkin().getTextureId(); }

    private abstract static class ColorSlider extends SliderWidget {
        public ColorSlider(int x, int y, int width, int height, Text text, double value) { super(x, y, width, height, text, value); this.updateMessage(); }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xFF202020);
        context.fill(leftZoneWidth, 0, leftZoneWidth + 1, this.height, 0xFF404040);
        context.fill(this.width - rightZoneWidth, 0, this.width - rightZoneWidth + 1, this.height, 0xFF404040);

        String catTitle = isBodyMode ? "Couleur Peau" : (currentFolder != null ? currentFolder.displayName : "Catégories");
        context.drawCenteredTextWithShadow(this.textRenderer, "Calques & Aperçu", leftZoneWidth / 2, 10, 0xFFAAAAAA);
        context.drawCenteredTextWithShadow(this.textRenderer, "Éditeur", this.width / 2, 10, 0xFFAAAAAA);
        context.drawCenteredTextWithShadow(this.textRenderer, catTitle, this.width - (rightZoneWidth / 2), 10, 0xFFAAAAAA);

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

        // --- CHANGEMENT DE POSITION ---
        // On le met au centre de la zone de GAUCHE (leftZoneWidth / 2)
        int x = leftZoneWidth / 2;

        // On le met tout en bas de l'écran (height - 20)
        int y = this.height - 20;

        // On ajuste la taille pour qu'il rentre dans la barre latérale
        int size = 65;

        Quaternionf q = new Quaternionf().rotateZ((float)Math.PI).mul(new Quaternionf().rotateY((float)Math.toRadians(-playerRotationY)));
        context.getMatrices().push();
        context.getMatrices().translate(0, 0, 50);
        InventoryScreen.drawEntity(context, x, y, size, new Vector3f(0,0,0), q, new Quaternionf(), this.client.player);
        context.getMatrices().pop();

        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 1. D'abord, on vérifie si on a cliqué sur un bouton (Calques, Fermer, etc.)
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        // 2. Sinon, si on est dans la zone de GAUCHE, on tourne le bonhomme
        if (button == 0 && mouseX < leftZoneWidth) {
            this.isDraggingModel = true;
            return true;
        }

        return false;
    }
    @Override public boolean mouseReleased(double mouseX, double mouseY, int button) { if (button == 0) isDraggingModel = false; return super.mouseReleased(mouseX, mouseY, button); }
    @Override public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) { if (this.isDraggingModel) { this.playerRotationY += (float) deltaX * 2.0f; return true; } return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY); }
}