package pl.kuba6000.gtnhextranerf.machines;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlockAdder;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofChain;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.ExoticEnergy;
import static gregtech.api.enums.HatchElement.InputBus;
import static gregtech.api.enums.HatchElement.InputHatch;
import static gregtech.api.enums.HatchElement.Maintenance;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;
import static gregtech.api.util.GTStructureUtility.ofCoil;
import static gregtech.api.util.GTStructureUtility.ofFrame;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.HeatingCoilLevel;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.INEIPreviewModifier;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.ICasingTextureProvider;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase;
import gregtech.api.metatileentity.implementations.MTEHatchInput;
import gregtech.api.metatileentity.implementations.MTEHatchInputBus;
import gregtech.api.objects.ItemData;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.structure.error.StructureError;
import gregtech.api.structure.error.StructureErrorRegistry;
import gregtech.api.structure.error.StructureErrors;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.OverclockCalculator;
import gregtech.common.blocks.BlockCasingsAbstract;
import gregtech.common.blocks.ItemMachines;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;
import gregtech.common.render.IMTERenderer;
import gregtech.common.tileentities.machines.IDualInputHatch;
import gregtech.common.tileentities.machines.IRecipeProcessingAwareHatch;
import io.netty.buffer.ByteBuf;
import pl.kuba6000.gtnhextranerf.client.render.IronBathSurfaceRenderer;
import pl.kuba6000.gtnhextranerf.machines.ironbath.IronBath;
import pl.kuba6000.gtnhextranerf.machines.ironbath.IronBath.Surface;
import pl.kuba6000.gtnhextranerf.machines.ironbath.IronProduct;

