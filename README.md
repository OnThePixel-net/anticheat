# OnThePixel Anti Cheat
This is an open source anti cheat that can be used by any Minestom server as a library.

## Checks

| Check | Category | Detects |
|---|---|---|
| [Flight](#flight) | Movement | Moving horizontally in mid-air without changing height |
| [BasicSpeed](#basicspeed) | Movement | Sustained horizontal speed above what the player's attributes allow |
| [Teleport](#teleport) | Movement | A single move that jumps further than a legitimate step |
| [TeleportSpam](#teleportspam) | Movement | Repeated large position jumps within a second |
| [UnaidedLevitation](#unaidedlevitation) | Movement | Rising more than three blocks without a legitimate source |
| [Phase](#phase) | Movement | Moving into a full solid block |
| [Reach](#reach) | Combat | Attacking a target further than five blocks away |
| [CPS](#cps) | Combat | Clicking at an inhumanly high rate |
| [HitConsistency](#hitconsistency) | Combat | Click timing that is too regular to be human |
| [Killaura](#killaura) | Combat (manual) | Attacking an invisible dummy entity the player cannot legitimately see |
| [FastBreak](#fastbreak) | Other | Finishing a block break faster than a tick |
| [IntOverflowCrash](#intoverflowcrash) | Exploit | Positions large enough to overflow an int and crash the server |

`Phase` and `Killaura` are not in the default flagging set — `Phase` ships as an "innocent check"
and `Killaura` only runs when triggered via `performManualCheck`.

## Requirements
Built against Minestom `2026.07.01-26.1.2` (Minecraft 26.1.2), which requires **Java 25**.

## Usage

### Dependency

Releases are published to **GitHub Packages**.

build.gradle.kts
```kotlin
repositories {
    mavenCentral()
    maven("https://maven.pkg.github.com/OnThePixel-net/anticheat") {
        credentials {
            username = providers.gradleProperty("gpr.user").orNull ?: System.getenv("GITHUB_ACTOR")
            password = providers.gradleProperty("gpr.key").orNull ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation("net.onthepixel:onthepixel-anti-cheat:1.0.0")
}
```

build.gradle
```groovy
repositories {
    mavenCentral()
    maven {
        url = 'https://maven.pkg.github.com/OnThePixel-net/anticheat'
        credentials {
            username = project.findProperty('gpr.user') ?: System.getenv('GITHUB_ACTOR')
            password = project.findProperty('gpr.key') ?: System.getenv('GITHUB_TOKEN')
        }
    }
}

dependencies {
    implementation 'net.onthepixel:onthepixel-anti-cheat:1.0.0'
}
```

pom.xml — with the matching `<server>` entry in your `~/.m2/settings.xml`
```xml
<repositories>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/OnThePixel-net/anticheat</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>net.onthepixel</groupId>
        <artifactId>onthepixel-anti-cheat</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>
```

The only transitive dependency is Minestom itself, which comes from Maven Central.

### Releasing

Publishing runs in CI. Create a GitHub release tagged `v1.0.0` and the
[publish workflow](.github/workflows/publish.yml) pushes
`net.onthepixel:onthepixel-anti-cheat:1.0.0` to GitHub Packages. The workflow can also be
started manually from the Actions tab with an explicit version.

To try a build locally without publishing:

```bash
./gradlew publishToMavenLocal
```

### Example
```java
package net.onthepixel.anticheat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.onthepixel.anticheat.events.PlayerFlagEvent;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.instance.Instance;

import java.util.List;

/**
 * Minimum example of how to use PixelAC.
 */
public class Test {

    public static void main(String[] args) {
        MinecraftServer server = MinecraftServer.init();

        // Spawn players into an empty instance
        Instance instance = MinecraftServer.getInstanceManager().createInstanceContainer();
        instance.enableAutoChunkLoad(true);

        MinecraftServer.getGlobalEventHandler().addListener(AsyncPlayerConfigurationEvent.class, e -> e.setSpawningInstance(instance));

        // This is triggered when the player is flagged for anything
        MinecraftServer.getGlobalEventHandler().addListener(PlayerFlagEvent.class, e ->
                e.player().sendMessage(Component
                        .text("You have been flagged for " + e.checkName() + " with a certainty of " + e.certainty())
                        .color(NamedTextColor.RED)));

        // Enable the anti cheat
        PixelAC.Config config = PixelAC.Config.create()
                .withPassive(false)
                .withDisabledChecks(List.of())
                .withDebugChecks(List.of());
        PixelAC ac = new PixelAC(config);
        ac.start();  // Start the anti cheat

        server.start("0.0.0.0", 25565);
    }
}
```
You can also use the `Test` class as a reference for how to use the library.

## Check Descriptions

### Flight
Detects flight by checking if player moves more than a certain amount of blocks without 
going up or down.

### BasicSpeed
Detects speed by checking if the player is moving too quickly, using distance over time.

### Teleport
Detects teleportation by checking if the player moves more than a certain amount of blocks
in one packet.

### TeleportSpam
Detects TP-Aura by checking if the player moves more than a certain amount of blocks at a 
time without a certain time frame a certain number of times.

### UnaidedLevitation
Detects flight by checking if the player moves too many blocks up vertically without
touching the ground or being otherwise aided by things such as a ladder, vines, or a liquid.

### Phase
Detects noclip by checking whether the player moved down into a full solid block, and pushes
them back out along the direction they came from. Players falling through the ground while a
world is still loading look identical to this, so it ships as an "innocent check" by default:
it corrects the position without producing a flag.

### IntOverflowCrash
Stops a crash exploit in Minestom by checking if the player moves past the integer limit
on any axis. If this happens they will be stopped and flagged with 100% certainty.

### CPS
Detects auto clickers and killaura by checking if the player's CPS is above a certain
value.

### HitConsistency
Detects auto clickers and killaura by checking how consistently timed the player's hits are.

### Reach
Detects reach by checking how far away players that the player hits are.

### Killaura
A manual check: it spawns a dummy player entity next to the target that a legitimate client
never renders as attackable, then counts how often the player hits it within a second. Run it
with `ac.performManualCheck(KillauraManualCheck.class, player)`, which returns a
`CompletableFuture<Float>` with the resulting certainty.

### FastBreak
Detects fast break by measuring the time between the start and the end of a dig. Finishing a
block in less than one tick is not achievable without instamine.

---

## Fork

This is a fork of [Mangolise/anticheat](https://github.com/Mangolise/anticheat), maintained by
[OnThePixel.net](https://onthepixel.net). The upstream project is licensed under the MIT
License, Copyright (c) 2024 Mangolise — see [LICENSE](LICENSE). Everything published from this
repository uses the `net.onthepixel` coordinates so it can be consumed alongside, or instead
of, the upstream artifact.
