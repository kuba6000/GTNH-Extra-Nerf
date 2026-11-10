package pl.kuba6000.gtnhextranerf.integration;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import com.gtnewhorizon.structurelib.structure.IItemSource;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;
import com.mojang.authlib.GameProfile;

import bartworks.MainMod;
import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.enums.VoltageIndex;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatchInput;
import gregtech.api.metatileentity.implementations.MTEHatchInputBus;
import gregtech.api.metatileentity.implementations.MTEHatchOutput;
import gregtech.api.metatileentity.implementations.MTEHatchOutputBus;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;
import gregtech.api.util.GlassTier;
import gregtech.api.util.shutdown.ShutDownReasonRegistry;
import pl.kuba6000.gtnhextranerf.machines.Brainrotter;
import pl.kuba6000.gtnhextranerf.machines.MTEIndustrialBrainrotter;
import pl.kuba6000.gtnhextranerf.materials.NerfMaterials;
import pl.kuba6000.gtnhextranerf.mixins.early.MixinCraftingManagerAccessor;
import pl.kuba6000.gtnhextranerf.mixins.early.MixinRecipeAccessor;
import pl.kuba6000.gtnhextranerf.recipes.machine.RecipeMatch;
import tectech.recipe.TecTechRecipeMaps;
import tectech.thing.CustomItemList;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.tiles.TilePedestal;

/** Native construction, hatch inventories and real server ticks in the disposable world. */
@GameTestHolder("gtnhextranerfrecipes")
public final class BrainrotterIntegrationTests {

    private BrainrotterIntegrationTests() {}

    @GameTest(timeoutTicks = 100)
    public static void compilation(GameTestHelper helper) {
        verifyCompilation();
        helper.succeed();
    }

    static void verifyCompilation() {
        RecipeMatch expected = RecipeMatch.builder()
            .itemInputs(
                ItemList.Tool_DataStick.get(1),
                GameRegistry.findItemStack("Thaumcraft", "ItemZombieBrain", 64),
                new ItemStack(Blocks.noteblock),
                new ItemStack(Items.clock),
                new ItemStack(Items.sugar, 16),
                new ItemStack(Items.redstone, 16))
            .fluidInputs(Materials.Glue.getFluid(144))
            .itemOutputs(Brainrotter.compilation(1))
            .fluidOutputs()
            .duration(400)
            .eut(TierEU.RECIPE_EV)
            .build();
        long matches = RecipeMaps.assemblerRecipes.getAllRecipes()
            .stream()
            .filter(expected)
            .count();
        check(matches == 1, "Expected one EV compilation recipe with 64 Zombie Brains and Glue");
        for (Object recipe : ((MixinCraftingManagerAccessor) CraftingManager.getInstance())
            .gtnhextranerf$getRecipes()) {
            ItemStack output = ((MixinRecipeAccessor) recipe).gtnhextranerf$getRecipeOutput();
            check(
                output == null || !output.isItemEqual(Brainrotter.compilation(1)),
                "Brainrot Compilation still has a hand-crafting recipe");
        }
    }

