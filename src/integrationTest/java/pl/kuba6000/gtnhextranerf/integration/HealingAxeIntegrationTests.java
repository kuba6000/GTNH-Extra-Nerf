package pl.kuba6000.gtnhextranerf.integration;

import java.util.UUID;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.FoodStats;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;

import com.mojang.authlib.GameProfile;
import com.rwtema.extrautils.ExtraUtils;

/** Exercises the registered axe through a player's real server-side inventory tick. */
final class HealingAxeIntegrationTests {

    private HealingAxeIntegrationTests() {}

    static void verify(WorldServer world) {
        check(ExtraUtils.healingAxe != null, "Extra Utilities Healing Axe is missing");
        FakePlayer player = FakePlayerFactory.get(world, new GameProfile(UUID.randomUUID(), "HealingAxeTest"));
        ItemStack axe = new ItemStack(ExtraUtils.healingAxe, 1);
        NBTTagCompound itemTag = new NBTTagCompound();
        itemTag.setString("otherModData", "preserved");
        axe.setTagCompound(itemTag);
        String ordinaryName = axe.getDisplayName();
        FoodStats food = player.getFoodStats();
        NBTTagCompound foodState = new NBTTagCompound();
        foodState.setInteger("foodLevel", 20);
        foodState.setInteger("foodTickTimer", 7);
        foodState.setFloat("foodSaturationLevel", 20.0F);
        foodState.setFloat("foodExhaustionLevel", 1.5F);
        food.readNBT(foodState);
        long originalTime = world.getTotalWorldTime();
        long effectTime = originalTime + (40 - originalTime % 40) % 40;
        try {
            player.inventory.setInventorySlotContents(0, axe);
            player.inventory.currentItem = 1;
            world.getWorldInfo()
                .incrementTotalWorldTime(effectTime);
            player.inventory.decrementAnimations();
            check(food.getFoodLevel() == 20, "An unheld axe changed hunger");
            check(!ordinaryName.equals(axe.getDisplayName()), "The axe did not acquire its new name");
            check(
                "preserved".equals(
                    axe.getTagCompound()
                        .getString("otherModData")),
                "The axe lost existing NBT");

            player.inventory.currentItem = 0;
            world.getWorldInfo()
                .incrementTotalWorldTime(effectTime + 1);
            player.inventory.decrementAnimations();
            check(food.getFoodLevel() == 20, "The held axe drained hunger between scheduled ticks");
            world.getWorldInfo()
                .incrementTotalWorldTime(effectTime + 40);
            player.inventory.decrementAnimations();
            check(food.getFoodLevel() == 19, "The held axe did not remove one hunger point");
            check(food.getSaturationLevel() == 20.0F, "The axe changed food saturation");
            NBTTagCompound persisted = new NBTTagCompound();
            food.writeNBT(persisted);
            check(
                persisted.getInteger("foodTickTimer") == 7 && persisted.getFloat("foodExhaustionLevel") == 1.5F,
                "The axe changed other food state");

            foodState.setInteger("foodLevel", 1);
            foodState.setFloat("foodSaturationLevel", 0);
            food.readNBT(foodState);
            world.getWorldInfo()
                .incrementTotalWorldTime(effectTime + 80);
            player.inventory.decrementAnimations();
            check(food.getFoodLevel() == 0, "The axe did not consume the last hunger point");
            world.getWorldInfo()
                .incrementTotalWorldTime(effectTime + 120);
            player.inventory.decrementAnimations();
            check(food.getFoodLevel() == 0, "The axe reduced hunger below zero");
        } finally {
            world.getWorldInfo()
                .incrementTotalWorldTime(originalTime);
            player.inventory.clearInventory(null, -1);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
