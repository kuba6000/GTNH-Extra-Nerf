package pl.kuba6000.gtnhextranerf.integration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.MinecraftException;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.ForgeChunkManager.Ticket;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fluids.FluidStack;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatchEnergyDebug;
import gregtech.api.metatileentity.implementations.MTEHatchMaintenance;
import gregtech.api.util.GTOreDictUnificator;
import pl.kuba6000.gtnhextranerf.machines.IronBathFurnace;
import pl.kuba6000.gtnhextranerf.machines.IronBathStructure;
import pl.kuba6000.gtnhextranerf.machines.MTEIronBathFurnace;
import pl.kuba6000.gtnhextranerf.machines.MTEIronBathTap;
import pl.kuba6000.gtnhextranerf.machines.ironbath.IronBath;
import pl.kuba6000.gtnhextranerf.machines.ironbath.IronProduct;
import pl.kuba6000.gtnhextranerf.materials.IronBathMaterials;
import pl.kuba6000.gtnhextranerf.progression.MachineProgression;
import pl.kuba6000.gtnhextranerf.progression.ProgressionTier;

/** Dedicated-server admission checks and two-run saved furnace verification. */
@GameTestHolder("gtnhextranerfpersistencetests")
public final class IronBathPersistenceScenarios {

    private static final int X = 4096;
    private static final int Y = 100;
    private static final int Z = 4096;
    private static final String PROCESS = UUID.randomUUID()
        .toString();
    private static final String SNAPSHOT = "iron-bath-persistence.nbt";

    private IronBathPersistenceScenarios() {}

    @GameTest(template = "gtnhextranerftests:native_fixture", timeoutTicks = 2400)
    public static void nativeMetalFormsEnterAndMeltInBath(GameTestHelper helper) {
        Materials[] materials = { Materials.Iron, Materials.WroughtIron, Materials.Steel, Materials.CastIron,
            Materials.PigIron, IronBathFurnace.material(IronProduct.OVERCARBURIZED_IRON), Materials.IronMagnetic,
            Materials.SteelMagnetic };
        IronProduct[] products = { IronProduct.IRON, IronProduct.WROUGHT_IRON, IronProduct.STEEL, IronProduct.CAST_IRON,
            IronProduct.PIG_IRON, IronProduct.OVERCARBURIZED_IRON, IronProduct.IRON, IronProduct.STEEL };
        for (int index = 0; index < materials.length; index++) {
            check(
                IronBathFurnace.product(materials[index]) == products[index],
                materials[index] + " does not map to its bath composition");
        }
        WorldServer world = helper.getWorld();
        helper.afterTest(() -> {
            for (int x = IronBathStructure.MIN_X; x <= IronBathStructure.MAX_X; x++)
                for (int y = IronBathStructure.MIN_Y; y <= IronBathStructure.MAX_Y; y++)
                    for (int z = IronBathStructure.MIN_Z; z <= IronBathStructure.MAX_Z; z++)
                        world.setBlockToAir(X + x, Y + y, Z + z);
        });
        MTEIronBathFurnace machine = buildFurnace(helper);
        BaseMetaTileEntity controller = (BaseMetaTileEntity) machine.getBaseMetaTileEntity();
        IInventory input = (IInventory) tile(world, IronBathStructure.INPUT_BUS).getMetaTileEntity();
        ((MTEIronBathTap) tile(world, IronBathStructure.SLAG_TAP).getMetaTileEntity()).setRedstoneMode(3);
        int slot = 0;
        for (Materials material : materials) {
            for (OrePrefixes form : new OrePrefixes[] { OrePrefixes.ingot, OrePrefixes.dust }) {
                ItemStack stack = GTOreDictUnificator.get(form, material, 1);
                check(stack != null, material + " is missing its " + form + " item");
                input.setInventorySlotContents(slot++, stack);
            }
        }
        input.markDirty();
        controller.enableWorking();
        helper.startSequence()
            .thenWaitUntil(() -> helper.assertTrue(state(machine).pendingMetalMb() > 0, "Waiting for native intake"))
            .thenExecute(() -> {
                for (int index = 0; index < 16; index++) {
                    check(input.getStackInSlot(index) == null, "The native bus retained a supported metal form");
                }
                check(state(machine).pendingMetalMb() == 2304, "Native intake lost queued metal");
                check(state(machine).meltingItems() == 16, "Native intake lost a supported metal charge");
            })
            .thenWaitUntil(() -> helper.assertTrue(state(machine).meltingItems() == 0, "Waiting for metal to melt"))
            .thenExecute(() -> {
                IronBath bath = state(machine);
                check(bath.metalMb() == 2304, "Melting changed the admitted metal volume");
                check(Math.abs(bath.carbon() - 26.9) < 0.01, "Melting changed the native materials' carbon content");
            })
            .thenSucceed();
    }

