package org.nerix.skineditor.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class SkinEditorScreen extends Screen {

    // Zones de l'écran
    private int leftZoneWidth;
    private int rightZoneWidth;

    // Gestion du modèle 3D
    private float playerRotationY = 0f;
    private boolean isDraggingModel = false;

    // Gestion du skin
    private EditableSkin currentSkin;

    // --- SYSTÈME DE CATÉGORIES ---
    // On définit les états possibles du menu de droite
    private enum MenuState {
        MAIN,       // Menu principal
        CLOTHES,    // Sous-menu Vêtements
        BODY        // Sous-menu Corps
    }
    private MenuState currentMenu = MenuState.MAIN;

    public SkinEditorScreen() {
        super(Text.literal("Skin Editor"));
    }

    @Override
    protected void init() {
        this.leftZoneWidth = this.width / 4;
        this.rightZoneWidth = this.width / 4;

        // 1. Initialisation du skin (si pas déjà fait)
        if (this.currentSkin == null) {
            try {
                this.currentSkin = new EditableSkin();
                // On applique le skin au démarrage pour que le perso devienne Blanc tout de suite
                updateGlobalSkin();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 2. Construction de l'interface (Boutons)
        rebuildUI();
    }

    // Cette méthode gère quels boutons afficher selon le menu actuel
    private void rebuildUI() {
        // On supprime les anciens boutons pour mettre les nouveaux
        this.clearChildren();

        // --- BOUTON FERMER (Toujours présent) ---
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Fermer"), button -> this.close())
                .dimensions(this.width - 120, this.height - 40, 100, 20).build());

        // --- CONFIGURATION ZONE DROITE ---
        int btnW = rightZoneWidth - 20; // Largeur bouton
        int startX = this.width - rightZoneWidth + 10; // Position X
        int startY = 40; // Position Y de départ
        int gap = 25; // Espace entre les boutons

        // --- AFFICHAGE SELON LE MENU ---
        if (currentMenu == MenuState.MAIN) {
            // === MENU PRINCIPAL ===

            this.addDrawableChild(ButtonWidget.builder(Text.literal("> Vêtements"), button -> {
                this.currentMenu = MenuState.CLOTHES;
                rebuildUI(); // On recharge l'interface
            }).dimensions(startX, startY, btnW, 20).build());

            this.addDrawableChild(ButtonWidget.builder(Text.literal("> Corps & Peau"), button -> {
                this.currentMenu = MenuState.BODY;
                rebuildUI();
            }).dimensions(startX, startY + gap, btnW, 20).build());

            // Bouton Reset
            this.addDrawableChild(ButtonWidget.builder(Text.literal("Réinitialiser Tout"), button -> {
                if(currentSkin != null) {
                    currentSkin.clear();
                    updateGlobalSkin();
                }
            }).dimensions(startX, startY + gap * 3, btnW, 20).build());

        } else if (currentMenu == MenuState.CLOTHES) {
            // === MENU VÊTEMENTS ===

            this.addDrawableChild(ButtonWidget.builder(Text.literal("T-Shirt Rouge"), button -> {
                applyTemplate(20, 20, 8, 12, 0xFF0000FF);
            }).dimensions(startX, startY, btnW, 20).build());

            this.addDrawableChild(ButtonWidget.builder(Text.literal("Pantalon Bleu"), button -> {
                applyTemplate(4, 20, 4, 12, 0xFFFF0000);
            }).dimensions(startX, startY + gap, btnW, 20).build());

            // Bouton RETOUR
            this.addDrawableChild(ButtonWidget.builder(Text.literal("< Retour"), button -> {
                this.currentMenu = MenuState.MAIN;
                rebuildUI();
            }).dimensions(startX, startY + gap * 3, btnW, 20).build());

        } else if (currentMenu == MenuState.BODY) {
            // === MENU CORPS ===

            this.addDrawableChild(ButtonWidget.builder(Text.literal("Peau Alien (Vert)"), button -> {
                applyTemplate(0, 0, 64, 64, 0xFF00FF00);
            }).dimensions(startX, startY, btnW, 20).build());

            this.addDrawableChild(ButtonWidget.builder(Text.literal("Peau Fantôme (Blanc)"), button -> {
                applyTemplate(0, 0, 64, 64, 0xFFFFFFFF);
            }).dimensions(startX, startY + gap, btnW, 20).build());

            // Bouton RETOUR
            this.addDrawableChild(ButtonWidget.builder(Text.literal("< Retour"), button -> {
                this.currentMenu = MenuState.MAIN;
                rebuildUI();
            }).dimensions(startX, startY + gap * 3, btnW, 20).build());
        }
    }

    // Méthode utilitaire pour appliquer une couleur et mettre à jour le jeu
    private void applyTemplate(int x, int y, int w, int h, int color) {
        if (currentSkin != null) {
            currentSkin.fillArea(x, y, w, h, color);
            updateGlobalSkin();
        }
    }

    // Met à jour la variable globale pour que le Mixin la voie
    private void updateGlobalSkin() {
        if (currentSkin != null) {
            SkinEditorModClient.TEMP_SKIN_ID = currentSkin.getTextureId();
        }
    }

    @Override
    public void close() {
        super.close();
        if (this.currentSkin != null) {
            this.currentSkin.close();
            this.currentSkin = null;
        }
    }

    // --- GESTION SOURIS (Rotation Modèle) ---
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX < leftZoneWidth) {
            this.isDraggingModel = true;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) isDraggingModel = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (this.isDraggingModel) {
            this.playerRotationY += (float) deltaX * 2.0f;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    // Pour éviter le flou d'arrière plan par défaut
    @Override
    public boolean shouldPause() { return false; }

    // --- RENDU GRAPHIQUE ---
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // 1. Fond Gris foncé
        this.renderBackground(context, mouseX, mouseY, delta);
        context.fill(0, 0, this.width, this.height, 0xFF202020);

        // 2. Délimitations (Lignes verticales)
        context.fill(leftZoneWidth, 0, leftZoneWidth + 1, this.height, 0xFF404040);
        context.fill(this.width - rightZoneWidth, 0, this.width - rightZoneWidth + 1, this.height, 0xFF404040);

        // 3. Titres
        String rightTitle = (currentMenu == MenuState.MAIN) ? "Catégories" : "Choix";
        context.drawCenteredTextWithShadow(this.textRenderer, "Aperçu", leftZoneWidth / 2, 10, 0xFFAAAAAA);
        context.drawCenteredTextWithShadow(this.textRenderer, "Éditeur", this.width / 2, 10, 0xFFAAAAAA);
        context.drawCenteredTextWithShadow(this.textRenderer, rightTitle, this.width - (rightZoneWidth / 2), 10, 0xFFAAAAAA);

        // 4. DESSIN DU CARRÉ CENTRAL (Texture)
        if (currentSkin != null) {
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            int size = 128; // Taille affichée (Zoom x2)

            // On pousse le rendu vers l'avant (Z-Index) pour qu'il soit bien visible
            context.getMatrices().push();
            context.getMatrices().translate(0, 0, 10);

            // Petit cadre gris autour
            context.fill(centerX - size/2 - 2, centerY - size/2 - 2, centerX + size/2 + 2, centerY + size/2 + 2, 0xFF888888);

            // Affichage de la texture
            context.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            context.drawTexture(currentSkin.getTextureId(), centerX - size/2, centerY - size/2, size, size, 0, 0, 64, 64, 64, 64);

            context.getMatrices().pop();
        }

        // 5. RENDU DU JOUEUR 3D (En dernier)
        renderPlayerDoll(context);

        // Affiche les boutons
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
        context.getMatrices().translate(0, 0, 50); // On pousse le modèle très en avant
        InventoryScreen.drawEntity(context, x, y, 70, new Vector3f(0,0,0), quaternion, new Quaternionf(), this.client.player);
        context.getMatrices().pop();
    }
}