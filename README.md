# SkinEditorMod

> Experimental Fabric mod for editing and composing Minecraft player skins from configurable layers and outfits.

## Project status

**Early prototype, incomplete and not functionally ready.**

The mod currently **starts and loads in Minecraft**, but the project is **far from finished** and should not be considered usable as a complete skin editor yet.

The repository is public mainly to document the experiment and the architecture explored around runtime skin editing, client/server synchronization and configurable outfit layers.

Expect unfinished behavior, missing features and code that may change significantly.

## Current state

What currently exists in the prototype:

- Fabric mod initialization;
- a `/skineditor` client command;
- an experimental skin editor screen;
- runtime skin representation through `EditableSkin`;
- basic layer/outfit concepts;
- server-side scanning of configurable outfit folders;
- JSON configuration for display-name overrides;
- server-to-client synchronization of outfit metadata;
- client-side skin replacement experiments using mixins.

What is **not** finished:

- a polished editing workflow;
- reliable persistence of edited skins;
- complete outfit/layer management;
- robust multiplayer synchronization;
- production-ready UI/UX;
- validation and error handling;
- compatibility guarantees;
- automated tests;
- packaging for end users.

## Target environment

The current project targets:

- Minecraft `1.21.1`
- Java 21
- Fabric Loader `0.17.1`
- Fabric API `0.116.7+1.21.1`

These versions reflect the development snapshot and may need updating before further work.

## Prototype architecture

```text
Minecraft Server
      |
      +--> config/skineditor/
      |      |
      |      +--> outfit_config.json
      |      +--> outfits/
      |
      +--> OutfitLoader
      |
      +--> outfit metadata
      |
      +--> Fabric S2C payload
                    |
                    v
              Minecraft Client
                    |
                    +--> SkinEditorScreen
                    +--> EditableSkin
                    +--> runtime skin/layer experiments
```

## Runtime configuration

The mod creates its local configuration under Fabric's config directory:

```text
config/
└── skineditor/
    ├── outfit_config.json
    └── outfits/
```

The `outfits/` directory is intended to contain PNG assets organized in folders/categories.

A default configuration file is generated automatically when missing.

Example concept:

```json
{
  "renames": {
    "Exemple_Chapeaux": "Chapeaux Rares"
  }
}
```

## Client command

The prototype registers:

```text
/skineditor
```

This opens the experimental editor screen on the client.

The existence of the command does **not** mean the editor is currently complete or reliable.

## Main areas of the codebase

```text
src/
├── main/java/org/nerix/skineditor/
│   ├── SkinEditorMod.java
│   ├── common/
│   │   ├── NetworkHandler.java
│   │   ├── OutfitCategory.java
│   │   ├── OutfitItem.java
│   │   └── SyncOutfitsPayload.java
│   └── server/
│       └── OutfitLoader.java
│
└── client/java/org/nerix/skineditor/client/
    ├── SkinEditorModClient.java
    ├── SkinEditorScreen.java
    ├── EditableSkin.java
    ├── SkinLayer.java
    ├── SkinResourceManager.java
    └── mixin/
```

## Development goals

The original direction of the project was to explore a system where a server could expose a catalog of cosmetic layers/outfits while clients compose or preview them dynamically.

Potential future work would include:

- proper layer compositing;
- reliable skin preview and application;
- better category navigation;
- persistence of user selections;
- cleaner server/client synchronization;
- permissions and multiplayer rules;
- asset validation;
- a finished UI;
- tests and compatibility checks.

## Build

Using the included Gradle wrapper:

```bash
./gradlew build
```

For local Fabric development, the usual Loom run tasks can be used.

Because this is an unfinished prototype, a successful build or game startup should not be interpreted as the mod being feature-complete.

## Why this repository is public

SkinEditorMod is a personal Minecraft/Fabric experiment around **dynamic skins, configurable cosmetic layers and client/server mod architecture**.

It is published as unfinished source code for documentation and portfolio purposes, not as a ready-to-install mod.

## License

All Rights Reserved.

## Author

**Léo Corvaisier-Palluy (Nerix)**  
GitHub: [leocorv](https://github.com/leocorv)