    @GameTest(template = "gtnhextranerftests:native_fixture")
    public static void breakingControllerDropsFreshItem(GameTestHelper helper) {
        WorldServer world = helper.getWorld();
        FixtureWorld.loadAround(world, X, Z);
        helper.afterTest(() -> world.setBlockToAir(X, Y, Z));
        FakePlayer player = FakePlayerFactory.getMinecraft(world);
        ItemStack original = IronBathFurnace.controller();
        BaseMetaTileEntity controller = place(world, player, original, 0, 0, 0);
        MTEIronBathFurnace machine = (MTEIronBathFurnace) controller.getMetaTileEntity();
        NBTTagCompound initial = machineTag(machine);
        NBTTagCompound seed = (NBTTagCompound) initial.copy();
        NBTTagCompound bath = seed.getCompoundTag("IronBath");
        bath.setInteger("Metal", 1440);
        bath.setInteger("Slag", 32);
        bath.setDouble("Carbon", 10);
        bath.setInteger("Calcite", 10);
        IronBath contents = new IronBath(0);
        contents.load(bath);
        bath.setDouble("Heat", contents.heatDemand(IronBath.WORKING_TEMPERATURE));
        machine.loadNBTData(seed);
        controller.setColorization((byte) 5);
        check(state(machine).liquidMetalMb() == 1440, "Pickup fixture is not full of molten metal");
        check(controller.getColorization() == 5, "Pickup fixture is not painted");
        check(bath.equals(machineTag(machine).getCompoundTag("IronBath")), "World save lost loaded bath state");

        var drops = world.getBlock(X, Y, Z)
            .getDrops(world, X, Y, Z, world.getBlockMetadata(X, Y, Z), 0);
        check(
            drops.size() == 1 && drops.get(0)
                .isItemEqual(original),
            "Wrong controller drop");
        ItemStack dropped = drops.get(0);
        check(!dropped.hasTagCompound(), "Broken controller retained item NBT");
        BaseMetaTileEntity replacement = place(world, player, dropped, 0, 0, 0);
        NBTTagCompound replaced = machineTag((MTEIronBathFurnace) replacement.getMetaTileEntity());
        check(initial.equals(replaced), "Replaced controller is not fresh");
        helper.succeed();
    }

    @GameTest(template = "gtnhextranerftests:native_fixture")
    public static void tappingHatchRespectsFluidLockWithEmptyBuffer(GameTestHelper helper) {
        WorldServer world = helper.getWorld();
        FixtureWorld.loadAround(world, X, Z);
        helper.afterTest(() -> world.setBlockToAir(X, Y, Z));
        BaseMetaTileEntity tile = place(world, FakePlayerFactory.getMinecraft(world), IronBathFurnace.tap(1), 0, 0, 0);
        tile.enableWorking();
        MTEIronBathTap tap = (MTEIronBathTap) tile.getMetaTileEntity();
        tap.setRedstoneMode(2);
        int metal = 4 * 144;
        NBTTagCompound contents = new NBTTagCompound();
        contents.setInteger("Metal", metal);
        contents.setDouble("Carbon", 4);
        IronBath bath = new IronBath(0);
        bath.load(contents);
        contents.setDouble("Heat", bath.heatDemand(IronBath.WORKING_TEMPERATURE));
        bath.load(contents);

        FluidStack wroughtIron = IronBathMaterials.wroughtIron()
            .getMolten(1);
        FluidStack steel = Materials.Steel.getMolten(1);
        tap.setLockedFluid(wroughtIron.getFluid());
        tap.lockFluid(true);
        helper.startSequence()
            .thenExecute(() -> {
                check(!tap.discharge(bath, 0), "Empty tapping hatch discharged Steel while locked to Wrought Iron");
                check(tap.getFluidAmount() == 0, "Mismatched fluid reached the hatch buffer");
                check(bath.metalMb() == metal, "A blocked discharge removed metal from the bath");
            })
            .thenIdle(1)
            .thenExecute(() -> {
                tap.setLockedFluid(steel.getFluid());
                check(tap.discharge(bath, 0), "Empty tapping hatch refused its selected Steel");
                FluidStack output = tap.getFluid();
                check(
                    output != null && output.isFluidEqual(steel) && output.amount == 144,
                    "Matching fluid did not fill the hatch");
                check(bath.metalMb() == metal - 144, "Matching discharge removed the wrong amount");
            })
            .thenSucceed();
    }

