package pl.kuba6000.gtnhextranerf.integration;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.WorldServer;

import gregtech.GTMod;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.IConfigurationCircuitSupport;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.util.GTUtility;
import pl.kuba6000.gtnhextranerf.mixins.support.PhysicalCircuitItemStackHandler;

/** Real machine inventories and world drops, including native explosion handling. */
final class PhysicalCircuitDropIntegrationTests {

    private PhysicalCircuitDropIntegrationTests() {}

    static void verify(WorldServer world) {
        int x = 1200;
        for (ItemList machine : new ItemList[] { ItemList.Machine_LV_Macerator, ItemList.Machine_MV_Macerator,
            ItemList.Hatch_Input_Bus_LV, ItemList.Hatch_Input_Bus_MV }) {
            verifyDrop(world, machine, x++, false, false);
        }
        verifyDrop(world, ItemList.Machine_LV_Macerator, x++, true, true);
        verifyDrop(world, ItemList.Hatch_Input_Bus_MV, x++, true, false);
        verifyGhostIsNotDropped(world, x);
    }

    private static void verifyDrop(WorldServer world, ItemList item, int x, boolean explode, boolean safeDrops) {
        boolean oldDropSetting = GTMod.proxy.mExplosionItemDrop;
        boolean oldExplosions = GregTechAPI.sMachineExplosions;
        BaseMetaTileEntity tile = TransformerIntegrationTests.placeAt(world, item.get(1), x, 230, 240);
        List<Object> before = new ArrayList<>(world.loadedEntityList);
        try {
            IMetaTileEntity machine = tile.getMetaTileEntity();
            check(machine instanceof IConfigurationCircuitSupport, "Test machine lacks a circuit slot");
            int circuitSlot = ((IConfigurationCircuitSupport) machine).getCircuitSlot();
            PhysicalCircuitItemStackHandler handler = new PhysicalCircuitItemStackHandler(machine, circuitSlot);
            ItemStack circuit = GTUtility.getIntegratedCircuit(7);
            circuit.stackSize = 1;
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("testMarker", "physical-circuit");
            circuit.setTagCompound(tag);
            check(handler.insertItem(0, circuit, false) == null, "Could not insert a physical circuit");
            int normalSlot = -1;
            for (int slot = 0; slot < tile.getSizeInventory(); slot++) {
                if (tile.isValidSlot(slot)) {
                    normalSlot = slot;
                    break;
                }
            }
            check(normalSlot >= 0, "Test machine lacks a normal inventory slot");
            tile.setInventorySlotContents(normalSlot, new ItemStack(Blocks.cobblestone, 3));
            if (explode) {
                GTMod.proxy.mExplosionItemDrop = safeDrops;
                GregTechAPI.sMachineExplosions = false;
                tile.doExplosion(32);
            } else {
                world.setBlockToAir(x, 230, 240);
            }
            EntityItem circuitDrop = null, normalDrop = null;
            for (Object object : new ArrayList<>(world.loadedEntityList)) {
                if (!(object instanceof EntityItem) || before.contains(object)) continue;
                EntityItem entity = (EntityItem) object;
                ItemStack dropped = entity.getEntityItem();
                if (dropped.isItemEqual(circuit)) {
                    check(circuitDrop == null, "Physical circuit was duplicated on destruction");
                    circuitDrop = entity;
                } else if (dropped.getItem() == Item.getItemFromBlock(Blocks.cobblestone)) {
                    check(normalDrop == null, "Normal inventory stack was duplicated on destruction");
                    normalDrop = entity;
                }
            }
            check(
                circuitDrop != null && circuitDrop.getEntityItem().stackSize == 1
                    && ItemStack.areItemStackTagsEqual(circuit, circuitDrop.getEntityItem()),
                "Physical circuit or its NBT disappeared when destroying " + item);
            check(
                normalDrop != null && normalDrop.getEntityItem().stackSize == 3,
                "Normal inventory drop changed when destroying " + item);
            if (explode) {
                check(
                    circuitDrop.hurtResistantTime == normalDrop.hurtResistantTime
                        && circuitDrop.lifespan == normalDrop.lifespan,
                    "Circuit did not use the same native explosion drop handling as other items");
            }
        } finally {
            GTMod.proxy.mExplosionItemDrop = oldDropSetting;
            GregTechAPI.sMachineExplosions = oldExplosions;
            world.setBlockToAir(x, 230, 240);
            for (Object object : new ArrayList<>(world.loadedEntityList))
                if (object instanceof EntityItem && !before.contains(object)) world.removeEntity((EntityItem) object);
        }
    }

    private static void verifyGhostIsNotDropped(WorldServer world, int x) {
        BaseMetaTileEntity tile = TransformerIntegrationTests
            .placeAt(world, ItemList.Machine_HV_Macerator.get(1), x, 230, 240);
        List<Object> before = new ArrayList<>(world.loadedEntityList);
        try {
            IMetaTileEntity machine = tile.getMetaTileEntity();
            int circuitSlot = ((IConfigurationCircuitSupport) machine).getCircuitSlot();
            ItemStack ghost = GTUtility.getIntegratedCircuit(7);
            check(ghost.stackSize == 0, "Expected a native phantom circuit");
            machine.getInventoryHandler()
                .setStackInSlot(circuitSlot, ghost);
            world.setBlockToAir(x, 230, 240);
            for (Object object : new ArrayList<>(world.loadedEntityList)) {
                if (object instanceof EntityItem && !before.contains(object)) {
                    check(
                        !((EntityItem) object).getEntityItem()
                            .isItemEqual(ghost),
                        "A phantom circuit became a physical drop");
                }
            }
        } finally {
            world.setBlockToAir(x, 230, 240);
            for (Object object : new ArrayList<>(world.loadedEntityList))
                if (object instanceof EntityItem && !before.contains(object)) world.removeEntity((EntityItem) object);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
