package org.nerix.skineditor.common;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import org.nerix.skineditor.server.OutfitLoader;

public class NetworkHandler {

    // --- 1. ENREGISTREMENT DU TYPE DE PAQUET ---
    // Cette méthode doit être appelée dans le onInitialize() commun (SkinEditorMod.java)
    public static void registerPayloads() {
        // On dit au jeu : "Ce paquet va du Serveur (S) vers le Client (C)"
        PayloadTypeRegistry.playS2C().register(SyncOutfitsPayload.ID, SyncOutfitsPayload.CODEC);
    }

    // --- 2. LOGIQUE SERVEUR ---
    public static void registerServer() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (OutfitLoader.ROOT_CATEGORY != null) {
                // CORRECTION : On envoie l'objet Payload directement !
                // Plus besoin de PacketByteBuf manuel ici.
                SyncOutfitsPayload payload = new SyncOutfitsPayload(OutfitLoader.ROOT_CATEGORY);
                ServerPlayNetworking.send(handler.player, payload);
            }
        });
    }

    // --- 3. MÉTHODES DE LECTURE/ÉCRITURE (Réutilisées par le Payload) ---

    // Note : On change PacketByteBuf en PacketByteBuf (compatible RegistryByteBuf)
    public static void writeCategory(PacketByteBuf buf, OutfitCategory cat) {
        buf.writeString(cat.id);
        buf.writeString(cat.displayName);

        buf.writeInt(cat.items.size());
        for (OutfitItem item : cat.items) {
            buf.writeString(item.id);
            buf.writeString(item.displayName);
            buf.writeString(item.path);
        }

        buf.writeInt(cat.subCategories.size());
        for (OutfitCategory sub : cat.subCategories) {
            writeCategory(buf, sub);
        }
    }

    public static OutfitCategory readCategory(PacketByteBuf buf) {
        String id = buf.readString();
        String name = buf.readString();
        OutfitCategory cat = new OutfitCategory(id, name);

        int itemsCount = buf.readInt();
        for (int i = 0; i < itemsCount; i++) {
            cat.items.add(new OutfitItem(buf.readString(), buf.readString(), buf.readString()));
        }

        int subCount = buf.readInt();
        for (int i = 0; i < subCount; i++) {
            cat.subCategories.add(readCategory(buf));
        }
        return cat;
    }
}