    @GameTest(template = "gtnhextranerftests:native_fixture", timeoutTicks = 2400)
    public static void fullBathAcceptsCarbonAndDrainsSlag(GameTestHelper helper) {
        WorldServer world = helper.getWorld();
        helper.afterTest(() -> {
            for (int x = IronBathStructure.MIN_X; x <= IronBathStructure.MAX_X; x++)
                for (int y = IronBathStructure.MIN_Y; y <= IronBathStructure.MAX_Y; y++)
                    for (int z = IronBathStructure.MIN_Z; z <= IronBathStructure.MAX_Z; z++)
                        world.setBlockToAir(X + x, Y + y, Z + z);
        });
        MTEIronBathFurnace machine = buildFurnace(helper);
        BaseMetaTileEntity controller = (BaseMetaTileEntity) machine.getBaseMetaTileEntity();
        IInventory input = (IInventory) tile(world, IronBathStructure.INPUT_BUS).getMetaTileEntity();
        MTEIronBathTap upper = (MTEIronBathTap) tile(world, IronBathStructure.SLAG_TAP).getMetaTileEntity();
        MTEIronBathTap lower = (MTEIronBathTap) tile(world, IronBathStructure.METAL_TAP).getMetaTileEntity();
        upper.setRedstoneMode(3);
        lower.setRedstoneMode(3);
        NBTTagCompound saved = machineTag(machine);
        NBTTagCompound bath = saved.getCompoundTag("IronBath");
        bath.setInteger("Metal", 496 * 144);
        bath.setInteger("Slag", 16 * 144);
        IronBath initial = new IronBath(0);
        initial.load(bath);
        bath.setDouble("Heat", initial.heatDemand(2000));
        machine.loadNBTData(saved);
        check(machine.checkStructure(true, controller), "The full bath did not form");
        input.setInventorySlotContents(0, GTOreDictUnificator.get(OrePrefixes.ingot, Materials.Iron, 1));
        input.setInventorySlotContents(1, GTOreDictUnificator.get(OrePrefixes.gem, Materials.Charcoal, 1));
        input.setInventorySlotContents(2, GTOreDictUnificator.get(OrePrefixes.dust, Materials.Calcite, 1));
        controller.enableWorking();
        helper.startSequence()
            .thenWaitUntil(() -> {
                IronBath observed = state(machine);
                helper.assertTrue(observed.impurities() == 10, "Waiting for the full bath to melt Charcoal");
                check(input.getStackInSlot(1) == null, "The full bath did not accept the carbon additive");
                check(input.getStackInSlot(0).stackSize == 1, "The full bath consumed excess Iron");
                check(observed.metalMb() == 496 * 144 && observed.pendingMetalMb() == 0, "Metal exceeded its limit");
                check(observed.occupiedMb() == 512 * 144, "The full bath overflowed");
                check(observed.calciteCapacity() == 20, "Blocked cleanup consumed Calcite");
            })
            .thenExecute(() -> {
                upper.setAmountPerPulse(144);
                upper.setRedstoneMode(2);
            })
            .thenWaitUntil(() -> {
                IronBath observed = state(machine);
                helper.assertTrue(observed.impurities() == 0, "Waiting for native slag drainage and cleanup");
                FluidStack discharged = upper.getFluid();
                check(
                    discharged != null && discharged.isFluidEqual(
                        IronBathFurnace.material(IronProduct.SLAG)
                            .getMolten(1))
                        && discharged.amount >= 320,
                    "The upper tap did not discharge Slag above the full metal bath");
                check(observed.calciteCapacity() == 10, "Cleanup used the wrong amount of Calcite");
                check(observed.occupiedMb() <= observed.capacityMb(), "Slag binding overflowed the native bath");
                lower.setAmountPerPulse(144);
                lower.setRedstoneMode(2);
            })
            .thenWaitUntil(() -> {
                helper.assertTrue(input.getStackInSlot(0) == null, "Waiting for freed capacity to admit Iron");
                check(lower.getFluid() != null, "The lower tap did not resume metal output");
                check(state(machine).occupiedMb() <= 512 * 144, "Native feed exceeded total capacity");
            })
            .thenSucceed();
    }

