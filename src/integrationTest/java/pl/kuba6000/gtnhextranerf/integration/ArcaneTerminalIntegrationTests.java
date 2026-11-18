package pl.kuba6000.gtnhextranerf.integration;

import net.minecraft.item.ItemStack;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import appeng.tile.networking.TileCableBus;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.util.GTOreDictUnificator;
import pl.kuba6000.gtnhextranerf.items.MagicnetizedRods;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumicenergistics.common.container.ContainerPartArcaneCraftingTerminal;
import thaumicenergistics.common.container.slot.SlotArcaneCraftingResult;
import thaumicenergistics.common.parts.AEPartsEnum;
import thaumicenergistics.common.parts.PartArcaneCraftingTerminal;

/** Exercises ingredient consumption in a native Arcane Crafting Terminal. */
@GameTestHolder(value = "gtnhextranerftests", requiredMods = "thaumicenergistics")
public final class ArcaneTerminalIntegrationTests {

    private ArcaneTerminalIntegrationTests() {}

    @GameTest(template = "native_fixture", timeoutTicks = 1200)
    public static void keepsSolegnolia(GameTestHelper helper) {
        helper.startSequence()
            .thenExecute("craft with one Solegnolia", () -> verify(helper.getWorld(), 1))
            .thenExecute("craft with a stack of Solegnolias", () -> verify(helper.getWorld(), 4))
            .thenSucceed();
    }

    private static void verify(WorldServer world, int flowerCount) {
        var player = FakePlayerFactory.getMinecraft(world);
        var host = new TileCableBus();
        host.setWorldObj(world);
        var terminal = new PartArcaneCraftingTerminal(AEPartsEnum.ArcaneCraftingTerminal.getStack());
        terminal.setPartHostInfo(ForgeDirection.NORTH, host, host);

        var grid = terminal.craftingGridInventory;
        grid.setInventorySlotContents(3, GTOreDictUnificator.get(OrePrefixes.stick, Materials.SteelMagnetic, 2));
        grid.setInventorySlotContents(4, GTOreDictUnificator.get(OrePrefixes.stick, Materials.Manasteel, 2));
        grid.setInventorySlotContents(5, GTOreDictUnificator.get(OrePrefixes.stick, Materials.SteelMagnetic, 2));
        ItemStack flower = MagicnetizedRods.solegnolia();
        flower.stackSize = flowerCount;
        flower.getTagCompound()
            .setString("test-marker", "preserve this tag");
        grid.setInventorySlotContents(7, flower.copy());

        ItemStack wand = new ItemStack(ConfigItems.itemWandCasting);
        var wandItem = (ItemWandCasting) wand.getItem();
        wandItem.setRod(wand, ConfigItems.WAND_ROD_SILVERWOOD);
        wandItem.setCap(wand, ConfigItems.WAND_CAP_THAUMIUM);
        wandItem.storeAllVis(
            wand,
            new AspectList().add(Aspect.AIR, 10000)
                .add(Aspect.ORDER, 10000));
        terminal.wandInventory.setInventorySlotContents(0, wand);

        var container = new ContainerPartArcaneCraftingTerminal(player.inventory, terminal);
        try {
            SlotArcaneCraftingResult resultSlot = findResultSlot(container);
            for (int remaining = 1; remaining >= 0; remaining--) {
                ItemStack output = resultSlot.getStack();
                if (output == null || !output.isItemEqual(MagicnetizedRods.manasteel())) {
                    throw new AssertionError(
                        "Expected a Magicnetized Manasteel Rod, got " + output
                            + " with costs "
                            + container.getAspectCosts());
                }
                container.consumeIngredients(player);
                if (!ItemStack.areItemStacksEqual(flower, grid.getStackInSlot(7))) {
                    throw new AssertionError("The terminal changed the Solegnolia stack of " + flowerCount);
                }
                for (int slot = 3; slot <= 5; slot++) {
                    ItemStack input = grid.getStackInSlot(slot);
                    if ((input == null ? 0 : input.stackSize) != remaining) {
                        throw new AssertionError("The terminal did not use exactly one rod from slot " + slot);
                    }
                }
            }
        } finally {
            container.onContainerClosed(player);
        }
    }

    private static SlotArcaneCraftingResult findResultSlot(ContainerPartArcaneCraftingTerminal container) {
        for (Object slot : container.inventorySlots) {
            if (slot instanceof SlotArcaneCraftingResult result) return result;
        }
        throw new AssertionError("The terminal has no arcane crafting result slot");
    }
}
