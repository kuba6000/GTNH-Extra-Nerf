package pl.kuba6000.gtnhextranerf.integration.opencomputers;

import java.util.Map;

import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import li.cil.oc.api.Driver;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.driver.SidedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.ManagedEnvironment;
import pl.kuba6000.gtnhextranerf.machines.MTEIronBathMeasurement;

/** Loaded only when OpenComputers is present. */
public final class IronBathMeasurementDriver implements SidedBlock {

    public static final String COMPONENT_NAME = "iron_bath_measurement";

    public static void register() {
        Driver.add(new IronBathMeasurementDriver());
    }

    private static MTEIronBathMeasurement hatch(World world, int x, int y, int z) {
        if (world.getTileEntity(x, y, z) instanceof IGregTechTileEntity tile
            && tile.getMetaTileEntity() instanceof MTEIronBathMeasurement measurement) return measurement;
        return null;
    }

    @Override
    public boolean worksWith(World world, int x, int y, int z, ForgeDirection side) {
        return hatch(world, x, y, z) != null;
    }

    @Override
    public li.cil.oc.api.network.ManagedEnvironment createEnvironment(World world, int x, int y, int z,
        ForgeDirection side) {
        MTEIronBathMeasurement hatch = hatch(world, x, y, z);
        return hatch == null ? null : new Environment(hatch);
    }

    public static final class Environment extends ManagedEnvironment implements NamedBlock {

        private final MTEIronBathMeasurement hatch;

        private Environment(MTEIronBathMeasurement hatch) {
            this.hatch = hatch;
            setNode(
                Network.newNode(this, Visibility.Network)
                    .withComponent(COMPONENT_NAME)
                    .create());
        }

        @Override
        public String preferredName() {
            return COMPONENT_NAME;
        }

        @Override
        public int priority() {
            return 10;
        }

        @Callback(
            doc = "function():table -- Reads a formed bath. Volumes use mB, temperatureK uses kelvin, carbonUnits/impurityUnits/calciteUnits use bath units. chargeGroups contains count and progress (0..1). Returns nil and an error when disconnected.")
        public Object[] read(Context context, Arguments arguments) {
            Map<String, Object> readings = hatch.readMeasurements();
            return readings == null ? new Object[] { null, "measurement hatch is not connected to a formed iron bath" }
                : new Object[] { readings };
        }
    }
}
