package net.onthepixel.anticheat.checks.movement;

import net.onthepixel.anticheat.ACCheck;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerMoveEvent;

public class TeleportCheck extends ACCheck {
    private static final float UP_THRESHOLD = 3;
    private static final float DOWN_THRESHOLD = 5;
    private static final float HORIZONTAL_THRESHOLD = 4;

    public TeleportCheck() {
        super("Teleport");
    }

    @Override
    public void register() {
        MinecraftServer.getGlobalEventHandler().addListener(PlayerMoveEvent.class, this::onMove);
    }

    private void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (isBypassing(p)) return;

        Pos from = p.getPosition();
        Pos to = e.getNewPosition();

        checkAxis(e, p, to.y() - from.y(), DOWN_THRESHOLD, UP_THRESHOLD);
        checkAxis(e, p, to.x() - from.x(), HORIZONTAL_THRESHOLD, HORIZONTAL_THRESHOLD);
        checkAxis(e, p, to.z() - from.z(), HORIZONTAL_THRESHOLD, HORIZONTAL_THRESHOLD);
    }

    private void checkAxis(PlayerMoveEvent e, Player p, double diff, float backwardThreshold, float forwardThreshold) {
        if (Math.abs(diff) > (diff < 0 ? backwardThreshold : forwardThreshold)) {  // If they are going backward, we allow a bit more
            float certainty = Math.min(1f, ((float) diff / 10f) + 0.5f);
            flag(p, certainty);
            if (isNotPassive()) {
                e.setCancelled(true);
            }
        }
    }
}
