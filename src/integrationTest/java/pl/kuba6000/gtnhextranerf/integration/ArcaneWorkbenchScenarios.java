package pl.kuba6000.gtnhextranerf.integration;

import java.awt.Rectangle;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldSettings;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;
import com.gtnewhorizons.horizonqa.api.client.ClickTarget;
import com.gtnewhorizons.horizonqa.api.client.ClientScenario;
import com.gtnewhorizons.horizonqa.api.client.ClientTarget;
import com.gtnewhorizons.horizonqa.api.client.ClientTest;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.util.GTOreDictUnificator;
import pl.kuba6000.gtnhextranerf.items.MagicnetizedManasteelRod;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.client.gui.GuiArcaneWorkbench;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.tiles.TileArcaneWorkbench;

/** Exercises native workbench input and client/server inventory agreement. */
@GameTestHolder(value = "gtnhextranerftests", clientOnly = true)
public final class ArcaneWorkbenchScenarios {

    private ArcaneWorkbenchScenarios() {}

    @GameTest(template = "native_fixture", timeoutTicks = 1800)
    public static void singleCraft(GameTestHelper helper) {
        crafting(helper, 1, 1, false, false);
    }

    @GameTest(template = "native_fixture", timeoutTicks = 1800)
    public static void shiftStackKeepsSingleFlower(GameTestHelper helper) {
        crafting(helper, 64, 1, true, false);
    }

    @GameTest(template = "native_fixture", timeoutTicks = 1800)
    public static void shiftStackKeepsFlowerStack(GameTestHelper helper) {
        crafting(helper, 64, 64, true, false);
    }

    @GameTest(template = "native_fixture", timeoutTicks = 1800)
    public static void fullInventoryDoesNotConsumeInputs(GameTestHelper helper) {
        crafting(helper, 64, 1, true, true);
    }

