package pl.kuba6000.gtnhextranerf.integration;

import static com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil.formatFluid;
import static com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil.formatNumber;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StringTranslate;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidTankInfo;

import com.cleanroommc.modularui.api.RecipeViewerSettings;
import com.cleanroommc.modularui.api.widget.ITooltip;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.text.RichText;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.RichTooltip;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.LongSyncValue;
import com.cleanroommc.modularui.value.sync.ModularSyncManager;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.SyncHandler;
import com.cleanroommc.modularui.widget.AbstractParentWidget;
import com.mojang.authlib.GameProfile;

import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicMachineBronze;
import gregtech.common.tileentities.boilers.MTEBoilerBronze;
import gregtech.common.tileentities.machines.multi.steam.MTESteamMacerator;
import gregtech.common.tileentities.machines.multi.steam.MTESteamWaterPump;
import gtPlusPlus.xmod.gregtech.api.enums.GregtechItemList;
import gtPlusPlus.xmod.gregtech.api.metatileentity.implementations.base.MTEHatchCustomFluidBase;
import mcp.mobius.waila.api.IWailaDataAccessor;

/** Measures native Steam Grinder consumption against its actual input hatch. */
final class SteamConsumptionIntegrationTests {

    private SteamConsumptionIntegrationTests() {}

    static void verifyGrinder(WorldServer world) {
        BaseMetaTileEntity hatchTile = tile(
            world,
            (short) GregtechItemList.Hatch_Input_Steam.get(1)
                .getItemDamage(),
            55);
        MTEHatchCustomFluidBase hatch = (MTEHatchCustomFluidBase) hatchTile.getMetaTileEntity();
        int accepted = hatch.fill(ForgeDirection.UNKNOWN, Materials.Steam.getGas(10_000), true);
        check(accepted == 10_000, "Steam Grinder hatch did not accept the test steam");

        BaseMetaTileEntity grinderTile = tile(
            world,
            (short) GregtechItemList.Controller_SteamMaceratorMulti.get(1)
                .getItemDamage(),
            56);
        MTESteamMacerator grinder = (MTESteamMacerator) grinderTile.getMetaTileEntity();
        grinder.mSteamInputFluids.add(hatch);
        grinder.lEUt = -10;
        grinder.mEfficiency = 10_000;

        int before = hatch.getFluidAmount();
        for (int tick = 0; tick < 20; tick++) {
            check(grinder.onRunningTick(null), "Steam Grinder stopped despite sufficient steam");
        }
        int consumed = before - hatch.getFluidAmount();
        check(consumed == 1_000, "Steam Grinder used " + consumed + " L instead of 1,000 L in 20 ticks");

        hatch.setFillableStack(Materials.Steam.getGas(49));
        check(!grinder.onRunningTick(null), "Steam Grinder ran without enough steam for one tick");
        check(hatch.getFluidAmount() == 49, "Steam Grinder consumed steam after a failed tick");
    }

    static void verifySingleMachine(WorldServer world) {
        BaseMetaTileEntity machineTile = tile(
            world,
            (short) ItemList.Machine_Bronze_Macerator.get(1)
                .getItemDamage(),
            57);
        MTEBasicMachineBronze machine = (MTEBasicMachineBronze) machineTile.getMetaTileEntity();
        int capacity = Math.toIntExact(machineTile.getSteamCapacity() * 10);

        int simulated = machine.fill(ForgeDirection.UNKNOWN, Materials.Steam.getGas(capacity), false);
        check(simulated == capacity && machineTile.getStoredSteam() == 0, "Steam fill simulation changed the machine");
        int filled = machine.fill(ForgeDirection.UNKNOWN, Materials.Steam.getGas(capacity), true);
        check(filled == capacity, "Steam machine accepted " + filled + " L instead of " + capacity + " L");

        FluidTankInfo[] tanks = machine.getTankInfo(ForgeDirection.UNKNOWN);
        check(
            tanks.length == 1 && tanks[0].fluid != null
                && tanks[0].fluid.amount == capacity
                && tanks[0].capacity == capacity,
            "Steam machine tank information does not match its actual amount and capacity");

        for (int tick = 0; tick < 20; tick++) {
            check(machineTile.decreaseStoredEnergyUnits(10, false), "Steam machine stopped despite sufficient steam");
        }
        long stored = machineTile.getStoredSteam();
        int refill = machine.fill(ForgeDirection.UNKNOWN, Materials.Steam.getGas(capacity), false);
        check(refill == 2_000, "Steam machine used " + refill + " L instead of 2,000 L in 20 ticks");
        check(machineTile.getStoredSteam() == stored, "Steam refill simulation consumed or created steam");
        check(
            machine.fill(ForgeDirection.UNKNOWN, Materials.Steam.getGas(capacity), true) == refill,
            "Steam refill differed from its simulation");
    }

