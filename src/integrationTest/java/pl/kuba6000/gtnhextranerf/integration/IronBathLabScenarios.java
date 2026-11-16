package pl.kuba6000.gtnhextranerf.integration;

import java.awt.Rectangle;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.MinecraftException;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBTimerQuery;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLContext;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.network.ModularNetwork;
import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.cleanroommc.modularui.widget.WidgetTree;
import com.cleanroommc.modularui.widgets.ItemDisplayWidget;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;
import com.gtnewhorizons.horizonqa.api.client.ClickTarget;
import com.gtnewhorizons.horizonqa.api.client.ClientScenario;
import com.gtnewhorizons.horizonqa.api.client.ClientTarget;
import com.gtnewhorizons.horizonqa.api.client.ClientTest;
import com.mojang.authlib.GameProfile;
import com.sun.management.ThreadMXBean;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.metatileentity.BaseMetaPipeEntity;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEFluidPipe;
import gregtech.api.metatileentity.implementations.MTEHatchEnergyDebug;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTUtility;
import pl.kuba6000.gtnhextranerf.Config;
import pl.kuba6000.gtnhextranerf.machines.IronBathFurnace;
import pl.kuba6000.gtnhextranerf.machines.IronBathStructure;
import pl.kuba6000.gtnhextranerf.machines.MTEIronBathFurnace;
import pl.kuba6000.gtnhextranerf.machines.MTEIronBathTap;
import pl.kuba6000.gtnhextranerf.machines.ironbath.IronBath;
import pl.kuba6000.gtnhextranerf.machines.ironbath.IronProduct;
import pl.kuba6000.gtnhextranerf.progression.MachineProgression;
import pl.kuba6000.gtnhextranerf.progression.ProgressionData;
import pl.kuba6000.gtnhextranerf.progression.ProgressionTier;
import pl.kuba6000.gtnhextranerf.telemetry.ConsentPrompt;

/** Builds the walkable Iron Bath laboratory and verifies its native automation. */
@GameTestHolder(value = "gtnhextranerflabtests", clientOnly = true)
public final class IronBathLabScenarios {

    private static final int FLOOR = 99;
    private static final int GROUND_Y = FLOOR + 1;
    private static final int STATION_Y = GROUND_Y + 3;
    private static final int STATION_Z = 100;
    private static final int MANUAL_X = 1024;
    private static final int AUTOMATIC_X = 1052;
    private static final int SPAWN_X = 1038;
    private static final int SPAWN_Z = 94;

    private IronBathLabScenarios() {}

    @GameTest(template = "gtnhextranerftests:native_fixture", timeoutTicks = 2400)
    public static void maximumCoilChargeGroups(GameTestHelper helper) {
        Station[] fixture = new Station[1];
        int[] guiScale = new int[1];
        ClientTest.scenario(helper)
            .defaultTimeoutTicks(600)
            .server("construct an isolated maximum-coil controller", () -> {
                WorldServer world = helper.getWorld();
                EntityPlayerMP viewer = player();
                double px = viewer.posX, py = viewer.posY, pz = viewer.posZ;
                helper.afterTest(() -> {
                    if (viewer.openContainer != viewer.inventoryContainer) viewer.closeScreen();
                    viewer.setPositionAndUpdate(px, py, pz);
                    for (int x = 1194; x <= 1214; x++) for (int y = FLOOR - 3; y <= STATION_Y + 11; y++)
                        for (int z = 94; z <= 108; z++) world.setBlockToAir(x, y, z);
                });
                FixtureWorld.loadAround(world, 1200, STATION_Z);
                for (int x = 1194; x <= 1214; x++)
                    for (int z = 94; z <= 108; z++) world.setBlock(x, FLOOR, z, Blocks.stonebrick, 0, 3);
                FakePlayer placer = FakePlayerFactory.getMinecraft(world);
                Station station = fixture[0] = new Station(world, placer, 1200, ItemList.Hatch_Input_Bus_LV, 1, 1);
                station.controller.disableWorking();
                Block coilBlock = Block.getBlockFromItem(
                    ItemList.Casing_Coil_Cupronickel.get(1)
                        .getItem());
                ItemStack highest = ItemList.Casing_Coil_Eternal.get(1);
                check(highest.getItemDamage() == 13, "The highest native coil metadata changed");
                int replaced = 0;
                for (IronBathStructure.Offset coil : IronBathStructure.COIL_POSITIONS) {
                    int x = 1200 + coil.x(), y = STATION_Y + coil.y(), z = STATION_Z + coil.z();
                    if (world.getBlock(x, y, z) != coilBlock) continue;
                    check(
                        ((ItemBlock) highest.getItem()).placeBlockAt(
                            highest,
                            placer,
                            world,
                            x,
                            y,
                            z,
                            1,
                            0.5f,
                            0.5f,
                            0.5f,
                            highest.getItemDamage()),
                        "Could not install the maximum coil");
                    replaced++;
                }
                check(
                    replaced == 50 && station.machine.checkStructure(true, station.controller),
                    "The fifty maximum-tier coils did not form the native structure");
                IronBath charged = station.state();
                check(
                    charged.capacityMb() == 603979776,
                    "Maximum native coils did not configure 4194304 ingots of capacity");
                check(
                    charged.addMetal(
                        IronProduct.IRON,
                        224 * 144,
                        GTOreDictUnificator.get(OrePrefixes.ingot, Materials.Iron, 1)),
                    "Maximum-coil fixture did not accept its cold Iron group");
                NBTTagCompound saved = new NBTTagCompound();
                station.machine.saveNBTData(saved);
                charged.save(saved.getCompoundTag("IronBath"));
                station.machine.loadNBTData(saved);
                station.controller.disableWorking();
                check(
                    station.state()
                        .meltingItems() == 224,
                    "The maximum-coil charge did not persist");
                viewer.setPositionAndUpdate(1200.5, STATION_Y, STATION_Z - 1.5);
            })
            .client("prepare maximum-coil GUI scale", client -> {
                guiScale[0] = Minecraft.getMinecraft().gameSettings.guiScale;
                Minecraft.getMinecraft().gameSettings.guiScale = 2;
            })
            .afterTest(client -> Minecraft.getMinecraft().gameSettings.guiScale = guiScale[0])
            .awaitClient("the maximum-coil controller reaches the client", client -> {
                Minecraft mc = Minecraft.getMinecraft();
                check(
                    mc.theWorld != null
                        && mc.theWorld.getTileEntity(1200, STATION_Y, STATION_Z) instanceof BaseMetaTileEntity
                        && ((BaseMetaTileEntity) mc.theWorld.getTileEntity(1200, STATION_Y, STATION_Z))
                            .getMetaTileEntity() instanceof MTEIronBathFurnace,
                    "The maximum-coil controller has not synchronized");
            })
            .server(
                "open the real maximum-coil controller",
                () -> check(
                    fixture[0].machine.onRightclick(fixture[0].controller, player()),
                    "Maximum-coil controller did not open"))
            .awaitScreen(GuiContainerWrapper.class)
            .awaitClient("the maximum native charge is one displayed group", client -> {
                ItemStack item = ((ItemDisplayWidget) chargeWidget("bathChargeItem0")).getStackForRecipeViewer();
                check(item != null && item.stackSize == 1, "The grouped Iron icon has not synchronized");
                check(
                    ((ItemDisplayWidget) chargeWidget("bathChargeItem1")).getStackForRecipeViewer() == null,
                    "Identical charges split into multiple displayed groups");
            })
            .click(blankCorner())
            .capture("max-coil-grouped-charge")
            .escape()
            .awaitClient(
                "maximum-coil screen closed",
                client -> check(Minecraft.getMinecraft().currentScreen == null, "Maximum-coil screen remains open"))
            .awaitServer(
                "maximum-coil container closed",
                () -> check(
                    player().openContainer == player().inventoryContainer,
                    "Maximum-coil container remains open"))
            .succeed();
    }