    @GameTest(template = "gtnhextranerftests:native_fixture", timeoutTicks = 2400)
    public static void preparePersistedBath(GameTestHelper helper) {
        WorldServer world = helper.getWorld();
        helper.afterTest(() -> saveWorld(world));
        check(!snapshotFile(world).exists(), "The preparation run needs a fresh world");
        MTEIronBathFurnace machine = buildFurnace(helper);
        BaseMetaTileEntity controller = (BaseMetaTileEntity) machine.getBaseMetaTileEntity();
        BaseMetaTileEntity input = tile(world, IronBathStructure.INPUT_BUS);
        BaseMetaTileEntity metal = tile(world, IronBathStructure.METAL_TAP);
        BaseMetaTileEntity slag = tile(world, IronBathStructure.SLAG_TAP);

        // Only the initial resident bath is seeded. New charges enter through the native bus.
        NBTTagCompound seed = machineTag(machine);
        NBTTagCompound bath = seed.getCompoundTag("IronBath");
        bath.setInteger("Metal", 384 * 144);
        bath.setInteger("Slag", 320);
        bath.setDouble("Carbon", 384);
        bath.setInteger("Calcite", 80);
        bath.setDouble("Heat", 0);
        IronBath initial = new IronBath(0);
        initial.load(bath);
        bath.setDouble("Heat", initial.heatDemand(IronBath.WORKING_TEMPERATURE));
        machine.loadNBTData(seed);
        check(machine.checkStructure(true, controller), "The seeded bath exceeds its physical coil capacity");
        IInventory bus = (IInventory) input.getMetaTileEntity();
        bus.setInventorySlotContents(0, GTOreDictUnificator.get(OrePrefixes.ingot, Materials.Iron, 4));
        bus.setInventorySlotContents(1, GTOreDictUnificator.get(OrePrefixes.gem, Materials.Coal, 4));
        input.markDirty();
        controller.enableWorking();
        controller.markDirty();
        helper.startSequence()
            .thenWaitUntil(() -> {
                IronBath observed = state(machine);
                helper.assertTrue(
                    partialCharge(observed) && observed.carbon() > 700,
                    "Waiting for partial Iron charges and admitted Coal carbon: loaded="
                        + world.theChunkProviderServer.chunkExists(X >> 4, Z >> 4)
                        + ", invalid="
                        + controller.isInvalid()
                        + ", EU/t="
                        + machine.lEUt
                        + ", pending="
                        + observed.pendingMetalMb());
                helper.assertTrue(machine.lEUt < 0, "The furnace has not drawn native EU");
            })
            .thenExecute(() -> checkpointOnWorldSave(world, controller, machine, metal, slag))
            .thenSucceed();
    }

