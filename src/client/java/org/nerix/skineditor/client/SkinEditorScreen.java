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
    private boolean isPainting = false; // Si on est en train de dessiner

    private OutfitCategory currentFolder = null;

    // On retire le menu "BODY" car les sliders sont maintenant toujours dispo pour le pinceau
    private boolean isCategoryMode = true;

    // Couleurs (Pinceau & Peau)
    private float red = 1.0f, green = 1.0f, blue = 1.0f;
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

        // --- GAUCHE : LISTE DES CALQUES ---
        int layerY = 150;
        int layerX = 10;
        int layerW = leftZoneWidth - 20;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("--- CALQUES ---"), b->{}).dimensions(layerX, layerY - 25, layerW, 20).build()).active = false;

        if (getSkin() != null) {
            for (int i = getSkin().layers.size() - 1; i >= 0; i--) {
                SkinLayer layer = getSkin().layers.get(i);
                final int index = i;
                String eyeText = layer.isVisible ? "[O]" : "[-]";

                this.addDrawableChild(ButtonWidget.builder(Text.literal(eyeText), b -> {
                    getSkin().toggleLayer(index); rebuildUI();
                }).dimensions(layerX, layerY, 20, 20).build());

                this.addDrawableChild(ButtonWidget.builder(Text.literal(layer.name), b -> {}).dimensions(layerX + 22, layerY, layerW - 44, 20).build());

                this.addDrawableChild(ButtonWidget.builder(Text.literal("X"), b -> {
                    getSkin().removeLayer(index); rebuildUI();
                }).dimensions(layerX + layerW - 20, layerY, 20, 20).build());
                layerY += 22;
            }
        }

        // --- CENTRE : OUTILS ---
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        CheckboxWidget baseToggle = CheckboxWidget.builder(Text.literal("Base Skin"), this.textRenderer)
                .pos(centerX - 40, centerY + 80)
                .checked(this.baseSkinEnabled)
                .callback((checkbox, checked) -> {
                    this.baseSkinEnabled = checked;
                    if(getSkin() != null) { getSkin().setShowBaseSkin(checked); updateGlobalSkin(); }
                }).build();
        this.addDrawableChild(baseToggle);

        // --- DROITE : COULEURS & NAVIGATION ---
        int btnW = rightZoneWidth - 20;
        int startX = this.width - rightZoneWidth + 10;
        int currentY = 40;
        int gap = 25;

        // SLIDERS COULEUR (Toujours visibles pour le Pinceau)
        this.addDrawableChild(ButtonWidget.builder(Text.literal("--- COULEURS ---"), b->{}).dimensions(startX, 10, btnW, 20).build()).active = false;

        this.addDrawableChild(new ColorSlider(startX, currentY, btnW, 20, Text.literal("R"), red) {
            @Override protected void updateMessage() { setMessage(Text.literal("Rouge: " + (int)(value*255))); }
            @Override protected void applyValue() { red = (float)value; }
        }); currentY += gap;

        this.addDrawableChild(new ColorSlider(startX, currentY, btnW, 20, Text.literal("V"), green) {
            @Override protected void updateMessage() { setMessage(Text.literal("Vert: " + (int)(value*255))); }
            @Override protected void applyValue() { green = (float)value; }
        }); currentY += gap;

        this.addDrawableChild(new ColorSlider(startX, currentY, btnW, 20, Text.literal("B"), blue) {
            @Override protected void updateMessage() { setMessage(Text.literal("Bleu: " + (int)(value*255))); }
            @Override protected void applyValue() { blue = (float)value; }
        }); currentY += gap;

        // BOUTON "APPLIQUER AU CORPS" (Transforme la couleur en calque)
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Remplir Corps"), b -> {
            applyColorToBody();
            rebuildUI(); // Pour voir le calque apparaître
        }).dimensions(startX, currentY, btnW, 20).build());
        currentY += gap * 2;

        // NAVIGATION
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Fermer"), button -> this.close())
                .dimensions(this.width - 120, this.height - 40, 100, 20).build());

        if (currentFolder == null) {
            this.addDrawableChild(ButtonWidget.builder(Text.literal("> Vêtements (Dossiers)"), button -> {
                if (ClientNetworkHandler.CLIENT_ROOT_CATEGORY != null) {
                    this.currentFolder = ClientNetworkHandler.CLIENT_ROOT_CATEGORY;
                    rebuildUI();
                }
            }).dimensions(startX, currentY, btnW, 20).build());
            currentY += gap;

            this.addDrawableChild(ButtonWidget.builder(Text.literal("Tout Effacer"), button -> {
                if(getSkin() != null) { getSkin().resetAll(); rebuildUI(); updateGlobalSkin(); }
            }).dimensions(startX, currentY + gap, btnW, 20).build());
        }
        else {
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
                        getSkin().addLayer(item.path, item.displayName);
                        rebuildUI(); updateGlobalSkin();
                    }
                }).dimensions(startX, currentY, btnW, 20).build());
                currentY += gap;
            }
            this.addDrawableChild(ButtonWidget.builder(Text.literal("< Retour"), button -> {
                this.currentFolder = null; rebuildUI();
            }).dimensions(startX, currentY + gap, btnW, 20).build());
        }
    }

    // --- LOGIQUE DESSIN ET COULEUR ---

    private void applyColorToBody() {
        if (getSkin() != null) {
            int r = (int)(red * 255); int g = (int)(green * 255); int b = (int)(blue * 255);
            int color = (0xFF << 24) | (b << 16) | (g << 8) | r;
            getSkin().updateBodyColorLayer(color);
            updateGlobalSkin();
        }
    }

    private void paintAtMouse(double mouseX, double mouseY) {
        if (getSkin() == null) return;

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int size = 128; // Taille visuelle
        int texSize = 64; // Taille réelle pixels

        int x = (int)mouseX - (centerX - size/2);
        int y = (int)mouseY - (centerY - size/2);

        // Si on est dans le carré
        if (x >= 0 && x < size && y >= 0 && y < size) {
            // Conversion Coordonnée écran -> Coordonnée Texture (0-63)
            int pixelX = (x * texSize) / size;
            int pixelY = (y * texSize) / size;

            int r = (int)(red * 255); int g = (int)(green * 255); int b = (int)(blue * 255);
            int color = (0xFF << 24) | (b << 16) | (g << 8) | r;

            getSkin().paintPixel(pixelX, pixelY, color);
            updateGlobalSkin();
        }
    }

    private EditableSkin getSkin() { return SkinEditorModClient.PERMANENT_SKIN; }
    private void updateGlobalSkin() { if (getSkin() != null) SkinEditorModClient.TEMP_SKIN_ID = getSkin().getTextureId(); }

    private abstract static class ColorSlider extends SliderWidget {
        public ColorSlider(int x, int y, int width, int height, Text text, double value) { super(x, y, width, height, text, value); updateMessage(); }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xFF202020);
        context.fill(leftZoneWidth, 0, leftZoneWidth + 1, this.height, 0xFF404040);
        context.fill(this.width - rightZoneWidth, 0, this.width - rightZoneWidth + 1, this.height, 0xFF404040);

        context.drawCenteredTextWithShadow(this.textRenderer, "Aperçu & Calques", leftZoneWidth / 2, 10, 0xFFAAAAAA);
        context.drawCenteredTextWithShadow(this.textRenderer, "Éditeur", this.width / 2, 10, 0xFFAAAAAA);
        context.drawCenteredTextWithShadow(this.textRenderer, "Palette & Catégories", this.width - (rightZoneWidth / 2), 10, 0xFFAAAAAA);

        // Dessin de l'Éditeur Central
        if (getSkin() != null) {
            int centerX = this.width / 2; int centerY = this.height / 2; int size = 128;
            context.getMatrices().push(); context.getMatrices().translate(0, 0, 10);
            context.fill(centerX - size/2 - 2, centerY - size/2 - 2, centerX + size/2 + 2, centerY + size/2 + 2, 0xFF888888);
            RenderSystem.texParameter(3553, 10241, 9728); RenderSystem.texParameter(3553, 10240, 9728);
            context.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            context.drawTexture(getSkin().getTextureId(), centerX - size/2, centerY - size/2, size, size, 0, 0, 64, 64, 64, 64);
            context.getMatrices().pop();

            // Cadre curseur si souris dessus
            if (mouseX >= centerX - size/2 && mouseX < centerX + size/2 &&
                    mouseY >= centerY - size/2 && mouseY < centerY + size/2) {
                context.drawBorder((int)mouseX - 2, (int)mouseY - 2, 4, 4, 0xFFFFFFFF);
            }
        }
        renderPlayerDoll(context);
        super.render(context, mouseX, mouseY, delta);
    }

    private void renderPlayerDoll(DrawContext context) {
        if (this.client == null || this.client.player == null) return;
        int x = leftZoneWidth / 2;
        int y = 110;
        int size = 50;
        Quaternionf q = new Quaternionf().rotateZ((float)Math.PI).mul(new Quaternionf().rotateY((float)Math.toRadians(-playerRotationY)));
        context.getMatrices().push(); context.getMatrices().translate(0, 0, 50);
        InventoryScreen.drawEntity(context, x, y, size, new Vector3f(0,0,0), q, new Quaternionf(), this.client.player);
        context.getMatrices().pop();
        RenderSystem.disableDepthTest(); RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc();
    }

    // --- INTERACTIONS SOURIS ---

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true; // Boutons prioritaires

        // Zone Gauche : Rotation
        if (button == 0 && mouseX < leftZoneWidth) {
            this.isDraggingModel = true; return true;
        }

        // Zone Centre : Dessin
        int centerX = this.width / 2; int size = 128;
        if (button == 0 && mouseX >= centerX - size/2 && mouseX < centerX + size/2) {
            this.isPainting = true;
            paintAtMouse(mouseX, mouseY); // Peindre le premier point
            return true;
        }

        return false;
    }

    @Override public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            isDraggingModel = false;
            isPainting = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (this.isDraggingModel) {
            this.playerRotationY += (float) deltaX * 2.0f;
            return true;
        }
        if (this.isPainting) {
            paintAtMouse(mouseX, mouseY); // Peinture continue
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }
}