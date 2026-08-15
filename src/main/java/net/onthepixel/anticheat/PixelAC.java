package net.onthepixel.anticheat;

import net.onthepixel.anticheat.checks.combat.CpsCheck;
import net.onthepixel.anticheat.checks.combat.HitConsistencyCheck;
import net.onthepixel.anticheat.checks.combat.KillauraManualCheck;
import net.onthepixel.anticheat.checks.combat.ReachCheck;
import net.onthepixel.anticheat.checks.exploits.IntOverflowCrashCheck;
import net.onthepixel.anticheat.checks.movement.*;
import net.onthepixel.anticheat.checks.other.FastBreakCheck;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerPacketOutEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.packet.server.play.BlockChangePacket;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class PixelAC {
    private final Config config;
    private final Map<UUID, List<FakeBlock>> fakeBlocks = new ConcurrentHashMap<>();
    private final List<ACCheck> checks = List.of(
        new IntOverflowCrashCheck(),
        new FlightCheck(),
        new UnaidedLevitationCheck(),
        new TeleportCheck(),
        new BasicSpeedCheck(),
        new ReachCheck(),
        new CpsCheck(),
        new HitConsistencyCheck(),
        new TeleportSpamCheck(),
        new FastBreakCheck(),
        new KillauraManualCheck(),
        new PhaseCheck()
    );

    public PixelAC(Config config) {
        this.config = config;
    }

    public Block getBlockAt(Player player, Point pos) {
        List<FakeBlock> playerFakeBlocks = fakeBlocks.get(player.getUuid());
        if (playerFakeBlocks != null) {
            for (FakeBlock fakeBlock : playerFakeBlocks) {
                if (pos.sameBlock(fakeBlock.position())) {
                    return fakeBlock.block();
                }
            }
        }
        return player.getInstance().getBlock(pos);
    }

    public void start() {
        checks.forEach(acCheck -> {
            if (config.disabledChecks().contains(acCheck.getClass())) return;
            acCheck.enable(this, config);
        });

        MinecraftServer.getGlobalEventHandler().addListener(PlayerSpawnEvent.class, e ->
                fakeBlocks.remove(e.getPlayer().getUuid()));

        // This is WIP, just disable flight and levi if you have fake blocks
        MinecraftServer.getGlobalEventHandler().addListener(PlayerPacketOutEvent.class, e -> {
            if (e.getPacket() instanceof BlockChangePacket packet) {
                fakeBlocks.computeIfAbsent(e.getPlayer().getUuid(), uuid -> new CopyOnWriteArrayList<>())
                        .add(new FakeBlock(packet.blockPosition(), Block.STONE));
            }
        });
    }

    public void tempDisableCheck(Player player, Class<? extends ACCheck> check, int time) {
        checks.stream().filter(acCheck -> acCheck.getClass().equals(check)).findFirst().ifPresent(acCheck ->
                acCheck.disableFor(player, time));
    }

    public CompletableFuture<Float> performManualCheck(Class<? extends ManualCheck> check, Player target) {
        ManualCheck manualCheck = checks.stream()
                .filter(check::isInstance)
                .map(check::cast)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Check is not registered or is disabled: " + check.getName()));
        return manualCheck.check(target);
    }

    /**
     * A block the server told the client about but which doesn't exist in the instance,
     * so checks can reason about the world the way the player sees it.
     *
     * @param position where the block was sent
     * @param block the block the client believes is there
     */
    private record FakeBlock(Point position, Block block) {
    }

    /**
     * @param passive whether the AC should disable lag backs and just observe players
     * @param disabledChecks checks which will not run or flag
     * @param debugChecks checks which will print debug info to players, this should not be used in production
     * @param innocentChecks checks which won't create flags but will create lag backs regardless of {@code passive}
     */
    public record Config(boolean passive, List<Class<? extends ACCheck>> disabledChecks, List<String> debugChecks, List<Class<? extends ACCheck>> innocentChecks) {
        public Config {
            disabledChecks = List.copyOf(disabledChecks);
            debugChecks = List.copyOf(debugChecks);
            innocentChecks = List.copyOf(innocentChecks);
        }

        public Config() {
            this(false, List.of(), List.of(), List.of(PhaseCheck.class));
        }

        /**
         * Create a new config with default values.
         * @return The new config.
         */
        public static Config create() {
            return new Config();
        }

        /**
         * Whether the AC should disable lag backs and just observe players.
         * @param passive The value.
         * @return The new config.
         */
        public Config withPassive(boolean passive) {
            return new Config(passive, disabledChecks, debugChecks, innocentChecks);
        }

        /**
         * A list of checks which won't create flags but will create lag backs regardless of `passive`.
         * @param innocentChecks The value.
         * @return The new config.
         */
        public Config withInnocentChecks(List<Class<? extends ACCheck>> innocentChecks) {
            return new Config(passive, disabledChecks, debugChecks, innocentChecks);
        }

        /**
         * A list of checks which will not run or flag.
         * @param disabledChecks The value.
         * @return The new config.
         */
        public Config withDisabledChecks(List<Class<? extends ACCheck>> disabledChecks) {
            return new Config(passive, disabledChecks, debugChecks, innocentChecks);
        }

        /**
         * A list of checks which will print debug info to players, this should not be used in production.
         * @param debugChecks The value.
         * @return The new config.
         */
        public Config withDebugChecks(List<String> debugChecks) {
            return new Config(passive, disabledChecks, debugChecks, innocentChecks);
        }
    }
}
