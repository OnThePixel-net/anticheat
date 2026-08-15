package net.onthepixel.anticheat.events;

import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;

/**
 * Fired when a check believes a player is cheating.
 *
 * @param checkName the name of the check that flagged
 * @param player the flagged player
 * @param certainty how sure the check is, between 0 and 1
 */
public record PlayerFlagEvent(String checkName, Player player, float certainty) implements Event {
}
