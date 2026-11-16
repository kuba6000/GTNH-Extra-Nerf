package pl.kuba6000.gtnhextranerf.integration;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityDropper;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.world.MinecraftException;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.covers.CoverRegistry;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.interfaces.tileentity.ICoverable;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.BaseMetaPipeEntity;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.MetaPipeEntity;
import gregtech.api.metatileentity.implementations.MTEBasicMachine;
import gregtech.api.metatileentity.implementations.MTEBuffer;
import gregtech.api.metatileentity.implementations.MTEFluidPipe;
import gregtech.api.metatileentity.implementations.MTEHatchInputBus;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.common.covers.Cover;
import gregtech.common.covers.CoverFluidRegulator;
import gregtech.common.covers.CoverFluidfilter;
import gregtech.common.covers.CoverItemMeter;
import gregtech.common.covers.CoverLegacyData;
import gregtech.common.covers.CoverLiquidMeter;
import gregtech.common.covers.CoverShutter;
import gregtech.common.covers.modes.ShutterMode;
import gregtech.common.tileentities.automation.MTEItemDistributor;
import gtPlusPlus.xmod.gregtech.api.enums.GregtechItemList;
import pl.kuba6000.gtnhextranerf.machines.MTEIronBathTap;
import pl.kuba6000.gtnhextranerf.materials.IronBathMaterials;
import tectech.thing.CustomItemList;
import tectech.thing.metaTileEntity.single.MTEDebugPowerGenerator;

/** Constructs native persistent lab hardware. It never runs a furnace control tick. */
public final class IronBathRedstoneLab {

    private IronBathRedstoneLab() {}

    public static final class Position {

        public final int x;
        public final int y;
        public final int z;

        Position(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public int x() {
            return x;
        }

        public int y() {
            return y;
        }

        public int z() {
            return z;
        }

        Position offset(ForgeDirection side) {
            return new Position(x + side.offsetX, y + side.offsetY, z + side.offsetZ);
        }

        @Override
        public String toString() {
            return x + "," + y + "," + z;
        }
    }

    public static final class Lab {

        private final Position stopLever;
        private final List<BaseMetaTileEntity> sampleTanks;
        private final List<Position> pipePositions;
        private final Map<String, Position> observedParts;
        private final BaseMetaTileEntity lowTap;
        private boolean failureSnapshotSaved;

        private final Hardware hardware;
        private final IronBathNativeCircuit circuit;

        Lab(Hardware hardware, IronBathNativeCircuit circuit) {
            this.hardware = hardware;
            this.circuit = circuit;
            stopLever = hardware.stopLever;
            sampleTanks = Collections.unmodifiableList(new ArrayList<>(hardware.sampleTanks));
            pipePositions = Collections.unmodifiableList(new ArrayList<>(hardware.fluidPipes));
            observedParts = Collections.unmodifiableMap(new LinkedHashMap<>(circuit.observations()));
            lowTap = hardware.lowTap;
        }

        public List<BaseMetaTileEntity> recyclingAccumulators() {
            return hardware.recyclingAccumulators;
        }

        public List<BaseMetaTileEntity> productStores() {
            return hardware.productStores;
        }

        public int stockCount(int index) {
            BaseMetaTileEntity store = hardware.productStores.get(index);
            int count = 0;
            for (int slot = 0; slot < store.getSizeInventory(); slot++) {
                ItemStack stack = store.getStackInSlot(slot);
                if (store.isValidSlot(slot) && stack != null) count += stack.stackSize;
            }
            return count;
        }

        public int sampledRank() {
            return circuit.sampledRank();
        }

        public Position stopLever() {
            return stopLever;
        }

        public Position ironSupply() {
            return new Position(
                hardware.freshSource.getXCoord(),
                hardware.freshSource.getYCoord(),
                hardware.freshSource.getZCoord());
        }

        public List<BaseMetaTileEntity> sampleTanks() {
            return sampleTanks;
        }

        public List<Position> pipePositions() {
            return pipePositions;
        }