/** Native ports and structure around the persistent iron-bath process. */
@IMetaTileEntity.SkipGenerateDescription
public final class MTEIronBathFurnace extends MTEExtendedPowerMultiBlockBase<MTEIronBathFurnace>
    implements ISurvivalConstructable, ICasingTextureProvider, IMTERenderer, INEIPreviewModifier {

    private static final String MAIN = "main";
    private static final int PLATFORM_PORT_HINT = 1;
    private static final int TAPPING_HATCH_HINT = 2;
    private static final int ENERGY_HINT = 5;
    private static final int MEASUREMENT_HATCH_HINT = 6;
    private static final String PLATFORM_HATCH_NAME = "gtnhextranerf.iron_bath.tooltip.structure.platform_hatch";
    private static final String TAP_NAME = "gtnhextranerf.iron_bath.tooltip.structure.tap_name";
    private static final String MEASUREMENT_NAME = "gtnhextranerf.iron_bath.measurement.name";
    private static final int STEEL_CASING_TEXTURE_INDEX = ((BlockCasingsAbstract) GregTechAPI.sBlockCasings2)
        .getTextureIndex(0);
    private static final int OVERCLOCK_DURATION = 1 << 28;
    private static final IronProduct[] PRODUCTS = IronProduct.values();
    private static IStructureDefinition<MTEIronBathFurnace> structure;
    private final IronBath bath = new IronBath(0);
    private final List<MTEIronBathTap> taps = new ArrayList<>();
    private final List<MTEIronBathMeasurement> measurementHatches = new ArrayList<>();
    private int reinforcedCasings;
    private int totalCasings;
    private long actualPower;
    private boolean placingNeiPreview;
    private HeatingCoilLevel coilLevel = HeatingCoilLevel.None;
    private Surface surface = Surface.EMPTY;

    public MTEIronBathFurnace(int id, String name, String regionalName) {
        super(id, name, regionalName);
    }

    public MTEIronBathFurnace(String name) {
        super(name);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new MTEIronBathFurnace(mName);
    }

    @Override
    public IStructureDefinition<MTEIronBathFurnace> getStructureDefinition() {
        if (structure == null) structure = createStructure();
        return structure;
    }

    private static IStructureDefinition<MTEIronBathFurnace> createStructure() {
        IStructureElement<MTEIronBathFurnace> platformPort = buildHatchAdder(MTEIronBathFurnace.class)
            .atLeast(InputBus, InputHatch, Maintenance)
            .hatchItemFilterAnd(machine -> stack -> hasLocalInputBuffer(ItemMachines.getMetaTileEntity(stack)))
            .casingIndex(STEEL_CASING_TEXTURE_INDEX)
            .hint(PLATFORM_PORT_HINT)
            .description(() -> PLATFORM_HATCH_NAME)
            .shouldReject(
                machine -> machine.placingNeiPreview
                    || (InputBus.count(machine) > 0 && InputHatch.count(machine) > 0 && Maintenance.count(machine) > 0))
            .build();
        IStructureElement<MTEIronBathFurnace> measurementPort = buildHatchAdder(MTEIronBathFurnace.class)
            .hatchClass(MTEIronBathMeasurement.class)
            .adder(MTEIronBathFurnace::addMeasurementHatch)
            .casingIndex(IronBathFurnace.CASING_TEXTURE_INDEX)
            .hint(MEASUREMENT_HATCH_HINT)
            .cacheHint(() -> StatCollector.translateToLocal(MEASUREMENT_NAME))
            .description(() -> MEASUREMENT_NAME)
            .shouldReject(machine -> machine.placingNeiPreview || !machine.measurementHatches.isEmpty())
            .build();
        return StructureDefinition.<MTEIronBathFurnace>builder()
            .addShape(MAIN, transpose(IronBathStructure.shape()))
            .addElement('F', ofFrame(Materials.Steel))
            .addElement('C', ofChain(measurementPort, casing(0), casing(1)))
            .addElement('M', ofChain(platformPort, ofBlock(GregTechAPI.sBlockCasings2, 0)))
            .addElement('P', ofBlock(GregTechAPI.sBlockCasings11, 1))
            .addElement('K', ofCoil(MTEIronBathFurnace::setCoilLevel, MTEIronBathFurnace::getCoilLevel))
            .addElement(
                'E',
                buildHatchAdder(MTEIronBathFurnace.class).atLeast(Energy.or(ExoticEnergy))
                    .casingIndex(IronBathFurnace.CASING_TEXTURE_INDEX)
                    .hint(ENERGY_HINT)
                    .shouldReject(machine -> machine.placingNeiPreview)
                    .buildAndChain(measurementPort, casing(0), casing(1)))
            .addElement(
                'T',
                buildHatchAdder(MTEIronBathFurnace.class).hatchClass(MTEIronBathTap.class)
                    .adder(MTEIronBathFurnace::addTap)
                    .casingIndex(IronBathFurnace.CASING_TEXTURE_INDEX)
                    .hint(TAPPING_HATCH_HINT)
                    .cacheHint(() -> StatCollector.translateToLocal(TAP_NAME))
                    .description(() -> TAP_NAME)
                    .shouldReject(machine -> machine.placingNeiPreview || !machine.taps.isEmpty())
                    .buildAndChain(measurementPort, casing(0), casing(1)))
            .build();
    }

    private static IStructureElement<MTEIronBathFurnace> casing(int metadata) {
        return ofBlockAdder((machine, block, meta) -> {
            if (block != IronBathFurnace.casingBlock() || meta != metadata) return false;
            machine.totalCasings++;
            if (metadata == 1) machine.reinforcedCasings++;
            return true;
        }, IronBathFurnace.casingBlock(), metadata);
    }

    private static boolean hasLocalInputBuffer(IMetaTileEntity hatch) {
        return hatch != null && !(hatch instanceof IDualInputHatch) && !(hatch instanceof IRecipeProcessingAwareHatch);
    }

    private boolean addTap(IGregTechTileEntity tile, int texture) {
        if (!(tile.getMetaTileEntity() instanceof MTEIronBathTap tap)) return false;
        tap.updateTexture(texture);
        taps.add(tap);
        return true;
    }

    private boolean addMeasurementHatch(IGregTechTileEntity tile, int texture) {
        if (!(tile.getMetaTileEntity() instanceof MTEIronBathMeasurement hatch)) return false;
        hatch.updateTexture(texture);
        hatch.bind(this);
        measurementHatches.add(hatch);
        return true;
    }

    Map<String, Object> readMeasurements(MTEIronBathMeasurement hatch) {
        IGregTechTileEntity tile = getBaseMetaTileEntity();
        IGregTechTileEntity port = hatch.getBaseMetaTileEntity();
        if (tile == null || port == null
            || tile.isDead()
            || port.isDead()
            || tile.getWorld() == null
            || port.getWorld() != tile.getWorld()
            || tile.getWorld()
                .getTileEntity(tile.getXCoord(), tile.getYCoord(), tile.getZCoord()) != tile
            || tile.getWorld()
                .getTileEntity(port.getXCoord(), port.getYCoord(), port.getZCoord()) != port
            || tile.getMetaTileEntity() != this
            || port.getMetaTileEntity() != hatch
            || !mMachine
            || !measurementHatches.contains(hatch)) return null;
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("metalMb", bath.metalMb());
        values.put("liquidMetalMb", bath.liquidMetalMb());
        values.put("slagMb", bath.slagMb());
        values.put("liquidSlagMb", bath.liquidSlagMb());
        values.put("carbonUnits", bath.carbon());
        values.put("impurityUnits", bath.impurities());
        values.put("calciteUnits", bath.calciteCapacity());
        values.put("temperatureK", bath.temperature());
        values.put("capacityMb", bath.capacityMb());
        values.put("pendingMetalMb", bath.pendingMetalMb());
        values.put("meltingItems", bath.meltingItems());
        values.put("melting", bath.meltingItems() > 0);
        values.put(
            "product",
            bath.currentProduct()
                .name());
        List<Map<String, Object>> groups = new ArrayList<>();
        for (IronBath.ChargeGroup group : bath.chargeGroups(0, bath.chargeGroupCount())) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("count", group.count());
            entry.put("progress", group.progress());
            groups.add(entry);
        }
        values.put("chargeGroups", groups.toArray());
        return values;
    }

    @Override
    public void checkMachine(IGregTechTileEntity tile, ItemStack stack, List<StructureError> errors) {
        taps.clear();
        measurementHatches.clear();
        reinforcedCasings = 0;
        totalCasings = 0;
        setCoilLevel(HeatingCoilLevel.None);
        if (!checkPiece(
            MAIN,
            IronBathStructure.OFFSET_X,
            IronBathStructure.OFFSET_Y,
            IronBathStructure.OFFSET_Z,
            errors)) return;
        checkOneMaintenanceHatch(errors);
        int energyHatches = mEnergyHatches.size() + mExoticEnergyHatches.size();
        if (energyHatches == 0 || energyHatches > 8)
            errors.add(StructureErrors.of("gtnhextranerf.iron_bath.energy_hatches"));
        if (measurementHatches.size() > 1)
            errors.add(StructureErrors.of("gtnhextranerf.iron_bath.measurement_hatches"));
        checkHasInputBus(errors);
        checkHasInputHatch(errors);
        if (!mDualInputHatches.isEmpty() || mInputBusses.stream()
            .anyMatch(input -> !hasLocalInputBuffer(input))
            || mInputHatches.stream()
                .anyMatch(input -> !hasLocalInputBuffer(input)))
            errors.add(StructureErrors.of("gtnhextranerf.iron_bath.buffered_inputs"));
        if (taps.isEmpty()) errors.add(StructureErrors.missingHatch(IronBathFurnace.tap(1)));
        if (getCoilLevel() == HeatingCoilLevel.None) errors.add(StructureErrorRegistry.COIL_LEVEL_NOT_ENOUGH);
        else if (!bath.configure(getCoilLevel().getTier()))
            errors.add(StructureErrors.of("gtnhextranerf.iron_bath.capacity_error"));
        bath.setInsulation(totalCasings == 0 ? 0 : (double) reinforcedCasings / totalCasings);
        doPeriodicChecks = true;
    }

    @Override
    public void construct(ItemStack stack, boolean hintsOnly) {
        buildPiece(
            MAIN,
            stack,
            hintsOnly,
            IronBathStructure.OFFSET_X,
            IronBathStructure.OFFSET_Y,
            IronBathStructure.OFFSET_Z);
    }

    @Override
    public int survivalConstruct(ItemStack stack, int budget, ISurvivalBuildEnvironment environment) {
        if (mMachine) return -1;
        return survivalBuildPiece(
            MAIN,
            stack,
            IronBathStructure.OFFSET_X,
            IronBathStructure.OFFSET_Y,
            IronBathStructure.OFFSET_Z,
            budget,
            environment,
            false,
            true);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void onPreviewConstruct(@NotNull ItemStack trigger) {
        // Preplace the spouts and energy hatch before StructureLib fills in the casings
        placingNeiPreview = true;
        IronBathStructure.Offset tap = IronBathStructure.METAL_TAP;
        for (int band = 0; band < IronBathStructure.TANK_HEIGHT; band++) {
            placePreviewHatch(
                IronBathFurnace.tap(1),
                new IronBathStructure.Offset(tap.x(), tap.y() + band, tap.z()),
                ForgeDirection.NORTH);
        }
        placePreviewHatch(ItemList.Hatch_Input_Bus_LV.get(1), IronBathStructure.INPUT_BUS, ForgeDirection.NORTH);
        placePreviewHatch(ItemList.Hatch_Input_LV.get(1), IronBathStructure.OXYGEN_INPUT, ForgeDirection.NORTH);
        placePreviewHatch(ItemList.Hatch_Maintenance.get(1), IronBathStructure.MAINTENANCE, ForgeDirection.SOUTH);
        placePreviewHatch(ItemList.Hatch_Energy_LV.get(1), IronBathStructure.ENERGY_A, ForgeDirection.DOWN);
        placePreviewHatch(
            IronBathFurnace.measurementHatch(),
            IronBathStructure.PREVIEW_MEASUREMENT,
            ForgeDirection.NORTH);
    }

    @Override
    public void onPreviewStructureComplete(@NotNull ItemStack trigger) {
        placingNeiPreview = false;
    }

    @SideOnly(Side.CLIENT)
    private void placePreviewHatch(ItemStack stack, IronBathStructure.Offset offset, ForgeDirection front) {
        IGregTechTileEntity controller = getBaseMetaTileEntity();
        int[] worldOffset = new int[3];
        getExtendedFacing().getWorldOffset(new int[] { -offset.x(), -offset.y(), offset.z() }, worldOffset);
        int x = controller.getXCoord() + worldOffset[0];
        int y = controller.getYCoord() + worldOffset[1];
        int z = controller.getZCoord() + worldOffset[2];
        var world = controller.getWorld();
        ForgeDirection face = getExtendedFacing().getWorldDirection(front);
        if (((ItemMachines) stack.getItem()).placeBlockAt(
            stack,
            Minecraft.getMinecraft().thePlayer,
            world,
            x,
            y,
            z,
            face.ordinal(),
            0.5f,
            0.5f,
            0.5f,
            0) && world.getTileEntity(x, y, z) instanceof IGregTechTileEntity hatch) {
            hatch.setFrontFacing(face);
        }
    }

    @Override
    public RecipeMap<?> getRecipeMap() {
        return IronBathFurnace.RECIPES;
    }

    @Override
    public CheckRecipeResult checkProcessing() {
        return CheckRecipeResultRegistry.NO_RECIPE;
    }

    @Override
    public void onPostTick(IGregTechTileEntity tile, long tick) {
        super.onPostTick(tile, tick);
        if (!tile.isServerSide()) return;
        actualPower = 0;
        double replacedHeatLoss = 0;
        long supply = 0;
        if (mMachine) {
            int voltageTier = GTUtility.getTier(heatingVoltage());
            supply = (long) (availableHeatingPower() * ((double) GTValues.VP[voltageTier] / GTValues.V[voltageTier]));
        }
        if (mMachine && tile.isAllowedToWork()) {
            OverclockCalculator overclock = heatingOverclock();
            admitInputs();
            mEfficiency = 10000 - (getIdealStatus() - getRepairStatus()) * 1000;
            double efficiency = Math.max(1000, mEfficiency) / 10000.0;
            double standbyConversion = IronBath.COLD_MELTING_HEAT_PER_INGOT / IronBath.COLD_MELTING_EU_PER_INGOT
                * efficiency;
            double bathConversion = standbyConversion * bath.heatingEfficiency();
            double chargeConversion = heatingConversion(overclock) * efficiency * bath.heatingEfficiency();
            long chargeSupply = heatingSupply(overclock);
            long chargePower = 0;
            long lossPower = Math.min(supply, (long) Math.ceil(bath.heatLoss() / standbyConversion));
            boolean processing = (lossPower > 0 || bath.powerDemand(chargeConversion, bathConversion) > 0)
                && doRandomMaintenanceDamage();
            if (processing) {
                if (lossPower > 0 && drainEnergyInput(lossPower)) {
                    actualPower += lossPower;
                    replacedHeatLoss = lossPower * standbyConversion;
                }
            }
            bath.cool(replacedHeatLoss);
            if (processing) {
                while (actualPower < supply) {
                    boolean heatCharge = bath.chargeHeatDemand() > 0 && chargePower < chargeSupply;
                    double demand = heatCharge ? bath.chargeHeatDemand() / chargeConversion
                        : bath.bathHeatDemand() / bathConversion;
                    long allowance = heatCharge ? Math.min(supply - actualPower, chargeSupply - chargePower)
                        : supply - actualPower;
                    long requested = Math.min(allowance, (long) Math.ceil(demand));
                    if (requested <= 0 || !drainEnergyInput(requested)) break;
                    actualPower += requested;
                    if (heatCharge) {
                        chargePower += requested;
                        bath.addPower(requested, chargeConversion, bathConversion);
                    } else {
                        bath.addHeat(requested * bathConversion, 0);
                    }
                    acceptSolids();
                }
            }
        } else bath.cool(0);
        bath.finishTick(actualPower > 0, mMachine && tile.isAllowedToWork());
        if (mMachine && tile.isAllowedToWork()) {
            for (MTEIronBathTap tap : GTUtility.validMTEList(taps)) {
                int y = tap.getBaseMetaTileEntity()
                    .getYCoord() - tile.getYCoord();
                int band = IronBathStructure.portBand(y);
                tap.discharge(bath, band);
            }
        }
        lEUt = -(long) (actualPower * (Math.max(1000, mEfficiency) / 10000.0));
        tile.setActive(actualPower > 0);
        if (tick % 20 == 0) markDirty();
        if (tick % 5 == 0) {
            Surface next = mMachine ? bath.surface() : Surface.EMPTY;
            if (!next.equals(surface)) {
                surface = next;
                tile.issueTileUpdate();
            }
        }
    }

    @Override
    public void writeToStream(ByteBuf buffer) {
        super.writeToStream(buffer);
        buffer.writeFloat(surface.fill());
        buffer.writeByte(
            surface.product()
                .ordinal());
        buffer.writeBoolean(surface.molten());
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void readFromStream(ByteBuf buffer) {
        super.readFromStream(buffer);
        surface = new Surface(buffer.readFloat(), PRODUCTS[buffer.readUnsignedByte()], buffer.readBoolean());
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderTESR(double x, double y, double z, float timeSinceLastTick) {
        IronBathSurfaceRenderer.render(getExtendedFacing(), surface, x, y, z);
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox(int x, int y, int z) {
        return IronBathStructure.tankBounds(getExtendedFacing(), x, y, z);
    }

    private OverclockCalculator heatingOverclock() {
        long voltage = heatingVoltage();
        int effectiveHeat = (int) getCoilLevel().getHeat() + 100 * (GTUtility.getTier(voltage) - 1);
        return new OverclockCalculator().setRecipeEUt(120)
            .setEUt(voltage)
            .setAmperage(4)
            .setDuration(OVERCLOCK_DURATION)
            .setAmperageOC(true)
            .setRecipeHeat(1700)
            .setMachineHeat(effectiveHeat)
            .setHeatOC(true)
            .setHeatDiscount(true)
            .calculate();
    }

    private long heatingVoltage() {
        long voltage = 0;
        for (var hatch : GTUtility.validMTEList(getExoticAndNormalEnergyHatchList())) voltage = Math.max(
            voltage,
            hatch.getBaseMetaTileEntity()
                .getInputVoltage());
        return voltage;
    }

    private long availableHeatingPower() {
        boolean singleAmp = !debugEnergyPresent && mEnergyHatches.size() == 1 && mExoticEnergyHatches.isEmpty();
        return singleAmp ? getAverageInputVoltage() : getMaxInputEu();
    }

    private void admitInputs() {
        acceptSolids();
        acceptOxygen();
        updateSlots();
    }

    private void acceptSolids() {
        for (MTEHatchInputBus bus : GTUtility.validMTEList(mInputBusses)) {
            IGregTechTileEntity inventory = bus.getBaseMetaTileEntity();
            for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
                ItemStack stack = inventory.getStackInSlot(slot);
                if (stack == null || stack.stackSize <= 0) continue;
                ItemData data = GTOreDictUnificator.getItemData(stack);
                int accepted = 0;
                if (data != null && data.mPrefix == OrePrefixes.dust && data.mMaterial.mMaterial == Materials.Calcite) {
                    accepted = bath.acceptCalcite(stack.stackSize);
                } else {
                    IronBath.Fuel fuel = IronBathFurnace.fuel(stack);
                    accepted = fuel == null ? acceptMetal(stack) : bath.acceptFuel(fuel, stack.stackSize);
                }
                if (accepted > 0) {
                    inventory.decrStackSize(slot, accepted);
                    inventory.markDirty();
                }
            }
        }
    }

    private int acceptMetal(ItemStack stack) {
        ItemData data = GTOreDictUnificator.getItemData(stack);
        if (data == null || (data.mPrefix != OrePrefixes.ingot && data.mPrefix != OrePrefixes.dust)) return 0;
        IronProduct product = IronBathFurnace.product(data.mMaterial.mMaterial);
        return product == null ? 0 : bath.acceptMetal(product, stack.stackSize, stack);
    }

    private double heatingConversion(OverclockCalculator overclock) {
        double speed = (double) OVERCLOCK_DURATION / overclock.getDuration();
        return (IronBath.COLD_MELTING_HEAT_PER_INGOT / IronBath.COLD_MELTING_EU_PER_INGOT) * speed
            * 120
            / overclock.getConsumption();
    }

    private long heatingSupply(OverclockCalculator overclock) {
        long available = availableHeatingPower();
        return Math.min(available, (long) (overclock.getConsumption() * (available / (4.0 * heatingVoltage()))));
    }

    int oxygenRateLimit() {
        return (int) Math.max(0, Math.min(Integer.MAX_VALUE, heatingSupply(heatingOverclock()) / 60));
    }

    private void acceptOxygen() {
        int budget = oxygenRateLimit();
        for (MTEHatchInput hatch : GTUtility.validMTEList(mInputHatches)) {
            if (budget <= 0) break;
            FluidStack available = hatch.getDrainableStack();
            if (available == null || !available.isFluidEqual(Materials.Oxygen.getGas(1))) continue;
            int consumed = bath.addOxygen(Math.min(available.amount, budget));
            if (consumed > 0) {
                hatch.drain(consumed, true);
                budget -= consumed;
            }
        }
    }

    private HeatingCoilLevel getCoilLevel() {
        return coilLevel;
    }

    private void setCoilLevel(HeatingCoilLevel value) {
        coilLevel = value;
    }

    IronBath bath() {
        return bath;
    }

    long actualPower() {
        return actualPower;
    }

    @Override
    protected MTEMultiBlockBaseGui<?> getGui() {
        return new IronBathGui(this);
    }

    @Override
    public void saveNBTData(NBTTagCompound tag) {
        super.saveNBTData(tag);
        NBTTagCompound state = new NBTTagCompound();
        bath.save(state);
        tag.setTag("IronBath", state);
    }

    @Override
    public void setItemNBT(NBTTagCompound tag) {
        // GregTech writes covers, paint and upgrades before this drop hook
        tag.func_150296_c()
            .clear();
    }

    @Override
    public void loadNBTData(NBTTagCompound tag) {
        super.loadNBTData(tag);
        if (!tag.hasKey("IronBath")) return;
        bath.load(tag.getCompoundTag("IronBath"));
    }

    @Override
    public ITexture getCasingTexture() {
        return Textures.BlockIcons.getCasingTextureForId(STEEL_CASING_TEXTURE_INDEX);
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity tile, ForgeDirection side, ForgeDirection facing, int color,
        boolean active, boolean redstone) {
        return Textures.BlockIcons.createTextureWithCasing(
            this,
            side,
            facing,
            active,
            Textures.BlockIcons.OVERLAY_FRONT_ELECTRIC_BLAST_FURNACE,
            Textures.BlockIcons.OVERLAY_FRONT_ELECTRIC_BLAST_FURNACE_GLOW,
            Textures.BlockIcons.OVERLAY_FRONT_ELECTRIC_BLAST_FURNACE_ACTIVE,
            Textures.BlockIcons.OVERLAY_FRONT_ELECTRIC_BLAST_FURNACE_ACTIVE_GLOW);
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        return new MultiblockTooltipBuilder()
            .addMachineType(StatCollector.translateToLocal("gtnhextranerf.iron_bath.tooltip.type"))
            .addInfo(StatCollector.translateToLocal("gtnhextranerf.iron_bath.tooltip.info.0"))
            .addInfo(StatCollector.translateToLocal("gtnhextranerf.iron_bath.tooltip.info.1"))
            .addInfo(StatCollector.translateToLocal("gtnhextranerf.iron_bath.tooltip.info.2"))
            .addInfo(StatCollector.translateToLocal("gtnhextranerf.iron_bath.tooltip.info.3"))
            .addInfo(StatCollector.translateToLocal("gtnhextranerf.iron_bath.tooltip.info.4"))
            .beginStructureBlock(IronBathStructure.WIDTH, IronBathStructure.HEIGHT, IronBathStructure.DEPTH, false)
            .addController(StatCollector.translateToLocal("gtnhextranerf.iron_bath.tooltip.structure.controller"))
            .addOtherStructurePart(
                StatCollector.translateToLocal("gtnhextranerf.iron_bath.tooltip.structure.coils"),
                StatCollector.translateToLocal("gtnhextranerf.iron_bath.tooltip.structure.coil_layers"),
                new int[0])
            .addEnergyHatch(
                "1-8",
                StatCollector.translateToLocal("gtnhextranerf.iron_bath.tooltip.structure.chimney_base"),
                ENERGY_HINT)
            .addOtherStructurePart(
                StatCollector.translateToLocal(PLATFORM_HATCH_NAME),
                StatCollector.translateToLocal("gtnhextranerf.iron_bath.tooltip.structure.platform_casings"),
                PLATFORM_PORT_HINT)
            .addStructureInfo(
                StatCollector.translateToLocal("gtnhextranerf.iron_bath.tooltip.structure.platform_requirements"))
            .addOtherStructurePart(
                StatCollector.translateToLocal(TAP_NAME),
                StatCollector.translateToLocal("gtnhextranerf.iron_bath.tooltip.structure.tap_walls"),
                TAPPING_HATCH_HINT)
            .addOtherStructurePart(
                StatCollector.translateToLocal(MEASUREMENT_NAME),
                StatCollector.translateToLocal("gtnhextranerf.iron_bath.tooltip.structure.measurement"),
                MEASUREMENT_HATCH_HINT)
            .toolTipFinisher();
    }
}
