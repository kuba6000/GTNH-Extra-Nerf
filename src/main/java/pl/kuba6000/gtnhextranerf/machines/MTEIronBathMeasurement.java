package pl.kuba6000.gtnhextranerf.machines;

import java.util.Map;

import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;

import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.render.TextureFactory;

/** Optional telemetry port. The furnace owns all measured state. */
public final class MTEIronBathMeasurement extends MTEHatch {

    private MTEIronBathFurnace controller;

    public MTEIronBathMeasurement(int id, String name, String regionalName) {
        super(id, name, regionalName, 6, 0, "Read-only iron bath measurements for OpenComputers");
    }

    private MTEIronBathMeasurement(String name, String[] description, ITexture[][][] textures) {
        super(name, 6, 0, description, textures);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new MTEIronBathMeasurement(mName, mDescriptionArray, mTextures);
    }

    void bind(MTEIronBathFurnace controller) {
        this.controller = controller;
    }

    public Map<String, Object> readMeasurements() {
        return controller == null ? null : controller.readMeasurements(this);
    }

    @Override
    public String getLocalName() {
        return StatCollector.translateToLocal("gtnhextranerf.iron_bath.measurement.name");
    }

    @Override
    public String[] getDescription() {
        return new String[] { StatCollector.translateToLocal("gtnhextranerf.iron_bath.measurement.description"),
            StatCollector.translateToLocal("gtnhextranerf.iron_bath.measurement.connection") };
    }

    @Override
    public boolean isValidSlot(int index) {
        return false;
    }

    @Override
    public boolean isFacingValid(ForgeDirection facing) {
        return true;
    }

    @Override
    public boolean allowPullStack(IGregTechTileEntity tile, int index, ForgeDirection side, ItemStack stack) {
        return false;
    }

    @Override
    public boolean allowPutStack(IGregTechTileEntity tile, int index, ForgeDirection side, ItemStack stack) {
        return false;
    }

    @Override
    public ITexture[] getTexturesActive(ITexture baseTexture) {
        return getTexturesInactive(baseTexture);
    }

    @Override
    public ITexture[] getTexturesInactive(ITexture baseTexture) {
        return new ITexture[] { baseTexture, TextureFactory.of(Textures.BlockIcons.OVERLAY_HATCH_HEAT_SENSOR) };
    }
}