        /** Observes native parts and already-extracted fluids without changing the controller. */
        public String diagnostics(WorldServer world) {
            StringBuilder result = new StringBuilder("tick=").append(world.getTotalWorldTime())
                .append(" STOPmeta=")
                .append(world.getBlockMetadata(stopLever.x, stopLever.y, stopLever.z));
            for (Map.Entry<String, Position> entry : observedParts.entrySet()) {
                Position p = entry.getValue();
                TileEntity tile = world.getTileEntity(p.x, p.y, p.z);
                result.append('\n')
                    .append(entry.getKey())
                    .append('@')
                    .append(p)
                    .append('=');
                if (tile == null) {
                    result.append("missing");
                    continue;
                }
                NBTTagCompound tag = new NBTTagCompound();
                tile.writeToNBT(tag);
                NBTTagList parts = tag.getTagList("parts", 10);
                for (int index = 0; index < parts.tagCount(); index++) result.append(parts.getCompoundTagAt(index));
            }
            result.append("\ntap redstone:");
            for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) result.append(' ')
                .append(side)
                .append('=')
                .append(lowTap.getInternalInputRedstoneSignal(side));
            result.append(" fluid=")
                .append(fluidContents(lowTap));
            for (int index = 0; index < sampleTanks.size(); index++) {
                BaseMetaTileEntity tank = sampleTanks.get(index);
                result.append("\nsample[")
                    .append(index)
                    .append("]=")
                    .append(fluidContents(tank))
                    .append(" stock=")
                    .append(
                        fluidContents(world.getTileEntity(tank.getXCoord(), tank.getYCoord() + 1, tank.getZCoord())));
            }
            result.append("\nsampledRank=")
                .append(sampledRank())
                .append(" stocks=");
            for (int index = 0; index < hardware.productStores.size(); index++) result.append(index)
                .append(':')
                .append(stockCount(index))
                .append(' ');
            result.append("\nnonempty pipes:");
            for (Position p : pipePositions) {
                String contents = fluidContents(world.getTileEntity(p.x, p.y, p.z));
                if (!contents.equals("empty")) result.append(' ')
                    .append(p)
                    .append('=')
                    .append(contents);
            }
            return result.toString();
        }

        /** Explicit test checkpoint, never called by the native circuit's operating logic. */
        public void saveCheckpoint(WorldServer world) {
            try {
                world.saveAllChunks(true, null);
                Files.write(
                    Paths.get("horizonqa-iron-bath-circuit-state.txt"),
                    diagnostics(world).getBytes(StandardCharsets.UTF_8));
            } catch (MinecraftException | IOException failure) {
                throw new IllegalStateException("Could not save the native circuit checkpoint", failure);
            }
        }

        public void saveFailureSnapshot(WorldServer world) {
            if (failureSnapshotSaved) return;
            saveCheckpoint(world);
            failureSnapshotSaved = true;
        }

        private static String fluidContents(TileEntity tile) {
            if (!(tile instanceof IFluidHandler handler)) return "missing fluid handler";
            StringBuilder result = new StringBuilder();
            FluidTankInfo[] info = handler.getTankInfo(ForgeDirection.UNKNOWN);
            if (info != null) for (FluidTankInfo tank : info) {
                if (tank == null || tank.fluid == null || tank.fluid.amount == 0) continue;
                if (result.length() > 0) result.append(',');
                result.append(
                    tank.fluid.getFluid()
                        .getName())
                    .append(':')
                    .append(tank.fluid.amount);
            }
            return result.length() == 0 ? "empty" : result.toString();
        }
    }

    public static final class Hardware {

        public final WorldServer world;
        public final int x, y, z;
        public BaseMetaTileEntity lowTap, upperTap, oxygenSupply, freshSource, returnSource, productionOutlet;
        public final List<BaseMetaTileEntity> sampleTanks = new ArrayList<>();
        public final List<BaseMetaTileEntity> recyclingAccumulators = new ArrayList<>();
        public final List<BaseMetaTileEntity> productStores = new ArrayList<>();
        public final List<Position> fluidPipes = new ArrayList<>();
        public Position cokeDropper, stopLever;