    static void verifyDevelopment(WorldServer world) {
        check(
            !Brainrotter.RECIPES.getAllRecipes()
                .isEmpty(),
            "Brainrotter processing recipes are absent; inspect the reduced-runtime startup audit");
        check(GameRegistry.findBlock("ExtraUtilities", "timer") == null, "Fixture must exercise a missing timer");
        check(GameRegistry.findBlock("Forestry", "logsFireproof") == null, "Fixture must exercise missing Forestry");
        check(GameRegistry.findBlock("Thaumcraft", "blockStoneDevice") == null, "Fixture must lack a pedestal");
        FakePlayer player = FakePlayerFactory.get(world, new GameProfile(UUID.randomUUID(), "BrainrotterDevTest"));
        BaseMetaTileEntity controller = place(world, player, Brainrotter.controller(), 40, 240, 8);
        controller.setFrontFacing(ForgeDirection.NORTH);
        MTEIndustrialBrainrotter machine = (MTEIndustrialBrainrotter) controller.getMetaTileEntity();
        machine.construct(Brainrotter.controller(), false);
        List<int[]> shell = new ArrayList<>();
        for (int x = 38; x <= 42; x++) for (int y = 240; y <= 244; y++) for (int z = 8; z <= 10; z++) {
            if (GlassTier.getGlassBlockTier(world.getBlock(x, y, z), world.getBlockMetadata(x, y, z)) != null)
                shell.add(new int[] { x, y, z });
        }
        check(shell.size() == 36, "Missing mods changed the supplied structure's glass shell");
        glass(world, shell, VoltageIndex.EV);
        // The outer columns of the supplied bottom layer accept GT hatches.
        BaseMetaTileEntity inputTile = place(world, player, ItemList.Hatch_Input_Bus_EV.get(1), 38, 240, 8);
        BaseMetaTileEntity outputTile = place(world, player, ItemList.Hatch_Output_Bus_EV.get(1), 38, 240, 9);
        BaseMetaTileEntity energy = place(world, player, ItemList.Hatch_Energy_EV.get(1), 38, 240, 10);
        BaseMetaTileEntity maintenance = place(world, player, ItemList.Hatch_Maintenance.get(1), 42, 240, 8);
        BaseMetaTileEntity fluidOutput = place(world, player, ItemList.Hatch_Output_EV.get(1), 42, 240, 9);
        check(machine.checkStructure(true, controller), "Placeholder structure did not form with native GT hatches");
        check(machine.getPedestal() == null, "Development placeholder connected a nonexistent pedestal");
        machine.fixAllIssues();
        controller.enableWorking();
        MTEHatchInputBus input = (MTEHatchInputBus) inputTile.getMetaTileEntity();
        MTEHatchOutputBus output = (MTEHatchOutputBus) outputTile.getMetaTileEntity();
        ItemStack raw = GTOreDictUnificator.get(OrePrefixes.ingot, NerfMaterials.rawTungstenSteel(), 1);
        ItemStack pulp = GTOreDictUnificator.get(OrePrefixes.dust, Materials.Wood, 1);
        input.setInventorySlotContents(0, raw.copy());
        input.setInventorySlotContents(1, Brainrotter.compilation(1));
        input.setInventorySlotContents(2, pulp.copy());
        BaseMetaTileEntity[] tiles = { inputTile, outputTile, energy, maintenance, fluidOutput, controller };
        long used = start(tiles, machine);
        used += tick(tiles, machine.mMaxProgresstime + 1);
        check(
            outputs(output) == 1 && items(input, raw) == 0,
            "Refining did not complete without the unavailable pedestal");
        check(items(input, pulp) == 0 && used > 0, "Development refining did not consume direction input and energy");
        check(items(input, Brainrotter.compilation(1)) == 1, "Development refining consumed its reusable compilation");
        tick(tiles, 100);
        check(outputs(output) == 1, "Development refining duplicated its output");
        BaseMetaTileEntity fluidInput = place(world, player, ItemList.Hatch_Input_EV.get(1), 42, 240, 10);
        MTEHatchInput fluid = (MTEHatchInput) fluidInput.getMetaTileEntity();
        check(machine.checkStructure(true, controller), "Development structure rejected its fluid input hatch");
        FluidStack molten = NerfMaterials.rawTungstenSteel()
            .getMolten(144);
        check(fluid.fill(ForgeDirection.UNKNOWN, molten, true) == 144, "Development input hatch rejected raw molten");
        input.setInventorySlotContents(2, pulp.copy());
        controller.enableWorking();
        tiles = new BaseMetaTileEntity[] { inputTile, outputTile, energy, maintenance, fluidOutput, fluidInput,
            controller };
        start(tiles, machine);
        tick(tiles, machine.mMaxProgresstime + 1);
        MTEHatchOutput tank = (MTEHatchOutput) fluidOutput.getMetaTileEntity();
        check(
            tank.mFluid != null && tank.mFluid.isFluidEqual(Materials.TungstenSteel.getMolten(144))
                && tank.mFluid.amount == 144,
            "Development molten refining did not emit finished molten without a pedestal");
        check(fluid.getFluidAmount() == 0 && items(input, pulp) == 0, "Development molten recipe retained its inputs");
        Block placeholder = world.getBlock(38, 244, 8);
        int metadata = world.getBlockMetadata(38, 244, 8);
        check(placeholder != Blocks.air, "Missing mod left a hole in the structure");
        world.setBlockToAir(38, 244, 8);
        check(!machine.checkStructure(true, controller), "Removing a placeholder left the machine formed");
        player.capabilities.isCreativeMode = false;
        player.inventory.setInventorySlotContents(0, new ItemStack(placeholder, 1, metadata));
        machine.survivalConstruct(
            Brainrotter.controller(),
            100,
            ISurvivalBuildEnvironment.create(IItemSource.fromInventory(player.inventory), player));
        check(machine.checkStructure(true, controller), "Survival construction did not restore the placeholder");
        check(player.inventory.getStackInSlot(0) == null, "Survival construction did not pay for its placeholder");
        world.setBlockToAir(controller.xCoord, controller.yCoord, controller.zCoord);
    }

