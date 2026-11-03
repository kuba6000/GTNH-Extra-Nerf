package pl.kuba6000.gtnhextranerf.integration;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.common.util.ForgeDirection;

import com.mojang.authlib.GameProfile;

import bartworks.common.loaders.ItemRegistry;
import bartworks.common.tileentities.multis.MTEManualTrafo;
import bartworks.common.tileentities.tiered.MTEEnergyDistributor;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.MetaTileEntityIDs;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.MTETransformer;
import gregtech.api.metatileentity.implementations.MTEWetTransformer;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTUtility;
import gtPlusPlus.xmod.gregtech.api.metatileentity.implementations.base.MTETransformerHiAmp;
import tectech.thing.metaTileEntity.single.MTEBuckConverter;

/** Public energy ports and real server ticks, including preloaded energy and an NBT-restored tile. */
final class TransformerIntegrationTests {

    private TransformerIntegrationTests() {}

    static void verify(WorldServer world) {
        int normal = 0, wet = 0, highAmp = 0, buck = 0;
        boolean restored = false;
        for (IMetaTileEntity prototype : GregTechAPI.METATILEENTITIES) {
            boolean transformer = prototype instanceof MTETransformer && !(prototype instanceof MTEEnergyDistributor);
            if (!transformer && !(prototype instanceof MTEBuckConverter)) continue;
            if (prototype instanceof MTEBuckConverter) buck++;
            else if (prototype instanceof MTEWetTransformer) wet++;
            else if (prototype instanceof MTETransformerHiAmp) highAmp++;
            else normal++;
            ItemStack item = ((MetaTileEntity) prototype).getStackForm(1);
            BaseMetaTileEntity tile = place(world, item);
            if (!restored) {
                NBTTagCompound save = new NBTTagCompound();
                tile.writeToNBT(save);
                BaseMetaTileEntity loaded = new BaseMetaTileEntity();
                loaded.readFromNBT(save);
                world.setTileEntity(800, 210, 160, loaded);
                tile = loaded;
                restored = true;
            }
            for (int tick = 0; tick < 25; tick++) tile.updateEntityProfiled();
            for (boolean upstep : new boolean[] { false, true }) {
                if (upstep) tile.disableWorking();
                else tile.enableWorking();
                tile.setStoredEU(0);
                long voltage = Math.max(1, Math.min(tile.getInputVoltage(), GTValues.V[1]));
                check(
                    tile.injectEnergyUnits(ForgeDirection.UNKNOWN, voltage, 1) == 0,
                    "Singleblock still accepts unsided energy: " + item);
                check(tile.getStoredEU() == 0, "Disabled singleblock accumulated energy");
                check(!tile.isEnetInput() && !tile.isEnetOutput(), "Disabled singleblock exposes energy network ports");
                for (ForgeDirection side : ForgeDirection.values()) {
                    check(!tile.inputEnergyFrom(side, false) && !tile.inputEnergyFrom(side), "Disabled input face");
                    check(!tile.outputsEnergyTo(side, false) && !tile.outputsEnergyTo(side), "Disabled output face");
                    check(tile.injectEnergyUnits(side, voltage, 1) == 0, "Sided injection bypasses transformer ban");
                }
                long savedEnergy = tile.getEUCapacity() / 2;
                tile.setStoredEU(savedEnergy);
                ForgeDirection output = ((MetaTileEntity) tile.getMetaTileEntity()).isOutputFacing(ForgeDirection.EAST)
                    ? ForgeDirection.EAST
                    : ForgeDirection.NORTH;
                BaseMetaTileEntity receiver = placeAt(
                    world,
                    ItemList.Hatch_Energy_LV.get(1),
                    tile.xCoord + output.offsetX,
                    tile.yCoord,
                    tile.zCoord + output.offsetZ);
                receiver.setFrontFacing(output.getOpposite());
                for (int tick = 0; tick < 25; tick++) receiver.updateEntityProfiled();
                receiver.setStoredEU(0);
                for (int tick = 0; tick < 25; tick++) tile.updateEntityProfiled();
                check(tile.getStoredEU() == savedEnergy, "Disabled singleblock still processes stored energy");
                check(receiver.getStoredEU() == 0, "Disabled singleblock powered its real energy receiver");
                check(
                    tile.getOfferedEnergy() == 0 && tile.demandedEnergyUnits() == 0 && tile.demandsEnergy() == 0,
                    "Legacy energy API bypasses transformer ban");
                check(
                    gregtech.api.interfaces.tileentity.IEnergyConnected.Util.emitEnergyToNetwork(voltage, 1, tile) == 0,
                    "Native network emits from disabled singleblock");
                check(receiver.getStoredEU() == 0, "Explicit network emission bypasses closed ports");
                world.setBlockToAir(receiver.xCoord, receiver.yCoord, receiver.zCoord);
            }
            world.setBlockToAir(800, 210, 160);
        }
        check(normal > 0 && wet > 0 && highAmp > 0 && buck > 0, "A registered transformer family was not exercised");
        ItemStack regular = gregtech.api.enums.ItemList.Transformer_LV_ULV.get(1);
        check(hasRecipe(regular), "Singleblock transformer acquisition recipe was removed");
        IMetaTileEntity converter = java.util.Arrays.stream(GregTechAPI.METATILEENTITIES)
            .filter(prototype -> prototype instanceof MTEBuckConverter)
            .findFirst()
            .orElse(null);
        check(
            converter != null && hasRecipe(((MetaTileEntity) converter).getStackForm(1)),
            "Buck converter acquisition recipe was removed");
        verifyManualTransformer(world);
    }