        Hardware(WorldServer world, int x, int y, int z) {
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    public static Lab build(WorldServer world, int x, int y, int z, BaseMetaTileEntity lowTap,
        BaseMetaTileEntity upperTap, BaseMetaTileEntity inputBus, BaseMetaTileEntity oxygen) {
        return build(world, x, y, z, lowTap, upperTap, inputBus, oxygen, 32, true);
    }

    public static Lab build(WorldServer world, int x, int y, int z, BaseMetaTileEntity lowTap,
        BaseMetaTileEntity upperTap, BaseMetaTileEntity inputBus, BaseMetaTileEntity oxygen, int stockTarget,
        boolean prefillStores) {
        z += 8;
        Hardware h = new Hardware(world, x, y, z);
        h.lowTap = lowTap;
        h.upperTap = upperTap;
        FakePlayer placer = FakePlayerFactory.getMinecraft(world);
        Materials[] products = { Materials.Iron, IronBathMaterials.wroughtIron(), Materials.Steel, Materials.CastIron,
            Materials.PigIron, IronBathMaterials.overcarburizedIron(), IronBathMaterials.slag() };
        int[][] header = { { -3, 2 }, { -4, 2 }, { -5, 2 }, { -5, 3 }, { -6, 3 }, { -7, 3 }, { -8, 3 }, { -8, 2 },
            { -9, 2 }, { -10, 2 }, { -11, 2 }, { -11, 3 } };
        for (int[] cell : header) pipe(world, placer, h.fluidPipes, x + cell[0], y + 2, z + cell[1]);
        attach(lowTap, ForgeDirection.NORTH, ItemList.Electric_Pump_LV.get(1));
        List<Position> returns = new ArrayList<>();
        int[][] samplePorts = { { -5, 1 }, { -5, 4 }, { -8, 4 }, { -8, 1 }, { -11, 1 }, { -11, 4 } };
        int[] branchIndices = { 2, 3, 6, 7, 10, 11 };
        for (int i = 0; i < samplePorts.length; i++) {
            int tx = x + samplePorts[i][0];
            int tz = z + samplePorts[i][1];
            ForgeDirection inlet = samplePorts[i][1] == 1 ? ForgeDirection.SOUTH : ForgeDirection.NORTH;
            BaseMetaTileEntity tank = (BaseMetaTileEntity) place(
                world,
                placer,
                GregtechItemList.GTFluidTank_LV.get(1),
                tx,
                y + 2,
                tz);
            tank.setFrontFacing(inlet);
            fluidFilter(tank, inlet, products[i], false);
            CoverLiquidMeter detector = (CoverLiquidMeter) attach(
                tank,
                inlet.getOpposite(),
                ItemList.Cover_FluidDetector.get(1));
            detector.setThreshold(1);
            detector.setTickRateAddition(4);
            ((CoverLegacyData) attach(tank, ForgeDirection.UP, ItemList.Electric_Pump_LV.get(1))).setVariable(2);
            ((CoverLegacyData) attach(tank, ForgeDirection.DOWN, ItemList.Cover_Controller.get(1))).setVariable(0);
            tank.disableWorking();
            h.sampleTanks.add(tank);
            BaseMetaTileEntity recycler = caster(world, placer, tx, y + 3, tz);
            recycler.setFrontFacing(inlet);
            h.recyclingAccumulators.add(recycler);
            itemPipe(world, placer, returns, tx, y + 3, tz + inlet.offsetZ);
            if (i + 1 < samplePorts.length) {
                Position branch = h.fluidPipes.get(branchIndices[i]);
                Position next = h.fluidPipes.get(branchIndices[i] + 1);
                ForgeDirection upstream = branch.x > next.x ? ForgeDirection.EAST
                    : branch.z > next.z ? ForgeDirection.SOUTH : ForgeDirection.NORTH;
                fluidFilter((ICoverable) world.getTileEntity(next.x, next.y, next.z), upstream, products[i], true);
            }
        }
        BaseMetaTileEntity slagSample = (BaseMetaTileEntity) place(
            world,
            placer,
            GregtechItemList.GTFluidTank_LV.get(1),
            x - 12,
            y + 2,
            z + 3);
        slagSample.setFrontFacing(ForgeDirection.EAST);
        fluidFilter(slagSample, ForgeDirection.EAST, products[6], false);
        ((CoverLiquidMeter) attach(slagSample, ForgeDirection.WEST, ItemList.Cover_FluidDetector.get(1)))
            .setThreshold(1)
            .setTickRateAddition(4);
        attach(slagSample, ForgeDirection.UP, ItemList.Electric_Pump_LV.get(1));
        h.sampleTanks.add(slagSample);
        place(world, placer, GregtechItemList.GTFluidTank_LV.get(1), x - 12, y + 3, z + 3);
        List<Position> sampleAdapter = new ArrayList<>();
        fluidRoute(
            world,
            placer,
            sampleAdapter,
            new Position(lowTap.getXCoord(), lowTap.getYCoord(), lowTap.getZCoord()),
            new Position(lowTap.getXCoord(), lowTap.getYCoord(), z - 11),
            new Position(x - 3, lowTap.getYCoord(), z - 11),
            new Position(x - 3, y + 2, z - 11),
            new Position(x - 3, y + 2, z + 1));
        h.fluidPipes.addAll(0, sampleAdapter);
        for (int i = 0; i < h.fluidPipes.size(); i++) {
            Position position = h.fluidPipes.get(i);
            Position previous = i == 0 ? new Position(lowTap.getXCoord(), lowTap.getYCoord(), lowTap.getZCoord())
                : h.fluidPipes.get(i - 1);
            IGregTechTileEntity tile = (IGregTechTileEntity) world.getTileEntity(position.x, position.y, position.z);
            MTEFluidPipe conduit = (MTEFluidPipe) tile.getMetaTileEntity();
            ForgeDirection upstream = previous.x > position.x ? ForgeDirection.EAST
                : previous.x < position.x ? ForgeDirection.WEST
                    : previous.y > position.y ? ForgeDirection.UP
                        : previous.y < position.y ? ForgeDirection.DOWN
                            : previous.z > position.z ? ForgeDirection.SOUTH : ForgeDirection.NORTH;
            conduit.mDisableInput = (byte) (63 & ~upstream.flag);
        }
        connect(world, h.fluidPipes);
        h.freshSource = (BaseMetaTileEntity) place(
            world,
            placer,
            ItemList.Automation_ItemDistributor_LV.get(1),
            x - 10,
            y,
            z + 10);
        h.freshSource.setFrontFacing(ForgeDirection.NORTH);
        // The distributor honors a work-enabled edge without the ChestBuffer's 200-tick transfer cadence.
        ((MTEItemDistributor) h.freshSource.getMetaTileEntity())
            .setItemsPerSide(ForgeDirection.SOUTH.ordinal(), (byte) 16);
        fillInventory(h.freshSource, GTOreDictUnificator.get(OrePrefixes.ingot, Materials.Iron, 64));
        h.returnSource = buffer(world, placer, x - 6, y, z + 10, 1);
        BaseMetaTileEntity calcite = buffer(world, placer, x - 2, y, z + 10, 1);
        fillInventory(calcite, GTOreDictUnificator.get(OrePrefixes.dust, Materials.Calcite, 64));
        attach(h.freshSource, ForgeDirection.WEST, ItemList.Cover_Controller.get(1));
        h.freshSource.disableWorking();
        h.returnSource.enableWorking();
        List<Position> admission = new ArrayList<>();
        for (int px : new int[] { x - 10, x - 6, x - 2 }) {
            for (int py = y; py >= y - 2; py--) itemPipe(world, placer, admission, px, py, z + 11);
            itemPipe(world, placer, admission, px, y - 2, z + 12);
        }
        for (int px = x - 10; px <= x - 1; px++) itemPipe(world, placer, admission, px, y - 2, z + 12);
        for (int pz = inputBus.getZCoord() - 3; pz < z + 12; pz++) itemPipe(world, placer, admission, x - 1, y - 2, pz);
        for (int px = x - 1; px <= inputBus.getXCoord(); px++)
            itemPipe(world, placer, admission, px, y - 2, inputBus.getZCoord() - 3);
        for (int py = y - 1; py <= inputBus.getYCoord(); py++)
            itemPipe(world, placer, admission, inputBus.getXCoord(), py, inputBus.getZCoord() - 3);
        for (int pz = inputBus.getZCoord() - 2; pz < inputBus.getZCoord(); pz++)
            itemPipe(world, placer, admission, inputBus.getXCoord(), inputBus.getYCoord(), pz);
        inputBus.setFrontFacing(ForgeDirection.NORTH);
        ((MTEHatchInputBus) inputBus.getMetaTileEntity()).disableLimited = false;
        for (int px = x - 11; px <= x - 5; px++) itemPipe(world, placer, returns, px, y + 3, z + 2);
        itemPipe(world, placer, returns, x - 6, y + 2, z + 2);
        for (int pz = z + 2; pz <= z + 10; pz++) itemPipe(world, placer, returns, x - 6, y + 1, pz);
        ((CoverShutter) attach(
            (ICoverable) world.getTileEntity(x - 11, y + 3, z + 3),
            ForgeDirection.WEST,
            ItemList.Cover_Shutter.get(1))).setShutterMode(ShutterMode.ONLY_INPUT);
        ((CoverShutter) attach(
            (ICoverable) world.getTileEntity(x - 6, y + 2, z + 2),
            ForgeDirection.EAST,
            ItemList.Cover_Shutter.get(1))).setShutterMode(ShutterMode.ONLY_INPUT);
        connect(world, returns);
        h.cokeDropper = new Position(x - 10, y, z + 14);
        dropper(
            world,
            h.cokeDropper.x,
            h.cokeDropper.y,
            h.cokeDropper.z,
            ForgeDirection.DOWN.ordinal(),
            new ItemStack(GameRegistry.findItem("Railcraft", "fuel.coke"), 64));
        itemPipe(world, placer, admission, x - 10, y - 1, z + 14);
        ((CoverShutter) attach(
            (ICoverable) world.getTileEntity(x - 10, y - 1, z + 14),
            ForgeDirection.UP,
            ItemList.Cover_Shutter.get(1))).setShutterMode(ShutterMode.ONLY_INPUT);
        itemPipe(world, placer, admission, x - 10, y - 2, z + 14);
        itemPipe(world, placer, admission, x - 10, y - 2, z + 13);
        connect(world, admission);
        MetaPipeEntity cokeEntry = (MetaPipeEntity) ((IGregTechTileEntity) world.getTileEntity(x - 10, y - 1, z + 14))
            .getMetaTileEntity();
        cokeEntry.connect(ForgeDirection.UP);
        if (!cokeEntry.isConnectedAtSide(ForgeDirection.UP) || !cokeEntry.isConnectedAtSide(ForgeDirection.DOWN))
            throw new IllegalStateException("The Coke entry pipe must connect the dropper to the admission network");
        oxygen.setFrontFacing(ForgeDirection.NORTH);
        h.oxygenSupply = (BaseMetaTileEntity) place(
            world,
            placer,
            GregtechItemList.GTFluidTank_LV.get(1),
            oxygen.getXCoord(),
            oxygen.getYCoord(),
            oxygen.getZCoord() - 1);
        if (h.oxygenSupply.getMetaTileEntity()
            .fill(ForgeDirection.UNKNOWN, Materials.Oxygen.getGas(64000), true) != 64000)
            throw new IllegalStateException("Native oxygen reserve did not accept 64000 mB");
        CoverFluidRegulator oxygenMeter = (CoverFluidRegulator) attach(
            h.oxygenSupply,
            ForgeDirection.SOUTH,
            ItemList.FluidRegulator_LV.get(1));
        oxygenMeter.setSpeed(1);
        oxygenMeter.setTickRateForUi(1);
        oxygenMeter.setCondition(CoverFluidRegulator.Conditional.Conditional);
        attach(h.oxygenSupply, ForgeDirection.DOWN, ItemList.Cover_Controller.get(1));
        h.oxygenSupply.disableWorking();
        List<Position> production = new ArrayList<>();
        for (int pz = z + 2; pz <= z + 5; pz++) pipe(world, placer, production, x + 4, y + 3, pz);
        attach(upperTap, ForgeDirection.NORTH, ItemList.Electric_Pump_LV.get(1));
        h.productionOutlet = (BaseMetaTileEntity) place(
            world,
            placer,
            GregtechItemList.GTFluidTank_LV.get(1),
            x + 4,
            y + 3,
            z + 6);
        CoverFluidRegulator regulator = (CoverFluidRegulator) attach(
            h.productionOutlet,
            ForgeDirection.DOWN,
            ItemList.FluidRegulator_LV.get(1));
        regulator.setSpeed(64);
        regulator.setTickRateForUi(1);
        for (int py = y + 2; py >= y - 1; py--) pipe(world, placer, production, x + 4, py, z + 6);
        for (int pz = z + 7; pz <= z + 21; pz++) pipe(world, placer, production, x + 4, y - 1, pz);
        for (int px = x + 5; px <= x + 31; px++) pipe(world, placer, production, px, y - 1, z + 21);
        List<Position> stockPipes = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            int tx = x + 12 + i * 3;
            caster(world, placer, tx, y, z + 22);
            pipe(world, placer, production, tx, y, z + 21);
            fluidFilter((ICoverable) world.getTileEntity(tx, y, z + 21), ForgeDirection.DOWN, products[i], false);
            itemPipe(world, placer, stockPipes, tx, y, z + 23);
            BaseMetaTileEntity store = buffer(world, placer, tx, y, z + 24, 1);
            store.disableWorking();
            if (prefillStores && i != 2)
                store.setInventorySlotContents(0, GTOreDictUnificator.get(OrePrefixes.ingot, products[i], stockTarget));
            ((CoverItemMeter) attach(store, ForgeDirection.WEST, ItemList.Cover_ItemDetector.get(1)))
                .setThreshold(stockTarget)
                .setInverted(true);
            h.productStores.add(store);
            fluidFilter(
                (ICoverable) world.getTileEntity(tx + 1, y - 1, z + 21),
                ForgeDirection.WEST,
                products[i],
                true);
        }
        for (int i = 5; i < 7; i++) {
            int tx = x + 28 + (i - 5) * 3;
            BaseMetaTileEntity recovery = (BaseMetaTileEntity) place(
                world,
                placer,
                ItemList.Hatch_Input_LV.get(1),
                tx,
                y - 1,
                z + 22);
            recovery.setFrontFacing(ForgeDirection.NORTH);
            fluidFilter(recovery, ForgeDirection.NORTH, products[i], false);
            if (i == 5) fluidFilter(
                (ICoverable) world.getTileEntity(tx + 1, y - 1, z + 21),
                ForgeDirection.WEST,
                products[i],
                true);
        }
        for (Position position : production) {
            IGregTechTileEntity tile = (IGregTechTileEntity) world.getTileEntity(position.x, position.y, position.z);
            MTEFluidPipe conduit = (MTEFluidPipe) tile.getMetaTileEntity();
            ForgeDirection upstream = position.y == y + 3 ? ForgeDirection.NORTH
                : position.z == z + 6 ? ForgeDirection.UP
                    : position.x == x + 4 ? ForgeDirection.NORTH
                        : position.y == y ? ForgeDirection.DOWN : ForgeDirection.WEST;
            conduit.mDisableInput = (byte) (63 & ~upstream.flag);
        }
        List<Position> bulkAdapter = new ArrayList<>();
        fluidRoute(
            world,
            placer,
            bulkAdapter,
            new Position(upperTap.getXCoord(), upperTap.getYCoord(), upperTap.getZCoord()),
            new Position(upperTap.getXCoord(), upperTap.getYCoord(), z - 12),
            new Position(x + 13, upperTap.getYCoord(), z - 12),
            new Position(x + 13, y + 3, z - 12),
            new Position(x + 13, y + 3, z + 1),
            new Position(x + 4, y + 3, z + 1));
        production.addAll(0, bulkAdapter);
        connect(world, production);
        connect(world, stockPipes);
        h.fluidPipes.addAll(production);
        powerRow(world, placer, x - 11, x - 5, y + 4, z + 1);
        powerRow(world, placer, x - 11, x - 5, y + 4, z + 4);
        powerRow(world, placer, x + 12, x + 24, y - 1, z + 22);
        for (int px : new int[] { x - 10, x - 6, x - 2 }) generator(world, placer, px, y, z + 9);
        h.stopLever = new Position(x - 10, y + 1, z + 18);
        world.setBlock(h.stopLever.x, y, h.stopLever.z, Blocks.stonebrick, 0, 3);
        world.setBlock(h.stopLever.x, h.stopLever.y, h.stopLever.z, Blocks.lever, 13, 3);
        ((MTEIronBathTap) lowTap.getMetaTileEntity()).setAmountPerPulse(1);
        ((MTEIronBathTap) lowTap.getMetaTileEntity()).setRedstoneMode(0);
        ((MTEIronBathTap) upperTap.getMetaTileEntity()).setAmountPerPulse(16 * 144);
        ((MTEIronBathTap) upperTap.getMetaTileEntity()).setRedstoneMode(1);
        IronBathNativeCircuit circuit = IronBathNativeCircuit.build(h);
        connect(world, h.fluidPipes);
        connect(world, returns);
        connect(world, admission);
        connect(world, stockPipes);
        return new Lab(h, circuit);
    }

