# Spark and Sift

Expand early-game survival with bowl sifting, primitive firemaking, archaeology, campfire progression, and configurable rewards.

## Requirements

- Minecraft 26.2, Fabric Loader 0.19.3 or newer, and Fabric API 0.155.2+26.2 or newer.
- Java 25 or newer (Java 25 is recommended for building this release).
- Install on the client and server.

Mod Menu is optional and provides access to the configuration screen. Punchy and Smooth Player Animations are optional animation compatibility integrations. Cloth Config is not required. Reopen the world after changing settings.

## Build

From the project folder, use the included Gradle 9.8.0 wrapper:

```powershell
.\gradlew.bat clean build
```

On Linux or macOS:

```sh
./gradlew clean build
```

The first build needs internet access to download Gradle and dependencies. The playable mod is `build/libs/Spark-and-Sift-1.0.0.jar`; the `-sources.jar` contains source code. Version settings are in `gradle.properties`.

The internal mod ID and packages retain `siftinggravel` for compatibility.

## License

Copyright (c) 2026 faefluffkrist. Licensed under the [MIT License](LICENSE).

Thank you for using Spark and Sift.
