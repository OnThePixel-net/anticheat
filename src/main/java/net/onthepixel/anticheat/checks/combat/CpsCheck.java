package net.onthepixel.anticheat.checks.combat;

import net.onthepixel.anticheat.ACCheck;
import net.onthepixel.anticheat.ACUtils;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.entity.EntityAttackEvent;
import net.minestom.server.tag.Tag;

import java.util.ArrayList;
import java.util.List;

public class CpsCheck extends ACCheck {
    private static final int THRESHOLD = 20;
    private static final long SAMPLE_TIME = 1000;
    private static final Tag<List<Long>> HITS_TAG = Tag.<List<Long>>Transient("anticheat_cps_hits").defaultValue(ArrayList::new);

    public CpsCheck() {
        super("CPS");
    }

    @Override
    public void register() {
        MinecraftServer.getGlobalEventHandler().addListener(EntityAttackEvent.class, e -> {
            if (!(e.getEntity() instanceof Player player)) {
                return;
            }

            if (isBypassing(player)) {
                return;
            }

            List<Long> pHits = player.getTag(HITS_TAG);
            long now = System.currentTimeMillis();
            pHits.add(now);
            pHits.removeIf(time -> time < now - SAMPLE_TIME);
            player.setTag(HITS_TAG, pHits);

            int cps = pHits.size();
            debug(player, "CPS: " + cps + " (SD: " + ACUtils.standardDeviation(pHits) + ")");

            if (cps >= THRESHOLD) {
                float certainty = Math.min(1f, ((float) (cps - THRESHOLD) / 10f) + 0.7f);
                flag(player, certainty);
            }
        });
    }
}