    @GameTest(template = "gtnhextranerftests:native_fixture", timeoutTicks = 6000)
    public static void verifyPersistedBath(GameTestHelper helper) {
        WorldServer world = helper.getWorld();
        helper.afterTest(() -> saveWorld(world));
        NBTTagCompound expected = readSnapshot(world);
        check(!PROCESS.equals(expected.getString("Process")), "Verification requires a fresh game JVM");
        holdFixtureChunks(helper);
        BaseMetaTileEntity controller = tile(world, 0, 0, 0);
        check(controller.getMetaTileEntity() instanceof MTEIronBathFurnace, "The saved native controller is absent");
        MTEIronBathFurnace machine = (MTEIronBathFurnace) controller.getMetaTileEntity();
        NBTTagCompound actual = machineTag(machine);
        check(
            expected.getCompoundTag("Bath")
                .equals(actual.getCompoundTag("IronBath")),
            "Restart changed resident material, heat, carbon, slag, calcite, charge stacks or charge progress");
        verifyTap(world, IronBathStructure.METAL_TAP, expected.getCompoundTag("MetalTap"));
        verifyTap(world, IronBathStructure.SLAG_TAP, expected.getCompoundTag("SlagTap"));
        for (IronBathStructure.Offset offset : new IronBathStructure.Offset[] { IronBathStructure.ENERGY_A,
            IronBathStructure.ENERGY_B })
            check(
                tile(world, offset).getMetaTileEntity() instanceof MTEHatchEnergyDebug,
                "Restart lost a native debug energy hatch");
        MTEHatchMaintenance maintenance = (MTEHatchMaintenance) tile(world, IronBathStructure.MAINTENANCE)
            .getMetaTileEntity();
        check(maintenance.mAuto, "Restart lost the native automatic maintenance hatch");
        check(
            tile(world, IronBathStructure.INPUT_BUS).getMetaTileEntity() instanceof IInventory,
            "Restart lost the physical input bus");
        check(controller.isAllowedToWork(), "Restart disabled the controller");
        IronBath restored = state(machine);
        check(partialCharge(restored) && restored.carbon() > 700, "The saved partial charge evidence is absent");
        check(
            restored.carbon() > 0 && restored.slagMb() > 0 && restored.calciteCapacity() > 0,
            "The saved constituent evidence is absent");
        int resident = restored.metalMb();
        int pending = restored.pendingMetalMb();
        helper.startSequence()
            .thenWaitUntil(
                () -> helper.assertTrue(
                    machine.checkStructure(true, controller),
                    "Waiting for the reopened native ports to initialize and reform"))
            .thenWaitUntil(() -> {
                IronBath continued = state(machine);
                helper.assertTrue(
                    continued.pendingMetalMb() == 0 && continued.meltingItems() == 0,
                    "Waiting for the restored native charge queue to finish");
                check(continued.metalMb() == resident + pending, "Continuation lost or duplicated the pending Iron");
                check(
                    continued.carbon() > 0 && continued.carbon() <= restored.carbon(),
                    "Continuation lost all carbon or duplicated the admitted Coal");
            })
            .thenSucceed();
    }

    private static MTEIronBathFurnace buildFurnace(GameTestHelper helper) {
        WorldServer world = helper.getWorld();
        holdFixtureChunks(helper);
        check(world.isAirBlock(X, Y, Z), "The persistence fixture location is already occupied");
        for (ProgressionTier tier : ProgressionTier.values()) MachineProgression.certify(world, tier);
        FakePlayer placer = FakePlayerFactory.getMinecraft(world);
        BaseMetaTileEntity controller = place(world, placer, IronBathFurnace.controller(), 0, 0, 0);
        controller.setFrontFacing(ForgeDirection.NORTH);
        MTEIronBathFurnace machine = (MTEIronBathFurnace) controller.getMetaTileEntity();
        machine.construct(IronBathFurnace.controller(), false);
        for (IronBathStructure.Offset coil : IronBathStructure.COIL_POSITIONS)
            place(world, placer, ItemList.Casing_Coil_Cupronickel.get(1), coil.x(), coil.y(), coil.z());
        BaseMetaTileEntity input = port(world, placer, ItemList.Hatch_Input_Bus_HV.get(1), IronBathStructure.INPUT_BUS);
        port(world, placer, ItemList.Hatch_Input_HV.get(1), IronBathStructure.OXYGEN_INPUT);
        BaseMetaTileEntity maintenance = port(
            world,
            placer,
            ItemList.Hatch_AutoMaintenance.get(1),
            IronBathStructure.MAINTENANCE);
        ItemStack[] supplies = MTEHatchMaintenance.getAutoMaintenanceInputs();
        IInventory maintenanceInventory = (IInventory) maintenance.getMetaTileEntity();
        for (int slot = 0; slot < supplies.length; slot++) {
            ItemStack supply = supplies[slot].copy();
            supply.stackSize = 64;
            maintenanceInventory.setInventorySlotContents(slot, supply);
        }
        maintenance.markDirty();
        for (IronBathStructure.Offset offset : new IronBathStructure.Offset[] { IronBathStructure.ENERGY_A,
            IronBathStructure.ENERGY_B }) {
            BaseMetaTileEntity energy = port(world, placer, ItemList.DebugEnergyHatch.get(1), offset);
            MTEHatchEnergyDebug debug = (MTEHatchEnergyDebug) energy.getMetaTileEntity();
            debug.setVoltageTier(3);
            debug.setAmperage(2);
            debug.setRefillInterval(20);
            energy.markDirty();
        }
        BaseMetaTileEntity metal = port(world, placer, IronBathFurnace.tap(1), IronBathStructure.METAL_TAP);
        BaseMetaTileEntity slag = port(world, placer, IronBathFurnace.tap(1), IronBathStructure.SLAG_TAP);
        ((MTEIronBathTap) metal.getMetaTileEntity()).setAmountPerPulse(16);
        ((MTEIronBathTap) metal.getMetaTileEntity()).setRedstoneMode(3);
        ((MTEIronBathTap) slag.getMetaTileEntity()).setAmountPerPulse(37);
        ((MTEIronBathTap) slag.getMetaTileEntity()).setRedstoneMode(1);
        check(machine.checkStructure(true, controller), "The persistence furnace did not form");
        machine.fixAllIssues();

        return machine;
    }

