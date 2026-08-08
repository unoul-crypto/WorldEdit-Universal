<h1>
    <img src="worldedit-logo.svg" alt="WorldEdit" width="400" /> 
</h1>

**A Minecraft Map Editor... that runs in-game!**

* With selections, schematics, copy and paste, brushes, and scripting!
* Use it in creative, survival in single player or on your server.
* Use it on your Minecraft server to fix griefing and mistakes.

Java Edition required. WorldEdit is compatible with NeoForge, Fabric, Bukkit, Spigot, Paper, and Sponge.

## WorldEdit Universal

WorldEdit Universal is a Bukkit-focused fork that keeps the modern WorldEdit
codebase usable on a wider range of server versions. It targets Java 21 and the
stable Bukkit 1.16 API while retaining native adapters for server versions where
a matching adapter is available.

The generic compatibility path is intended for Spigot-derived servers and
hybrid Bukkit/Forge implementations such as Mohist and Youer. It does not depend
on Paper API.

### Changes in this fork

* Added a central dynamic block registry for namespaced mod IDs. Unknown IDs are
  resolved through Bukkit `BlockData` and cached as WorldEdit `BlockType` and
  Bukkit `Material` mappings. This is shared by command parsing, block editing,
  clipboard operations, undo/redo, and third-party integrations instead of being
  implemented separately for every command.
* Added generic Bukkit block-state property discovery. Modded boolean, integer,
  directional, and enum properties can be preserved without a version-specific
  NMS adapter.
* Added API-only block editing for unsupported server versions. Commands such as
  `//set`, `//replace`, `//copy`, `//paste`, `//undo`, and `//redo` continue to
  work when no native adapter is available.
* Added generic inventory serialization for copied containers. Bukkit inventory
  contents are stored in clipboard NBT and restored during paste operations.
* Fixed modded block conversion used by CoreProtect-compatible WorldEdit logging.
  A dynamically discovered mod block no longer becomes a `null` Bukkit material
  during undo or redo.
* Made block property initialization tolerate third-party registries returning
  no property map, preventing platform startup failures on hybrid servers.
* Restored compatibility with the Bukkit 1.16 tree-generation API and removed
  mandatory modern Paper API references from the universal path.
* The full shaded plugin is now the normal build artifact. The smaller `-dev.jar`
  remains available only for development and does not contain WorldEdit core.

### Compatibility and limitations

* Runtime: Java 21.
* Bukkit API baseline: Spigot 1.16.5 (`api-version: 1.16`).
* Intended server range: Bukkit, Spigot, Paper, Mohist, Youer, and compatible
  derivatives from Minecraft 1.16.5 onward.
* Hybrid support requires the server to expose a modded block through Bukkit
  `BlockData`/`Material`. This is the normal behavior on supported Mohist-style
  implementations.
* When no native adapter is available, complete arbitrary block-entity NBT,
  entity internals, native structure generation, and region regeneration may be
  unavailable. Container inventories have a dedicated generic fallback.

### Installation

1. Stop the server completely.
2. Remove older or duplicate WorldEdit JARs from the `plugins` directory.
3. Copy `worldedit-bukkit-7.4.5-universal-SNAPSHOT.jar` into `plugins`.
4. Start the server on Java 21.

### Building

The build currently uses JDK 25 tooling and emits Java 21-compatible bytecode.
Build the distributable Bukkit plugin with:

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk-25'
.\gradlew.bat :worldedit-bukkit:shadowJar
```

On Linux or macOS:

```bash
export JAVA_HOME=/path/to/jdk-25
./gradlew :worldedit-bukkit:shadowJar
```

The deployable plugin is written to `worldedit-bukkit/build/libs` without a
classifier. Do not install the `-dev.jar` on a server.

## Download WorldEdit

This place contains the Java code for WorldEdit, but if you want to just use WorldEdit, get the mod or plugin from Modrinth:

https://modrinth.com/plugin/worldedit/versions

Edit the Code
---------

Want to add new features to WorldEdit or fix bugs yourself? You can get the game running, with WorldEdit, from the code here, without any additional outside steps, by doing the following *four* things:

1. Download WorldEdit's source code and put it somewhere. We recommend you use something called Git if you already know how to use it, but [you can also just download a .zip file](https://github.com/EngineHub/WorldEdit/archive/master.zip). (If you plan on contributing the changes, you will need to figure out Git.)
2. Install any version of Java greater than or equal to 21.
   * Note that if you do _not_ install JDK 21 exactly, Gradle will download it for you on first run. However, it is still required to have some form of Java installed for Gradle to start at all.
3. Open terminal / command prompt / bash and navigate to the directory where you put the source code.
4. Run **one** of these following commands:
   * Mac OS X / Linux: `./gradlew :worldedit-fabric:runClient`
   * Windows - Command Prompt: `gradlew :worldedit-fabric:runClient`
   * Windows - PowerShell: `.\gradlew :worldedit-fabric:runClient`

🎉 That's it. 🎉 It takes a long time to actually transform WorldEdit into a mod. If it succeeds, **the Minecraft game will open and you can create a single player world with WorldEdit**.

When you make changes to the code, you have to restart the game by re-running the command for your changes to take effect. If there are errors in your Java syntax, the command will fail.

For additional information about compiling WorldEdit, see [COMPILING.md](COMPILING.md).

### Using a Java IDE

To edit WorldEdit in a Java IDE, follow these steps:

1. Download and install [IntelliJ IDEA Community Edition](https://www.jetbrains.com/idea/download/).
2. In the IDE, open the folder that you saved WorldEdit's code in. This creates a new project in IDEA.

That's pretty much it.

If you want to be able to run the game also, follow these instructions:

1. Go to Run -> Edit Configurations.
2. Add a Gradle task:
   1. Choose `worldedit-fabric` for the project.
   2. For the tasks, type in `runClient`
3. Click OK
4. Under the Run menu again, go to "Debug [your new task]".

### Speeding up the Edit-Test-Edit-Test Cycle

It's a little annoying have to restart the game to test your changes. The best way to reduce the time is to run the server instead (using `runServer` instead of `runClient`) and then reconnect to the server after restarting it.

Submitting Your Changes
------------

WorldEdit is open source (specifically licensed under GPL v3), so note that your contributions will also be open source. The best way to submit a change is to create a fork on GitHub, put your changes there, and then create a "pull request" on our WorldEdit repository.

Please read [CONTRIBUTING.md](CONTRIBUTING.md) for important guidelines to follow.

Links
-----

* [Visit our website](https://enginehub.org/)
* [Discord](https://discord.gg/enginehub)
* [Issue tracker](https://github.com/EngineHub/WorldEdit/issues)
* [Continuous integration](https://builds.enginehub.org) [![Build Status](https://ci.enginehub.org/app/rest/builds/buildType:bt10,branch:master/statusIcon.svg)](https://ci.enginehub.org/viewType.html?buildTypeId=bt10&guest=1)
* [End-user documentation](https://worldedit.enginehub.org/en/latest/)

Supporters
----------

[![YourKit Logo](https://www.yourkit.com/images/yklogo.png)](https://www.yourkit.com/)

YourKit supports open source projects with innovative and intelligent tools for monitoring and profiling Java and .NET applications.
YourKit is the creator of [YourKit Java Profiler](https://www.yourkit.com/java/profiler/),
[YourKit .NET Profiler](https://www.yourkit.com/.net/profiler/),
and [YourKit YouMonitor](https://www.yourkit.com/youmonitor/).