    private static void verifyManualTransformer(WorldServer world) {
        ItemStack controller = ((MetaTileEntity) GregTechAPI.METATILEENTITIES[MetaTileEntityIDs.ManualTrafo.ID])
            .getStackForm(1);
        ItemStack screw = GTOreDictUnificator.get(OrePrefixes.screw, Materials.Titanium, 1);
        ItemStack core = new ItemStack(ItemRegistry.BW_BLOCKS[2]);
        ItemStack circuit = GTOreDictUnificator.getFirstOre("circuitAdvanced", 1);
        ItemStack crafted = LvRecipeIntegrationTests
            .craft(screw, core, screw, core, ItemList.Hull_HV.get(1), core, circuit, core, circuit);
        check(crafted != null && controller.isItemEqual(crafted), "Native HV Manual Transformer craft is unavailable");
        BaseMetaTileEntity tile = placeAt(world, controller, 880, 210, 160);
        tile.setFrontFacing(ForgeDirection.NORTH);
        MTEManualTrafo machine = (MTEManualTrafo) tile.getMetaTileEntity();
        machine.construct(controller, false);
        List<int[]> bottom = new ArrayList<>(), top = new ArrayList<>();
        for (int x = 876; x <= 884; x++) for (int y = 206; y <= 214; y++) for (int z = 156; z <= 164; z++) {
            if (world.getBlock(x, y, z) != GregTechAPI.sBlockCasings1 || world.getBlockMetadata(x, y, z) != 2) continue;
            (y == tile.yCoord ? bottom : top).add(new int[] { x, y, z });
        }
        check(bottom.size() >= 2 && !top.isEmpty(), "Native Manual Transformer builder omitted hatch positions");
        int[] in = bottom.get(0), maintenance = bottom.get(1), out = top.get(0);
        placeAt(world, ItemList.Hatch_Maintenance.get(1), maintenance[0], maintenance[1], maintenance[2]);
        for (boolean upstep : new boolean[] { true, false }) {
            BaseMetaTileEntity energy = placeAt(
                world,
                (upstep ? ItemList.Hatch_Energy_LV : ItemList.Hatch_Energy_MV).get(1),
                in[0],
                in[1],
                in[2]);
            BaseMetaTileEntity dynamo = placeAt(
                world,
                (upstep ? ItemList.Hatch_Dynamo_MV : ItemList.Hatch_Dynamo_LV).get(1),
                out[0],
                out[1],
                out[2]);
            machine.setInventorySlotContents(1, GTUtility.getIntegratedCircuit(upstep ? 0 : 1));
            check(machine.checkStructure(true, tile), "Native Manual Transformer structure did not form");
            check(
                machine.checkProcessing()
                    .wasSuccessful(),
                "Native Manual Transformer rejected voltage conversion");
            energy.setStoredEU(energy.getEUCapacity());
            dynamo.setStoredEU(0);
            long before = energy.getStoredEU();
            // Exercise the native transfer in its fully efficient operating state.
            machine.mEfficiency = 10000;
            check(machine.onRunningTick(null), "Native Manual Transformer stopped transferring");
            check(
                energy.getStoredEU() < before && dynamo.getStoredEU() > 0,
                "Manual Transformer did not consume and output energy");
            world.setBlockToAir(energy.xCoord, energy.yCoord, energy.zCoord);
            world.setBlockToAir(dynamo.xCoord, dynamo.yCoord, dynamo.zCoord);
        }
        world.setBlockToAir(tile.xCoord, tile.yCoord, tile.zCoord);
    }

    private static boolean hasRecipe(ItemStack output) {
        for (Object entry : CraftingManager.getInstance()
            .getRecipeList()) {
            ItemStack result = ((IRecipe) entry).getRecipeOutput();
            if (result != null && result.isItemEqual(output)) return true;
        }
        return RecipeMaps.assemblerRecipes.getAllRecipes()
            .stream()
            .anyMatch(
                recipe -> java.util.Arrays.stream(recipe.mOutputs)
                    .anyMatch(output::isItemEqual));
    }

    static BaseMetaTileEntity place(WorldServer world, ItemStack item) {
        return placeAt(world, item, 800, 210, 160);
    }

    static BaseMetaTileEntity placeAt(WorldServer world, ItemStack item, int x, int y, int z) {
        FakePlayer player = FakePlayerFactory.get(world, new GameProfile(UUID.randomUUID(), "TransformerTest"));
        FixtureWorld.loadAround(world, x, z);
        world.setBlockToAir(x, y, z);
        check(
            ((ItemBlock) item.getItem()).placeBlockAt(item, player, world, x, y, z, 1, .5f, .5f, .5f, 0),
            "Could not place native transformer");
        return (BaseMetaTileEntity) world.getTileEntity(x, y, z);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