    static void verify(WorldServer world) {
        verifyCompilation();
        // The public world-entry callback rebuilds GT indexes and derives FoTG's molten recipes.
        MainMod.runOnPlayerJoined(false, false);
        verifyProduction();
        verifyPower(world);
        FakePlayer player = FakePlayerFactory.get(world, new GameProfile(UUID.randomUUID(), "BrainrotterTest"));
        BaseMetaTileEntity tile = place(world, player, Brainrotter.controller(), 40, 240, 8);
        tile.setFrontFacing(ForgeDirection.NORTH);
        MTEIndustrialBrainrotter machine = (MTEIndustrialBrainrotter) tile.getMetaTileEntity();
        machine.construct(Brainrotter.controller(), false);
        Block wood = GameRegistry.findBlock("Forestry", "logsFireproof");
        List<int[]> shell = new ArrayList<>();
        List<int[]> base = new ArrayList<>();
        List<int[]> skulls = new ArrayList<>();
        int logs = 0;
        TilePedestal pedestal = null;
        for (int x = 37; x <= 43; x++) for (int y = 237; y <= 245; y++) for (int z = 5; z <= 11; z++) {
            if (GlassTier.getGlassBlockTier(world.getBlock(x, y, z), world.getBlockMetadata(x, y, z)) != null)
                shell.add(new int[] { x, y, z });
            if (world.getBlock(x, y, z) == Blocks.skull) skulls.add(new int[] { x, y, z });
            if (world.getBlock(x, y, z) == wood) {
                logs++;
                if (y == tile.getYCoord()) base.add(new int[] { x, y, z });
            }
            if (world.getTileEntity(x, y, z) instanceof TilePedestal found) pedestal = found;
            if (world.getBlock(x, y, z) == ConfigBlocks.blockTable) world.setBlockMetadataWithNotify(x, y, z, 1, 2);
        }
        check(
            shell.size() == 36 && logs == 20 && base.size() == 10,
            "Brainrotter did not construct the supplied 5x5x3 design");
        check(skulls.size() == 3, "Brainrotter did not construct its three skulls");
        for (int[] pos : skulls) world.setBlockToAir(pos[0], pos[1], pos[2]);
        player.capabilities.isCreativeMode = false;
        ISurvivalBuildEnvironment environment = ISurvivalBuildEnvironment
            .create(IItemSource.fromInventory(player.inventory), player);
        check(
            machine.survivalConstruct(Brainrotter.controller(), 100, environment) == 0,
            "autobuilder placed skulls without the required items");
        player.inventory.setInventorySlotContents(0, new ItemStack(Items.skull, 3, 1));
        check(
            machine.survivalConstruct(Brainrotter.controller(), 100, environment) == 3,
            "autobuilder did not restore the three skulls");
        check(items(player.inventory, new ItemStack(Items.skull, 1, 1)) == 0, "autobuilder did not consume skulls");
        for (int[] pos : skulls) {
            check(world.getBlock(pos[0], pos[1], pos[2]) == Blocks.skull, "autobuilder left a skull position empty");
            check(
                ((TileEntitySkull) world.getTileEntity(pos[0], pos[1], pos[2])).func_145904_a() == 1,
                "autobuilder changed the supplied skull type");
        }
        check(
            machine.survivalConstruct(Brainrotter.controller(), 100, environment) <= 0,
            "autobuilder rebuilt already present skulls");
        BaseMetaTileEntity inputTile = replace(world, player, ItemList.Hatch_Input_Bus_EV.get(1), base.get(0));
        BaseMetaTileEntity outputTile = replace(world, player, ItemList.Hatch_Output_Bus_EV.get(1), base.get(1));
        BaseMetaTileEntity energy = replace(world, player, ItemList.Hatch_Energy_EV.get(1), base.get(2));
        BaseMetaTileEntity maintenance = replace(world, player, ItemList.Hatch_Maintenance.get(1), base.get(3));
        BaseMetaTileEntity fluidOutputTile = replace(world, player, ItemList.Hatch_Output_EV.get(1), base.get(5));
        MTEHatchOutput fluidOutput = (MTEHatchOutput) fluidOutputTile.getMetaTileEntity();
        glass(world, shell, VoltageIndex.EV);
        check(machine.checkStructure(true, tile), "valid Brainrotter did not form");
        check(pedestal != null && machine.getPedestal() == pedestal, "Brainrotter did not connect its real pedestal");
        pedestal.setInventorySlotContents(0, new ItemStack(Items.stick));
        MTEHatchInputBus input = (MTEHatchInputBus) inputTile.getMetaTileEntity();
        MTEHatchOutputBus output = (MTEHatchOutputBus) outputTile.getMetaTileEntity();
        BaseMetaTileEntity[] tiles = { inputTile, outputTile, energy, maintenance, fluidOutputTile, tile };
        machine.fixAllIssues();
        ItemStack hot = GTOreDictUnificator.get(OrePrefixes.ingotHot, NerfMaterials.rawTungstenSteel(), 1);
        ItemStack pulp = GTOreDictUnificator.get(OrePrefixes.dust, Materials.Wood, 1);
        input.setInventorySlotContents(0, hot.copy());
        input.setInventorySlotContents(1, Brainrotter.compilation(1));
        input.setInventorySlotContents(2, pulp.copy());
        long consumed = tick(tiles, 600);
        check(
            items(input, hot) == 1 && outputs(output) == 0 && consumed == 0,
            "Brainrotter bypassed cooling by processing a hot raw ingot");
        for (int slot = 0; slot < input.getSizeInventory(); slot++) input.setInventorySlotContents(slot, null);
        ItemStack cold = GTOreDictUnificator.get(OrePrefixes.ingot, NerfMaterials.rawTungstenSteel(), 1);
        input.setInventorySlotContents(0, cold.copy());
        input.setInventorySlotContents(1, pulp.copy());
        tick(tiles, 600);
        check(items(input, cold) == 1 && outputs(output) == 0, "missing compilation did not prevent processing");
        for (int slot = 0; slot < input.getSizeInventory(); slot++) input.setInventorySlotContents(slot, null);
        input.setInventorySlotContents(0, cold.copy());
        input.setInventorySlotContents(1, Brainrotter.compilation(1));
        consumed = tick(tiles, 600);
        check(
            items(input, cold) == 1 && outputs(output) == 0 && consumed == 0,
            "generic compilation refined material without the Wood Pulp direction ingredient");
        ItemStack pulpSupply = pulp.copy();
        pulpSupply.stackSize = 64;
        supply(input, pulpSupply);
        consumed = tick(tiles, 600);
        check(
            items(input, cold) == 1 && items(input, pulp) == 64 && outputs(output) == 0 && consumed == 0,
            "occupied pedestal did not prevent resource consumption");
        check(
            pedestal.getStackInSlot(0)
                .getItem() == Items.stick,
            "occupied pedestal item was overwritten");
        pedestal.setInventorySlotContents(0, null);
        consumed = start(tiles, machine);
        check(items(input, cold) == 0, "pedestal display appeared before raw input was consumed");
        check(
            pedestal.getStackInSlot(0) != null && pedestal.getStackInSlot(0)
                .isItemEqual(cold) && pedestal.getStackInSlot(0).stackSize == 1,
            "running recipe did not display one raw ingot on the pedestal");
        ItemStack previousDisplay = pedestal.getStackInSlot(0)
            .copy();
        consumed += tick(tiles, 600);
        check(outputs(output) == 1 && items(input, cold) == 0, "cooled raw ingot was not refined");
        check(pedestal.getStackInSlot(0) == null, "finished recipe left its display item on the pedestal");
        check(items(input, Brainrotter.compilation(1)) == 1, "compilation was consumed");
        check(items(input, pulp) < 64 && items(input, pulp) > 0, "solid refining did not consume Wood Pulp");
        check(consumed > 0, "working Brainrotter did not consume energy");
        tick(tiles, 100);
        check(outputs(output) == 1, "completed recipe duplicated its output");

        int[] broken = shell.get(5);
        world.setBlockToAir(broken[0], broken[1], broken[2]);
        check(!machine.checkStructure(true, tile), "broken Brainrotter remained formed");
        check(machine.getPedestal() == null, "broken structure retained its pedestal connection");
        supply(input, cold.copy());
        consumed = tick(tiles, 100);
        check(items(input, cold) == 1 && outputs(output) == 1, "broken machine processed input");
        check(consumed == 0, "broken machine consumed processing energy");
        glass(world, shell, VoltageIndex.EV);
        check(machine.checkStructure(true, tile), "repaired Brainrotter did not form");
        tile.enableWorking();
        tick(tiles, 600);
        check(outputs(output) == 2, "cold raw ingot was not refined after repairing the structure");
        check(items(input, cold) == 0, "cold raw ingot was not consumed");
        BaseMetaTileEntity fluidTile = replace(world, player, ItemList.Hatch_Input_EV.get(1), base.get(4));
        MTEHatchInput fluid = (MTEHatchInput) fluidTile.getMetaTileEntity();
        tiles = new BaseMetaTileEntity[] { inputTile, outputTile, energy, maintenance, fluidOutputTile, fluidTile,
            tile };
        check(machine.checkStructure(true, tile), "Brainrotter did not form with a fluid input hatch");
        check(
            fluid.fill(
                ForgeDirection.UNKNOWN,
                NerfMaterials.rawTungstenSteel()
                    .getMolten(144),
                true) == 144,
            "raw molten was rejected by the input hatch");
        int pulpBeforeMolten = items(input, pulp);
        consumed = tick(tiles, 600);
        check(
            outputs(output) == 2 && fluidOutput.getFluidAmount() == 144
                && fluidOutput.getFluid()
                    .isFluidEqual(Materials.TungstenSteel.getMolten(144))
                && consumed > 0,
            "molten raw material did not stay molten through refining");
        check(fluid.getFluidAmount() == 0, "molten raw material was not consumed");
        check(items(input, pulp) < pulpBeforeMolten, "molten refining did not consume Wood Pulp");
        check(items(input, Brainrotter.compilation(1)) == 1, "repeated processing consumed the compilation");
        check(pedestal.getStackInSlot(0) == null, "molten recipe left its display item on the pedestal");

        // GT caps batch duration: use an overclocked supply so several inputs fit in one cycle.
        energy = replace(world, player, ItemList.Hatch_Energy_UV.get(1), base.get(2));
        tiles[2] = energy;
        glass(world, shell, VoltageIndex.UV);
        check(machine.checkStructure(true, tile), "Brainrotter did not form with upgraded power");
        ItemStack batch = cold.copy();
        batch.stackSize = 4;
        machine.setBatchMode(true);
        supply(input, batch.copy());
        start(tiles, machine);
        check(items(input, cold) == 0, "batch did not withdraw all of its raw inputs");
        ItemStack savedDisplay = pedestal.getStackInSlot(0)
            .copy();
        check(savedDisplay.stackSize == 1, "batch displayed more than one ingot");
        tile = reload(world, tile, pedestal);
        machine = (MTEIndustrialBrainrotter) tile.getMetaTileEntity();
        tiles[tiles.length - 1] = tile;
        check(machine.checkStructure(true, tile), "loaded Brainrotter did not form");
        pedestal = machine.getPedestal();
        check(
            ItemStack.areItemStacksEqual(savedDisplay, pedestal.getStackInSlot(0)),
            "loading changed the pedestal's active display");
        tick(tiles, machine.mMaxProgresstime + 100);
        check(
            outputs(output) == 6 && pedestal.getStackInSlot(0) == null,
            "saved batch did not resume and release its whole output once");
        tick(tiles, 100);
        check(outputs(output) == 6, "loaded batch duplicated output");

        supply(input, batch.copy());
        start(tiles, machine);
        pedestal.decrStackSize(0, 1);
        tile = reload(world, tile, pedestal);
        machine = (MTEIndustrialBrainrotter) tile.getMetaTileEntity();
        tiles[tiles.length - 1] = tile;
        check(machine.checkStructure(true, tile), "loaded tampered Brainrotter did not form");
        pedestal = machine.getPedestal();
        tick(tiles, machine.mMaxProgresstime + 100);
        check(
            outputs(output) == 6 && pedestal.getStackInSlot(0) == null && !tile.isAllowedToWork(),
            "loading recreated a stolen display or released output from a tampered batch");
        machine.setBatchMode(false);
        int completedOutputs = outputs(output);

        supply(input, cold.copy());
        tile.enableWorking();
        start(tiles, machine);
        ItemStack stolen = pedestal.decrStackSize(0, 1);
        check(stolen != null && stolen.isItemEqual(cold), "pedestal did not expose a raw display ingot");
        tick(tiles, 600);
        check(
            outputs(output) == completedOutputs && machine.mMaxProgresstime == 0
                && !tile.isAllowedToWork()
                && tile.getLastShutDownReason()
                    .wasCritical(),
            "stolen display did not critically stop the cycle before releasing output");
        check(items(input, cold) == 0, "aborted cycle refunded consumed raw material");

        for (ItemStack replacement : new ItemStack[] { cold.copy(), previousDisplay }) {
            supply(input, cold.copy());
            tile.enableWorking();
            start(tiles, machine);
            pedestal.setInventorySlotContents(0, replacement.copy());
            tick(tiles, 600);
            check(
                outputs(output) == completedOutputs && machine.mMaxProgresstime == 0 && !tile.isAllowedToWork(),
                "ordinary or previous-cycle ingot was accepted as the active display");
            check(
                ItemStack.areItemStacksEqual(replacement, pedestal.getStackInSlot(0)),
                "critical stop deleted a replacement item");
            pedestal.setInventorySlotContents(0, null);
        }

        supply(input, cold.copy());
        tile.enableWorking();
        start(tiles, machine);
        energy.setStoredEU(0);
        tile.updateEntityProfiled();
        check(
            machine.mMaxProgresstime == 0 && pedestal.getStackInSlot(0) == null && outputs(output) == completedOutputs,
            "power loss did not discard the pending output and clear the display");

        supply(input, cold.copy());
        tile.enableWorking();
        start(tiles, machine);
        world.setBlockToAir(broken[0], broken[1], broken[2]);
        check(!machine.checkStructure(true, tile), "broken active Brainrotter remained formed");
        tick(tiles, 1);
        check(
            pedestal.getStackInSlot(0) == null && machine.mMaxProgresstime == 0 && outputs(output) == completedOutputs,
            "structure loss left the display behind after disconnecting its pedestal");
        glass(world, shell, VoltageIndex.UV);
        world.setBlockToAir(pedestal.xCoord, pedestal.yCoord, pedestal.zCoord);
        check(
            !machine.checkStructure(true, tile) && machine.getPedestal() == null,
            "missing pedestal did not invalidate the structure");
        world.setBlock(pedestal.xCoord, pedestal.yCoord, pedestal.zCoord, ConfigBlocks.blockStoneDevice, 1, 3);
        check(machine.checkStructure(true, tile), "repaired pedestal did not reconnect");
        supply(input, cold.copy());
        tile.enableWorking();
        start(tiles, machine);
        TilePedestal lastPedestal = machine.getPedestal();
        world.setBlockToAir(tile.xCoord, tile.yCoord, tile.zCoord);
        check(lastPedestal.getStackInSlot(0) == null, "controller removal did not clear its own display");
    }