    private static boolean partialCharge(IronBath bath) {
        for (IronBath.ChargeGroup group : bath.chargeGroups(0, bath.chargeGroupCount())) {
            if (group.stack() != null && group.progress() > 0 && group.progress() < 1) return true;
        }
        return false;
    }

    private static void checkpointOnWorldSave(WorldServer world, BaseMetaTileEntity controller,
        MTEIronBathFurnace machine, BaseMetaTileEntity metal, BaseMetaTileEntity slag) {
        check(
            partialCharge(state(machine)) && state(machine).carbon() > 700,
            "Charges finished before the save checkpoint");
        PersistenceCheckpoint checkpoint = new PersistenceCheckpoint(
            world,
            machine,
            (MTEIronBathTap) metal.getMetaTileEntity(),
            (MTEIronBathTap) slag.getMetaTileEntity());
        MinecraftForge.EVENT_BUS.register(checkpoint);
        controller.markDirty();
        saveWorld(world);
        check(snapshotFile(world).isFile(), "The native world-save event did not write a checkpoint");
    }

    /** Tracks the actual final world save, including the orderly server shutdown. */
    public static final class PersistenceCheckpoint {

        private final WorldServer world;
        private final MTEIronBathFurnace machine;
        private final MTEIronBathTap metal;
        private final MTEIronBathTap slag;

        private PersistenceCheckpoint(WorldServer world, MTEIronBathFurnace machine, MTEIronBathTap metal,
            MTEIronBathTap slag) {
            this.world = world;
            this.machine = machine;
            this.metal = metal;
            this.slag = slag;
        }

        @SubscribeEvent
        public void saved(WorldEvent.Save event) {
            if (event.world != world) return;
            NBTTagCompound expected = new NBTTagCompound();
            NBTTagCompound observed = machineTag(machine);
            expected.setString("Process", PROCESS);
            expected.setLong("SavedWorldTick", world.getTotalWorldTime());
            expected.setTag(
                "Bath",
                observed.getCompoundTag("IronBath")
                    .copy());
            expected.setTag("MetalTap", tapTag(metal));
            expected.setTag("SlagTap", tapTag(slag));
            try (FileOutputStream output = new FileOutputStream(snapshotFile(world))) {
                CompressedStreamTools.writeCompressed(expected, output);
            } catch (IOException failure) {
                throw new IllegalStateException("Could not retain the native world-save checkpoint", failure);
            }
        }

        @SubscribeEvent
        public void unloaded(WorldEvent.Unload event) {
            if (event.world == world) MinecraftForge.EVENT_BUS.unregister(this);
        }
    }

