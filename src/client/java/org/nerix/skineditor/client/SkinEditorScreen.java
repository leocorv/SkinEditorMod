package org.nerix.skineditor.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class SkinEditorScreen extends Screen {

    private int leftZoneWidth;
    private int rightZoneWidth;
    private float playerRotationY = 0f;
    private boolean isDraggingModel = false;

    // COULEURS ACTUELLES (0.0 à 1.0)
    // On met une couleur "chair" par défaut (un peu orange/beige)
    private float skinRed = 1.0f;
    private float skinGreen = 0.8f;
    private float skinBlue = 0.6f;

    private enum MenuState { MAIN, CLOTHES, BODY }
    private MenuState currentMenu = MenuState.MAIN;

    public SkinEditorScreen() {
        super(Text.literal("Skin Editor"));
    }

    // 1. Désactiver la PAUSE (Source n°1 du flou)
    @Override
    public boolean shouldPause() {
        return false;
    }

    // 2. Désactiver le FOND PAR DÉFAUT (Source n°2 du flou)
    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Vide : on ne veut pas que Minecraft dessine son dégradé
    }

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

        // Bouton Fermer
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Fermer"), button -> this.close())
                .dimensions(this.width - 120, this.height - 40, 100, 20).build());

        int btnW = rightZoneWidth - 20;
        int startX = this.width - rightZoneWidth + 10;
        int startY = 40;
        int gap = 25;

        // Navigation Menus
        if (currentMenu == MenuState.MAIN) {
            this.addDrawableChild(ButtonWidget.builder(Text.literal("> Vêtements"), b -> { currentMenu = MenuState.CLOTHES; rebuildUI(); }).dimensions(startX, startY, btnW, 20).build());
            this.addDrawableChild(ButtonWidget.builder(Text.literal("> Corps & Peau"), b -> { currentMenu = MenuState.BODY; rebuildUI(); }).dimensions(startX, startY + gap, btnW, 20).build());
            this.addDrawableChild(ButtonWidget.builder(Text.literal("Reset à Steve"), b -> { if(getSkin() != null) { getSkin().loadDefaultSteve(); updateGlobalSkin(); } }).dimensions(startX, startY + gap * 3, btnW, 20).build());
        }
        else if (currentMenu == MenuState.CLOTHES) {
            this.addDrawableChild(ButtonWidget.builder(Text.literal("Ajouter T-Shirt (PNG)"), b -> { if(getSkin() != null) { getSkin().mergeLayer(Identifier.of("skineditor", "textures/tshirt.png")); updateGlobalSkin(); } }).dimensions(startX, startY, btnW, 20).build());
            this.addDrawableChild(ButtonWidget.builder(Text.literal("Ajouter Rouge"), b -> { if(getSkin() != null) { getSkin().fillArea(20, 20, 8, 12, 0xFF0000FF); updateGlobalSkin(); } }).dimensions(startX, startY + gap, btnW, 20).build());
            this.addDrawableChild(ButtonWidget.builder(Text.literal("< Retour"), b -> { currentMenu = MenuState.MAIN; rebuildUI(); }).dimensions(startX, startY + gap * 3, btnW, 20).build());
        }
        else if (currentMenu == MenuState.BODY) {

            // SLIDER ROUGE
            this.addDrawableChild(new ColorSlider(startX, startY, btnW, 20, Text.literal("Rouge"), skinRed) {
                @Override protected void updateMessage() { this.setMessage(Text.literal("Rouge: " + (int)(value * 255))); }
                @Override protected void applyValue() { skinRed = (float)this.value; updateSkinColor(); }
            });

            // SLIDER VERT
            this.addDrawableChild(new ColorSlider(startX, startY + gap, btnW, 20, Text.literal("Vert"), skinGreen) {
                @Override protected void updateMessage() { this.setMessage(Text.literal("Vert: " + (int)(value * 255))); }
                @Override protected void applyValue() { skinGreen = (float)this.value; updateSkinColor(); }
            });

            // SLIDER BLEU
            this.addDrawableChild(new ColorSlider(startX, startY + gap * 2, btnW, 20, Text.literal("Bleu"), skinBlue) {
                @Override protected void updateMessage() { this.setMessage(Text.literal("Bleu: " + (int)(value * 255))); }
                @Override protected void applyValue() { skinBlue = (float)this.value; updateSkinColor(); }
            });

            // Bouton Retour
            this.addDrawableChild(ButtonWidget.builder(Text.literal("< Retour"), button -> {
                this.currentMenu = MenuState.MAIN;
                rebuildUI();
            }).dimensions(startX, startY + gap * 4, btnW, 20).build());
        }
    }

    // --- LE RENDU SÉCURISÉ ---
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {

        // 1. Fond Gris Opaque (Priorité absolue)
        context.fill(0, 0, this.width, this.height, 0xFF202020);

        // 2. Le Personnage 3D (C'est lui le danger pour le flou)
        // On le dessine MAINTENANT, et on nettoie juste après.
        renderPlayerDoll(context);

        // --- ZONE DE NETTOYAGE OBLIGATOIRE ---
        // Après avoir dessiné de la 3D, on force le moteur à revenir en mode "2D propre"
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        // -------------------------------------

        // 3. Interface 2D (Lignes, Textes) - Dessinés par dessus le joueur nettoyé
        context.fill(leftZoneWidth, 0, leftZoneWidth + 1, this.height, 0xFF404040);
        context.fill(this.width - rightZoneWidth, 0, this.width - rightZoneWidth + 1, this.height, 0xFF404040);

        String catTitle = switch(currentMenu) { case CLOTHES -> "Vêtements"; case BODY -> "Corps"; default -> "Menu"; };
        context.drawCenteredTextWithShadow(this.textRenderer, "Aperçu", leftZoneWidth / 2, 10, 0xFFAAAAAA);
        context.drawCenteredTextWithShadow(this.textRenderer, "Éditeur", this.width / 2, 10, 0xFFAAAAAA);
        context.drawCenteredTextWithShadow(this.textRenderer, catTitle, this.width - (rightZoneWidth / 2), 10, 0xFFAAAAAA);

        // 4. Le Skin Flat (Central)
        if (getSkin() != null) {
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            int size = 128;

            context.getMatrices().push();
            context.getMatrices().translate(0, 0, 10);

            // Cadre
            context.fill(centerX - size/2 - 2, centerY - size/2 - 2, centerX + size/2 + 2, centerY + size/2 + 2, 0xFF888888);

            // Force la netteté pour le skin
            RenderSystem.texParameter(3553, 10241, 9728);
            RenderSystem.texParameter(3553, 10240, 9728);

            context.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            context.drawTexture(getSkin().getTextureId(), centerX - size/2, centerY - size/2, size, size, 0, 0, 64, 64, 64, 64);

            context.getMatrices().pop();
        }

        // 5. Les Boutons (super.render dessine les boutons par dessus tout le reste)
        super.render(context, mouseX, mouseY, delta);
    }

    private void renderPlayerDoll(DrawContext context) {
        if (this.client == null || this.client.player == null) return;
        int x = leftZoneWidth / 2;
        int y = this.height / 2 + 60;
        Quaternionf quaternion = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf rotationDrag = new Quaternionf().rotateY((float) Math.toRadians(-playerRotationY));
        quaternion.mul(rotationDrag);

        context.getMatrices().push();
        // On rapproche le modèle pour qu'il ne soit pas coupé par le fond
        context.getMatrices().translate(0, 0, 50);
        InventoryScreen.drawEntity(context, x, y, 70, new Vector3f(0,0,0), quaternion, new Quaternionf(), this.client.player);
        context.getMatrices().pop();
    }

    private EditableSkin getSkin() { return SkinEditorModClient.PERMANENT_SKIN; }
    private void updateGlobalSkin() { if (getSkin() != null) SkinEditorModClient.TEMP_SKIN_ID = getSkin().getTextureId(); }

    // Interactions Souris
    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) { if (button == 0 && mouseX < leftZoneWidth) { this.isDraggingModel = true; return true; } return super.mouseClicked(mouseX, mouseY, button); }
    @Override public boolean mouseReleased(double mouseX, double mouseY, int button) { if (button == 0) isDraggingModel = false; return super.mouseReleased(mouseX, mouseY, button); }
    @Override public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) { if (this.isDraggingModel) { this.playerRotationY += (float) deltaX * 2.0f; return true; } return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY); }

    // --- CLASSE UTILITAIRE POUR LES SLIDERS ---
    private abstract static class ColorSlider extends net.minecraft.client.gui.widget.SliderWidget {
        public ColorSlider(int x, int y, int width, int height, Text text, double value) {
            super(x, y, width, height, text, value);
            this.updateMessage();
        }
        // Ces méthodes seront définies quand on créera les sliders
    }

    // Applique la couleur des sliders sur le corps
    private void updateSkinColor() {
        if (getSkin() != null) {
            // Conversion 0.0-1.0 vers 0-255
            int r = (int)(skinRed * 255);
            int g = (int)(skinGreen * 255);
            int b = (int)(skinBlue * 255);

            // Format ABGR pour Minecraft (Alpha, Bleu, Vert, Rouge)
            // 0xFF = Alpha 255 (Opaque)
            int color = (0xFF << 24) | (b << 16) | (g << 8) | r;

            // On appelle la méthode de peinture intelligente (que tu as déjà dans EditableSkin normalement)
            // Si tu ne l'as pas, je te la remets plus bas
            getSkin().paintSkin(color);

            updateGlobalSkin();
        }
    }
}