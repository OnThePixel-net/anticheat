package net.onthepixel.anticheat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.onthepixel.anticheat.checks.combat.KillauraManualCheck;
import net.onthepixel.anticheat.checks.movement.UnaidedLevitationCheck;
import net.onthepixel.anticheat.events.PlayerFlagEvent;
import net.mangolise.combat.CombatConfig;
import net.mangolise.combat.MangoCombat;
import net.mangolise.gamesdk.features.AdminCommandsFeature;
import net.mangolise.gamesdk.permissions.Permissions;
import net.mangolise.gamesdk.util.GameSdkUtils;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerChatEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.instance.ChunkLoader;
import net.minestom.server.instance.Instance;
import net.minestom.server.timer.TaskSchedule;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Test server with a world that we can use to test the AC.
 */
public class Test {
    // Checks that debug messages should be sent for
    private static final List<String> DEBUG_CHECKS = List.of("Phase");

    public static void main(String[] args) {
        System.out.println("Starting test server...");

        // It's hard to test when minestom kicks cheaters
        System.setProperty("minestom.packet-queue-size", "10000");
        System.setProperty("minestom.packet-per-tick", "10000");

        MinecraftServer server = MinecraftServer.init();

        ChunkLoader chunkLoader = GameSdkUtils.getPolarLoaderFromResource("test-world.polar");
        Instance instance = MinecraftServer.getInstanceManager().createInstanceContainer(chunkLoader);
        instance.enableAutoChunkLoad(true);

        MangoCombat.enableGlobal(CombatConfig.create().withAutomaticRespawn(true));

        MinecraftServer.getGlobalEventHandler().addListener(AsyncPlayerConfigurationEvent.class, e -> {
            Permissions.setPermission(e.getPlayer(), "*", true);
            e.setSpawningInstance(instance);
        });
        MinecraftServer.getGlobalEventHandler().addListener(PlayerSpawnEvent.class, e -> {
            final Pos spawnPoint = GameSdkUtils.getSpawnPosition(instance).sub(0, 1, 0);
            e.getPlayer().setRespawnPoint(spawnPoint);
            e.getPlayer().teleport(spawnPoint);
        });

        PixelAC ac = new PixelAC(PixelAC.Config.create()
                .withDebugChecks(DEBUG_CHECKS));

        MinecraftServer.getGlobalEventHandler().addListener(PlayerChatEvent.class, e -> {
            if (e.getRawMessage().equals("t")) {
                e.getPlayer().teleport(e.getPlayer().getPosition().add(10, 10, 0));
            }

            if (e.getRawMessage().equals("ping")) {
                e.getPlayer().sendMessage("Latency: " + e.getPlayer().getLatency());
            }

            if (e.getRawMessage().equals("dislev")) {
                ac.tempDisableCheck(e.getPlayer(), UnaidedLevitationCheck.class, 100);
            }

            if (e.getRawMessage().equals("killaura")) {
                MinecraftServer.getSchedulerManager().submitTask(() -> {
                    ac.performManualCheck(KillauraManualCheck.class, e.getPlayer());
                    return TaskSchedule.millis(ThreadLocalRandom.current().nextInt(0, 8000));
                });
            }
        });

//        MinecraftServer.getGlobalEventHandler().addListener(EntityAttackEvent.class, e -> {
//            if (!(e.getTarget() instanceof LivingEntity target)) {
//                return;
//            }
//
//            target.damage(Damage.fromEntity(e.getEntity(), 0f));
//        });

        MinecraftServer.getGlobalEventHandler().addListener(PlayerFlagEvent.class, e ->
                e.player().sendMessage(Component
                    .text("You have been flagged for " + e.checkName() + " with a certainty of " + e.certainty())
                    .color(NamedTextColor.RED)));

        ac.start();
        new AdminCommandsFeature().setup(null);

        server.start("0.0.0.0", GameSdkUtils.getConfiguredPort());
    }
}