    static void verifyBoiler(WorldServer world) {
        BaseMetaTileEntity boilerTile = tile(
            world,
            (short) ItemList.Machine_Bronze_Boiler.get(1)
                .getItemDamage(),
            58);
        MTEBoilerBronze boiler = (MTEBoilerBronze) boilerTile.getMetaTileEntity();
        boiler.mTemperature = 200;
        boiler.mProcessingEnergy = 1_000;
        check(
            boiler.fill(ForgeDirection.UNKNOWN, Materials.Water.getFluid(1_000), true) == 1_000,
            "Small Coal Boiler did not accept its water");
        for (int tick = 21; tick <= 40; tick++) boiler.onPostTick(boilerTile, tick);
        check(
            boiler.getDrainableStack() != null && boiler.getDrainableStack().amount == 120,
            "A heated Small Coal Boiler did not produce 120 L in 20 ticks");
    }

    static void verifyDisplays(WorldServer world) {
        String translations = "GT5U.waila.machine.use_steam=STEAM_COST:%s\n" + "GTPP.waila.steam.use=STEAM_COST:%s\n"
            + "GT5U.machines.steam.amount=STEAM_GAUGE:%s/%s\n";
        StringTranslate.inject(new ByteArrayInputStream(translations.getBytes(StandardCharsets.UTF_8)));

        BaseMetaTileEntity singleTile = tile(
            world,
            (short) ItemList.Machine_Bronze_Macerator.get(1)
                .getItemDamage(),
            59);
        MTEBasicMachineBronze single = (MTEBasicMachineBronze) singleTile.getMetaTileEntity();
        NBTTagCompound singleTag = new NBTTagCompound();
        singleTag.setBoolean("isActiveSingleBlock", true);
        singleTag.setBoolean("isAllowedToWorkSingleBlock", true);
        singleTag.setInteger("eut", 13);
        singleTag.setInteger("maxProgressSingleBlock", 100);
        List<String> singleLines = new ArrayList<>();
        single.getWailaBody(null, singleLines, accessor(singleTag), null);
        check(
            singleLines.contains("STEAM_COST:" + formatNumber(2_600)),
            "Single Steam machine WAILA does not show its actual steam consumption: " + singleLines);

        BaseMetaTileEntity multiTile = tile(
            world,
            (short) GregtechItemList.Controller_SteamMaceratorMulti.get(1)
                .getItemDamage(),
            60);
        MTESteamMacerator grinder = (MTESteamMacerator) multiTile.getMetaTileEntity();
        NBTTagCompound multiTag = new NBTTagCompound();
        multiTag.setBoolean("isActive", true);
        multiTag.setBoolean("isAllowedToWork", true);
        multiTag.setInteger("maxProgress", 100);
        multiTag.setLong("energyUsage", 13);
        List<String> multiLines = new ArrayList<>();
        grinder.getWailaBody(null, multiLines, accessor(multiTag), null);
        check(
            multiLines.contains("STEAM_COST:" + formatNumber(1_300)),
            "Steam Grinder WAILA does not show its actual steam consumption: " + multiLines);

        FakePlayer player = FakePlayerFactory.get(world, new GameProfile(UUID.randomUUID(), "SteamGaugeTest"));
        BaseMetaTileEntity hatchTile = tile(
            world,
            (short) GregtechItemList.Hatch_Input_Steam.get(1)
                .getItemDamage(),
            63);
        MTEHatchCustomFluidBase hatch = (MTEHatchCustomFluidBase) hatchTile.getMetaTileEntity();
        hatch.setFillableStack(Materials.Steam.getGas(10_000));
        grinder.mSteamInputFluids.add(hatch);
        grinder.lEUt = -19;
        grinder.mEfficiency = 1_520;
        multiTile.setActive(true);
        NBTTagCompound actualTag = new NBTTagCompound();
        grinder.getWailaNBTData(player, multiTile, actualTag, world, 60, 240, 8);
        int before = hatch.getFluidAmount();
        check(grinder.onRunningTick(null), "Steam Grinder stopped despite sufficient steam");
        int usedPerTick = before - hatch.getFluidAmount();
        List<String> actualLines = new ArrayList<>();
        grinder.getWailaBody(null, actualLines, accessor(actualTag), null);
        check(
            usedPerTick > 0 && actualLines.contains("STEAM_COST:" + formatNumber(usedPerTick * 20L)),
            "Steam Grinder WAILA differs from measured consumption: " + actualLines + ", " + usedPerTick + " L/t");

        singleTile.setStoredSteam(1_234);
        GaugeSyncManager sync = new GaugeSyncManager();
        ModularPanel panel = single.buildUI(
            new PosGuiData(player, singleTile.xCoord, singleTile.yCoord, singleTile.zCoord),
            sync,
            new UISettings(RecipeViewerSettings.DUMMY));
        for (LongSyncValue value : sync.longValues) value.updateCacheFromSource(false);
        List<String> tooltips = new ArrayList<>();
        collectTooltips(panel, tooltips);
        check(
            tooltips.contains(
                "STEAM_GAUGE:" + formatNumber(12_340) + "/" + formatFluid(singleTile.getSteamCapacity() * 10)),
            "Steam machine gauge does not show its actual amount and capacity: " + tooltips);
    }