    @GameTest(template = "gtnhextranerftests:native_fixture", timeoutTicks = 2400)
    public static void controllerRenderingProfile(GameTestHelper helper) {
        Station[] fixture = new Station[1];
        int[] previousScale = new int[1];
        List<ControllerRenderProfile> profiles = new ArrayList<>();
        ClientScenario scenario = IronBathScenarios.prepareClientView(helper)
            .defaultTimeoutTicks(600)
            .server("construct a stopped controller for matched rendering samples", () -> {
                WorldServer world = helper.getWorld();
                EntityPlayerMP viewer = player();
                double px = viewer.posX, py = viewer.posY, pz = viewer.posZ;
                WorldSettings.GameType previousMode = viewer.theItemInWorldManager.getGameType();
                boolean previousFlying = viewer.capabilities.isFlying;
                helper.afterTest(() -> {
                    viewer.closeScreen();
                    viewer.theItemInWorldManager.setGameType(previousMode);
                    viewer.capabilities.isFlying = previousFlying;
                    viewer.sendPlayerAbilities();
                    viewer.setPositionAndUpdate(px, py, pz);
                    for (int x = 1194; x <= 1214; x++) for (int y = FLOOR - 3; y <= STATION_Y + 11; y++)
                        for (int z = 94; z <= 108; z++) world.setBlockToAir(x, y, z);
                });
                FixtureWorld.loadAround(world, 1200, STATION_Z);
                for (int x = 1194; x <= 1214; x++)
                    for (int z = 94; z <= 108; z++) world.setBlock(x, FLOOR, z, Blocks.stonebrick, 0, 3);
                fixture[0] = new Station(
                    world,
                    FakePlayerFactory.getMinecraft(world),
                    1200,
                    ItemList.Hatch_Input_Bus_LV,
                    1,
                    1);
                fixture[0].controller.disableWorking();
                viewer.theItemInWorldManager.setGameType(WorldSettings.GameType.CREATIVE);
                viewer.capabilities.isFlying = true;
                viewer.motionY = 0;
                viewer.sendPlayerAbilities();
                viewer.setPositionAndUpdate(1200.5, STATION_Y, STATION_Z - 1.5);
                int[] previousWindow = { -1 };
                helper.onEachTick("render profile player and container lifecycle", () -> {
                    int window = viewer.openContainer.windowId;
                    if (window == previousWindow[0]) return;
                    previousWindow[0] = window;
                    helper.recordDiagnostic(
                        "render profile lifecycle: worldTick=" + world.getTotalWorldTime()
                            + ", window="
                            + window
                            + ", position="
                            + viewer.posX
                            + ","
                            + viewer.posY
                            + ","
                            + viewer.posZ
                            + ", controllerDistanceSq="
                            + viewer.getDistanceSq(1200.5, STATION_Y + 0.5, STATION_Z + 0.5)
                            + ", flying="
                            + viewer.capabilities.isFlying);
                });
            })
            .resizeWindow(1280, 720)
            .client("set the matched rendering scale", client -> {
                previousScale[0] = Minecraft.getMinecraft().gameSettings.guiScale;
                Minecraft.getMinecraft().gameSettings.guiScale = 2;
            })
            .afterTest(client -> {
                for (int index = profiles.size() - 1; index >= 0; index--) profiles.get(index)
                    .close();
                Minecraft.getMinecraft().gameSettings.guiScale = previousScale[0];
            })
            .awaitClient("the render-profile controller reaches the client", client -> {
                Minecraft mc = Minecraft.getMinecraft();
                check(
                    mc.theWorld != null
                        && mc.theWorld.getTileEntity(1200, STATION_Y, STATION_Z) instanceof BaseMetaTileEntity
                        && ((BaseMetaTileEntity) mc.theWorld.getTileEntity(1200, STATION_Y, STATION_Z))
                            .getMetaTileEntity() instanceof MTEIronBathFurnace,
                    "The render-profile controller has not synchronized");
            })
            .server(
                "open the empty Cupronickel controller",
                () -> check(
                    fixture[0].machine.onRightclick(fixture[0].controller, player()),
                    "Empty controller did not open"))
            .awaitScreen(GuiContainerWrapper.class)
            .awaitClient("empty sixteen-slot panel synchronized", client -> {
                check(
                    ((ItemDisplayWidget) chargeWidget("bathChargeItem0")).getStackForRecipeViewer() == null,
                    "The empty profile has a charge");
            })
            .capture("profile-empty-16");
        profileController(scenario, profiles, "empty-cupronickel-16");
        scenario.server("close the rendering fixture container", IronBathLabScenarios::closeRenderingContainer)
            .awaitServer(
                "empty profile container closed",
                () -> check(player().openContainer == player().inventoryContainer, "Profile container remains open"))
            .server("prepare the native occupied maximum-coil panel", () -> {
                Station station = fixture[0];
                WorldServer world = helper.getWorld();
                FakePlayer placer = FakePlayerFactory.getMinecraft(world);
                ItemStack highest = ItemList.Casing_Coil_Eternal.get(1);
                for (IronBathStructure.Offset coil : IronBathStructure.COIL_POSITIONS) check(
                    ((ItemBlock) highest.getItem()).placeBlockAt(
                        highest,
                        placer,
                        world,
                        1200 + coil.x(),
                        STATION_Y + coil.y(),
                        STATION_Z + coil.z(),
                        1,
                        0.5f,
                        0.5f,
                        0.5f,
                        highest.getItemDamage()),
                    "Could not install the profile's Eternal coils");
                check(station.machine.checkStructure(true, station.controller), "Maximum profile furnace did not form");
                IronBath charged = station.state();
                check(charged.capacityMb() == 603979776, "Maximum profile has the wrong tank capacity");
                NBTTagCompound hot = new NBTTagCompound();
                charged.save(hot);
                int residentMetal = charged.capacityMb() / 4 * 3;
                hot.setInteger("Metal", residentMetal);
                hot.setInteger("Slag", charged.capacityMb() / 20);
                hot.setDouble("Carbon", residentMetal / 144.0);
                hot.setDouble("Heat", 0);
                charged.load(hot);
                hot.setDouble("Heat", charged.heatDemand(2000));
                charged.load(hot);
                check(
                    charged.addMetal(
                        IronProduct.IRON,
                        224 * 144,
                        GTOreDictUnificator.get(OrePrefixes.ingot, Materials.Iron, 1)),
                    "Maximum profile charge was rejected");
                NBTTagCompound tag = new NBTTagCompound();
                station.machine.saveNBTData(tag);
                charged.save(tag.getCompoundTag("IronBath"));
                station.machine.loadNBTData(tag);
                station.controller.disableWorking();
                check(station.machine.onRightclick(station.controller, player()), "Occupied controller did not open");
            })
            .awaitScreen(GuiContainerWrapper.class)
            .awaitClient("the occupied profile group synchronized", client -> {
                check(
                    ((ItemDisplayWidget) chargeWidget("bathChargeItem0")).getStackForRecipeViewer() != null,
                    "Occupied profile group has not synchronized");
                check(
                    ((ItemDisplayWidget) chargeWidget("bathChargeItem1")).getStackForRecipeViewer() == null,
                    "The occupied profile split identical items");
            })
            .capture("profile-occupied-224");
        profileController(scenario, profiles, "occupied-eternal-224");
        scenario.server("close the rendering fixture container", IronBathLabScenarios::closeRenderingContainer)
            .awaitServer(
                "close the matched occupied profile",
                () -> check(player().openContainer == player().inventoryContainer, "Profile container remains open"))
            .server("queue seven hundred items in two groups", () -> {
                IronBath bath = fixture[0].state();
                check(
                    bath.addMetal(
                        IronProduct.CAST_IRON,
                        476 * 144,
                        GTOreDictUnificator.get(OrePrefixes.ingot, Materials.CastIron, 1)),
                    "Expanded processing buffer rejected its additional charge");
                NBTTagCompound saved = new NBTTagCompound();
                fixture[0].machine.saveNBTData(saved);
                bath.save(saved.getCompoundTag("IronBath"));
                fixture[0].machine.loadNBTData(saved);
                check(
                    fixture[0].machine.onRightclick(fixture[0].controller, player()),
                    "Expanded controller did not open");
            })
            .awaitScreen(GuiContainerWrapper.class)
            .awaitClient("the seven-hundred-item buffer shows two groups", client -> {
                ItemStack first = ((ItemDisplayWidget) chargeWidget("bathChargeItem0")).getStackForRecipeViewer();
                ItemStack second = ((ItemDisplayWidget) chargeWidget("bathChargeItem1")).getStackForRecipeViewer();
                check(
                    ItemStack.areItemStacksEqual(first, GTOreDictUnificator.get(OrePrefixes.ingot, Materials.Iron, 1)),
                    "The Iron group did not synchronize");
                check(
                    ItemStack
                        .areItemStacksEqual(second, GTOreDictUnificator.get(OrePrefixes.ingot, Materials.CastIron, 1)),
                    "The Cast Iron group did not synchronize");
                check(
                    ((ItemDisplayWidget) chargeWidget("bathChargeItem2")).getStackForRecipeViewer() == null,
                    "The seven-hundred-item buffer has more than two groups");
            })
            .capture("profile-occupied-700-grouped");
        profileController(scenario, profiles, "occupied-eternal-700-grouped");
        scenario.server("close the rendering fixture container", IronBathLabScenarios::closeRenderingContainer)
            .awaitServer(
                "close the grouped buffer",
                () -> check(player().openContainer == player().inventoryContainer, "Grouped buffer remains open"))
            .server("prepare many independent charge groups", () -> {
                IronBath bath = fixture[0].state();
                NBTTagCompound state = new NBTTagCompound();
                bath.save(state);
                state.setTag("Charges", new NBTTagCompound());
                bath.load(state);
                for (int index = 0; index < 700; index++) {
                    ItemStack item = GTOreDictUnificator.get(OrePrefixes.ingot, Materials.Iron, 1);
                    item.setStackDisplayName("Profile charge " + index);
                    check(bath.addMetal(IronProduct.IRON, 144, item), "Independent charge rejected");
                }
                check(bath.chargeGroupCount() == 700, "Stress charges merged despite different NBT");
                NBTTagCompound saved = new NBTTagCompound();
                fixture[0].machine.saveNBTData(saved);
                bath.save(saved.getCompoundTag("IronBath"));
                fixture[0].machine.loadNBTData(saved);
                check(
                    fixture[0].machine.onRightclick(fixture[0].controller, player()),
                    "Stress controller did not open");
            })
            .awaitScreen(GuiContainerWrapper.class)
            .awaitClient("the first distinct group page is synchronized", client -> {
                ItemStack item = ((ItemDisplayWidget) chargeWidget("bathChargeItem0")).getStackForRecipeViewer();
                check(
                    item != null && "Profile charge 0".equals(item.getDisplayName()),
                    "The new group panel has not synchronized");
            })
            .click(chargePageTarget("bathChargeNextPage"))
            .awaitClient("the second group page shows its real items", client -> {
                ItemStack item = ((ItemDisplayWidget) chargeWidget("bathChargeItem0")).getStackForRecipeViewer();
                check(
                    item != null && "Profile charge 24".equals(item.getDisplayName()),
                    "Group pagination did not synchronize its actual items: "
                        + (item == null ? "null" : item.getDisplayName()));
            })
            .capture("profile-distinct-700-page-2");
        profileController(scenario, profiles, "distinct-eternal-700-page-2");
        scenario.client("write raw separate CPU GPU and allocation samples", client -> writeRenderingProfiles(profiles))
            .succeed();
    }

    private static void closeRenderingContainer() {
        ModularNetwork.SERVER.closeAll(player());
        player().closeContainer();
    }

    private static void profileController(ClientScenario scenario, List<ControllerRenderProfile> profiles,
        String name) {
        ControllerRenderProfile[] current = new ControllerRenderProfile[1];
        scenario.client("start " + name + " profiling", client -> {
            current[0] = new ControllerRenderProfile(name);
            profiles.add(current[0]);
        });
        for (int warmup = 0; warmup < 8; warmup++)
            scenario.client("warm native renderer " + name + " " + warmup, client -> current[0].drawBatch(false));
        for (int batch = 0; batch < 6; batch++)
            scenario.client("measure native renderer " + name + " " + batch, client -> current[0].drawBatch(true));
        scenario.awaitClient("collect asynchronous GPU samples " + name, client -> current[0].collectGpu());
    }

    private static void writeRenderingProfiles(List<ControllerRenderProfile> profiles) {
        JsonObject report = new JsonObject();
        report.addProperty("scope", "current implementation, native Forge GUI Pre/default/Post rendering batches");
        report.addProperty(
            "comparison",
            "matched empty, 224-item and 700-item panels, plus 700 distinct groups on their second page");
        report.addProperty("historicalRegressionComparison", false);
        report.addProperty(
            "limits",
            "GUI submission only, excludes world rendering, frame pacing and swap. "
                + "At most 24 groups per page are synchronized. Cache thresholds are unknown, "
                + "so this is a capacity stress comparison and does not establish cache exhaustion");
        JsonArray cases = new JsonArray();
        boolean complete = true;
        for (ControllerRenderProfile profile : profiles) {
            cases.add(profile.result);
            for (String metric : new String[] { "cpuStatus", "gpuStatus", "allocationStatus" })
                complete &= "AVAILABLE".equals(
                    profile.result.get(metric)
                        .getAsString());
        }
        report.addProperty("status", complete ? "MEASURED" : "INCONCLUSIVE");
        report.add("cases", cases);
        String reportPath = System.getProperty("horizonqa.reportDir");
        check(reportPath != null, "The profile requires an explicit HQA report directory");
        File output = new File(reportPath, "artifacts/controller-rendering-profile.json");
        check(
            output.getParentFile()
                .isDirectory()
                || output.getParentFile()
                    .mkdirs(),
            "Could not create profile artifacts");
        try (FileWriter writer = new FileWriter(output)) {
            new GsonBuilder().setPrettyPrinting()
                .create()
                .toJson(report, writer);
        } catch (IOException failure) {
            throw new IllegalStateException("Could not retain the raw rendering profile", failure);
        }
    }

