package net.onthepixel.anticheat;

import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.component.EnchantmentList;
import net.minestom.server.item.enchant.Enchantment;
import net.minestom.server.registry.RegistryKey;

import java.util.Collection;

public final class ACUtils {

    private ACUtils() {
    }

    public static boolean isUsingSoulSpeed(Player p) {
        return hasEnchantment(p.getEquipment(EquipmentSlot.BOOTS), Enchantment.SOUL_SPEED);
    }

    public static boolean hasEnchantment(ItemStack item, RegistryKey<Enchantment> enchantment) {
        EnchantmentList enchantments = item.get(DataComponents.ENCHANTMENTS);
        return enchantments != null && enchantments.has(enchantment);
    }

    /**
     * Population standard deviation of the given values.
     *
     * @param values the values to measure, must not be empty
     * @return the standard deviation
     * @throws IllegalArgumentException if {@code values} is empty
     */
    public static double standardDeviation(Collection<? extends Number> values) {
        if (values.isEmpty()) {
            throw new IllegalArgumentException("Cannot compute the standard deviation of no values");
        }

        double mean = values.stream().mapToDouble(Number::doubleValue).average().orElseThrow();
        double squaredDifferenceSum = values.stream()
                .mapToDouble(value -> {
                    double difference = value.doubleValue() - mean;
                    return difference * difference;
                })
                .sum();
        return Math.sqrt(squaredDifferenceSum / values.size());
    }
}
