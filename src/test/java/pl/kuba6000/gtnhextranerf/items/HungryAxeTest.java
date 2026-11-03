package pl.kuba6000.gtnhextranerf.items;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.FoodStats;

import org.junit.jupiter.api.Test;

class HungryAxeTest {

    private static ItemStack axe() {
        return new ItemStack((Item) null);
    }

    @Test
    void anAxeIsOrdinaryUntilAPlayerHasItInTheInventory() {
        ItemStack stack = axe();
        assertFalse(HungryAxe.isMarked(stack));
        assertFalse(HungryAxe.isMarked(null));
        HungryAxe.mark(stack);
        assertTrue(HungryAxe.isMarked(stack));
    }

    @Test
    void markingKeepsOtherTagsAndIsRepeatable() {
        ItemStack stack = axe();
        stack.setTagCompound(new net.minecraft.nbt.NBTTagCompound());
        stack.getTagCompound()
            .setString("other", "kept");
        HungryAxe.mark(stack);
        HungryAxe.mark(stack);
        assertTrue(HungryAxe.isMarked(stack));
        assertEquals(
            "kept",
            stack.getTagCompound()
                .getString("other"));
        assertEquals(
            2,
            stack.getTagCompound()
                .func_150296_c()
                .size());
        HungryAxe.mark(null);
    }

    @Test
    void theMarkSurvivesACopyLikeADroppedOrStoredStack() {
        ItemStack stack = axe();
        HungryAxe.mark(stack);
        assertTrue(HungryAxe.isMarked(stack.copy()));
    }

    @Test
    void theHeldEffectTakesOneHungerPointAndStopsAtEmpty() {
        FoodStats stats = new FoodStats();
        assertEquals(20, stats.getFoodLevel());
        HungryAxe.drain(stats);
        assertEquals(19, stats.getFoodLevel());
        net.minecraft.nbt.NBTTagCompound state = new net.minecraft.nbt.NBTTagCompound();
        stats.writeNBT(state);
        state.setInteger("foodLevel", 1);
        stats.readNBT(state);
        HungryAxe.drain(stats);
        assertEquals(0, stats.getFoodLevel());
        HungryAxe.drain(stats);
        assertEquals(0, stats.getFoodLevel());
    }
}