    private static void verifyProduction() {
        ItemStack steel = GTOreDictUnificator.get(OrePrefixes.dust, Materials.Steel, 1);
        ItemStack tungsten = GTOreDictUnificator.get(OrePrefixes.dust, Materials.Tungsten, 1);
        GTRecipe mixer = find(
            RecipeMaps.mixerRecipes,
            new ItemStack[] { steel, tungsten, GTUtility.getIntegratedCircuit(1) });
        check(mixer != null, "Steel and Tungsten have no mixer recipe");
        ItemStack dust = mixer.getOutput(0);
        check(
            dust.isItemEqual(GTOreDictUnificator.get(OrePrefixes.dust, NerfMaterials.rawTungstenSteel(), 1)),
            "fresh alloy production bypasses Brainrotter: mixer produces finished material");
        GTRecipe multiMixer = find(
            RecipeMaps.mixerNonCellRecipes,
            new ItemStack[] { steel, tungsten, GTUtility.getIntegratedCircuit(1) });
        check(
            multiMixer != null && multiMixer.getOutput(0)
                .isItemEqual(dust),
            "multiblock mixer bypasses Brainrotter by producing finished material");
        RecipeMap<?> quantum = RecipeMap.ALL_RECIPE_MAPS.get("gt.recipe.quantumforcesmelter");
        check(quantum != null, "Quantum Force recipe map is missing");
        for (ItemStack[] inputs : new ItemStack[][] {
            { Materials.Lead.getDust(16), Materials.Bauxite.getDust(32), Materials.Tungstate.getDust(16) },
            { Materials.Rutile.getDust(32), Materials.Scheelite.getDust(16), Materials.Ilmenite.getDust(16) } }) {
            GTRecipe recipe = find(quantum, inputs);
            check(recipe != null, "Quantum Force ore-processing recipe is missing");
            boolean rawOutput = false;
            for (ItemStack output : recipe.mOutputs) {
                check(
                    !output.isItemEqual(GTOreDictUnificator.get(OrePrefixes.dust, Materials.TungstenSteel, 1)),
                    "Quantum Force ore-processing bypasses Brainrotter");
                if (output.isItemEqual(dust)) rawOutput = true;
            }
            check(rawOutput, "Quantum Force ore-processing does not supply raw Tungstensteel");
        }
        GTRecipe blast = find(
            RecipeMaps.blastFurnaceRecipes,
            new ItemStack[] { dust, GTUtility.getIntegratedCircuit(1) });
        check(blast != null, "raw dust has no blast furnace recipe");
        ItemStack hot = blast.getOutput(0);
        check(
            hot.isItemEqual(GTOreDictUnificator.get(OrePrefixes.ingotHot, NerfMaterials.rawTungstenSteel(), 1)),
            "blast furnace refined raw material into finished material");
        GTRecipe cooling = find(RecipeMaps.vacuumFreezerRecipes, new ItemStack[] { hot });
        check(cooling != null, "raw hot ingot has no cooling recipe");
        ItemStack cold = cooling.getOutput(0);
        check(
            cold.isItemEqual(GTOreDictUnificator.get(OrePrefixes.ingot, NerfMaterials.rawTungstenSteel(), 1)),
            "cooling refined raw material into finished material");
        ItemStack pulp = GTOreDictUnificator.get(OrePrefixes.dust, Materials.Wood, 64);
        GTRecipe refined = find(Brainrotter.RECIPES, new ItemStack[] { cold, Brainrotter.compilation(1), pulp });
        check(
            refined != null && refined.getOutput(0)
                .isItemEqual(GTOreDictUnificator.get(OrePrefixes.ingot, Materials.TungstenSteel, 1)),
            "cooled alloy cannot be refined into the material used by IV recipes");
        GTRecipe molten = find(
            TecTechRecipeMaps.godforgeMoltenRecipes,
            new ItemStack[] { dust, GTUtility.getIntegratedCircuit(1) });
        check(
            molten != null && molten.mFluidOutputs.length == 1
                && molten.mFluidOutputs[0].isFluidEqual(
                    NerfMaterials.rawTungstenSteel()
                        .getMolten(1)),
            "FoTG's derived recipe bypasses Brainrotter");
        GTRecipe fluidRefining = find(
            Brainrotter.RECIPES,
            new ItemStack[] { Brainrotter.compilation(1), pulp },
            molten.getFluidOutput(0));
        check(
            fluidRefining != null && fluidRefining.mOutputs.length == 0
                && fluidRefining.mFluidOutputs.length == 1
                && fluidRefining.getFluidOutput(0)
                    .isFluidEqual(Materials.TungstenSteel.getMolten(1)),
            "FoTG molten must refine into finished molten in Brainrotter");
    }

