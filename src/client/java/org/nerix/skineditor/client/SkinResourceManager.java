package org.nerix.skineditor.client;

import net.minecraft.client.MinecraftClient;

import java.io.File;
import java.util.*;

public class SkinResourceManager {

    // Cette Map stocke : Nom du Dossier (Catégorie) -> Liste des Fichiers images
    public static final Map<String, List<File>> CATEGORIES = new LinkedHashMap<>();

    public static void reload() {
        CATEGORIES.clear();
        System.out.println(">>> SkinEditor: Scan des dossiers de config...");

        // Chemin : .minecraft/config/skineditor
        File rootDir = new File(MinecraftClient.getInstance().runDirectory, "config/skineditor");

        // Si le dossier n'existe pas, on le crée avec des exemples
        if (!rootDir.exists()) {
            rootDir.mkdirs();
            new File(rootDir, "Vêtements").mkdirs();
            new File(rootDir, "Chapeaux").mkdirs();
            System.out.println(">>> SkinEditor: Dossiers créés ! Mettez vos PNG dedans.");
        }

        // 1. On liste tous les dossiers (Ce seront les Catégories)
        File[] directories = rootDir.listFiles(File::isDirectory);

        if (directories != null) {
            for (File categoryDir : directories) {
                // 2. Dans chaque dossier, on cherche les .png
                File[] images = categoryDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));

                if (images != null && images.length > 0) {
                    // On trie par ordre alphabétique
                    Arrays.sort(images);

                    // On ajoute à la mémoire
                    List<File> fileList = new ArrayList<>(Arrays.asList(images));
                    CATEGORIES.put(categoryDir.getName(), fileList);

                    System.out.println("   + Catégorie chargée : " + categoryDir.getName() + " (" + fileList.size() + " items)");
                }
            }
        }
    }
}