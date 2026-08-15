package net.onthepixel.anticheat.checks.movement;

import net.onthepixel.anticheat.ACCheck;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.tag.Tag;

import java.util.ArrayList;
import java.util.List;

public class TeleportSpamCheck extends ACCheck {
    private static final double TELEPORT_DISTANCE_THRESHOLD = 1.5;
    private static final long SAMPLING_PERIOD = 1000;
    private static final int MAX_TELEPORTS = 2;
    private static final Tag<List<Long>> TELEPORT_TIMES_TAG = Tag.<List<Long>>Transient("anticheat_tpspam_times").defaultValue(ArrayList::new);

    public TeleportSpamCheck() {
        super("TeleportSpam");
    }

    @Override
    public void register() {
        MinecraftServer.getGlobalEventHandler().addListener(PlayerMoveEvent.class, e -> {
            Player player = e.getPlayer();
            if (isBypassing(player)) return;

            Pos pos1 = player.getPosition().withY(0);
            Pos pos2 = e.getNewPosition().withY(0);

            if (pos1.distance(pos2) < TELEPORT_DISTANCE_THRESHOLD) {
                return;
            }

            List<Long> times = player.getTag(TELEPORT_TIMES_TAG);
            long currentTime = System.currentTimeMillis();
            times.add(currentTime);
            player.setTag(TELEPORT_TIMES_TAG, times);

            times.removeIf(time -> time < currentTime - SAMPLING_PERIOD);

            debug(player, "Teleports: " + times.size());

            if (times.size() > MAX_TELEPORTS) {
                float certainty = Math.min(1f, ((float) (times.size() - MAX_TELEPORTS) / 10f) + 0.5f);
                flag(player, certainty);
                if (isNotPassive()) e.setCancelled(true);
            }
        });
    }
}