    private static void verifyPower(WorldServer world) {
        FakePlayer player = FakePlayerFactory.get(world, new GameProfile(UUID.randomUUID(), "BrainrotterPower"));
        BaseMetaTileEntity controller = place(world, player, Brainrotter.controller(), 56, 240, 8);
        controller.setFrontFacing(ForgeDirection.NORTH);
        MTEIndustrialBrainrotter machine = (MTEIndustrialBrainrotter) controller.getMetaTileEntity();
        machine.setBatchMode(false);
        machine.construct(Brainrotter.controller(), false);
        Block wood = GameRegistry.findBlock("Forestry", "logsFireproof");
        List<int[]> floor = new ArrayList<>(), shell = new ArrayList<>();
        for (int x = 53; x <= 59; x++) for (int y = 240; y <= 244; y++) for (int z = 5; z <= 11; z++) {
            if (world.getBlock(x, y, z) == wood && y == 240) floor.add(new int[] { x, y, z });
            if (GlassTier.getGlassBlockTier(world.getBlock(x, y, z), world.getBlockMetadata(x, y, z)) != null)
                shell.add(new int[] { x, y, z });
        }
        BaseMetaTileEntity inputTile = replace(world, player, ItemList.Hatch_Input_Bus_EV.get(1), floor.get(0));
        BaseMetaTileEntity outputTile = replace(world, player, ItemList.Hatch_Output_Bus_EV.get(1), floor.get(1));
        BaseMetaTileEntity energy = replace(world, player, ItemList.Hatch_Energy_EV.get(1), floor.get(2));
        BaseMetaTileEntity maintenance = replace(world, player, ItemList.Hatch_Maintenance.get(1), floor.get(3));
        BaseMetaTileEntity fluidOutput = replace(world, player, ItemList.Hatch_Output_EV.get(1), floor.get(4));
        check(!machine.checkStructure(true, controller), "HV glass accepted EV energy");
        glass(world, shell, VoltageIndex.EV);
        check(machine.checkStructure(true, controller), "EV glass did not accept EV energy");
        MTEHatchInputBus input = (MTEHatchInputBus) inputTile.getMetaTileEntity();
        MTEHatchOutputBus output = (MTEHatchOutputBus) outputTile.getMetaTileEntity();
        ItemStack raw = GTOreDictUnificator.get(OrePrefixes.ingot, NerfMaterials.rawTungstenSteel(), 1);
        ItemStack pulp = GTOreDictUnificator.get(OrePrefixes.dust, Materials.Wood, 64);
        ItemStack[] power = { ItemList.Hatch_Energy_EV.get(1), ItemList.Hatch_Energy_IV.get(1),
            ItemList.Hatch_Energy_LuV.get(1), CustomItemList.eM_energyTunnel1_UEV.get(1),
            CustomItemList.eM_energyTunnel1_UXV.get(1) };
        int[] tiers = { VoltageIndex.EV, VoltageIndex.IV, VoltageIndex.LuV, VoltageIndex.UEV, VoltageIndex.UXV };
        int[] limits = { 1, 3, 5, 13, 19 };
        for (int i = 0; i < power.length; i++) {
            energy = replace(world, player, power[i], floor.get(2));
            glass(world, shell, tiers[i]);
            check(
                machine.checkStructure(true, controller),
                "valid glass/energy combination did not form at " + tiers[i]);
            machine.fixAllIssues();
            for (int slot = 0; slot < input.getSizeInventory(); slot++) input.setInventorySlotContents(slot, null);
            input.setInventorySlotContents(0, GTUtility.copyAmount(32, raw));
            input.setInventorySlotContents(1, Brainrotter.compilation(1));
            input.setInventorySlotContents(2, pulp.copy());
            controller.enableWorking();
            BaseMetaTileEntity[] tiles = { inputTile, outputTile, energy, maintenance, fluidOutput, controller };
            start(tiles, machine);
            check(
                32 - items(input, raw) == limits[i],
                "voltage-based parallel limit was not respected at " + tiers[i]
                    + ": consumed "
                    + (32 - items(input, raw)));
            check(
                machine.getPedestal()
                    .getStackInSlot(0).stackSize == 1,
                "parallel cycle displayed multiple ingots");
            machine.stopMachine(ShutDownReasonRegistry.NONE);
        }
        energy = replace(world, player, CustomItemList.eM_energyTunnel1_UV.get(1), floor.get(2));
        glass(world, shell, VoltageIndex.UV);
        check(!machine.checkStructure(true, controller), "laser was accepted below UEV glass");
        energy = replace(world, player, ItemList.Hatch_Energy_IV.get(1), floor.get(2));
        glass(world, shell, VoltageIndex.IV);
        check(machine.checkStructure(true, controller), "IV single-recipe fixture did not form");
        for (int slot = 0; slot < input.getSizeInventory(); slot++) input.setInventorySlotContents(slot, null);
        input.setInventorySlotContents(0, raw.copy());
        input.setInventorySlotContents(1, Brainrotter.compilation(1));
        input.setInventorySlotContents(2, pulp.copy());
        controller.enableWorking();
        BaseMetaTileEntity[] tiles = { inputTile, outputTile, energy, maintenance, fluidOutput, controller };
        start(tiles, machine);
        GTRecipe recipe = find(Brainrotter.RECIPES, new ItemStack[] { raw, Brainrotter.compilation(1), pulp });
        check(
            machine.mMaxProgresstime * 3 <= recipe.mDuration && machine.mMaxProgresstime * 3 >= recipe.mDuration - 3,
            "one overclock did not divide duration by three");
        check(-machine.lEUt == 4L * recipe.mEUt, "one overclock did not multiply EU/t by four");
        tick(tiles, machine.mMaxProgresstime + 1);
        check(outputs(output) == 1, "overclocked recipe did not complete on native ticks");
        world.setBlockToAir(controller.xCoord, controller.yCoord, controller.zCoord);
    }

