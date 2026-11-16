package pl.kuba6000.gtnhextranerf.machines;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.CycleButtonWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;

import gregtech.api.enums.GTValues;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatchOutput;
import gregtech.api.modularui2.GTGuiTextures;
import gregtech.common.gui.modularui.hatch.MTEHatchOutputGui;
import pl.kuba6000.gtnhextranerf.machines.ironbath.IronBath;

/** A physical outlet whose ordinary output tank contains only material already removed from the bath. */
public final class MTEIronBathTap extends MTEHatchOutput {

    private int amountPerPulse = 144;
    private int redstoneMode;
    private boolean wasPowered;
    private long lastDischargeTick = Long.MIN_VALUE;

    public MTEIronBathTap(int id, String name, String regionalName, int tier) {
        super(id, name, regionalName, tier);
    }

    private MTEIronBathTap(String name, int tier, String[] description, ITexture[][][] textures) {
        super(name, tier, description, textures);
    }

    @Override
    public MTEIronBathTap newMetaEntity(IGregTechTileEntity tile) {
        return new MTEIronBathTap(mName, mTier, mDescriptionArray, mTextures);
    }

    @Override
    public String getLocalName() {
        return StatCollector.translateToLocalFormatted("gtnhextranerf.iron_bath.tap.name", GTValues.VN[mTier]);
    }

    @Override
    public String[] getDescription() {
        return new String[] { StatCollector.translateToLocal("gtnhextranerf.iron_bath.tap.description"),
            StatCollector.translateToLocal("gtnhextranerf.iron_bath.tap.screwdriver") };
    }

    public int getAmountPerPulse() {
        return amountPerPulse;
    }

    public void setAmountPerPulse(int amount) {
        amountPerPulse = Math.max(1, Math.min(amount, getPulseLimit()));
        if (getBaseMetaTileEntity() != null) getBaseMetaTileEntity().markDirty();
    }

    public int getPulseLimit() {
        return (int) Math.min(Integer.MAX_VALUE, 144L << (2 * Math.max(0, mTier - 1)));
    }

    /** 0: rising edge, 1: powered, 2: continuous, 3: stopped. */
    public int getRedstoneMode() {
        return redstoneMode;
    }

    public void setRedstoneMode(int mode) {
        redstoneMode = Math.max(0, Math.min(3, mode));
        if (getBaseMetaTileEntity() != null) getBaseMetaTileEntity().markDirty();
    }

    public boolean discharge(IronBath bath, int band) {
        IGregTechTileEntity tile = getBaseMetaTileEntity();
        if (tile == null || !tile.isServerSide()) return false;
        long tick = tile.getWorld()
            .getTotalWorldTime();
        if (lastDischargeTick == tick) return false;
        lastDischargeTick = tick;
        boolean powered = false;
        for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS)
            powered |= tile.getInternalInputRedstoneSignal(side) > 0;
        boolean requested = redstoneMode == 2 || (redstoneMode == 1 && powered)
            || (redstoneMode == 0 && powered && !wasPowered);
        if (wasPowered != powered) {
            wasPowered = powered;
            tile.markDirty();
        }
        if (!requested || !tile.isAllowedToWork()) return false;

