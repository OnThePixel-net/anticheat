package net.onthepixel.anticheat.checks.movement;

import net.onthepixel.anticheat.ACCheck;
import net.onthepixel.anticheat.Sample;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.instance.AddEntityToInstanceEvent;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.tag.Tag;

import java.util.ArrayList;
import java.util.List;

public class FlightCheck extends ACCheck {
    private static final long MIN_SAMPLE_TIME = 500;
    private static final long MAX_SAMPLE_AGE = 1000;
    private static final int MIN_SAMPLE_SIZE = 5;
    private static final float CERTAINTY = 0.7f;  // We are cancelling so it's binary, we can't have varying certainty.
    private static final Tag<List<Sample<Pos>>> PLAYER_DETAILS_TAG = Tag.<List<Sample<Pos>>>Transient("anticheat_flight_player_details").defaultValue(ArrayList::new);

    public FlightCheck() {
        super("Flight");
    }

    @Override
    public void register() {
        MinecraftServer.getGlobalEventHandler().addListener(PlayerMoveEvent.class, this::onMove);
        MinecraftServer.getGlobalEventHandler().addListener(AddEntityToInstanceEvent.class, e -> {
            if (e.getEntity() instanceof Player p) {
                p.removeTag(PLAYER_DETAILS_TAG);
            }
        });
    }

    private void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (isBypassing(p)) return;

        if (isBypassFly(p)) {
            debug(p, "bypassed");
            return;
        }

        Pos from = p.getPosition();
        long fromTime = System.currentTimeMillis();
        Pos to = e.getNewPosition();
        List<Sample<Pos>> details = p.getTag(PLAYER_DETAILS_TAG);
        if (!details.isEmpty()) {  // Only bother removing stuff if there is stuff to remove
            Sample.pruneOlderThan(details, MAX_SAMPLE_AGE);
            details.add(Sample.now(to));

            if (details.size() == 1) {
                debug(p, "not enough samples");
                return;
            }
            Sample<Pos> oldest = details.getFirst();
            from = oldest.value();
            fromTime = oldest.timeMillis();
        } else {
            details.add(Sample.now(to));
            p.setTag(PLAYER_DETAILS_TAG, details);
        }

        if (to.y() != from.y()) {
            p.removeTag(PLAYER_DETAILS_TAG);
            debug(p, "changed y");
            return;
        }

        // Make sure they moved on the x or z axis.
        if (to.x() == from.x() && to.z() == from.z()) {
            debug(p, "didn't move");
            return;
        }

        if (isOnGround(p)) {
            p.removeTag(PLAYER_DETAILS_TAG);
            debug(p, "on ground");
            return;
        }

        // Return if the oldest location is newer than half a second
        if (fromTime > System.currentTimeMillis() - MIN_SAMPLE_TIME) {
            debug(p, "sample time not met");
            return;
        }

        if (details.size() < MIN_SAMPLE_SIZE) {
            debug(p, "sample size too low");
            return;
        }

        flag(p, CERTAINTY);  // They have moved without going up or down, and they are not on the ground.
        if (isNotPassive()) {
            e.setCancelled(true);
        }
    }
}
