package org.nerix.skineditor.client.mixin;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;
import org.nerix.skineditor.client.SkinEditorModClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// On dit au jeu qu'on veut modifier la classe qui gère les joueurs clients
@Mixin(AbstractClientPlayerEntity.class)
public class PlayerSkinMixin {

    // On injecte notre code au début (HEAD) de la méthode "getSkinTextures"
    @Inject(method = "getSkinTextures", at = @At("HEAD"), cancellable = true)
    public void getCustomSkin(CallbackInfoReturnable<SkinTextures> cir) {

        // On vérifie si la variable globale dans notre mod contient un ID de texture
        if (SkinEditorModClient.TEMP_SKIN_ID != null) {

            Identifier customId = SkinEditorModClient.TEMP_SKIN_ID;

            // On crée un nouvel objet SkinTextures avec NOTRE texture
            // Les paramètres sont : Texture, TextureUrl, Cape, Elytra, Modèle, Secure
            SkinTextures customSkin = new SkinTextures(
                    customId,
                    null,
                    null,
                    null,
                    SkinEditorModClient.TEMP_SKIN_MODEL,
                    true
            );

            // On renvoie notre skin et on arrête la logique normale de Minecraft
            cir.setReturnValue(customSkin);
        }
    }
}