    private static final class ControllerRenderProfile {

        private static final int DRAWS = 128;
        private final JsonObject result = new JsonObject();
        private final JsonArray samples = new JsonArray();
        private final List<Integer> queries = new ArrayList<>();
        private final List<JsonObject> querySamples = new ArrayList<>();
        private final ThreadMXBean counters;
        private final java.lang.management.ThreadMXBean cpuCounters;
        private final boolean originalCpuEnabled;
        private final boolean originalAllocationEnabled;
        private final boolean cpu;
        private final boolean allocation;
        private final boolean gpu;
        private final String initialState;
        private long gpuWaitStarted;

        private ControllerRenderProfile(String name) {
            Minecraft mc = Minecraft.getMinecraft();
            check(Display.isCreated() && GLContext.getCapabilities() != null, "The profile needs a current GL context");
            check(
                mc.displayWidth == 1280 && mc.displayHeight == 720 && mc.gameSettings.guiScale == 2,
                "Rendering profile display conditions differ");
            java.lang.management.ThreadMXBean bean = ManagementFactory.getThreadMXBean();
            cpuCounters = bean;
            counters = bean instanceof ThreadMXBean ? (ThreadMXBean) bean : null;
            boolean cpuSupported = bean.isCurrentThreadCpuTimeSupported();
            boolean allocationSupported = counters != null && counters.isThreadAllocatedMemorySupported();
            originalCpuEnabled = cpuSupported && bean.isThreadCpuTimeEnabled();
            originalAllocationEnabled = allocationSupported && counters.isThreadAllocatedMemoryEnabled();
            try {
                if (cpuSupported && !bean.isThreadCpuTimeEnabled()) bean.setThreadCpuTimeEnabled(true);
                if (allocationSupported && !counters.isThreadAllocatedMemoryEnabled())
                    counters.setThreadAllocatedMemoryEnabled(true);
            } catch (SecurityException | UnsupportedOperationException unavailable) {
                result.addProperty("counterConfigurationIssue", unavailable.toString());
            }
            cpu = cpuSupported && bean.isThreadCpuTimeEnabled();
            allocation = allocationSupported && counters.isThreadAllocatedMemoryEnabled();
            gpu = GLContext.getCapabilities().OpenGL33 || GLContext.getCapabilities().GL_ARB_timer_query;
            initialState = screenState();
            result.addProperty("name", name);
            result.addProperty("width", mc.displayWidth);
            result.addProperty("height", mc.displayHeight);
            result.addProperty("guiScale", mc.gameSettings.guiScale);
            result.addProperty("screenWidth", mc.currentScreen.width);
            result.addProperty("screenHeight", mc.currentScreen.height);
            result.addProperty("warmupDraws", 8 * DRAWS);
            result.addProperty("drawsPerBatch", DRAWS);
            result.addProperty("cpuStatus", cpu ? "AVAILABLE" : "INCONCLUSIVE");
            result.addProperty("allocationStatus", allocation ? "AVAILABLE" : "INCONCLUSIVE");
            result.addProperty("gpuStatus", gpu ? "AVAILABLE" : "INCONCLUSIVE");
            result.addProperty("gpuRenderer", GL11.glGetString(GL11.GL_RENDERER));
            result.addProperty("initialState", initialState);
            result.add("samples", samples);
        }

        private void drawBatch(boolean measured) {
            Minecraft mc = Minecraft.getMinecraft();
            check(initialState.equals(screenState()), "The measured screen state changed");
            GuiContainerWrapper screen = (GuiContainerWrapper) mc.currentScreen;
            int matrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
            int framebuffer = framebuffer();
            IntBuffer viewport = BufferUtils.createIntBuffer(16);
            GL11.glGetInteger(GL11.GL_VIEWPORT, viewport);
            check(GL11.glGetError() == GL11.GL_NO_ERROR, "GL has an error before the rendering profile batch");
            boolean queryFree = !gpu || GL15.glGetQueryi(ARBTimerQuery.GL_TIME_ELAPSED, GL15.GL_CURRENT_QUERY) == 0;
            if (!queryFree) {
                result.addProperty("gpuStatus", "INCONCLUSIVE");
                result.addProperty("gpuIssue", "Another GPU elapsed-time query is active");
            }
            int query = measured && gpu && queryFree ? GL15.glGenQueries() : 0;
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPushMatrix();
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPushMatrix();
            JsonObject sample = new JsonObject();
            if (query != 0) {
                queries.add(query);
                querySamples.add(sample);
            }
            try {
                // GUI renderers restore Minecraft's main target, which can differ from a tick callback's target.
                mc.getFramebuffer()
                    .bindFramebuffer(true);
                int guiFramebuffer = framebuffer();
                check(
                    !OpenGlHelper.isFramebufferEnabled() || guiFramebuffer == mc.getFramebuffer().framebufferObject,
                    "Rendering profile did not bind Minecraft's native GUI framebuffer");
                IntBuffer guiViewport = BufferUtils.createIntBuffer(16);
                GL11.glGetInteger(GL11.GL_VIEWPORT, guiViewport);
                check(
                    guiViewport.get(0) == 0 && guiViewport.get(1) == 0
                        && guiViewport.get(2) == mc.displayWidth
                        && guiViewport.get(3) == mc.displayHeight,
                    "Rendering profile did not establish the native GUI viewport");
                mc.entityRenderer.setupOverlayRendering();
                long thread = Thread.currentThread()
                    .getId();
                long allocatedBefore = allocation ? counters.getThreadAllocatedBytes(thread) : -1;
                long cpuBefore = cpu ? cpuCounters.getCurrentThreadCpuTime() : -1;
                if (query != 0) GL15.glBeginQuery(ARBTimerQuery.GL_TIME_ELAPSED, query);
                try {
                    for (int draw = 0; draw < DRAWS; draw++) {
                        GuiScreenEvent.DrawScreenEvent.Pre event = new GuiScreenEvent.DrawScreenEvent.Pre(
                            screen,
                            2,
                            2,
                            0);
                        if (!MinecraftForge.EVENT_BUS.post(event)) screen.drawScreen(2, 2, 0);
                        MinecraftForge.EVENT_BUS.post(new GuiScreenEvent.DrawScreenEvent.Post(screen, 2, 2, 0));
                    }
                } finally {
                    if (query != 0) GL15.glEndQuery(ARBTimerQuery.GL_TIME_ELAPSED);
                }
                long cpuAfter = cpu ? cpuCounters.getCurrentThreadCpuTime() : -1;
                long allocatedAfter = allocation ? counters.getThreadAllocatedBytes(thread) : -1;
                if (cpuBefore >= 0 && cpuAfter >= cpuBefore) sample.addProperty("cpuNs", cpuAfter - cpuBefore);
                else result.addProperty("cpuStatus", "INCONCLUSIVE");
                if (allocatedBefore >= 0 && allocatedAfter >= allocatedBefore)
                    sample.addProperty("allocatedBytes", allocatedAfter - allocatedBefore);
                else result.addProperty("allocationStatus", "INCONCLUSIVE");
                sample.addProperty("callbackFramebuffer", framebuffer);
                sample.addProperty("guiFramebuffer", guiFramebuffer);
                sample.addProperty("viewportWidthBefore", viewport.get(2));
                sample.addProperty("viewportHeightBefore", viewport.get(3));
                check(framebuffer() == guiFramebuffer, "Native screen rendering changed the GUI framebuffer");
                check(GL11.glGetError() == GL11.GL_NO_ERROR, "Native screen rendering produced a GL error");
            } finally {
                GL11.glMatrixMode(GL11.GL_MODELVIEW);
                GL11.glPopMatrix();
                GL11.glMatrixMode(GL11.GL_PROJECTION);
                GL11.glPopMatrix();
                GL11.glMatrixMode(matrixMode);
                GL11.glPopAttrib();
                if (GLContext.getCapabilities().OpenGL30) GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
                else if (GLContext.getCapabilities().GL_EXT_framebuffer_object)
                    EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, framebuffer);
                GL11.glViewport(viewport.get(0), viewport.get(1), viewport.get(2), viewport.get(3));
            }
            check(
                framebuffer() == framebuffer && GL11.glGetInteger(GL11.GL_MATRIX_MODE) == matrixMode,
                "Rendering profile did not restore framebuffer and matrix mode");
            IntBuffer restoredViewport = BufferUtils.createIntBuffer(16);
            GL11.glGetInteger(GL11.GL_VIEWPORT, restoredViewport);
            for (int component = 0; component < 4; component++) check(
                viewport.get(component) == restoredViewport.get(component),
                "Rendering profile did not restore viewport");
            check(initialState.equals(screenState()), "Native redraw changed the profile's charge or scroll state");
            result.addProperty("finalState", screenState());
            if (measured) {
                samples.add(sample);
                if (query != 0) {
                    GL11.glFlush();
                }
            }
        }

        private void collectGpu() {
            if (gpuWaitStarted == 0) gpuWaitStarted = System.nanoTime();
            for (int index = 0; index < queries.size(); index++) {
                int query = queries.get(index);
                if (querySamples.get(index)
                    .has("gpuNs")) continue;
                if (GL15.glGetQueryObjecti(query, GL15.GL_QUERY_RESULT_AVAILABLE) == 0) {
                    if (System.nanoTime() - gpuWaitStarted > 2_000_000_000L) {
                        result.addProperty("gpuStatus", "INCONCLUSIVE");
                        result.addProperty("gpuIssue", "Elapsed-time query unavailable after two seconds");
                        return;
                    }
                    throw new AssertionError("Waiting for the GPU elapsed-time result");
                }
                querySamples.get(index)
                    .addProperty("gpuNs", ARBTimerQuery.glGetQueryObjectui64(query, GL15.GL_QUERY_RESULT));
            }
        }

