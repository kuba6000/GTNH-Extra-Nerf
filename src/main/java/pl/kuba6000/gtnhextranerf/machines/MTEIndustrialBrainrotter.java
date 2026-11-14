package pl.kuba6000.gtnhextranerf.machines;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlockAdder;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlockAnyMeta;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofChain;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofSpecificTileAdder;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.ExoticEnergy;
import static gregtech.api.enums.HatchElement.InputBus;
import static gregtech.api.enums.HatchElement.InputHatch;
import static gregtech.api.enums.HatchElement.Maintenance;
import static gregtech.api.enums.HatchElement.OutputBus;
import static gregtech.api.enums.HatchElement.OutputHatch;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;
import static gregtech.api.util.GTStructureUtility.chainAllGlasses;

import java.util.List;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.StructureLibAPI;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.AutoPlaceEnvironment;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.Textures;
import gregtech.api.enums.VoltageIndex;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.ICasingTextureProvider;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.recipe.check.SimpleCheckRecipeResult;
import gregtech.api.structure.error.StructureError;
import gregtech.api.structure.error.StructureErrors;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.OverclockCalculator;
import gregtech.api.util.shutdown.ShutDownReason;
import gregtech.api.util.shutdown.SimpleShutDownReason;
import mods.railcraft.common.blocks.machine.alpha.EnumMachineAlpha;
import mods.railcraft.common.blocks.machine.epsilon.EnumMachineEpsilon;
import mods.railcraft.common.blocks.signals.EnumSignal;
import pl.kuba6000.gtnhextranerf.GTNHExtraNerf;
import pl.kuba6000.gtnhextranerf.materials.NerfMaterials;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.tiles.TilePedestal;