    private static void glass(WorldServer world, List<int[]> positions, int tier) {
        var glass = GlassTier.getGlassList()
            .stream()
            .filter(block -> GlassTier.getGlassBlockTier(block.getLeft(), block.getRight()) == tier)
            .findFirst()
            .orElseThrow(() -> new AssertionError("No native glass for tier " + tier));
        for (int[] pos : positions) world.setBlock(pos[0], pos[1], pos[2], glass.getLeft(), glass.getRight(), 3);
    }

    private static GTRecipe find(RecipeMap<?> map, ItemStack[] items, FluidStack... fluids) {
        return map.findRecipeQuery()
            .items(items)
            .fluids(fluids)
            .voltage(Long.MAX_VALUE)
            .find();
    }

    private static BaseMetaTileEntity replace(WorldServer world, FakePlayer player, ItemStack item, int[] pos) {
        return place(world, player, item, pos[0], pos[1], pos[2]);
    }

    private static BaseMetaTileEntity reload(WorldServer world, BaseMetaTileEntity controller, TilePedestal pedestal) {
        NBTTagCompound controllerSave = new NBTTagCompound();
        controller.writeToNBT(controllerSave);
        NBTTagCompound pedestalSave = new NBTTagCompound();
        pedestal.writeToNBT(pedestalSave);
        ItemStack beforeUnload = pedestal.getStackInSlot(0);
        beforeUnload = beforeUnload == null ? null : beforeUnload.copy();
        controller.getMetaTileEntity()
            .onUnload();
        check(
            ItemStack.areItemStacksEqual(beforeUnload, pedestal.getStackInSlot(0)),
            "unloading the controller deleted its active display");
        BaseMetaTileEntity loaded = (BaseMetaTileEntity) TileEntity.createAndLoadEntity(controllerSave);
        world.setTileEntity(controller.xCoord, controller.yCoord, controller.zCoord, loaded);
        // Restore both real tiles from their saved NBT, as a chunk load does.
        world.setTileEntity(
            pedestal.xCoord,
            pedestal.yCoord,
            pedestal.zCoord,
            TileEntity.createAndLoadEntity(pedestalSave));
        return loaded;
    }

