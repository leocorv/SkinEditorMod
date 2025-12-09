package org.nerix.skineditor.common;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// 1. On crée un "Record" qui contient nos données (la catégorie racine)
public record SyncOutfitsPayload(OutfitCategory root) implements CustomPayload {

    // 2. L'identifiant unique du paquet
    public static final CustomPayload.Id<SyncOutfitsPayload> ID = new CustomPayload.Id<>(Identifier.of("skineditor", "sync_outfits"));

    // 3. Le Codec (Comment lire/écrire ce paquet)
    // On utilise les méthodes statiques qu'on a déjà écrites dans NetworkHandler
    public static final PacketCodec<RegistryByteBuf, SyncOutfitsPayload> CODEC = CustomPayload.codecOf(SyncOutfitsPayload::write, SyncOutfitsPayload::new);

    // Constructeur de lecture (reçoit le buffer)
    private SyncOutfitsPayload(RegistryByteBuf buf) {
        this(NetworkHandler.readCategory(buf));
    }

    // Méthode d'écriture (écrit dans le buffer)
    private void write(RegistryByteBuf buf) {
        NetworkHandler.writeCategory(buf, this.root);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}