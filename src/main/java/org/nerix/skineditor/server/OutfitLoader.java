package org.nerix.skineditor.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import org.nerix.skineditor.common.OutfitCategory;
import org.nerix.skineditor.common.OutfitItem;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class OutfitLoader {

    public static OutfitCategory ROOT_CATEGORY;
    private static Map<String, String> RENAMES = new HashMap<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void load() {
        // 1. On récupère le chemin OFFICIEL des configs via Fabric
        // Cela pointe vers ".minecraft/config" ou le dossier config du serveur
        Path configRoot = FabricLoader.getInstance().getConfigDir();

        // 2. On définit le dossier de NOTRE mod : "config/skineditor"
        Path modConfigDir = configRoot.resolve("skineditor");

        // 3. On définit le dossier où les gens mettront les images
        // On le met DANS le dossier de config pour que ce soit rangé
        // Chemin final : .minecraft/config/skineditor/outfits/
        Path outfitsDir = modConfigDir.resolve("outfits");

        try {
            // Création automatique et propre des dossiers s'ils n'existent pas
            if (!Files.exists(modConfigDir)) Files.createDirectories(modConfigDir);
            if (!Files.exists(outfitsDir)) {
                Files.createDirectories(outfitsDir);
                // On crée un dossier exemple pour l'admin
                Files.createDirectories(outfitsDir.resolve("Exemple_Chapeaux"));
                System.out.println(">>> Serveur : Dossiers créés dans " + modConfigDir.toAbsolutePath());
            }

            // 4. Gestion du JSON de config
            File configFile = modConfigDir.resolve("outfit_config.json").toFile();
            if (!configFile.exists()) {
                createDefaultConfig(configFile);
            } else {
                loadConfig(configFile);
            }

            // 5. Scan des fichiers
            ROOT_CATEGORY = new OutfitCategory("root", "Racine");
            scanDir(outfitsDir.toFile(), ROOT_CATEGORY, "");

            System.out.println(">>> Serveur : Chargement terminé depuis " + outfitsDir);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void createDefaultConfig(File file) {
        Map<String, Object> defaultData = new HashMap<>();
        Map<String, String> exampleRenames = new HashMap<>();
        exampleRenames.put("Exemple_Chapeaux", "Chapeaux Rares");
        defaultData.put("renames", exampleRenames);

        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(defaultData, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void loadConfig(File file) {
        try (FileReader reader = new FileReader(file)) {
            Type type = new TypeToken<Map<String, Map<String, String>>>(){}.getType();
            Map<String, Map<String, String>> json = GSON.fromJson(reader, type);
            if (json != null && json.containsKey("renames")) {
                RENAMES = json.get("renames");
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private static void scanDir(File dir, OutfitCategory parent, String relativePath) {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File f : files) {
            String pathKey = relativePath + f.getName();
            String displayName = RENAMES.getOrDefault(pathKey, f.getName().replace(".png", "").replace("_", " "));

            if (f.isDirectory()) {
                OutfitCategory sub = new OutfitCategory(f.getName(), displayName);
                parent.subCategories.add(sub);
                scanDir(f, sub, pathKey + "/");
            } else if (f.getName().toLowerCase().endsWith(".png")) {
                parent.items.add(new OutfitItem(f.getName(), displayName, pathKey));
            }
        }
    }
}