/** The supplied 5x5x3 structure, native GT processing and a live connection to its pedestal. */
@IMetaTileEntity.SkipGenerateDescription
public final class MTEIndustrialBrainrotter extends MTEExtendedPowerMultiBlockBase<MTEIndustrialBrainrotter>
    implements ISurvivalConstructable, ICasingTextureProvider {

    private static final String MAIN = "main";
    private static final String DISPLAY_NBT = "BrainrotterDisplay";
    private static final IIconContainer FRONT = Textures.BlockIcons
        .custom(GTNHExtraNerf.MODID, "machines/brainrotter/front");
    private static final IIconContainer FRONT_GLOW = Textures.BlockIcons
        .custom(GTNHExtraNerf.MODID, "machines/brainrotter/front_glow");
    private static final IIconContainer FRONT_ACTIVE = Textures.BlockIcons
        .custom(GTNHExtraNerf.MODID, "machines/brainrotter/front_active");
    private static final IIconContainer FRONT_ACTIVE_GLOW = Textures.BlockIcons
        .custom(GTNHExtraNerf.MODID, "machines/brainrotter/front_active_glow");
    // F is fixed fireproof wood; W is fireproof wood or a GT hatch in the bottom layer.
    // spotless:off
    private static final String[][] SHAPE = {
        { "FFDFF", "FDADF", "FFDFF" },
        { "BBBBB", "BIIIB", "BBBBB" },
        { "BBBBB", "BJ-MB", "BBBBB" },
        { "BBBBB", "BHEKB", "BBBBB" },
        { "WL~WW", "WCCCW", "WWWWW" }
    };
    // spotless:on
    private static volatile IStructureDefinition<MTEIndustrialBrainrotter> definition;
    private TilePedestal pedestal;
    private ItemStack display;
    private ChunkCoordinates displayPosition;
    private int glassTier = -1;

    public MTEIndustrialBrainrotter(int id, String name, String regionalName) {
        super(id, name, regionalName);
    }

    public MTEIndustrialBrainrotter(String name) {
        super(name);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new MTEIndustrialBrainrotter(mName);
    }

    @Override
    public IStructureDefinition<MTEIndustrialBrainrotter> getStructureDefinition() {
        if (definition != null) return definition;
        Block wood = structureBlock(
            GameRegistry.findBlock("Forestry", "logsFireproof"),
            Blocks.log,
            "Forestry:logsFireproof");
        boolean railcraft = Loader.isModLoaded("Railcraft");
        boolean thaumcraft = Loader.isModLoaded("Thaumcraft");
        Block pedestalBlock = structureBlock(
            thaumcraft ? ConfigBlocks.blockStoneDevice : null,
            Blocks.gold_block,
            "Thaumcraft:pedestal");
        Block table = structureBlock(
            thaumcraft ? ConfigBlocks.blockTable : null,
            Blocks.crafting_table,
            "Thaumcraft:table");
        definition = StructureDefinition.<MTEIndustrialBrainrotter>builder()
            .addShape(MAIN, transpose(SHAPE))
            // The timer's metadata changes as it pulses; this is a block identity requirement.
            .addElement(
                'A',
                ofBlockAnyMeta(
                    structureBlock(
                        GameRegistry.findBlock("ExtraUtilities", "timer"),
                        Blocks.redstone_block,
                        "ExtraUtilities:timer")))
            .addElement(
                'B',
                chainAllGlasses(-1, (machine, tier) -> machine.glassTier = tier, machine -> machine.glassTier))
            .addElement('C', ofBlock(Blocks.mycelium, 0))
            .addElement(
                'D',
                block(
                    railcraft ? EnumMachineEpsilon.FORCE_TRACK_EMITTER.getItem() : null,
                    Blocks.iron_block,
                    "Railcraft:force_track_emitter"))
            .addElement(
                'E',
                block(
                    railcraft ? EnumSignal.SWITCH_ROUTING.getItem() : null,
                    Blocks.emerald_block,
                    "Railcraft:switch_routing"))
            .addElement('F', ofBlockAnyMeta(wood))
            .addElement(
                'W',
                buildHatchAdder(MTEIndustrialBrainrotter.class)
                    .atLeast(InputBus, OutputBus, InputHatch, OutputHatch, Maintenance, Energy.or(ExoticEnergy))
                    .casingIndex(Brainrotter.CASING_TEXTURE_INDEX)
                    .hint(1)
                    .buildAndChain(ofBlockAnyMeta(wood)))
            .addElement(
                'H',
                pedestalMechanicsEnabled()
                    ? ofChain(ofSpecificTileAdder((MTEIndustrialBrainrotter machine, TilePedestal tile) -> {
                        if (tile.getBlockType() != pedestalBlock || tile.getBlockMetadata() != 1) return false;
                        machine.pedestal = tile;
                        return true;
                    }, TilePedestal.class, pedestalBlock, 1),
                        // Placement fallback only: validation still requires the real pedestal tile above.
                        ofBlockAdder((machine, block, meta) -> false, pedestalBlock, 1))
                    : ofBlock(pedestalBlock, 0))
            .addElement('I', skull())
            .addElement(
                'J',
                ofBlock(
                    structureBlock(
                        GameRegistry.findBlock("ThaumicExploration", "thinkTankJar"),
                        Blocks.bookshelf,
                        "ThaumicExploration:thinkTankJar"),
                    0))
            .addElement('K', ofBlock(table, 0))
            .addElement(
                'L',
                block(
                    railcraft ? EnumMachineAlpha.FEED_STATION.getItem() : null,
                    Blocks.hay_block,
                    "Railcraft:feed_station"))
            .addElement(
                'M',
                block(
                    railcraft ? EnumSignal.DISTANT_SIGNAL.getItem() : null,
                    Blocks.lapis_block,
                    "Railcraft:distant_signal"))
            .build();
        return definition;
    }

    private static IStructureElement<MTEIndustrialBrainrotter> block(ItemStack stack, Block placeholder, String name) {
        Block found = stack == null ? null : Block.getBlockFromItem(stack.getItem());
        return ofBlock(structureBlock(found, placeholder, name), found == null ? 0 : stack.getItemDamage());
    }

    private static Block structureBlock(Block block, Block placeholder, String name) {
        if (block != null) return block;
        if (!GTNHExtraNerf.isDevelopmentEnvironment())
            throw new IllegalStateException("Missing Brainrotter structure block: " + name);
        GTNHExtraNerf.LOG.warn(
            "Development Brainrotter structure: replacing missing {} with {}",
            name,
            Block.blockRegistry.getNameForObject(placeholder));
        return placeholder;
    }

    private static boolean pedestalMechanicsEnabled() {
        return !GTNHExtraNerf.isDevelopmentEnvironment()
            || Loader.isModLoaded("Thaumcraft") && ConfigBlocks.blockStoneDevice != null;
    }

    /** Skull items are not ItemBlocks, so StructureLib's default survival placement cannot handle them. */
    private static IStructureElement<MTEIndustrialBrainrotter> skull() {
        return new IStructureElement<>() {

            @Override
            public boolean check(MTEIndustrialBrainrotter machine, World world, int x, int y, int z) {
                return world.getBlock(x, y, z) == Blocks.skull;
            }

            @Override
            public boolean spawnHint(MTEIndustrialBrainrotter machine, World world, int x, int y, int z,
                ItemStack trigger) {
                StructureLibAPI.hintParticle(world, x, y, z, Blocks.skull, 1);
                return true;
            }

            @Override
            public boolean placeBlock(MTEIndustrialBrainrotter machine, World world, int x, int y, int z,
                ItemStack trigger) {
                return world.setBlock(x, y, z, Blocks.skull, 1, 2);
            }

            @Override
            public BlocksToPlace getBlocksToPlace(MTEIndustrialBrainrotter machine, World world, int x, int y, int z,
                ItemStack trigger, AutoPlaceEnvironment env) {
                return BlocksToPlace.create(new ItemStack(Items.skull));
            }

            @Override
            public PlaceResult survivalPlaceBlock(MTEIndustrialBrainrotter machine, World world, int x, int y, int z,
                ItemStack trigger, AutoPlaceEnvironment env) {
                if (check(machine, world, x, y, z)) return PlaceResult.SKIP;
                if (!StructureLibAPI.isBlockTriviallyReplaceable(world, x, y, z, env.getActor()))
                    return PlaceResult.REJECT;
                ItemStack supplied = env.getSource()
                    .takeOne(stack -> stack.getItem() == Items.skull, true);
                if (supplied == null) {
                    if (env.getChatter() != null) env.getChatter()
                        .accept(
                            new ChatComponentTranslation(
                                "structurelib.autoplace.missing_block",
                                Items.skull.getItemStackDisplayName(new ItemStack(Items.skull))));
                    return PlaceResult.REJECT;
                }
                // The middle skull floats over air, which vanilla ItemSkull.onItemUse refuses to place.
                if (!placeBlock(machine, world, x, y, z, trigger)) return PlaceResult.REJECT;
                TileEntitySkull tile = (TileEntitySkull) world.getTileEntity(x, y, z);
                NBTTagCompound data = new NBTTagCompound();
                tile.writeToNBT(data);
                data.setByte("SkullType", (byte) supplied.getItemDamage());
                if (supplied.getItemDamage() == 3 && supplied.hasTagCompound()) {
                    NBTTagCompound tag = supplied.getTagCompound();
                    if (tag.hasKey("SkullOwner", 10)) data.setTag(
                        "Owner",
                        tag.getCompoundTag("SkullOwner")
                            .copy());
                    else if (tag.hasKey("SkullOwner", 8)) {
                        NBTTagCompound owner = new NBTTagCompound();
                        owner.setString("Name", tag.getString("SkullOwner"));
                        data.setTag("Owner", owner);
                    }
                }
                tile.readFromNBT(data);
                tile.markDirty();
                ItemStack payment = supplied.copy();
                payment.stackSize = 1;
                if (!env.getSource()
                    .takeOne(payment, false)) {
                    world.setBlockToAir(x, y, z);
                    return PlaceResult.REJECT;
                }
                world.markBlockForUpdate(x, y, z);
                return PlaceResult.ACCEPT;
            }
        };
    }

    /** Live pedestal connection established by the structure check. */
    public TilePedestal getPedestal() {
        return pedestal == null || pedestal.isInvalid() ? null : pedestal;
    }

    @Override
    public void checkMachine(IGregTechTileEntity tile, ItemStack stack, List<StructureError> errors) {
        pedestal = null;
        glassTier = -1;
        if (!checkPiece(MAIN, 2, 4, 0, errors)) {
            pedestal = null;
            return;
        }
        checkOneMaintenanceHatch(errors);
        checkExoticAndNormalEnergyHatches(errors);
        checkHasInputBus(errors);
        checkHasOutputBus(errors);
        checkHasOutputHatch(errors);
        for (MTEHatch hatch : getExoticAndNormalEnergyHatchList()) {
            int required = hatch.getTierForStructure();
            if (hatch.getConnectionType() == MTEHatch.ConnectionType.LASER)
                required = Math.max(required, VoltageIndex.UEV);
            if (glassTier < required) {
                errors.add(StructureErrors.glassTierNotEnough(required));
                break;
            }
        }
        if (!errors.isEmpty()) pedestal = null;
        // The pedestal is an external inventory and does not notify GT's hatch watchers.
        doPeriodicChecks = pedestal != null;
    }

    @Override
    public void construct(ItemStack stack, boolean hintsOnly) {
        buildPiece(MAIN, stack, hintsOnly, 2, 4, 0);
    }

    @Override
    public int survivalConstruct(ItemStack stack, int budget, ISurvivalBuildEnvironment env) {
        if (mMachine) return -1;
        return survivalBuildPiece(MAIN, stack, 2, 4, 0, budget, env, false, true);
    }

    @Override
    public RecipeMap<?> getRecipeMap() {
        return Brainrotter.RECIPES;
    }

    @Override
    protected ProcessingLogic createProcessingLogic() {
        return new ProcessingLogic() {

            @Override
            protected CheckRecipeResult validateRecipe(GTRecipe recipe) {
                if (pedestalMechanicsEnabled()) {
                    TilePedestal target = getPedestal();
                    if (target == null || target.getStackInSlot(0) != null)
                        return SimpleCheckRecipeResult.ofFailure("gtnhextranerf.brainrotter.pedestal_occupied");
                }
                setMaxParallel(
                    1 + 2 * Math.max(0, GTUtility.getTier(getAverageInputVoltage()) - GTUtility.getTier(recipe.mEUt)));
                return CheckRecipeResultRegistry.SUCCESSFUL;
            }

            @Override
            protected OverclockCalculator createOverclockCalculator(GTRecipe recipe) {
                return new OverclockCalculator() {

                    @Override
                    public double calculateMultiplierUnderOneTick() {
                        // Sub-tick optimizations must not multiply the configured parallel limit.
                        return 1;
                    }
                }.setRecipeEUt(recipe.mEUt)
                    .setEUt(availableVoltage)
                    .setAmperage(availableAmperage)
                    .setDuration(recipe.mDuration)
                    .setDurationModifier(speedBoost)
                    .setEUtDiscount(euModifier)
                    .setMaxTierSkips(maxTierSkips)
                    .setAmperageOC(true)
                    .setDurationDecreasePerOC(overClockTimeReduction)
                    .setEUtIncreasePerOC(overClockPowerIncrease);
            }
        }.setOverclock(3, 4)
            .noRecipeCaching();
    }

    @Override
    protected void setProcessingLogicPower(ProcessingLogic logic) {
        logic.setAvailableVoltage(getAverageInputVoltage());
        logic.setAvailableAmperage(getMaxInputAmps());
        logic.setAmperageOC(true);
    }

    @Override
    public void endRecipeProcessing() {
        super.endRecipeProcessing();
        if (!getBaseMetaTileEntity().isServerSide() || mMaxProgresstime <= 0) return;
        if (!checkRecipeResult.wasSuccessful()) {
            stopMachine(SimpleShutDownReason.ofCritical("gtnhextranerf.brainrotter.input_commit_failed"));
            return;
        }
        if (!pedestalMechanicsEnabled()) return;
        TilePedestal target = getPedestal();
        if (target == null || target.getStackInSlot(0) != null) {
            setResultIfFailure(SimpleCheckRecipeResult.ofFailure("gtnhextranerf.brainrotter.pedestal_occupied"));
            stopMachine(SimpleShutDownReason.ofCritical("gtnhextranerf.brainrotter.display_changed"));
            return;
        }
        // ME hatches have committed their input transaction before the display is published.
        display = GTOreDictUnificator.get(OrePrefixes.ingot, NerfMaterials.rawTungstenSteel(), 1);
        if (!display.hasTagCompound()) display.setTagCompound(new NBTTagCompound());
        display.getTagCompound()
            .setString(
                "gtnhextranerfBrainrotCycle",
                UUID.randomUUID()
                    .toString());
        displayPosition = new ChunkCoordinates(target.xCoord, target.yCoord, target.zCoord);
        target.setInventorySlotContents(0, display.copy());
        markDirty();
    }

    @Override
    public boolean onRunningTick(ItemStack stack) {
        // GT emits outputs immediately after incrementing progress, so validation must happen first.
        if (pedestalMechanicsEnabled() && mMaxProgresstime > 0 && mProgresstime + 1 >= mMaxProgresstime) {
            TilePedestal target = getPedestal();
            if (display == null || target == null
                || target != displayPedestal()
                || !ItemStack.areItemStacksEqual(display, target.getStackInSlot(0))) {
                stopMachine(SimpleShutDownReason.ofCritical("gtnhextranerf.brainrotter.display_changed"));
                return false;
            }
        }
        return super.onRunningTick(stack);
    }

    private TilePedestal displayPedestal() {
        if (displayPosition == null || getBaseMetaTileEntity() == null) return null;
        var world = getBaseMetaTileEntity().getWorld();
        if (!world.blockExists(displayPosition.posX, displayPosition.posY, displayPosition.posZ)) return null;
        return world.getTileEntity(
            displayPosition.posX,
            displayPosition.posY,
            displayPosition.posZ) instanceof TilePedestal target && !target.isInvalid() ? target : null;
    }

    private void clearDisplay() {
        if (getBaseMetaTileEntity() == null || !getBaseMetaTileEntity().isServerSide()) return;
        TilePedestal target = pedestalMechanicsEnabled() ? displayPedestal() : null;
        if (display != null && target != null && ItemStack.areItemStacksEqual(display, target.getStackInSlot(0))) {
            target.setInventorySlotContents(0, null);
        }
        display = null;
        displayPosition = null;
        markDirty();
    }

    @Override
    protected void outputAfterRecipe() {
        clearDisplay();
        super.outputAfterRecipe();
    }

    @Override
    public void stopMachine(ShutDownReason reason) {
        clearDisplay();
        super.stopMachine(reason);
    }

    @Override
    public void onRemoval() {
        clearDisplay();
        super.onRemoval();
    }

    @Override
    public void saveNBTData(NBTTagCompound nbt) {
        super.saveNBTData(nbt);
        nbt.removeTag(DISPLAY_NBT);
        if (display == null) return;
        NBTTagCompound saved = new NBTTagCompound();
        saved.setTag("Item", display.writeToNBT(new NBTTagCompound()));
        saved.setInteger("X", displayPosition.posX);
        saved.setInteger("Y", displayPosition.posY);
        saved.setInteger("Z", displayPosition.posZ);
        nbt.setTag(DISPLAY_NBT, saved);
    }

    @Override
    public void loadNBTData(NBTTagCompound nbt) {
        super.loadNBTData(nbt);
        NBTTagCompound saved = nbt.getCompoundTag(DISPLAY_NBT);
        display = ItemStack.loadItemStackFromNBT(saved.getCompoundTag("Item"));
        displayPosition = display == null ? null
            : new ChunkCoordinates(saved.getInteger("X"), saved.getInteger("Y"), saved.getInteger("Z"));
        // Never recreate the display here: missing pedestal contents must still abort on completion.
    }

    @Override
    public boolean supportsVoidProtection() {
        return true;
    }

    @Override
    public boolean supportsBatchMode() {
        return true;
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        return new MultiblockTooltipBuilder()
            .addMachineType(StatCollector.translateToLocal("gtnhextranerf.brainrotter.type"))
            .addInfo(StatCollector.translateToLocal("gtnhextranerf.brainrotter.power"))
            .addInfo(StatCollector.translateToLocal("gtnhextranerf.brainrotter.parallel"))
            .beginStructureBlock(5, 5, 3, true)
            .addController(StatCollector.translateToLocal("gtnhextranerf.brainrotter.structure.controller"))
            .addCasing("36", StatCollector.translateToLocal("GT5U.structure.tiered_glass"), false)
            .addCasing("10+", StatCollector.translateToLocal("gtnhextranerf.brainrotter.structure.wood"), false)
            .addOtherStructurePart("Pedestal", "1")
            .addStructureInfo(StatCollector.translateToLocal("gtnhextranerf.brainrotter.structure.hints"))
            .addEnergyHatch("1+", StatCollector.translateToLocal("gtnhextranerf.brainrotter.structure.base"), 1)
            .addMaintenanceHatch("1", StatCollector.translateToLocal("gtnhextranerf.brainrotter.structure.base"), 1)
            .addInputBus("1+", StatCollector.translateToLocal("gtnhextranerf.brainrotter.structure.base"), 1)
            .addOutputBus("1+", StatCollector.translateToLocal("gtnhextranerf.brainrotter.structure.base"), 1)
            .addInputHatch("0+", StatCollector.translateToLocal("gtnhextranerf.brainrotter.structure.base"), 1)
            .addOutputHatch("1+", StatCollector.translateToLocal("gtnhextranerf.brainrotter.structure.base"), 1)
            .toolTipFinisher();
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity tile, ForgeDirection side, ForgeDirection facing, int colorIndex,
        boolean active, boolean redstone) {
        return Textures.BlockIcons
            .createTextureWithCasing(this, side, facing, active, FRONT, FRONT_GLOW, FRONT_ACTIVE, FRONT_ACTIVE_GLOW);
    }

    @Override
    public ITexture getCasingTexture() {
        return Brainrotter.CASING_TEXTURE;
    }
}
