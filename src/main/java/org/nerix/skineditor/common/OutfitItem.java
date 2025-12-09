package org.nerix.skineditor.common;

public class OutfitItem {
    public String id;           // ex: "red_cap.png"
    public String displayName;  // ex: "Casquette Rouge"
    public String path;         // ex: "hats/red_cap.png"

    // Constructeur avec 3 arguments (C'est ce qu'il te manquait)
    public OutfitItem(String id, String displayName, String path) {
        this.id = id;
        this.displayName = displayName;
        this.path = path;
    }
}