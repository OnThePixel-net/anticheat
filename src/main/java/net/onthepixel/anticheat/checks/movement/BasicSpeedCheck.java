package net.onthepixel.anticheat.checks.movement;

import net.onthepixel.anticheat.ACCheck;
import net.onthepixel.anticheat.Sample;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.entity.EntityTeleportEvent;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.tag.Tag;

import java.util.ArrayList;
import java.util.List;

/**
 * We can't get when the player teleports, so to flag for speed we need the following:
 * - Player isn't flying or otherwise bypassing
 * - All movements are consistent, the speed shouldn't spike once (e.g. teleporting)
 */
public class BasicSpeedCheck extends ACCheck {
    private static final float THRESHOLD = 4f;  // Usually starts false flagging at 0.6
    private static final int SAMPLE_TIME = 1500;
    private static final int AVERAGE_TIME_PERIOD_MS = 1000;
    private static final int MIN_SAMPLE_SIZE = 5;

    private static final Tag<List<Sample<Pos>>> PLAYER_DETAILS_TAG = Tag.<List<Sample<Pos>>>Transient("anticheat_basicspeed_player_details").defaultValue(ArrayList::new);

    public BasicSpeedCheck() {
        super("BasicSpeed");
    }

    @Override
    public void register() {
        MinecraftServer.getGlobalEventHandler().addListener(PlayerMoveEvent.class, this::onMove);
        MinecraftServer.getGlobalEventHandler().addListener(EntityTeleportEvent.class, e -> {
            if (e.getEntity() instanceof Player player) {
                player.getTag(PLAYER_DETAILS_TAG).clear(); // Won't be accurate
            }
        });
    }

    @Override
    public void disableFor(Player player, int time) {
        super.disableFor(player, time);

        // reset details if the check is disabled
        player.getTag(PLAYER_DETAILS_TAG).clear();
    }

    private void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (isBypassing(p)) return;

        if (isBypassSpeed(p)) {
            debug(p, "bypassed");
            return;
        }

        List<Sample<Pos>> details = p.getTag(PLAYER_DETAILS_TAG);
        Pos to = e.getNewPosition();

        Sample.pruneOlderThan(details, SAMPLE_TIME);
        details.add(Sample.now(to));
        p.setTag(PLAYER_DETAILS_TAG, details);

        Sample<Pos> oldest = details.getFirst();
        Pos from = oldest.value();
        long timeSinceFrom = System.currentTimeMillis() - oldest.timeMillis();

        if (timeSinceFrom < AVERAGE_TIME_PERIOD_MS) {
            debug(p, "averaging time not met");
            return;  // Average must be over the averaging period.
        }

        if (details.size() < MIN_SAMPLE_SIZE) {
            debug(p, "min sample size not met");
            return;
        }

        double horizontalDistance = Math.hypot(to.x() - from.x(), to.z() - from.z());
        double speed = horizontalDistance / (timeSinceFrom / (double) AVERAGE_TIME_PERIOD_MS);
        double expectedMaxWalkSpeed = 4.317 * (getRunSpeed(p) / 0.2) * 1.3 * 1.5;

        if (speed > expectedMaxWalkSpeed + THRESHOLD) {
            float certainty = (float) Math.min(1f, (speed - expectedMaxWalkSpeed) / 2f);
            flag(p, certainty);
            debug(p, "Lowest valid threshold: " + (speed - expectedMaxWalkSpeed));
            if (isNotPassive()) {
                e.setCancelled(true);
            }
        }
    }
}
