package org.nerix.skineditor.common;

import java.util.ArrayList;
import java.util.List;

public class OutfitCategory {
    public String id;           // Nom du dossier (ex: "hats")
    public String displayName;  // Nom affiché (ex: "Chapeaux")
    public List<OutfitItem> items = new ArrayList<>();
    public List<OutfitCategory> subCategories = new ArrayList<>();

    public OutfitCategory(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }
}