    private static void supply(IInventory inventory, ItemStack stack) {
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            if (inventory.getStackInSlot(slot) != null) continue;
            inventory.setInventorySlotContents(slot, stack);
            return;
        }
        throw new AssertionError("Brainrotter fixture input bus is full");
    }

    private static BaseMetaTileEntity place(WorldServer world, FakePlayer player, ItemStack item, int x, int y, int z) {
        FixtureWorld.loadAround(world, x, z);
        world.setBlockToAir(x, y, z);
        check(
            ((ItemBlock) item.getItem()).placeBlockAt(item, player, world, x, y, z, 1, 0.5f, 0.5f, 0.5f, 0),
            "could not place Brainrotter fixture item");
        return (BaseMetaTileEntity) world.getTileEntity(x, y, z);
    }

    private static long tick(BaseMetaTileEntity[] tiles, int count) {
        BaseMetaTileEntity energy = tiles[2];
        long consumed = 0;
        for (int i = 0; i < count; i++) {
            // External test power supply replenishes GT's small hatch buffer every tick.
            energy.setStoredEU(energy.getEUCapacity());
            long before = energy.getStoredEU();
            for (BaseMetaTileEntity tile : tiles) tile.updateEntityProfiled();
            consumed += before - energy.getStoredEU();
        }
        return consumed;
    }

    private static long start(BaseMetaTileEntity[] tiles, MTEIndustrialBrainrotter machine) {
        long consumed = 0;
        for (int i = 0; i < 600; i++) {
            consumed += tick(tiles, 1);
            if (machine.mMaxProgresstime > 0) return consumed;
        }
        throw new AssertionError(
            "Brainrotter did not start processing: " + machine.getCheckRecipeResult()
                .getID());
    }

    private static int outputs(MTEHatchOutputBus bus) {
        return items(bus, GTOreDictUnificator.get(OrePrefixes.ingot, Materials.TungstenSteel, 1));
    }

    private static int items(IInventory inventory, ItemStack expected) {
        int count = 0;
        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack != null && stack.isItemEqual(expected)) count += stack.stackSize;
        }
        return count;
    }

    private static void check(boolean success, String detail) {
        if (!success) throw new AssertionError(detail);
    }
}