    static void verifyPump(WorldServer world) {
        BaseMetaTileEntity hatchTile = tile(
            world,
            (short) GregtechItemList.Hatch_Input_Steam.get(1)
                .getItemDamage(),
            61);
        MTEHatchCustomFluidBase hatch = (MTEHatchCustomFluidBase) hatchTile.getMetaTileEntity();
        hatch.setFillableStack(Materials.Steam.getGas(10_000));
        BaseMetaTileEntity pumpTile = tile(
            world,
            (short) GregtechItemList.WaterPump.get(1)
                .getItemDamage(),
            62);
        MTESteamWaterPump pump = (MTESteamWaterPump) pumpTile.getMetaTileEntity();
        pump.mSteamInputFluids.add(hatch);
        check(pump.tryConsumeSteam(1_500), "Steam Water Pump could not consume its normal cost");
        check(hatch.getFluidAmount() == 8_500, "Steam Water Pump's normal cost was changed");
        pump.lEUt = -10;
        pump.mEfficiency = 10_000;
        check(pump.onRunningTick(null), "Steam Water Pump could not run with sufficient steam");
        check(hatch.getFluidAmount() == 8_490, "Steam Water Pump inherited the multiblock multiplier");

        NBTTagCompound tag = new NBTTagCompound();
        tag.setBoolean("isActive", true);
        tag.setLong("energyUsage", 13);
        List<String> lines = new ArrayList<>();
        pump.getWailaBody(null, lines, accessor(tag), null);
        check(lines.contains("STEAM_COST:" + formatNumber(260)), "Steam Water Pump WAILA was changed: " + lines);
    }

    private static IWailaDataAccessor accessor(NBTTagCompound tag) {
        return (IWailaDataAccessor) Proxy.newProxyInstance(
            IWailaDataAccessor.class.getClassLoader(),
            new Class<?>[] { IWailaDataAccessor.class },
            (proxy, method, args) -> method.getName()
                .equals("getNBTData") ? tag : null);
    }

    private static boolean collectTooltips(IWidget widget, List<String> lines) {
        if (widget instanceof AbstractParentWidget) {
            List<IWidget> children = ((AbstractParentWidget<?, ?>) widget).getChildren();
            for (int i = children.size() - 1; i >= 0; i--) {
                if (collectTooltips(children.get(i), lines)) return true;
            }
        }
        if (widget instanceof ITooltip) {
            RichTooltip tooltip = ((ITooltip<?>) widget).getTooltip();
            if (tooltip != null) {
                tooltip.buildTooltip();
                if (tooltip.getRichText() instanceof RichText) {
                    for (String line : ((RichText) tooltip.getRichText()).getAsStrings()) {
                        if (line.startsWith("STEAM_GAUGE:")) {
                            lines.add(line);
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private static final class GaugeSyncManager extends PanelSyncManager {

        private final List<LongSyncValue> longValues = new ArrayList<>();

        private GaugeSyncManager() {
            super(new ModularSyncManager(false), false);
        }

        @Override
        public PanelSyncManager syncValue(String key, int id, SyncHandler<?> handler) {
            if (handler instanceof LongSyncValue) longValues.add((LongSyncValue) handler);
            return super.syncValue(key, id, handler);
        }
    }

    private static BaseMetaTileEntity tile(WorldServer world, short id, int x) {
        BaseMetaTileEntity tile = new BaseMetaTileEntity();
        tile.setWorldObj(world);
        tile.xCoord = x;
        tile.yCoord = 240;
        tile.zCoord = 8;
        tile.setInitialValuesAsNBT(new NBTTagCompound(), id);
        tile.setOwnerUuid(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        tile.setOwnerName("SteamTest");
        return tile;
    }

    private static void check(boolean success, String message) {
        if (!success) throw new AssertionError(message);
    }
}
