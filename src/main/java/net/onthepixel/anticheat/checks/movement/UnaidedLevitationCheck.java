package net.onthepixel.anticheat.checks.movement;

import net.onthepixel.anticheat.ACCheck;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.entity.EntityDamageEvent;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

public class UnaidedLevitationCheck extends ACCheck {
    private static final int THRESHOLD = 3;
    private static final Tag<Double> BLOCKS_RAISED_TAG = Tag.Double("anticheat_unaidedlev_raisedblocks").defaultValue(0D);

    public UnaidedLevitationCheck() {
        super("UnaidedLevitation");
    }

    @Override
    public void register() {
        MinecraftServer.getGlobalEventHandler().addListener(PlayerMoveEvent.class, this::onMove);
        MinecraftServer.getGlobalEventHandler().addListener(EntityDamageEvent.class, this::onDamage);
    }

    private void onDamage(@NotNull EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        player.removeTag(BLOCKS_RAISED_TAG);
    }

    private void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (isBypassing(p)) return;

        if (isBypassFly(p)) {
            return;
        }

        if (isOnGround(p)) {
            p.removeTag(BLOCKS_RAISED_TAG);
            return;
        }

        Pos from = p.getPosition();
        Pos to = e.getNewPosition();
        if (from.y() > to.y()) {
            return;
        }

        // accumulate how far they have risen since they last touched the ground
        double newRaised = p.updateAndGetTag(BLOCKS_RAISED_TAG, old -> old + (to.y() - from.y()));

        // check if the player has gone up more than 3 blocks without going down then fail
        if (newRaised > THRESHOLD) {
            float certainty = Math.min(1f, (float) ((newRaised - THRESHOLD) / 10d) + 0.5f);
            flag(p, certainty);
            if (isNotPassive()) {
                e.setCancelled(true);
            }
        }
    }
}