        IronBath.Tap preview = bath.previewTap(band, amountPerPulse);
        if (preview.amountMb() == 0) return false;
        FluidStack fluid = IronBathFurnace.material(preview.product())
            .getMolten(preview.amountMb());
        if (fluid == null) throw new IllegalStateException("Missing molten bath product: " + preview.product());
        int accepted = fill(fluid, false);
        if (accepted == 0) return false;
        IronBath.Tap removed = bath.tap(band, accepted);
        if (removed.amountMb() == 0) return false;
        fluid.amount = removed.amountMb();
        if (fill(fluid, true) != removed.amountMb())
            throw new IllegalStateException("Iron bath outlet changed during discharge");
        tile.markDirty();
        return true;
    }

    @Override
    public void onScrewdriverRightClick(ForgeDirection side, EntityPlayer player, float x, float y, float z,
        ItemStack tool) {
        if (!getBaseMetaTileEntity().isServerSide()) return;
        if (player.isSneaking()) {
            setRedstoneMode((redstoneMode + 1) % 4);
        } else {
            int limit = getPulseLimit();
            setAmountPerPulse(
                amountPerPulse < 16 ? 16 : amountPerPulse < 144 ? 144 : amountPerPulse < limit ? limit : 1);
        }
        player.addChatMessage(
            new ChatComponentTranslation(
                "gtnhextranerf.iron_bath.tap.settings",
                amountPerPulse,
                StatCollector.translateToLocal("gtnhextranerf.iron_bath.tap.mode." + redstoneMode)));
    }

    @Override
    public void saveNBTData(NBTTagCompound nbt) {
        super.saveNBTData(nbt);
        nbt.setInteger("BathTapAmount", amountPerPulse);
        nbt.setInteger("BathTapRedstone", redstoneMode);
        nbt.setBoolean("BathTapWasPowered", wasPowered);
    }

    @Override
    public void loadNBTData(NBTTagCompound nbt) {
        super.loadNBTData(nbt);
        if (nbt.hasKey("BathTapAmount")) setAmountPerPulse(nbt.getInteger("BathTapAmount"));
        setRedstoneMode(nbt.getInteger("BathTapRedstone"));
        wasPowered = nbt.getBoolean("BathTapWasPowered");
    }

    @Override
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager sync, UISettings settings) {
        return new TapGui(this).build(data, sync, settings);
    }

    private static final class TapGui extends MTEHatchOutputGui {

        private final MTEIronBathTap tap;

        private TapGui(MTEIronBathTap tap) {
            super(tap);
            this.tap = tap;
        }

        @Override
        public void registerSyncValues(PanelSyncManager sync) {
            super.registerSyncValues(sync);
            sync.syncValue("bathAmount", new IntSyncValue(tap::getAmountPerPulse, tap::setAmountPerPulse).allowC2S());
            sync.syncValue("bathTrigger", new IntSyncValue(tap::getRedstoneMode, tap::setRedstoneMode).allowC2S());
        }

        @Override
        protected int getBasePanelHeight() {
            return 222;
        }

        @Override
        protected ParentWidget<?> createContentSection(ModularPanel panel, PanelSyncManager sync) {
            IntSyncValue amount = sync.findSyncHandler("bathAmount", IntSyncValue.class);
            IntSyncValue trigger = sync.findSyncHandler("bathTrigger", IntSyncValue.class);
            CycleButtonWidget mode = new CycleButtonWidget().stateCount(4)
                .value(trigger)
                .name("bathTrigger")
                .pos(58, 26)
                .size(18)
                .stateOverlay(0, GTGuiTextures.OVERLAY_BUTTON_CYCLIC)
                .stateOverlay(1, GTGuiTextures.OVERLAY_BUTTON_REDSTONE_ON)
                .stateOverlay(2, GTGuiTextures.OVERLAY_BUTTON_CHECKMARK)
                .stateOverlay(3, GTGuiTextures.OVERLAY_BUTTON_CROSS);
            for (int state = 0; state < 4; state++) {
                mode.addTooltip(state, IKey.lang("gtnhextranerf.iron_bath.tap.mode.description." + state));
            }
            mode.addTooltipLine(IKey.lang("gtnhextranerf.iron_bath.tap.mode.cycle"));
            ParentWidget<?> settings = new ParentWidget<>().size(162, 46)
                .child(
                    IKey.lang("gtnhextranerf.iron_bath.tap.amount")
                        .alignment(Alignment.CenterLeft)
                        .asWidget()
                        .pos(0, 0)
                        .size(54, 18))
                .child(
                    new TextFieldWidget().value(amount)
                        .name("bathAmount")
                        .numbersInt(() -> 1, tap::getPulseLimit)
                        .background(GTGuiTextures.BACKGROUND_TEXT_FIELD)
                        .pos(58, 0)
                        .size(80, 18)
                        .addTooltipLine(IKey.lang("gtnhextranerf.iron_bath.tap.amount.range", tap.getPulseLimit())))
                .child(
                    IKey.str("mB")
                        .alignment(Alignment.CenterLeft)
                        .asWidget()
                        .pos(142, 0)
                        .size(20, 18))
                .child(
                    IKey.lang("gtnhextranerf.iron_bath.tap.control")
                        .alignment(Alignment.CenterLeft)
                        .asWidget()
                        .pos(0, 26)
                        .size(54, 18))
                .child(mode)
                .child(
                    IKey.dynamic(
                        () -> StatCollector.translateToLocal("gtnhextranerf.iron_bath.tap.mode." + trigger.getValue()))
                        .alignment(Alignment.CenterLeft)
                        .asWidget()
                        .pos(82, 26)
                        .size(80, 18));
            return Flow.column()
                .expanded()
                .fullWidth()
                .crossAxisAlignment(Alignment.CrossAxis.START)
                .childPadding(8)
                .child(super.createContentSection(panel, sync).height(54))
                .child(settings);
        }
    }
}