    private static void crafting(GameTestHelper helper, int amount, int flowers, boolean shift, boolean full) {
        TestPos pos = helper.absolute("workbench");
        ClientTarget result = ClientTarget.of("arcane result", window -> {
            GuiArcaneWorkbench gui = window.screen(GuiArcaneWorkbench.class);
            Slot slot = gui.inventorySlots.getSlot(0);
            return new ClickTarget(
                slot,
                new Rectangle(
                    (gui.width - 190) / 2 + slot.xDisplayPosition,
                    (gui.height - 234) / 2 + slot.yDisplayPosition,
                    16,
                    16),
                slot::getHasStack);
        });
        ClientScenario scenario = ClientTest.scenario(helper)
            .defaultTimeoutTicks(400)
            .server("prepare workbench and supplied ingredients", () -> {
                EntityPlayerMP player = player();
                ItemStack[] saved = new ItemStack[player.inventory.getSizeInventory()];
                for (int index = 0; index < saved.length; index++) {
                    ItemStack stack = player.inventory.getStackInSlot(index);
                    saved[index] = stack == null ? null : stack.copy();
                    player.inventory.setInventorySlotContents(index, null);
                }
                double x = player.posX, y = player.posY, z = player.posZ;
                WorldSettings.GameType mode = player.theItemInWorldManager.getGameType();
                player.theItemInWorldManager.setGameType(WorldSettings.GameType.SURVIVAL);
                helper.afterTest(() -> {
                    player.closeScreen();
                    player.theItemInWorldManager.setGameType(mode);
                    for (int index = 0; index < saved.length; index++)
                        player.inventory.setInventorySlotContents(index, saved[index]);
                    player.inventory.setItemStack(null);
                    player.setPositionAndUpdate(x, y, z);
                    helper.getWorld()
                        .setBlockToAir(pos.x(), pos.y(), pos.z());
                    player.sendContainerToPlayer(player.openContainer);
                });
                if (full) {
                    for (int index = 0; index < player.inventory.mainInventory.length; index++) {
                        player.inventory.setInventorySlotContents(index, new ItemStack(Blocks.cobblestone, 64));
                    }
                }
                helper.getWorld()
                    .setBlock(pos.x(), pos.y(), pos.z(), ConfigBlocks.blockTable, 15, 3);
                TileArcaneWorkbench bench = (TileArcaneWorkbench) helper.getWorld()
                    .getTileEntity(pos.x(), pos.y(), pos.z());
                bench.setInventorySlotContents(
                    3,
                    GTOreDictUnificator.get(OrePrefixes.stick, Materials.SteelMagnetic, amount));
                bench.setInventorySlotContents(
                    4,
                    GTOreDictUnificator.get(OrePrefixes.stick, Materials.Manasteel, amount));
                bench.setInventorySlotContents(
                    5,
                    GTOreDictUnificator.get(OrePrefixes.stick, Materials.SteelMagnetic, amount));
                ItemStack flower = new ItemStack(GameRegistry.findItem("Botania", "specialFlower"), flowers);
                NBTTagCompound tags = new NBTTagCompound();
                tags.setString("type", "solegnolia");
                tags.setString("display-test", "preserved");
                flower.setTagCompound(tags);
                bench.setInventorySlotContents(7, flower);
                ItemStack wand = new ItemStack(ConfigItems.itemWandCasting);
                ItemWandCasting item = (ItemWandCasting) wand.getItem();
                item.setRod(wand, ConfigItems.WAND_ROD_SILVERWOOD);
                item.setCap(wand, ConfigItems.WAND_CAP_THAUMIUM);
                AspectList vis = new AspectList();
                for (Aspect aspect : Aspect.getPrimalAspects()) vis.add(aspect, 100000);
                item.storeAllVis(wand, vis);
                bench.setInventorySlotContents(10, wand);
                player.setPositionAndUpdate(pos.x() + 1.5, pos.y(), pos.z() + 1.5);
                player.sendContainerToPlayer(player.openContainer);
            })
            .awaitClient("workbench tile reached the client", client -> {
                check(
                    Minecraft.getMinecraft().theWorld
                        .getTileEntity(pos.x(), pos.y(), pos.z()) instanceof TileArcaneWorkbench,
                    "workbench tile is not synchronized");
            })
            .server("open the synchronized workbench", () -> {
                EntityPlayerMP player = player();
                check(
                    ConfigBlocks.blockTable
                        .onBlockActivated(helper.getWorld(), pos.x(), pos.y(), pos.z(), player, 1, 0.5f, 0.5f, 0.5f),
                    "workbench did not open");
                player.sendContainerToPlayer(player.openContainer);
            })
            .awaitScreen(GuiArcaneWorkbench.class)
            .awaitClient("recipe preview reached the client", client -> {
                check(
                    Minecraft.getMinecraft().thePlayer.openContainer.getSlot(0)
                        .getHasStack(),
                    "missing arcane output");
                for (ItemStack initial : Minecraft.getMinecraft().thePlayer.inventory.mainInventory) {
                    check(
                        full ? initial != null && initial.stackSize == 64 : initial == null,
                        "initial player inventory is not synchronized");
                }
                check(
                    Minecraft.getMinecraft().thePlayer.inventory.getItemStack() == null,
                    "initial cursor is not empty");
            });
        if (shift) scenario.shiftClick(result);
        else scenario.click(result);
        scenario.awaitServer("verify authoritative crafting outcome", () -> {
            TileArcaneWorkbench bench = (TileArcaneWorkbench) helper.getWorld()
                .getTileEntity(pos.x(), pos.y(), pos.z());
            check(bench.getStackInSlot(7).stackSize == flowers, "Solegnolia count changed");
            check(
                "preserved".equals(
                    bench.getStackInSlot(7)
                        .getTagCompound()
                        .getString("display-test")),
                "flower NBT changed");
            if (full) {
                check(bench.getStackInSlot(4).stackSize == amount, "full inventory consumed Manasteel");
                return;
            }
            check(
                bench.getStackInSlot(3) == null && bench.getStackInSlot(4) == null && bench.getStackInSlot(5) == null,
                "crafting inputs remain");
            check(
                count(player().inventory.mainInventory, GTOreDictUnificator.get(OrePrefixes.stick, Materials.Steel, 1))
                    == amount * 2,
                "steel rods were lost");
            ItemStack wand = bench.getStackInSlot(10);
            check(((ItemWandCasting) wand.getItem()).getVis(wand, Aspect.AIR) < 100000, "crafting did not use Aer");
            check(((ItemWandCasting) wand.getItem()).getVis(wand, Aspect.ORDER) < 100000, "crafting did not use Ordo");
        })
            .awaitClient("crafted items are visible without reopening inventory", client -> {
                if (full) return;
                Minecraft mc = Minecraft.getMinecraft();
                check(
                    count(
                        mc.thePlayer.inventory.mainInventory,
                        GTOreDictUnificator.get(OrePrefixes.stick, Materials.Steel, 1)) == amount * 2,
                    "client Steel Rod count: " + count(
                        mc.thePlayer.inventory.mainInventory,
                        GTOreDictUnificator.get(OrePrefixes.stick, Materials.Steel, 1)) + ", expected " + (amount * 2));
                if (shift) check(
                    count(mc.thePlayer.inventory.mainInventory, MagicnetizedManasteelRod.stack()) == amount,
                    "client has not received crafted rods");
                else check(
                    mc.thePlayer.inventory.getItemStack() != null && mc.thePlayer.inventory.getItemStack()
                        .isItemEqual(MagicnetizedManasteelRod.stack()),
                    "crafted rod is missing from cursor");
            })
            .capture("crafted-inventory")
            .succeed();
    }

    private static EntityPlayerMP player() {
        return (EntityPlayerMP) MinecraftServer.getServer()
            .getConfigurationManager().playerEntityList.get(0);
    }

    private static int count(ItemStack[] inventory, ItemStack wanted) {
        int result = 0;
        for (ItemStack stack : inventory) if (stack != null && stack.isItemEqual(wanted)) result += stack.stackSize;
        return result;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
