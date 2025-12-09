package org.nerix.skineditor.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import org.nerix.skineditor.common.OutfitCategory;
import org.nerix.skineditor.common.SyncOutfitsPayload;

public class ClientNetworkHandler {

    public static OutfitCategory CLIENT_ROOT_CATEGORY = null;

    public static void registerClient() {
        // CORRECTION : On enregistre un receveur pour le Payload spécifique
        // La lambda prend maintenant 2 arguments : (payload, context)
        ClientPlayNetworking.registerGlobalReceiver(SyncOutfitsPayload.ID, (payload, context) -> {

            // On récupère les données directement depuis le payload
            OutfitCategory root = payload.root();

            // On exécute sur le thread principal du client
            context.client().execute(() -> {
                CLIENT_ROOT_CATEGORY = root;
                System.out.println(">>> Client : Outfits reçus du serveur ! (" + root.subCategories.size() + " dossiers)");
            });
        });
    }
}