    private static void fluidRoute(WorldServer world, FakePlayer placer, List<Position> pipes, Position... bends) {
        Position previous = bends[0];
        for (int index = 1; index < bends.length; index++) {
            Position end = bends[index];
            int dx = Integer.signum(end.x - previous.x), dy = Integer.signum(end.y - previous.y),
                dz = Integer.signum(end.z - previous.z);
            if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) != 1)
                throw new IllegalArgumentException("A fluid adapter must specify each bend");
            while (previous.x != end.x || previous.y != end.y || previous.z != end.z) {
                Position next = new Position(previous.x + dx, previous.y + dy, previous.z + dz);
                pipe(world, placer, pipes, next.x, next.y, next.z);
                MTEFluidPipe conduit = (MTEFluidPipe) ((IGregTechTileEntity) world
                    .getTileEntity(next.x, next.y, next.z)).getMetaTileEntity();
                ForgeDirection upstream = dx > 0 ? ForgeDirection.WEST
                    : dx < 0 ? ForgeDirection.EAST
                        : dy > 0 ? ForgeDirection.DOWN
                            : dy < 0 ? ForgeDirection.UP : dz > 0 ? ForgeDirection.NORTH : ForgeDirection.SOUTH;
                conduit.mDisableInput = (byte) (63 & ~upstream.flag);
                previous = next;
            }
        }
    }

    private static BaseMetaTileEntity buffer(WorldServer world, FakePlayer placer, int x, int y, int z, int amount) {
        BaseMetaTileEntity tile = (BaseMetaTileEntity) place(
            world,
            placer,
            ItemList.Automation_ChestBuffer_LV.get(1),
            x,
            y,
            z);
        tile.setFrontFacing(ForgeDirection.NORTH);
        MTEBuffer buffer = (MTEBuffer) tile.getMetaTileEntity();
        buffer.mTargetStackSize = amount;
        buffer.setStockingMode(false);
        buffer.setSortStacks(true);
        return tile;
    }

    private static void fillInventory(BaseMetaTileEntity tile, ItemStack stack) {
        for (int slot = 0; slot < tile.getSizeInventory(); slot++)
            if (tile.isValidSlot(slot)) tile.setInventorySlotContents(slot, stack.copy());
        tile.markDirty();
    }

    private static BaseMetaTileEntity caster(WorldServer world, FakePlayer placer, int x, int y, int z) {
        BaseMetaTileEntity tile = (BaseMetaTileEntity) place(
            world,
            placer,
            ItemList.Machine_LV_FluidSolidifier.get(1),
            x,
            y,
            z);
        MTEBasicMachine machine = (MTEBasicMachine) tile.getMetaTileEntity();
        machine.setMainFacing(ForgeDirection.WEST);
        tile.setFrontFacing(ForgeDirection.SOUTH);
        machine.setInventorySlotContents(machine.getInputSlot(), ItemList.Shape_Mold_Ingot.get(1));
        machine.mItemTransfer = true;
        return tile;
    }

    private static void fluidFilter(ICoverable tile, ForgeDirection side, Materials material, boolean blacklist) {
        CoverFluidfilter filter = (CoverFluidfilter) attach(tile, side, ItemList.FluidFilter.get(1));
        filter.setFluidId(
            material.getMolten(1)
                .getFluidID());
        filter.setFilterMode(blacklist ? 1 : 0);
    }

    private static void itemPipe(WorldServer world, FakePlayer placer, List<Position> pipes, int x, int y, int z) {
        for (Position p : pipes) if (p.x == x && p.y == y && p.z == z) return;
        place(world, placer, GTOreDictUnificator.get(OrePrefixes.pipeMedium, Materials.Brass, 1), x, y, z);
        pipes.add(new Position(x, y, z));
    }

    private static void connect(WorldServer world, List<Position> pipes) {
        for (Position p : pipes) {
            IGregTechTileEntity tile = (IGregTechTileEntity) world.getTileEntity(p.x, p.y, p.z);
            MetaPipeEntity pipe = (MetaPipeEntity) tile.getMetaTileEntity();
            for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
                Position adjacent = p.offset(side);
                TileEntity neighbor = world.getTileEntity(adjacent.x, adjacent.y, adjacent.z);
                if (neighbor instanceof ICoverable || neighbor instanceof IInventory
                    || neighbor instanceof IFluidHandler) pipe.connect(side);
                else pipe.disconnect(side);
            }
        }
    }

    private static void generator(WorldServer world, FakePlayer placer, int x, int y, int z) {
        BaseMetaTileEntity tile = (BaseMetaTileEntity) place(
            world,
            placer,
            CustomItemList.Machine_DebugGenny.get(1),
            x,
            y,
            z);
        MTEDebugPowerGenerator generator = (MTEDebugPowerGenerator) tile.getMetaTileEntity();
        tile.setFrontFacing(ForgeDirection.NORTH);
        generator.setUsingTiers(true);
        generator.setVoltageTier((byte) 1);
        generator.setAmperage(16);
        generator.setLASER(false);
        generator.setProducing(true);
    }

    private static void powerRow(WorldServer world, FakePlayer placer, int from, int to, int y, int z) {
        List<Position> cable = new ArrayList<>();
        for (int x = from; x <= to; x++) {
            place(world, placer, GTOreDictUnificator.get(OrePrefixes.cableGt16, Materials.Tin, 1), x, y, z);
            cable.add(new Position(x, y, z));
        }
        generator(world, placer, from - 1, y, z);
        connect(world, cable);
    }

    private static void dropper(WorldServer world, int x, int y, int z, int facing, ItemStack stock) {
        world.setBlock(x, y, z, Blocks.dropper, facing, 3);
        // Vanilla chooses a default facing in onBlockAdded, before the normal item-placement orientation step.
        world.setBlockMetadataWithNotify(x, y, z, facing, 3);
        if ((world.getBlockMetadata(x, y, z) & 7) != facing)
            throw new IllegalStateException("The supply dropper is not facing its receiving hopper");
        TileEntityDropper dropper = (TileEntityDropper) world.getTileEntity(x, y, z);
        for (int slot = 1; slot < dropper.getSizeInventory(); slot++)
            dropper.setInventorySlotContents(slot, stock.copy());
        dropper.markDirty();
    }

    private static void pipe(WorldServer world, FakePlayer placer, List<Position> pipes, int x, int y, int z) {
        place(world, placer, GTOreDictUnificator.get(OrePrefixes.pipeMedium, Materials.Steel, 1), x, y, z);
        pipes.add(new Position(x, y, z));
    }

    private static TileEntity place(WorldServer world, FakePlayer placer, ItemStack stack, int x, int y, int z) {
        if (stack == null || !(stack.getItem() instanceof ItemBlock block))
            throw new IllegalStateException("Missing native lab block at " + x + "," + y + "," + z);
        world.setBlockToAir(x, y, z);
        if (!block.placeBlockAt(stack, placer, world, x, y, z, 1, 0.5f, 0.5f, 0.5f, 0))
            throw new IllegalStateException("Native lab placement failed at " + x + "," + y + "," + z);
        return world.getTileEntity(x, y, z);
    }

    private static Cover attach(ICoverable tile, ForgeDirection side, ItemStack stack) {
        Cover cover = CoverRegistry.buildCover(stack, side, tile);
        tile.attachCover(cover);
        if (cover.getMinimumTickRate() <= 1) cover.setTickRateAddition(1 - cover.getMinimumTickRate());
        return cover;
    }

    /** Reflection keeps optional lab-only ProjectRed classes out of the production dependency surface. */
    static Object gate(WorldServer world, Position position, int kind, int rotation, int interval) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setByte("orient", (byte) rotation);
        tag.setByte("subID", (byte) kind);
        tag.setBoolean("nolegacy", true);
        tag.setLong("schedTime", -1);
        tag.setInteger("pmax", interval - 2);
        tag.setLong("pelapsed", -1);
        world.setBlock(position.x, position.y - 1, position.z, Blocks.stonebrick, 0, 3);
        return part(world, position, kind == 12 || kind >= 17 && kind <= 21 ? "pr_igate" : "pr_sgate", tag);
    }

    static Position port(Object gate, Position position, int internal) {
        int absolute = ((Number) invoke(gate, "toAbsolute", new Class<?>[] { int.class }, internal)).intValue();
        int direction = ((Number) invoke(gate, "absoluteDir", new Class<?>[] { int.class }, absolute)).intValue();
        return position.offset(ForgeDirection.getOrientation(direction));
    }

    static void wire(WorldServer world, Position position, int colour) {
        if (world.getTileEntity(position.x, position.y, position.z) instanceof BaseMetaPipeEntity)
            throw new IllegalStateException("Native framed wire would replace a GregTech pipe at " + position);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setBoolean("nolegacy", true);
        tag.setString("mat", "");
        tag.setByte("colour", (byte) colour);
        part(world, position, "pr_finsulated", tag);
    }

    static Object part(WorldServer world, Position position, String type, NBTTagCompound tag) {
        try {
            Class<?> registry = Class.forName("codechicken.multipart.MultiPartRegistry");
            Object part = registry.getMethod("createPart", String.class, boolean.class)
                .invoke(null, type, false);
            if (part == null) throw new IllegalStateException("Native multipart unavailable: " + type);
            invoke(part, "load", new Class<?>[] { NBTTagCompound.class }, tag);
            Class<?> coord = Class.forName("codechicken.lib.vec.BlockCoord");
            Object pos = coord.getConstructor(int.class, int.class, int.class)
                .newInstance(position.x, position.y, position.z);
            Class.forName("codechicken.multipart.TileMultipart")
                .getMethod(
                    "addPart",
                    net.minecraft.world.World.class,
                    coord,
                    Class.forName("codechicken.multipart.TMultiPart"))
                .invoke(null, world, pos, part);
            return part;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot place native ProjectRed " + type + " at " + position, exception);
        }
    }

    static Object invoke(Object receiver, String name, Class<?>[] types, Object... arguments) {
        try {
            Method method = receiver.getClass()
                .getMethod(name, types);
            return method.invoke(receiver, arguments);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Native ProjectRed method " + name + " failed", exception);
        }
    }

    private static void sign(WorldServer world, int x, int y, int z, String a, String b, String c, String d) {
        world.setBlock(x, y - 1, z, Blocks.stonebrick, 0, 3);
        world.setBlock(x, y, z, Blocks.standing_sign, 8, 3);
        TileEntitySign sign = (TileEntitySign) world.getTileEntity(x, y, z);
        sign.signText = new String[] { a, b, c, d };
        sign.markDirty();
    }
}
