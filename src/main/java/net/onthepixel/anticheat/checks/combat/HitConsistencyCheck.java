package net.onthepixel.anticheat.checks.combat;

import net.onthepixel.anticheat.ACCheck;
import net.onthepixel.anticheat.ACUtils;
import net.onthepixel.anticheat.Sample;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.entity.EntityAttackEvent;
import net.minestom.server.tag.Tag;

import java.util.ArrayList;
import java.util.List;

public class HitConsistencyCheck extends ACCheck {
    private static final double THRESHOLD = 1;
    private static final long SAMPLE_TIME = 1000;
    private static final int MIN_SAMPLE_SIZE = 5;
    private static final Tag<List<Long>> HITS_TAG = Tag.<List<Long>>Transient("anticheat_hitconsis_hits").defaultValue(ArrayList::new);
    private static final Tag<List<Sample<Double>>> HIT_STDS_TAG = Tag.<List<Sample<Double>>>Transient("anticheat_hitconsis_hitstds").defaultValue(ArrayList::new);

    public HitConsistencyCheck() {
        super("HitConsistency");
    }

    @Override
    public void register() {
        MinecraftServer.getGlobalEventHandler().addListener(EntityAttackEvent.class, e -> {
            if (!(e.getEntity() instanceof Player player)) {
                return;
            }

            if (isBypassing(player)) return;

            List<Long> pHits = player.getTag(HITS_TAG);
            long now = System.currentTimeMillis();
            pHits.add(now);
            pHits.removeIf(time -> time < now - SAMPLE_TIME);
            player.setTag(HITS_TAG, pHits);

            double std = ACUtils.standardDeviation(pHits);

            List<Sample<Double>> pHitStds = player.getTag(HIT_STDS_TAG);
            pHitStds.add(Sample.now(std));
            Sample.pruneOlderThan(pHitStds, SAMPLE_TIME);
            player.setTag(HIT_STDS_TAG, pHitStds);

            if (pHitStds.size() < MIN_SAMPLE_SIZE) {
                debug(player, "No hit stds");
                return;
            }

            double stdStd = ACUtils.standardDeviation(pHitStds.stream().map(Sample::value).toList());
            debug(player, "SDSD: " + stdStd);

            if (stdStd <= THRESHOLD) {
                float certainty = Math.max(0.1f, 1f - (float) (stdStd / THRESHOLD));
                flag(player, certainty);
            }
        });
    }
}