        private static int framebuffer() {
            return GLContext.getCapabilities().OpenGL30 || GLContext.getCapabilities().GL_EXT_framebuffer_object
                ? GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING)
                : 0;
        }

        private static String screenState() {
            Minecraft mc = Minecraft.getMinecraft();
            check(
                mc.currentScreen instanceof GuiContainerWrapper,
                "Rendering profile lost its native controller screen: screen="
                    + (mc.currentScreen == null ? "null"
                        : mc.currentScreen.getClass()
                            .getName())
                    + ", clientPosition="
                    + mc.thePlayer.posX
                    + ","
                    + mc.thePlayer.posY
                    + ","
                    + mc.thePlayer.posZ
                    + ", window="
                    + mc.thePlayer.openContainer.windowId);
            GuiContainerWrapper screen = (GuiContainerWrapper) mc.currentScreen;
            int occupied = 0;
            for (int slot = 0; slot < 24; slot++)
                if (((ItemDisplayWidget) chargeWidget("bathChargeItem" + slot)).getStackForRecipeViewer() != null)
                    occupied++;
            return screen.width + "x" + screen.height + ":groups=" + occupied;
        }

        private void close() {
            for (int query : queries) GL15.glDeleteQueries(query);
            queries.clear();
            if (cpu) cpuCounters.setThreadCpuTimeEnabled(originalCpuEnabled);
            if (allocation) counters.setThreadAllocatedMemoryEnabled(originalAllocationEnabled);
        }
    }

    private static ClientTarget chargePageTarget(String name) {
        return ClientTarget.of(name, window -> {
            window.screen(GuiContainerWrapper.class);
            IWidget widget = chargeWidget(name);
            return new ClickTarget(widget, new java.awt.Rectangle(widget.getArea()), widget::isEnabled);
        });
    }

    private static IWidget chargeWidget(String name) {
        return WidgetTree.findFirstWithName(
            ((GuiContainerWrapper) Minecraft.getMinecraft().currentScreen).getScreen()
                .getMainPanel(),
            name);
    }

    @GameTest(template = "gtnhextranerftests:native_fixture", timeoutTicks = 600000)
    public static void persistentLaboratory(GameTestHelper helper) {
        laboratory(helper);
    }

    @GameTest(template = "gtnhextranerftests:native_fixture", timeoutTicks = 510000)
    public static void emptyStoresProduceEveryMetalStack(GameTestHelper helper) {
        Station[] stations = new Station[2];
        IronBathRedstoneLab.Lab[] circuit = new IronBathRedstoneLab.Lab[1];
        long[] started = new long[1];
        int[] reported = { -1, -1, -1, -1, -1 };
        String[] names = { "Iron", "Wrought Iron", "Steel", "Cast Iron", "Pig Iron" };
        ClientScenario scenario = ClientTest.scenario(helper)
            .defaultTimeoutTicks(600)
            .server("build five empty output stores with simultaneous 64-ingot requests", () -> {
                Config.setTelemetryEnabled(false);
                ConsentPrompt.stop();
                player().closeScreen();
                buildLaboratory(helper.getWorld(), player(), stations, circuit, 64, false);
                for (int i = 0; i < names.length; i++)
                    check(circuit[0].stockCount(i) == 0, names[i] + " output was prefilled");
                started[0] = helper.getWorld()
                    .getTotalWorldTime();
                IronBathRedstoneLab.Position stop = circuit[0].stopLever();
                setLever(helper.getWorld(), stop.x(), stop.y(), stop.z(), false);
            });
        view(scenario, "empty-stores-start", 1060, 121, 151, 180, 45);
        scenario.withinTicks(500000)
            .awaitServerAccelerated("native redstone delivers at least 64 of all five metals", 100, () -> {
                verifySampleAndReserve(stations[1], circuit[0]);
                boolean complete = true;
                for (int i = 0; i < names.length; i++) {
                    int count = circuit[0].stockCount(i);
                    if (count != reported[i]) {
                        System.out.println(
                            "IRON_BATH_STACK_ORDER " + names[i]
                                + "="
                                + count
                                + " elapsedTicks="
                                + (helper.getWorld()
                                    .getTotalWorldTime() - started[0]));
                        reported[i] = count;
                    }
                    complete &= count >= 64;
                }
                if (helper.getWorld()
                    .getTotalWorldTime() - started[0] >= 499800) circuit[0].saveFailureSnapshot(helper.getWorld());
                check(complete, "Unfilled stack requests: " + circuit[0].diagnostics(helper.getWorld()));
            });
        view(scenario, "all-five-stacks-delivered", 1060, 121, 151, 180, 45);
        scenario.succeed();
    }

    @GameTest(template = "gtnhextranerftests:native_fixture", timeoutTicks = 2400)
    public static void sampleDrainsWhileControllerIsStopped(GameTestHelper helper) {
        Station[] stations = new Station[2];
        IronBathRedstoneLab.Lab[] circuit = new IronBathRedstoneLab.Lab[1];
        ClientScenario scenario = ClientTest.scenario(helper)
            .server(
                "build the stopped sampling station",
                () -> buildLaboratory(helper.getWorld(), player(), stations, circuit));
        verifyPassiveSample(scenario, helper.getWorld(), stations, circuit);
        scenario.succeed();
    }

    private static void verifyPassiveSample(ClientScenario scenario, WorldServer world, Station[] stations,
        IronBathRedstoneLab.Lab[] circuit) {
        int[] metalBefore = new int[1];
        long[] started = new long[1];
        scenario.server("seed the previous 143 mB of recycled Steel", () -> {
            seedRecyclingSample(stations[1], circuit[0]);
            IronBath bath = stations[1].state();
            metalBefore[0] = bath.metalMb() + bath.pendingMetalMb();
            started[0] = world.getTotalWorldTime();
        });
        scenario.serverSequence()
            .thenIdle(20);
        scenario.server(
            "deliver one Steel sample to its filtered chamber",
            () -> check(
                circuit[0].sampleTanks()
                    .get(2)
                    .getMetaTileEntity()
                    .fill(ForgeDirection.NORTH, Materials.Steel.getMolten(1), true) == 1,
                "The sample chamber rejected one millibucket of Steel"))
            .withinTicks(20)
            .awaitServer("the sample locally enables its pump", () -> {
                for (FluidTankInfo tank : circuit[0].sampleTanks()
                    .get(2)
                    .getTankInfo(ForgeDirection.UNKNOWN))
                    check(tank.fluid == null || tank.fluid.amount == 0, "The sample still awaits central release");
                check(circuit[0].sampledRank() == 3, "The emptied sample chamber did not retain Steel in RS memory");
            })
            .withinTicks(1200)
            .awaitServerAccelerated("the sample ingot returns while the controller stays stopped", 100, () -> {
                IronBath bath = stations[1].state();
                if (world.getTotalWorldTime() - started[0] >= 1180) circuit[0].saveFailureSnapshot(world);
                check(
                    bath.metalMb() + bath.pendingMetalMb() >= metalBefore[0] + 144,
                    "The completed sample ingot has not returned to the bath");
            });
    }

    /** Builds the native laboratory for a manual session. */
    public static void prepareManualLaboratory(WorldServer world, EntityPlayerMP viewer) {
        Config.setTelemetryEnabled(false);
        viewer.closeScreen();
        buildLaboratory(world, viewer, new Station[2], new IronBathRedstoneLab.Lab[1]);
    }

    private static void buildLaboratory(WorldServer world, EntityPlayerMP viewer, Station[] stations,
        IronBathRedstoneLab.Lab[] circuit) {
        buildLaboratory(world, viewer, stations, circuit, 32, true);
    }

    private static void buildLaboratory(WorldServer world, EntityPlayerMP viewer, Station[] stations,
        IronBathRedstoneLab.Lab[] circuit, int stockTarget, boolean prefillStores) {
        prepareWorld(world);
        buildFloor(world);
        FakePlayer placer = FakePlayerFactory.get(world, new GameProfile(UUID.randomUUID(), "IronBathLaboratory"));
        stations[0] = new Station(world, placer, MANUAL_X, ItemList.Hatch_Input_Bus_LV, 1, 1);
        stations[1] = new Station(world, placer, AUTOMATIC_X, ItemList.Hatch_Input_Bus_HV, 3, 2);
        buildServiceAccess(world);
        manualSupplies(world, placer, stations[0]);
        Station automatic = stations[1];
        BaseMetaTileEntity production = Station.port(
            world,
            placer,
            IronBathFurnace.tap(3),
            AUTOMATIC_X,
            new IronBathStructure.Offset(7, 5, -1),
            ForgeDirection.NORTH);
        automatic.slag.setFrontFacing(ForgeDirection.WEST);
        outletTank(world, placer, automatic.slag);
        MTEIronBathTap slagOutlet = (MTEIronBathTap) automatic.slag.getMetaTileEntity();
        slagOutlet.setAmountPerPulse(16 * 144);
        slagOutlet.setRedstoneMode(2);
        circuit[0] = IronBathRedstoneLab.build(
            world,
            AUTOMATIC_X,
            GROUND_Y,
            STATION_Z,
            automatic.metal,
            production,
            automatic.input,
            automatic.oxygen,
            stockTarget,
            prefillStores);
        check(automatic.machine.checkStructure(true, automatic.controller), "The automatic outlet layout did not form");
        sign(world, 1043, 100, 106, "PASSIVE SAMPLES", "1 mB -> RS", "Pump -> caster", "Ingot -> furnace");
        sign(world, 1078, 100, 133, "STOCK REQUESTS", "Missing stock", "Keep " + stockTarget + " ingots", "");
        sign(world, 1046, 100, 126, "RECYCLE SAMPLES", "144 mB / ingot", "Back to input", "");
        sign(world, 1042, 100, 128, "FRESH IRON", "16-ingot pulse", "While producing", "");
        sign(world, 1073, 100, 127, "CARBON CONTROL", "Coke / Oxygen", "Sample again", "");
        sign(world, 1082, 100, 128, "PRODUCT OUTPUT", "Middle tap", "Metal + Slag", "");
        IronBathRedstoneLab.Position stop = circuit[0].stopLever();
        sign(world, stop.x() + 1, stop.y(), stop.z() - 1, "STOP", "Lever ON: stop", "Lever OFF: run", "Native circuit");
        stations[0].seedHotSteel(384, 320);
        stations[1].seedHotSteel(400, 0);
        sign(world, MANUAL_X, GROUND_Y, 94, "01 MANUAL BATH", "384 Steel ready", "Coke + Oxygen", "control the alloy");
        sign(
            world,
            AUTOMATIC_X,
            GROUND_Y,
            94,
            "02 AUTOMATION",
            "Current alloy first",
            "Passive samples",
            "STOP on panel");
        sign(world, SPAWN_X, GROUND_Y, 90, "IRON BATH LAB", "Left: manual", "Right: automatic", "Creative mode");
        sign(world, SPAWN_X, GROUND_Y, 109, "ENERGY ACCESS", "Walk below shaft", "Native EU supply", "World is saved");
        for (Station station : stations) {
            check(
                station.machine.checkStructure(true, station.controller),
                "Laboratory hardware obstructed the production furnace structure");
            for (BaseMetaTileEntity hatch : station.energy) {
                for (int y = GROUND_Y; y < hatch.getYCoord(); y++) check(
                    world.isAirBlock(hatch.getXCoord(), y, hatch.getZCoord()),
                    "The furnace needs clear service space below its energy hatches");
            }
        }
        placePlayerAtEntrance(viewer);
        verifyServiceAccess(world, automatic);
        verifyManualTapAccess(world);
    }

    private static void seedRecyclingSample(Station automatic, IronBathRedstoneLab.Lab circuit) {
        IronBath history = automatic.state();
        IronBath.Tap previousSamples = history.tap(0, 143);
        check(
            previousSamples.product() == IronProduct.STEEL && previousSamples.amountMb() == 143,
            "Could not commission the previous Steel samples");
        NBTTagCompound commissioned = new NBTTagCompound();
        automatic.machine.saveNBTData(commissioned);
        history.save(commissioned.getCompoundTag("IronBath"));
        automatic.machine.loadNBTData(commissioned);
        automatic.controller.markDirty();
        check(
            circuit.recyclingAccumulators()
                .get(2)
                .getMetaTileEntity()
                .fill(ForgeDirection.UNKNOWN, Materials.Steel.getMolten(143), true) == 143,
            "The native recycling caster did not accept the previous samples");
    }

    private static void laboratory(GameTestHelper helper) {
        Station[] stations = new Station[2];
        IronBathRedstoneLab.Lab[] circuit = new IronBathRedstoneLab.Lab[1];
        long[] started = new long[1];
        ClientScenario scenario = ClientTest.scenario(helper)
            .defaultTimeoutTicks(600)
            .server("prepare the isolated laboratory session", () -> {
                Config.setTelemetryEnabled(false);
                ConsentPrompt.stop();
                player().closeScreen();
            })
            .client("prepare the laboratory view", client -> {
                Minecraft mc = Minecraft.getMinecraft();
                mc.displayGuiScreen(null);
                mc.gameSettings.guiScale = 2;
                mc.gameSettings.thirdPersonView = 0;
                mc.gameSettings.hideGUI = true;
            })
            .server("build the persistent walkable iron bath laboratory", () -> {
                WorldServer world = helper.getWorld();
                buildLaboratory(world, player(), stations, circuit);
                stations[1].seedHotSteel(496, 320);
                circuit[0].productStores()
                    .get(4)
                    .setInventorySlotContents(0, null);
                circuit[0].productStores()
                    .get(4)
                    .markDirty();
            })
            .awaitServerAccelerated("native debug hatches maintain both seeded hot furnaces", 20, () -> {
                for (Station station : stations) {
                    check(
                        station.state()
                            .temperature() >= IronBath.MELTING_TEMPERATURE,
                        "A laboratory furnace has not maintained its hot initial state");
                    check(station.machine.lEUt < 0, "A laboratory furnace has not drawn native EU");
                    for (BaseMetaTileEntity energy : station.energy) {
                        check(energy.getStoredEU() > 0, "A debug hatch did not refill itself");
                        check(
                            energy.getMetaTileEntity() instanceof MTEHatchEnergyDebug,
                            "Laboratory power does not use native debug hatches");
                    }
                }
                check(
                    circuit[0].sampleTanks()
                        .size() == 7,
                    "The output classification rack is incomplete");
                check(
                    circuit[0].productStores()
                        .size() == 5,
                    "The five native product stores are missing");
            })
            .withinTicks(400)
            .awaitServerAccelerated("both manual tap pipes carry their construction samples into the tanks", 20, () -> {
                verifyManualTransfer(helper.getWorld(), -1, Materials.Steel.getMolten(144));
                verifyManualTransfer(
                    helper.getWorld(),
                    1,
                    IronBathFurnace.material(IronProduct.SLAG)
                        .getMolten(144));
            })
            .withinTicks(30000)
            .awaitServerAccelerated("the populated manual station remains ready for player experiments", 1, () -> {
                IronBath bath = stations[0].state();
                check(
                    bath.pendingMetalMb() == 0 && bath.liquidMetalMb() == 384 * 144
                        && bath.currentProduct() == IronProduct.STEEL
                        && bath.meltingItems() == 0,
                    "The manual furnace did not retain the ready Steel bath and an empty heating queue");
                check(
                    stations[0].machine.checkStructure(true, stations[0].controller),
                    "The manual furnace lost its structure");
                check(
                    stations[1].machine.checkStructure(true, stations[1].controller),
                    "The automated furnace lost its structure");
            });
        scenario.withinTicks(400)
            .awaitServerAccelerated("the reserved slag layer drains through its dedicated outlet", 20, () -> {
                IronBathStructure.Offset tap = IronBathStructure.SLAG_TAP;
                IFluidHandler tank = (IFluidHandler) helper.getWorld()
                    .getTileEntity(AUTOMATIC_X + tap.x() - 3, STATION_Y + tap.y(), STATION_Z + tap.z());
                int received = 0;
                for (FluidTankInfo info : tank.getTankInfo(ForgeDirection.UNKNOWN)) {
                    if (info.fluid != null && info.fluid.isFluidEqual(
                        IronBathFurnace.material(IronProduct.SLAG)
                            .getMolten(1)))
                        received += info.fluid.amount;
                }
                check(received == 320, "The dedicated laboratory Slag tank did not receive the reserved layer");
                check(
                    stations[1].state()
                        .slagMb() == 0,
                    "The reserved Slag layer was not drained");
            })
            .server("prepare the automatic material-control bath", () -> stations[1].seedHotSteel(400, 0));
        verifyPassiveSample(scenario, helper.getWorld(), stations, circuit);
        scenario.server("release the stock controller STOP switch", () -> {
            started[0] = helper.getWorld()
                .getTotalWorldTime();
            IronBathRedstoneLab.Position stop = circuit[0].stopLever();
            WorldServer world = helper.getWorld();
            check(
                world.getBlock(stop.x(), stop.y(), stop.z()) == Blocks.lever,
                "The physical probe STOP lever is missing");
            if ((world.getBlockMetadata(stop.x(), stop.y(), stop.z()) & 8) != 0)
                Blocks.lever.onBlockActivated(world, stop.x(), stop.y(), stop.z(), player(), 1, 0.5f, 0.5f, 0.5f);
        });
        scenario.withinTicks(8000)
            .awaitServerAccelerated("the current needed alloy is produced before another shortage", 100, () -> {
                verifySampleAndReserve(stations[1], circuit[0]);
                if (helper.getWorld()
                    .getTotalWorldTime() - started[0] >= 7800) circuit[0].saveFailureSnapshot(helper.getWorld());
                check(
                    circuit[0].stockCount(4) == 0,
                    "Pig Iron was produced before the already available Steel was used");
                check(circuit[0].stockCount(2) >= 16, "The controller has not produced the currently needed Steel");
            });
        scenario.withinTicks(1200)
            .awaitServerAccelerated(
                "the timer resets material memory before a fresh probe",
                100,
                () -> check(circuit[0].sampledRank() == 0, "The sampler has not reset its previous material"))
            .withinTicks(400)
            .awaitServerAccelerated(
                "an automatic furnace probe refreshes material memory",
                100,
                () -> check(circuit[0].sampledRank() > 0, "No automatic sample reached the classifier"));
        view(scenario, "automation-layout", 1060, 121, 151, 180, 45);
        guiViews(scenario, stations);
        exerciseStockRequests(scenario, helper.getWorld(), stations, circuit);
        scenario.server(
            "prepare a fresh manual practice charge after circuit verification",
            () -> stations[0].seedHotSteel(384, 320));
        view(scenario, "lab-overview", 1050, 140, 70, 0, 43);
        view(scenario, "manual-station", MANUAL_X + 0.5, 103, 92, 0, 12);
        view(scenario, "automated-station", AUTOMATIC_X + 0.5, 104, 91, 0, 16);
        view(scenario, "automation-board", 1060, 121, 151, 180, 45);
        view(scenario, "service-pit", 1047, 100, 119, 180, 24);
        scenario.server("leave the player at the laboratory entrance and save all persistent state", () -> {
            WorldServer world = helper.getWorld();
            placePlayerAtEntrance();
            player().inventory.setInventorySlotContents(0, new ItemStack(Items.redstone, 64));
            player().inventory.setInventorySlotContents(1, new ItemStack(Blocks.lever, 16));
            player().inventory.setInventorySlotContents(2, ItemList.Cell_Empty.get(16));
            player().inventory.markDirty();
            MinecraftServer.getServer()
                .getConfigurationManager()
                .saveAllPlayerData();
            try {
                world.saveAllChunks(true, null);
            } catch (MinecraftException failure) {
                throw new IllegalStateException("Could not save the persistent laboratory", failure);
            }
        })
            .client("leave a normal playable HUD", client -> Minecraft.getMinecraft().gameSettings.hideGUI = false)
            .succeed();
    }

    private static void exerciseStockRequests(ClientScenario scenario, WorldServer world, Station[] stations,
        IronBathRedstoneLab.Lab[] circuit) {
        int[] targets = { 2, 4, 1, 3, 0 };
        Materials[] products = { Materials.Iron, IronBathFurnace.material(IronProduct.WROUGHT_IRON), Materials.Steel,
            Materials.CastIron, Materials.PigIron };
        for (int target : targets) {
            int[] baseline = new int[1];
            long[] started = new long[1];
            scenario.server("consume the stored " + products[target].mName + " to request native replenishment", () -> {
                started[0] = world.getTotalWorldTime();
                IInventory store = circuit[0].productStores()
                    .get(target);
                for (int slot = 0; slot < store.getSizeInventory(); slot++) store.setInventorySlotContents(slot, null);
                store.markDirty();
                baseline[0] = ironSupply(world, circuit[0]);
                check(baseline[0] > 0, "The physical Iron feeder has run out of stock");
            });
            scenario.withinTicks(100000)
                .awaitServerAccelerated("physical stock request replenishes " + products[target].mName, 100, () -> {
                    verifySampleAndReserve(stations[1], circuit[0]);
                    if (world.getTotalWorldTime() - started[0] >= 99800) circuit[0].saveFailureSnapshot(world);
                    check(
                        ironSupply(world, circuit[0]) < baseline[0],
                        "The native circuit has not fed replacement Iron");
                    check(
                        circuit[0].stockCount(target) >= 32,
                        "The native circuit has not replenished " + products[target].mName
                            + "\n"
                            + circuit[0].diagnostics(world));
                });
        }
    }

    private static void verifySampleAndReserve(Station station, IronBathRedstoneLab.Lab circuit) {
        int samples = 0;
        for (BaseMetaTileEntity tile : circuit.sampleTanks()) {
            for (FluidTankInfo tank : tile.getTankInfo(ForgeDirection.UNKNOWN)) {
                if (tank.fluid != null) samples += tank.fluid.amount;
            }
        }
        check(samples <= 1, "More than one millibucket is waiting for a sample decision");
        IronBath bath = station.state();
        check(
            bath.metalMb() + bath.pendingMetalMb() >= 128 * 144,
            "Production or unrecycled samples consumed the startup metal reserve");
    }

    private static int ironSupply(WorldServer world, IronBathRedstoneLab.Lab circuit) {
        IronBathRedstoneLab.Position supply = circuit.ironSupply();
        IInventory source = (IInventory) world.getTileEntity(supply.x(), supply.y(), supply.z());
        int items = 0;
        for (int slot = 0; slot < source.getSizeInventory(); slot++) {
            ItemStack stack = source.getStackInSlot(slot);
            if (stack != null) items += stack.stackSize;
        }
        return items;
    }

    private static void setLever(WorldServer world, int x, int y, int z, boolean powered) {
        check(world.getBlock(x, y, z) == Blocks.lever, "A physical automation control lever is missing");
        if (((world.getBlockMetadata(x, y, z) & 8) != 0) != powered)
            Blocks.lever.onBlockActivated(world, x, y, z, player(), 1, 0.5f, 0.5f, 0.5f);
    }

    private static void prepareWorld(WorldServer world) {
        ConsentPrompt.stop();
        world.getWorldInfo()
            .setWorldName("Iron Bath Laboratory - Stock Control");
        WorldServer overworld = DimensionManager.getWorld(0);
        check(overworld != null, "The laboratory needs an overworld");
        if (overworld.mapStorage.loadData(ProgressionData.class, ProgressionData.NAME) == null)
            overworld.mapStorage.setData(ProgressionData.NAME, new ProgressionData());
        for (ProgressionTier tier : ProgressionTier.values()) MachineProgression.certify(world, tier);
        world.setWorldTime(6000);
        world.getGameRules()
            .setOrCreateGameRule("doDaylightCycle", "false");
        world.getGameRules()
            .setOrCreateGameRule("doMobSpawning", "false");
        world.getGameRules()
            .setOrCreateGameRule("keepInventory", "true");
        world.getWorldInfo()
            .setRaining(false);
        world.getWorldInfo()
            .setThundering(false);
        world.getWorldInfo()
            .setRainTime(1000000);
        world.setSpawnLocation(SPAWN_X, GROUND_Y, SPAWN_Z);
        for (int x = 1014 >> 4; x <= 1086 >> 4; x++) for (int z = 84 >> 4; z <= 146 >> 4; z++) world.getChunkProvider()
            .loadChunk(x, z);
        for (int x = 1014; x <= 1086; x++)
            for (int z = 84; z <= 146; z++) for (int y = FLOOR - 4; y <= STATION_Y + 14; y++) {
                if (world.isAirBlock(x, y, z)) continue;
                world.removeTileEntity(x, y, z);
                world.setBlockToAir(x, y, z);
            }
    }

    private static void buildFloor(WorldServer world) {
        for (int x = 1014; x <= 1086; x++) for (int z = 84; z <= 146; z++) {
            boolean walk = z >= 92 && z <= 94 || z >= 107 && z <= 109 || x >= 1036 && x <= 1039 || z == 118;
            Block floor = walk ? Blocks.quartz_block : Blocks.stained_hardened_clay;
            int color = walk ? 0 : 7;
            if (x % 8 == 0 && z % 8 == 0) floor = Blocks.glowstone;
            world.setBlock(x, FLOOR, z, floor, color, 3);
            if (x == 1014 || x == 1086 || z == 84 || z == 146) world.setBlock(x, GROUND_Y, z, Blocks.iron_bars, 0, 3);
        }
        for (int station : new int[] { MANUAL_X, AUTOMATIC_X }) {
            int color = station == MANUAL_X ? 11 : 4;
            for (int x = station - 5; x <= station + 5; x++) for (int z = 98; z <= 106; z++) {
                if (x == station - 5 || x == station + 5 || z == 98 || z == 106)
                    world.setBlock(x, FLOOR, z, Blocks.stained_hardened_clay, color, 3);
            }
        }
    }

    private static void buildServiceAccess(WorldServer world) {
        for (int station : new int[] { MANUAL_X, AUTOMATIC_X }) {
            for (int x = station - 2; x <= station + 2; x++) for (int z = STATION_Z - 2; z <= STATION_Z + 3; z++) {
                if (world.isAirBlock(x, STATION_Y - 2, z)) world.setBlock(x, STATION_Y - 2, z, Blocks.stonebrick, 0, 3);
            }
            for (int step = 0; step < STATION_Y - GROUND_Y - 1; step++)
                world.setBlock(station - 2, GROUND_Y + step, STATION_Z - 5 + step, Blocks.stone_stairs, 2, 3);
        }
    }

    private static void verifyServiceAccess(WorldServer world, Station station) {
        StringBuilder obstructions = new StringBuilder();
        int controllerX = station.controller.getXCoord();
        int serviceY = STATION_Y - 1;
        check(
            world.getBlock(controllerX, serviceY - 1, STATION_Z - 2) == Blocks.stonebrick,
            "Controller service platform is not one block below the structure base");
        clearStandingCell(world, controllerX, serviceY, STATION_Z - 2);
        reachable(
            world,
            obstructions,
            controllerX + 0.5,
            serviceY + 1.62,
            STATION_Z - 1.5,
            controllerX,
            STATION_Y,
            STATION_Z,
            controllerX + 0.5,
            STATION_Y + 0.5,
            STATION_Z + 0.01);
        for (BaseMetaTileEntity tile : new BaseMetaTileEntity[] { station.input, station.oxygen }) {
            int outward = tile == station.input ? 1 : -1;
            int x = tile.getXCoord(), y = tile.getYCoord(), z = tile.getZCoord();
            clearStandingCell(world, x + outward, serviceY, z);
            reachable(
                world,
                obstructions,
                x + 0.5 + outward,
                serviceY + 1.62,
                z + 0.5,
                x,
                y,
                z,
                x + (outward > 0 ? 0.99 : 0.01),
                y + 0.5,
                z + 0.5);
        }
        IronBathStructure.Offset maintenance = IronBathStructure.MAINTENANCE;
        int maintenanceX = controllerX + maintenance.x(), maintenanceY = STATION_Y + maintenance.y(),
            maintenanceZ = STATION_Z + maintenance.z();
        clearStandingCell(world, maintenanceX, serviceY, maintenanceZ + 1);
        reachable(
            world,
            obstructions,
            maintenanceX + 0.5,
            serviceY + 1.62,
            maintenanceZ + 1.5,
            maintenanceX,
            maintenanceY,
            maintenanceZ,
            maintenanceX + 0.5,
            maintenanceY + 0.5,
            maintenanceZ + 0.99);
        for (BaseMetaTileEntity tile : station.energy) {
            ForgeDirection front = tile.getFrontFacing();
            int x = tile.getXCoord(), y = tile.getYCoord(), z = tile.getZCoord();
            int standingZ = z + front.offsetZ;
            clearStandingCell(world, x, GROUND_Y, standingZ);
            reachable(
                world,
                obstructions,
                x + 0.5,
                GROUND_Y + 1.62,
                standingZ + 0.5,
                x,
                y,
                z,
                x + 0.5,
                y + 0.5,
                z + (front == ForgeDirection.NORTH ? 0.01 : 0.99));
        }
        check(obstructions.length() == 0, "Laboratory control access failed:\n" + obstructions);
    }

    private static void clearStandingCell(WorldServer world, int x, int y, int z) {
        check(
            !world.isAirBlock(x, y - 1, z) && world.isAirBlock(x, y, z) && world.isAirBlock(x, y + 1, z),
            "Blocked laboratory access at " + x + "," + y + "," + z);
    }

    private static void reachable(WorldServer world, StringBuilder obstructions, double ex, double ey, double ez, int x,
        int y, int z, double tx, double ty, double tz) {
        Vec3 eye = Vec3.createVectorHelper(ex, ey, ez), target = Vec3.createVectorHelper(tx, ty, tz);
        if (eye.distanceTo(target) > 4.5) {
            obstructions.append("Control at ")
                .append(x)
                .append(',')
                .append(y)
                .append(',')
                .append(z)
                .append(" exceeds normal interaction reach\n");
        }
        MovingObjectPosition hit = world.rayTraceBlocks(eye, target);
        if (hit == null || hit.blockX != x || hit.blockY != y || hit.blockZ != z) {
            obstructions
                .append(
                    "Control is obstructed at " + x
                        + ","
                        + y
                        + ","
                        + z
                        + ": ray hit "
                        + (hit == null ? "nothing"
                            : hit.blockX + ","
                                + hit.blockY
                                + ","
                                + hit.blockZ
                                + " ("
                                + world.getBlock(hit.blockX, hit.blockY, hit.blockZ)
                                    .getUnlocalizedName()
                                + ")"))
                .append('\n');
        }
    }

    private static void verifyManualTapAccess(WorldServer world) {
        StringBuilder obstructions = new StringBuilder();
        for (IronBathStructure.Offset tap : new IronBathStructure.Offset[] { IronBathStructure.METAL_TAP,
            IronBathStructure.SLAG_TAP }) {
            boolean lower = tap == IronBathStructure.METAL_TAP;
            int x = MANUAL_X + tap.x(), y = STATION_Y + tap.y(), z = STATION_Z + tap.z();
            clearStandingCell(world, x + (lower ? 1 : -1), y, z - (lower ? 2 : 0));
            reachable(
                world,
                obstructions,
                x + (lower ? 1.5 : -0.5),
                y + 1.62,
                z + (lower ? -1.5 : 0.5),
                x,
                y,
                z,
                x + (lower ? 0.90 : 0.01),
                y + (lower ? 0.50 : 0.85),
                z + (lower ? 0.01 : 0.3));
        }
        check(obstructions.length() == 0, "Manual tap access failed:\n" + obstructions);
    }

    private static void verifyManualTransfer(WorldServer world, int side, FluidStack expected) {
        IronBathStructure.Offset tap = side < 0 ? IronBathStructure.METAL_TAP : IronBathStructure.SLAG_TAP;
        IFluidHandler tank = (IFluidHandler) world
            .getTileEntity(MANUAL_X + tap.x(), STATION_Y + tap.y(), STATION_Z + tap.z() - 3);
        int received = 0;
        for (FluidTankInfo info : tank.getTankInfo(ForgeDirection.UNKNOWN)) {
            if (info.fluid != null && info.fluid.isFluidEqual(expected)) received += info.fluid.amount;
        }
        check(
            received == expected.amount,
            "Manual " + (side < 0 ? "metal" : "slag") + " pipe did not deliver its 144 mB sample");
    }

    private static BaseMetaTileEntity outletTank(WorldServer world, FakePlayer placer, BaseMetaTileEntity outlet) {
        ForgeDirection direction = outlet.getFrontFacing();
        ForgeDirection inlet = direction.getOpposite();
        int x = outlet.getXCoord(), y = outlet.getYCoord(), z = outlet.getZCoord();
        BaseMetaTileEntity tank = place(
            world,
            placer,
            ItemList.Super_Tank_HV.get(1),
            x + 3 * direction.offsetX,
            y,
            z + 3 * direction.offsetZ);
        tank.setFrontFacing(direction);
        for (int offset = 1; offset <= 2; offset++) {
            BaseMetaPipeEntity tile = (BaseMetaPipeEntity) placeTile(
                world,
                placer,
                GTOreDictUnificator.get(OrePrefixes.pipeSmall, Materials.Steel, 1),
                x + offset * direction.offsetX,
                y,
                z + offset * direction.offsetZ);
            MTEFluidPipe pipe = (MTEFluidPipe) tile.getMetaTileEntity();
            pipe.connect(direction);
            pipe.connect(inlet);
            pipe.mDisableInput = (byte) (63 & ~inlet.flag);
            tile.markDirty();
        }
        return tank;
    }

    private static void manualSupplies(WorldServer world, FakePlayer placer, Station station) {
        for (IronBathStructure.Offset tap : new IronBathStructure.Offset[] { IronBathStructure.METAL_TAP,
            IronBathStructure.SLAG_TAP }) {
            int x = MANUAL_X + tap.x(), y = STATION_Y + tap.y(), z = STATION_Z + tap.z();
            outletTank(world, placer, tap == IronBathStructure.METAL_TAP ? station.metal : station.slag);
            boolean lower = tap == IronBathStructure.METAL_TAP;
            int platformX = x + (lower ? 1 : -1);
            int platformStartZ = z - (lower ? 4 : 3), platformEndZ = z - (lower ? 2 : 0);
            for (int platformZ = platformStartZ; platformZ <= platformEndZ; platformZ++)
                if (world.isAirBlock(platformX, y - 1, platformZ))
                    world.setBlock(platformX, y - 1, platformZ, Blocks.stonebrick, 0, 3);
            int height = y - GROUND_Y;
            for (int step = 0; step < height; step++)
                world.setBlock(platformX, GROUND_Y + step, platformStartZ - height + step, Blocks.stone_stairs, 2, 3);
        }
        ((MTEIronBathTap) station.metal.getMetaTileEntity()).setRedstoneMode(3);
        ((MTEIronBathTap) station.slag.getMetaTileEntity()).setRedstoneMode(3);
        check(
            ((MTEIronBathTap) station.metal.getMetaTileEntity()).fill(Materials.Steel.getMolten(144), true) == 144
                && ((MTEIronBathTap) station.slag.getMetaTileEntity()).fill(
                    IronBathFurnace.material(IronProduct.SLAG)
                        .getMolten(144),
                    true) == 144,
            "Could not load the manual pipe construction samples");
        chest(
            world,
            MANUAL_X - 7,
            97,
            GTOreDictUnificator.get(OrePrefixes.ingot, Materials.Iron, 64),
            GTOreDictUnificator.get(OrePrefixes.ingot, Materials.Iron, 64),
            GTOreDictUnificator.get(OrePrefixes.dust, Materials.Iron, 64),
            GTOreDictUnificator.get(OrePrefixes.dust, Materials.Calcite, 64),
            new ItemStack(GameRegistry.findItem("Railcraft", "fuel.coke"), 64),
            new ItemStack(Items.coal, 64),
            new ItemStack(Items.coal, 64, 1));
        ItemStack oxygenCell = GTUtility
            .fillFluidContainer(Materials.Oxygen.getGas(1000), ItemList.Cell_Empty.get(1), false, true);
        check(oxygenCell != null, "No native Oxygen cell is available for manual dosing");
        oxygenCell.stackSize = 64;
        chest(
            world,
            MANUAL_X + 7,
            97,
            oxygenCell,
            ItemList.Cell_Empty.get(64),
            new ItemStack(Blocks.lever, 16),
            new ItemStack(Items.redstone, 64),
            IronBathFurnace.tap(1));
        sign(
            world,
            MANUAL_X - 7,
            GROUND_Y + 1,
            96,
            "SOLID SUPPLIES",
            "Iron / fuel / flux",
            "Feed north bus",
            "Volume limited");
        sign(
            world,
            MANUAL_X + 7,
            GROUND_Y + 1,
            96,
            "OXYGEN SUPPLIES",
            "Cells: 1000 L",
            "Use north hatch",
            "Rate tracks power");
        sign(
            world,
            MANUAL_X + 12,
            STATION_Y + IronBathStructure.METAL_TAP.y(),
            STATION_Z - 2,
            "LOWER METAL TAP",
            "Open tap GUI",
            "Set rate + mode",
            "Tank to north");
        sign(
            world,
            MANUAL_X + 5,
            STATION_Y + IronBathStructure.SLAG_TAP.y(),
            STATION_Z - 2,
            "UPPER SLAG TAP",
            "Open tap GUI",
            "Set rate + mode",
            "Tank to north");
        chest(
            world,
            MANUAL_X - 7,
            105,
            ItemList.Casing_Coil_Cupronickel.get(50),
            ItemList.Casing_Coil_Kanthal.get(50),
            ItemList.Casing_Coil_Nichrome.get(50),
            ItemList.Casing_Coil_TungstenSteel.get(50),
            ItemList.Casing_Coil_HSSG.get(50),
            ItemList.Casing_Coil_HSSS.get(50),
            ItemList.Casing_Coil_Naquadah.get(50),
            ItemList.Casing_Coil_Trinium.get(50),
            ItemList.Casing_Coil_NaquadahAlloy.get(50),
            ItemList.Casing_Coil_ElectrumFlux.get(50),
            ItemList.Casing_Coil_AwakenedDraconium.get(50),
            ItemList.Casing_Coil_Eternal.get(50),
            ItemList.Casing_Coil_Infinity.get(50),
            ItemList.Casing_Coil_Hypogen.get(50),
            ItemList.DebugEnergyHatch.get(2),
            ItemList.Hatch_Energy_LV.get(2),
            ItemList.Hatch_Energy_HV.get(2),
            ItemList.Hatch_Energy_EV.get(2),
            IronBathFurnace.casing(0, 16),
            IronBathFurnace.casing(1, 16));
        sign(
            world,
            MANUAL_X - 7,
            GROUND_Y + 1,
            104,
            "COIL UPGRADES",
            "Replace all 50",
            "Larger bath",
            "Faster melting");
    }

    private static void chest(WorldServer world, int x, int z, ItemStack... supplies) {
        world.setBlock(x, GROUND_Y, z, Blocks.chest, 2, 3);
        TileEntityChest chest = (TileEntityChest) world.getTileEntity(x, GROUND_Y, z);
        for (int index = 0; index < supplies.length; index++) chest.setInventorySlotContents(index, supplies[index]);
        chest.markDirty();
    }

    private static void sign(WorldServer world, int x, int y, int z, String... lines) {
        if (y > GROUND_Y) world.setBlock(x, y - 1, z, Blocks.stonebrick, 0, 3);
        world.setBlock(x, y, z, Blocks.standing_sign, 8, 3);
        TileEntity tile = world.getTileEntity(x, y, z);
        check(tile instanceof TileEntitySign, "Laboratory sign needs solid support at " + x + "," + y + "," + z);
        TileEntitySign sign = (TileEntitySign) tile;
        for (int line = 0; line < Math.min(4, lines.length); line++) sign.signText[line] = lines[line];
        sign.markDirty();
        world.markBlockForUpdate(x, y, z);
    }

    private static BaseMetaTileEntity place(WorldServer world, FakePlayer placer, ItemStack stack, int x, int y,
        int z) {
        return (BaseMetaTileEntity) placeTile(world, placer, stack, x, y, z);
    }

    private static TileEntity placeTile(WorldServer world, FakePlayer placer, ItemStack stack, int x, int y, int z) {
        world.setBlockToAir(x, y, z);
        check(stack != null && stack.getItem() instanceof ItemBlock, "Missing native laboratory block");
        check(
            ((ItemBlock) stack.getItem()).placeBlockAt(stack, placer, world, x, y, z, 1, 0.5f, 0.5f, 0.5f, 0),
            "Could not place laboratory block at " + x + "," + y + "," + z);
        return world.getTileEntity(x, y, z);
    }

    private static void placePlayerAtEntrance() {
        placePlayerAtEntrance(player());
    }

    private static void placePlayerAtEntrance(EntityPlayerMP viewer) {
        viewer.theItemInWorldManager.setGameType(WorldSettings.GameType.CREATIVE);
        viewer.capabilities.isFlying = false;
        viewer.sendPlayerAbilities();
        viewer.setSpawnChunk(new ChunkCoordinates(SPAWN_X, GROUND_Y, SPAWN_Z), true);
        viewer.setPositionAndUpdate(SPAWN_X + 0.5, GROUND_Y, SPAWN_Z + 0.5);
        viewer.rotationYaw = 0;
        viewer.rotationPitch = 0;
    }

    private static void view(ClientScenario scenario, String name, double x, double y, double z, float yaw,
        float pitch) {
        scenario.server("position the laboratory camera for " + name, () -> {
            player().capabilities.isFlying = true;
            player().sendPlayerAbilities();
            player().setPositionAndUpdate(x, y, z);
        })
            .awaitClient(
                "receive the laboratory camera position",
                client -> check(
                    Math.abs(Minecraft.getMinecraft().thePlayer.posX - x) < 1
                        && Math.abs(Minecraft.getMinecraft().thePlayer.posZ - z) < 1,
                    "Laboratory camera teleport is pending"))
            .client("orient the laboratory camera", client -> {
                Minecraft mc = Minecraft.getMinecraft();
                mc.thePlayer.rotationYaw = mc.thePlayer.prevRotationYaw = yaw;
                mc.thePlayer.rotationPitch = mc.thePlayer.prevRotationPitch = pitch;
            });
        scenario.serverSequence()
            .thenIdle(15);
        scenario.capture(name);
    }

    private static void guiViews(ClientScenario scenario, Station[] stations) {
        scenario
            .client(
                "show the manual station interfaces",
                client -> Minecraft.getMinecraft().gameSettings.hideGUI = false)
            .server("approach the populated laboratory controller", () -> {
                player().inventory.currentItem = 8;
                player().inventory.setInventorySlotContents(8, null);
                player().setPositionAndUpdate(MANUAL_X + 0.5, STATION_Y, STATION_Z - 1.5);
            })
            .awaitClient("the laboratory controller reaches the client", client -> {
                Minecraft mc = Minecraft.getMinecraft();
                check(
                    mc.theWorld != null
                        && mc.theWorld.getTileEntity(MANUAL_X, STATION_Y, STATION_Z) instanceof BaseMetaTileEntity
                        && ((BaseMetaTileEntity) mc.theWorld.getTileEntity(MANUAL_X, STATION_Y, STATION_Z))
                            .getMetaTileEntity() instanceof MTEIronBathFurnace,
                    "The laboratory controller has not synchronized");
            })
            .server("open the populated laboratory controller", () -> {
                Station manual = stations[0];
                check(manual.machine.onRightclick(manual.controller, player()), "Laboratory controller did not open");
            })
            .awaitScreen(GuiContainerWrapper.class)
            .click(blankCorner())
            .capture("lab-controller");
        controllerCompositionViews(scenario, stations);
        scenario.escape()
            .awaitClient(
                "laboratory controller screen closed",
                client -> check(Minecraft.getMinecraft().currentScreen == null, "Laboratory controller is still open"))
            .server("clean up the inspected controller window", () -> {
                ModularNetwork.SERVER.closeAll(player());
                player().closeContainer();
            })
            .awaitServer(
                "laboratory controller container closed",
                () -> check(
                    player().openContainer == player().inventoryContainer,
                    "Laboratory controller container is still open"))
            .server("open the manual special tap", () -> {
                Station manual = stations[0];
                player().setPositionAndUpdate(
                    MANUAL_X + IronBathStructure.METAL_TAP.x() + 1.5,
                    STATION_Y + IronBathStructure.METAL_TAP.y(),
                    STATION_Z + IronBathStructure.METAL_TAP.z() - 1.5);
                check(
                    ((MTEIronBathTap) manual.metal.getMetaTileEntity()).onRightclick(manual.metal, player()),
                    "Laboratory tap did not open");
            })
            .awaitScreen(GuiContainerWrapper.class)
            .click(blankCorner())
            .capture("lab-tap")
            .escape()
            .awaitClient(
                "laboratory tap screen closed",
                client -> check(Minecraft.getMinecraft().currentScreen == null, "Laboratory tap is still open"))
            .server("clean up the inspected tap window", () -> {
                ModularNetwork.SERVER.closeAll(player());
                player().closeContainer();
            })
            .awaitServer(
                "laboratory tap container closed",
                () -> check(
                    player().openContainer == player().inventoryContainer,
                    "Laboratory tap container is still open"));
    }

    private static void controllerCompositionViews(ClientScenario scenario, Station[] stations) {
        NBTTagCompound[] original = new NBTTagCompound[1];
        scenario.server("save the manual bath before composition views", () -> {
            original[0] = new NBTTagCompound();
            stations[0].machine.saveNBTData(original[0]);
        });
        for (IronProduct product : new IronProduct[] { IronProduct.IRON, IronProduct.OVERCARBURIZED_IRON }) {
            scenario.server("show composition band " + product, () -> {
                NBTTagCompound sample = (NBTTagCompound) original[0].copy();
                NBTTagCompound bath = sample.getCompoundTag("IronBath");
                bath.setDouble("Carbon", product == IronProduct.IRON ? 0 : bath.getInteger("Metal") / 144.0 * 5);
                stations[0].machine.loadNBTData(sample);
                check(
                    stations[0].state()
                        .currentProduct() == product,
                    "Composition render fixture has the wrong product");
            });
            scenario.serverSequence()
                .thenIdle(20);
            scenario.click(blankCorner())
                .capture(
                    "controller-band-" + product.name()
                        .toLowerCase(java.util.Locale.ROOT));
        }
        scenario
            .server("restore molten contents for their tooltip", () -> stations[0].machine.loadNBTData(original[0]));
        scenario.serverSequence()
            .thenIdle(20);
        ClientTarget contents = ClientTarget.of("bath contents tooltip", window -> {
            window.screen(GuiContainerWrapper.class);
            IWidget widget = chargeWidget("bathContents");
            check(widget != null, "Bath contents widget is missing");
            return new ClickTarget(widget, new Rectangle(widget.getArea()), widget::isEnabled);
        });
        scenario.click(contents);
        scenario.serverSequence()
            .thenIdle(20);
        scenario.capture("controller-contents-molten")
            .server("show partially molten contents", () -> {
                NBTTagCompound sample = (NBTTagCompound) original[0].copy();
                NBTTagCompound bath = sample.getCompoundTag("IronBath");
                bath.setDouble("Heat", bath.getDouble("Heat") * 0.6);
                stations[0].machine.loadNBTData(sample);
                IronBath state = stations[0].state();
                check(
                    state.liquidMetalMb() > 0 && state.liquidMetalMb() < state.metalMb(),
                    "Partial-melting render fixture has the wrong phase");
            });
        scenario.serverSequence()
            .thenIdle(20);
        scenario.capture("controller-contents-partial")
            .server("restore the manual laboratory bath", () -> stations[0].machine.loadNBTData(original[0]));
    }

    private static ClientTarget blankCorner() {
        return ClientTarget.of("blank laboratory screen corner", window -> {
            GuiContainerWrapper screen = window.screen(GuiContainerWrapper.class);
            check(
                Minecraft.getMinecraft().thePlayer.inventory.getItemStack() == null,
                "Laboratory pointer is holding an item");
            return new ClickTarget(screen, new Rectangle(2, 2, 2, 2), () -> true);
        });
    }

    private static EntityPlayerMP player() {
        return (EntityPlayerMP) MinecraftServer.getServer()
            .getConfigurationManager().playerEntityList.get(0);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class Station {

        private final BaseMetaTileEntity controller;
        private final MTEIronBathFurnace machine;
        private final BaseMetaTileEntity input;
        private final BaseMetaTileEntity oxygen;
        private final BaseMetaTileEntity metal;
        private final BaseMetaTileEntity slag;
        private final BaseMetaTileEntity[] energy = new BaseMetaTileEntity[2];

        private Station(WorldServer world, FakePlayer placer, int x, ItemList inputBusType, int upperTapTier,
            int sampleHeight) {
            controller = place(world, placer, IronBathFurnace.controller(), x, STATION_Y, STATION_Z);
            controller.setFrontFacing(ForgeDirection.NORTH);
            machine = (MTEIronBathFurnace) controller.getMetaTileEntity();
            machine.construct(IronBathFurnace.controller(), false);
            for (IronBathStructure.Offset coil : IronBathStructure.COIL_POSITIONS) place(
                world,
                placer,
                ItemList.Casing_Coil_Cupronickel.get(1),
                x + coil.x(),
                STATION_Y + coil.y(),
                STATION_Z + coil.z());
            input = port(world, placer, inputBusType.get(1), x, IronBathStructure.INPUT_BUS, ForgeDirection.NORTH);
            oxygen = port(
                world,
                placer,
                ItemList.Hatch_Input_HV.get(1),
                x,
                IronBathStructure.OXYGEN_INPUT,
                ForgeDirection.NORTH);
            port(
                world,
                placer,
                ItemList.Hatch_Maintenance.get(1),
                x,
                IronBathStructure.MAINTENANCE,
                ForgeDirection.SOUTH);
            energy[0] = port(
                world,
                placer,
                ItemList.DebugEnergyHatch.get(1),
                x,
                IronBathStructure.ENERGY_A,
                ForgeDirection.NORTH);
            energy[1] = port(
                world,
                placer,
                ItemList.DebugEnergyHatch.get(1),
                x,
                IronBathStructure.ENERGY_B,
                ForgeDirection.SOUTH);
            for (BaseMetaTileEntity tile : energy) {
                MTEHatchEnergyDebug debug = (MTEHatchEnergyDebug) tile.getMetaTileEntity();
                debug.setVoltageTier(3);
                debug.setAmperage(2);
                debug.setRefillInterval(20);
                tile.markDirty();
            }
            metal = port(
                world,
                placer,
                IronBathFurnace.tap(1),
                x,
                sampleHeight == 2 ? IronBathStructure.SAMPLE_TAP : IronBathStructure.METAL_TAP,
                ForgeDirection.NORTH);
            slag = port(
                world,
                placer,
                IronBathFurnace.tap(upperTapTier),
                x,
                IronBathStructure.SLAG_TAP,
                ForgeDirection.NORTH);
            check(machine.checkStructure(true, controller), "Laboratory furnace did not form at " + x);
            machine.fixAllIssues();
            controller.enableWorking();
        }

        private static BaseMetaTileEntity port(WorldServer world, FakePlayer placer, ItemStack stack, int x,
            IronBathStructure.Offset offset, ForgeDirection facing) {
            BaseMetaTileEntity tile = place(
                world,
                placer,
                stack,
                x + offset.x(),
                STATION_Y + offset.y(),
                STATION_Z + offset.z());
            tile.setFrontFacing(facing);
            return tile;
        }

        private void seedHotSteel(int ingots, int slagMb) {
            NBTTagCompound tag = new NBTTagCompound();
            machine.saveNBTData(tag);
            NBTTagCompound bathTag = tag.getCompoundTag("IronBath");
            bathTag.setInteger("Metal", ingots * 144);
            bathTag.setInteger("Slag", slagMb);
            bathTag.setDouble("Carbon", ingots);
            bathTag.setDouble("Heat", 0);
            IronBath seeded = new IronBath(0);
            seeded.load(bathTag);
            bathTag.setDouble("Heat", seeded.heatDemand(IronBath.WORKING_TEMPERATURE));
            machine.loadNBTData(tag);
            controller.markDirty();
            check(
                state().liquidMetalMb() == ingots * 144 && state().liquidSlagMb() == slagMb
                    && state().currentProduct() == IronProduct.STEEL,
                "Construction-only hot Steel seed did not match the laboratory starting state");
        }

        private IronBath state() {
            NBTTagCompound tag = new NBTTagCompound();
            machine.saveNBTData(tag);
            IronBath observed = new IronBath(0);
            observed.load(tag.getCompoundTag("IronBath"));
            return observed;
        }
    }
}
