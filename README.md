# BaseFinder Pro (Fabric, Minecraft 26.3)

Brand-new project (package `dev.basefinder`, mod id `basefinder`), nothing reused from the Codex jar.

## Build
1. Install JDK 25. 2. In this folder run `gradle wrapper` once (Gradle 9.3+), then `./gradlew build`.
   Jar: `build/libs/basefinder-pro-2.0.0.jar`.
   No local setup? Push this folder to a GitHub repo - `.github/workflows/build.yml` builds the jar for you (Actions tab -> artifact).
3. Put the jar + Fabric API in `.minecraft/mods`.

## Keys
B = base finder on/off, G = freecam, H = auction sniper.

## Commands
`/bf radius <64-5000>`, `/bf minpercent <n>`, `/bf light`, `/bf column`, `/bf cards`, `/bf loot`,
`/bf ratio <0.05-1>`, `/bf maxspend <$>`, `/bf prices`, `/bf status`

## Files
`config/basefinder/config.json` and `prices.json` (edit prices there).
