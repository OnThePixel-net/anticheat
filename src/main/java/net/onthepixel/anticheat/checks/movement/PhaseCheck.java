package net.onthepixel.anticheat.checks.movement;

import net.onthepixel.anticheat.ACCheck;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

/**
 * This will false flag when players first load in and fall through the ground.
 * You should make this an "innocent check" in the config to fix this bug and also
 * stop false flags. This behaviour is implemented by default.
 */
public class PhaseCheck extends ACCheck {
    private static final double THRESHOLD = 0.2;
    private static final double PUSH_BACK_STEP = 0.5;
    private static final int MAX_PUSH_BACK_STEPS = 16;

    public PhaseCheck() {
        super("Phase");
    }

    @Override
    public void register() {
        MinecraftServer.getGlobalEventHandler().addListener(PlayerMoveEvent.class, this::onMove);
    }

    private void onMove(@NotNull PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }

        final Pos to = event.getNewPosition();
        final Pos from = player.getPosition();
        if (to.y() > from.y()) {
            return;
        }

        Block blockAt = player.getInstance().getBlock(to.add(0, THRESHOLD, 0));
        if (!isFullBlock(blockAt)) {
            return;
        }

        debug(player, blockAt.name() + " is solid and you went into it");
        flag(player, 0.7f);

        if (!isNotPassive()) {
            return;
        }

        // Try and get them out, push in the direction they came from until they hit air
        Vec movement = to.sub(from).asVec();
        if (movement.isZero()) {
            event.setCancelled(true);  // Nowhere sensible to push them, just refuse the move
            return;
        }

        Vec dir = movement.normalize();
        Pos attempt = to;
        for (int step = 0; step < MAX_PUSH_BACK_STEPS && attempt.sameBlock(to); step++) {
            attempt = attempt.sub(dir.mul(PUSH_BACK_STEP));
        }
        player.teleport(attempt);
        event.setCancelled(true);
    }
}