    private static void holdFixtureChunks(GameTestHelper helper) {
        WorldServer world = helper.getWorld();
        Object testMod = Loader.instance()
            .getIndexedModList()
            .get("horizonqa")
            .getMod();
        Ticket ticket = ForgeChunkManager.requestTicket(testMod, world, ForgeChunkManager.Type.NORMAL);
        check(ticket != null, "Forge refused a ticket for the remote persistence fixture");
        helper.afterTest(() -> ForgeChunkManager.releaseTicket(ticket));
        check(
            ticket.getChunkListDepth() <= 0 || ticket.getChunkListDepth() >= 9,
            "The fixture ticket cannot hold its nine construction chunks");
        FixtureWorld.loadAround(world, X, Z);
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++)
            ForgeChunkManager.forceChunk(ticket, new ChunkCoordIntPair((X >> 4) + dx, (Z >> 4) + dz));
    }

    private static File snapshotFile(WorldServer world) {
        return new File(
            world.getSaveHandler()
                .getWorldDirectory(),
            SNAPSHOT);
    }

    private static void saveWorld(WorldServer world) {
        try {
            world.saveAllChunks(true, null);
        } catch (MinecraftException failure) {
            throw new IllegalStateException("Could not retain the furnace world after the test", failure);
        }
    }

    private static NBTTagCompound readSnapshot(WorldServer world) {
        try (FileInputStream input = new FileInputStream(snapshotFile(world))) {
            return CompressedStreamTools.readCompressed(input);
        } catch (IOException failure) {
            throw new IllegalStateException("Seed the verification world from the successful preparation run", failure);
        }
    }

    private static NBTTagCompound machineTag(MTEIronBathFurnace machine) {
        NBTTagCompound tag = new NBTTagCompound();
        machine.saveNBTData(tag);
        return tag;
    }

    private static IronBath state(MTEIronBathFurnace machine) {
        IronBath bath = new IronBath(0);
        bath.load(machineTag(machine).getCompoundTag("IronBath"));
        return bath;
    }

    private static NBTTagCompound tapTag(MTEIronBathTap tap) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("Amount", tap.getAmountPerPulse());
        tag.setInteger("Mode", tap.getRedstoneMode());
        return tag;
    }

    private static void verifyTap(WorldServer world, IronBathStructure.Offset offset, NBTTagCompound expected) {
        BaseMetaTileEntity tile = tile(world, offset);
        check(tile.getMetaTileEntity() instanceof MTEIronBathTap, "A saved physical tap is absent");
        check(expected.equals(tapTag((MTEIronBathTap) tile.getMetaTileEntity())), "Restart changed a tap's controls");
    }

    private static BaseMetaTileEntity port(WorldServer world, FakePlayer placer, ItemStack stack,
        IronBathStructure.Offset offset) {
        BaseMetaTileEntity result = place(world, placer, stack, offset.x(), offset.y(), offset.z());
        result.setFrontFacing(ForgeDirection.NORTH);
        return result;
    }

    private static BaseMetaTileEntity place(WorldServer world, FakePlayer placer, ItemStack stack, int dx, int dy,
        int dz) {
        world.setBlockToAir(X + dx, Y + dy, Z + dz);
        check(
            ((ItemBlock) stack.getItem())
                .placeBlockAt(stack, placer, world, X + dx, Y + dy, Z + dz, 1, 0.5f, 0.5f, 0.5f, stack.getItemDamage()),
            "Could not place a persistence fixture block");
        return (BaseMetaTileEntity) world.getTileEntity(X + dx, Y + dy, Z + dz);
    }

    private static BaseMetaTileEntity tile(WorldServer world, IronBathStructure.Offset offset) {
        return tile(world, offset.x(), offset.y(), offset.z());
    }

    private static BaseMetaTileEntity tile(WorldServer world, int dx, int dy, int dz) {
        check(
            world.getTileEntity(X + dx, Y + dy, Z + dz) instanceof BaseMetaTileEntity,
            "A saved fixture tile is absent at " + dx + "," + dy + "," + dz);
        return (BaseMetaTileEntity) world.getTileEntity(X + dx, Y + dy, Z + dz);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
