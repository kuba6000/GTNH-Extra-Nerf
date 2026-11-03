package pl.kuba6000.gtnhextranerf.items;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.FoodStats;
import net.minecraft.util.StatCollector;

/**
 * Extra Utilities' Healing Axe feeds whoever holds it. Once a player has it in their inventory the stack is marked, is
 * named "Hungry Axe" and takes the hunger it used to give. The rules live here; the mixin only connects them.
 */
public final class HungryAxe {

    static final String TAG = "gtnhextranerf_hungry";
    private static final String NAME_KEY = "item.gtnhextranerf.hungry_axe.name";

    private HungryAxe() {}

    public static boolean isMarked(ItemStack stack) {
        return stack != null && stack.hasTagCompound()
            && stack.getTagCompound()
                .getBoolean(TAG);
    }

    /** Called on the server for a stack that is in a player's inventory. The mark stays with the stack. */
    public static void mark(ItemStack stack) {
        if (stack == null || isMarked(stack)) return;
        if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
        stack.getTagCompound()
            .setBoolean(TAG, true);
    }

    /** The exact opposite of the axe's +1 hunger point: one point is taken, and never below empty. */
    public static void drain(FoodStats stats) {
        int level = stats.getFoodLevel();
        if (level <= 0) return;

        // FoodStats.setFoodLevel is client-only in 1.7.10; its NBT API exists on both sides.
        NBTTagCompound state = new NBTTagCompound();
        stats.writeNBT(state);
        state.setInteger("foodLevel", level - 1);
        stats.readNBT(state);
    }

    public static String displayName() {
        return StatCollector.translateToLocal(NAME_KEY);
    }
}
