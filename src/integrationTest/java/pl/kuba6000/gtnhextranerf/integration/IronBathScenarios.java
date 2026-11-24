package pl.kuba6000.gtnhextranerf.integration;

import java.awt.Rectangle;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import org.lwjgl.input.Keyboard;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.cleanroommc.modularui.widget.WidgetTree;
import com.cleanroommc.modularui.widgets.CycleButtonWidget;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;
import com.gtnewhorizons.horizonqa.api.client.ClickTarget;
import com.gtnewhorizons.horizonqa.api.client.ClientScenario;
import com.gtnewhorizons.horizonqa.api.client.ClientTarget;
import com.gtnewhorizons.horizonqa.api.client.ClientTest;
import com.mojang.authlib.GameProfile;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.GuiCraftingRecipe;
import codechicken.nei.recipe.ICraftingHandler;
import codechicken.nei.recipe.Recipe;
import codechicken.nei.recipe.StackInfo;
import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicMachine;
import gregtech.api.metatileentity.implementations.MTEHatchInput;
import gregtech.api.metatileentity.implementations.MTEHatchMaintenance;
import gregtech.api.objects.ItemData;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
import gregtech.nei.GTNEIDefaultHandler;
import gregtech.nei.GTNEIDefaultHandler.FixedPositionedStack;
import li.cil.oc.api.Driver;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.SidedBlock;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.ManagedEnvironment;
import pl.kuba6000.gtnhextranerf.GTNHExtraNerf;
import pl.kuba6000.gtnhextranerf.client.nei.IronBathRecipeHandler;
import pl.kuba6000.gtnhextranerf.integration.opencomputers.IronBathMeasurementDriver;
import pl.kuba6000.gtnhextranerf.machines.IronBathFurnace;
import pl.kuba6000.gtnhextranerf.machines.IronBathStructure;
import pl.kuba6000.gtnhextranerf.machines.IronBathStructure.Offset;
import pl.kuba6000.gtnhextranerf.machines.MTEIronBathFurnace;
import pl.kuba6000.gtnhextranerf.machines.MTEIronBathMeasurement;
import pl.kuba6000.gtnhextranerf.machines.MTEIronBathTap;
import pl.kuba6000.gtnhextranerf.machines.ironbath.IronBath;
import pl.kuba6000.gtnhextranerf.machines.ironbath.IronProduct;
import pl.kuba6000.gtnhextranerf.materials.IronBathMaterials;
import pl.kuba6000.gtnhextranerf.progression.EnergyHatchCertification;
import pl.kuba6000.gtnhextranerf.progression.MachineProgression;
import pl.kuba6000.gtnhextranerf.progression.ProgressionData;
import pl.kuba6000.gtnhextranerf.progression.ProgressionTier;
import pl.kuba6000.gtnhextranerf.telemetry.ConsentPrompt;

/** Native construction, powered melting, phase-dependent outlets and rendered furnace views. */
@GameTestHolder(value = "gtnhextranerftests", clientOnly = true)
public final class IronBathScenarios {

    // Transient fixtures must not overlap the persistent laboratory around X=1024..1052.
    private static final int X = 1280;
    private static final int Y = 100;
    private static final int Z = 96;
    private static final int WORKING_CHARGE_INGOTS = 384;
    private static final int EXPECTED_COIL_COUNT = 50;
    // The timeout includes cold shell heating and the native two-HV overclock charge budget.
    private static final int COLD_CHARGE_TIMEOUT_TICKS = 125000;
    private static final String DIAGRAM_HANDLER = "gtnhextranerf.iron_bath.diagram";

    private IronBathScenarios() {}

    @GameTest(template = "native_fixture", timeoutTicks = 600)
    public static void volumeLimitedInputKeepsExcessInBus(GameTestHelper helper) {
        ClientTest.scenario(helper)
            .server("admit a tank-sized native charge without heating-position limits", () -> {
                Fixture f = new Fixture(helper);
                NBTTagCompound saved = new NBTTagCompound();
                f.machine.saveNBTData(saved);
                saved.getCompoundTag("IronBath")
                    .setInteger("Metal", 400 * 144);
                f.machine.loadNBTData(saved);
                for (int slot = 0; slot < 2; slot++) f.input
                    .setInventorySlotContents(slot, GTOreDictUnificator.get(OrePrefixes.ingot, Materials.Iron, 64));
                f.input.setInventorySlotContents(2, new ItemStack(GameRegistry.findItem("Railcraft", "fuel.coke"), 1));
                f.input.markDirty();
                f.machine.onPostTick(f.controller, f.world.getTotalWorldTime());
                IronBath bath = f.state();
                check(
                    bath.pendingMetalMb() == 96 * 144 && bath.meltingItems() == 96,
                    "Native intake retained a heating-position limit");
                int remaining = 0;
                for (int slot = 0; slot < f.input.getSizeInventory(); slot++) {
                    ItemStack stack = f.input.getStackInSlot(slot);
                    if (stack != null) remaining += stack.stackSize;
                }
                check(remaining == 32, "Native intake consumed items beyond the metal volume allowance");
                check(
                    bath.carbon() == 100 && bath.impurities() == 1,
                    "A full metal allowance blocked direct carbon admission");
                check(
                    bath.chargeGroupCount() == 1 && bath.chargeGroups(0, 1)
                        .get(0)
                        .count() == 96,
                    "Equal native input stacks did not share one heating group");
                double progress = bath.chargeGroups(0, 1)
                    .get(0)
                    .progress();
                check(progress > 0 && progress < 1, "The native heating group did not receive paid heat");
                f.machine.saveNBTData(saved);
                f.machine.loadNBTData(saved);
                bath = f.state();
                check(
                    bath.pendingMetalMb() == 96 * 144 && bath.chargeGroups(0, 1)
                        .get(0)
                        .progress() == progress,
                    "Native reload lost queued metal or paid heating progress");
                check(f.forms(), "The reloaded volume-limited furnace did not form");
            })
            .succeed();
    }

    @GameTest(template = "native_fixture", timeoutTicks = 1800)
    public static void disabledBathCoolsAndStopsInputsAndTapping(GameTestHelper helper) {
        Fixture[] fixture = new Fixture[1];
        NBTTagCompound[] warmState = new NBTTagCompound[1];
        double[] temperature = new double[1];
        long[] startTick = new long[1];
        long[] storedEu = new long[1];
        ClientScenario scenario = prepareClientView(helper).defaultTimeoutTicks(1200)
            .server("seed a hot native HV bath", () -> {
                Fixture f = fixture[0] = new Fixture(helper);
                NBTTagCompound controllerState = new NBTTagCompound();
                f.machine.saveNBTData(controllerState);
                NBTTagCompound state = controllerState.getCompoundTag("IronBath");
                state.setInteger("Metal", WORKING_CHARGE_INGOTS * 144);
                state.setDouble("Carbon", WORKING_CHARGE_INGOTS);
                state.setInteger("Calcite", 20);
                state.setInteger("Impurities", 1);
                IronBath seeded = new IronBath(0);
                seeded.load(state);
                state.setDouble("Heat", seeded.heatDemand(seeded.maximumTemperature()));
                warmState[0] = controllerState;
                f.machine.loadNBTData(controllerState);
                f.controller.disableWorking();
                f.metalTap.setRedstoneMode(2);
                f.input.setInventorySlotContents(0, GTOreDictUnificator.get(OrePrefixes.ingot, Materials.Iron, 1));
                f.input.setInventorySlotContents(1, GTOreDictUnificator.get(OrePrefixes.gem, Materials.Coal, 1));
                f.input.setInventorySlotContents(2, GTOreDictUnificator.get(OrePrefixes.dust, Materials.Calcite, 1));
                check(
                    f.oxygen()
                        .fill(ForgeDirection.UNKNOWN, Materials.Oxygen.getGas(1000), true) == 1000,
                    "Oxygen hatch rejected the disabled-controller fixture");
                temperature[0] = f.state()
                    .temperature();
                startTick[0] = f.world.getTotalWorldTime();
                storedEu[0] = f.energy.stream()
                    .mapToLong(BaseMetaTileEntity::getStoredEU)
                    .sum();
            })
            .awaitServerAccelerated(
                "disabled bath cools for 600 real world ticks",
                100,
                () -> {
                    check(
                        fixture[0].world.getTotalWorldTime() - startTick[0] >= 600,
                        "Cooling observation is still running");
                })
            .server("verify cooling and disabled input and outlet paths", () -> {
                Fixture f = fixture[0];
                IronBath observed = f.state();
                check(observed.temperature() < temperature[0] - 10, "Disabled HV bath did not visibly cool");
                check(observed.metalMb() == WORKING_CHARGE_INGOTS * 144, "Disabled tap withdrew metal");
                check(f.metalTap.getFluidAmount() == 0, "Disabled tap contains freshly withdrawn fluid");
                check(
                    observed.meltingItems() == 0 && observed.calciteCapacity() == 20
                        && observed.impurities() == 1
                        && observed.slagMb() == 0,
                    "Disabled bath admitted solid feed");
                for (int slot = 0; slot < 3; slot++) check(
                    f.input.getStackInSlot(slot) != null && f.input.getStackInSlot(slot).stackSize == 1,
                    "Disabled input bus consumed an item");
                check(
                    f.oxygen()
                        .getFluidAmount() == 1000,
                    "Disabled bath consumed Oxygen");
                check(
                    f.energy.stream()
                        .mapToLong(BaseMetaTileEntity::getStoredEU)
                        .sum() == storedEu[0],
                    "Disabled bath consumed electricity");
                f.controller.enableWorking();
                f.machine.onPostTick(f.controller, f.world.getTotalWorldTime());
                check(
                    f.state()
                        .pendingMetalMb() == 144
                        && f.state()
                            .carbon() > observed.carbon(),
                    "Reenabled bath did not admit metal and carbon");
                check(
                    f.oxygen()
                        .getFluidAmount() < 1000,
                    "Reenabled bath did not admit Oxygen");
                check(f.metalTap.getFluidAmount() > 0, "Reenabled bath did not resume tapping");
                f.metalTap.setRedstoneMode(3);
                for (int slot = 0; slot < f.input.getSizeInventory(); slot++)
                    f.input.setInventorySlotContents(slot, null);
                f.oxygen()
                    .drain(ForgeDirection.UNKNOWN, 1000, true);
                f.machine.loadNBTData(warmState[0]);
                check(f.forms(), "Idle fixture did not retain its structure after loading the warm state");
                f.controller.enableWorking();
                f.refillPower();
            });
        scenario.awaitServerAccelerated("idle draw follows Cupronickel coils despite HV power", 1, () -> {
            Fixture f = fixture[0];
            f.refillPower();
            check(
                -f.machine.lEUt >= 30 && -f.machine.lEUt <= 31,
                "Expected about 30 EU/t idle draw despite 1920 EU/t installed supply, got " + -f.machine.lEUt
                    + ", allowed="
                    + f.controller.isAllowedToWork()
                    + ", temperature="
                    + f.state()
                        .temperature());
        })
            .server("open the stable idle controller", () -> {
                Fixture f = fixture[0];
                player().setPositionAndUpdate(X + 0.5, Y + 1, Z - 1.5);
                check(f.machine.onRightclick(f.controller, player()), "Controller rejected GUI opening");
            })
            .awaitScreen(GuiContainerWrapper.class)
            .click(blankScreenCorner())
            .capture("controller-idle-thermal")
            .escape()
            .succeed();
    }

    @GameTest(template = "native_fixture", timeoutTicks = 1800)
    public static void highPowerIdleHeatingKeepsTheBathMolten(GameTestHelper helper) {
        Fixture[] fixture = new Fixture[1];
        prepareClientView(helper).server("prepare UXV power on a cold Cupronickel bath", () -> {
            Fixture f = fixture[0] = new Fixture(helper);
            for (ProgressionTier tier : ProgressionTier.values()) MachineProgression.certify(f.world, tier);
            FakePlayer placer = FakePlayerFactory.getMinecraft(f.world);
            f.energy.clear();
            for (Offset offset : new Offset[] { IronBathStructure.ENERGY_A, IronBathStructure.ENERGY_B }) {
                BaseMetaTileEntity tile = f.place(placer, ItemList.DebugEnergyHatch.get(1), offset);
                gregtech.api.metatileentity.implementations.MTEHatchEnergyDebug energy = (gregtech.api.metatileentity.implementations.MTEHatchEnergyDebug) tile
                    .getMetaTileEntity();
                energy.setVoltageTier(13);
                energy.setAmperage(2);
                energy.setRefillInterval(1);
                tile.setStoredEU(Long.MAX_VALUE);
                f.energy.add(tile);
            }
            check(f.forms(), "High-power idle fixture did not form");
            NBTTagCompound controllerState = new NBTTagCompound();
            f.machine.saveNBTData(controllerState);
            NBTTagCompound state = controllerState.getCompoundTag("IronBath");
            state.setInteger("Metal", 71424);
            state.setInteger("Slag", 352);
            state.setDouble("Carbon", 188);
            state.setDouble("Heat", 0);
            f.machine.loadNBTData(controllerState);
            f.metalTap.setRedstoneMode(3);
            f.machine.onPostTick(f.controller, f.world.getTotalWorldTime());
            check(
                -f.machine.lEUt >= 15_441_085 && -f.machine.lEUt <= 15_441_086,
                "Cold UXV start should pay for the stored heat and 10% working loss, got " + -f.machine.lEUt);
        })
            .awaitServerAccelerated("high-power idle bath stays liquid at its coil ceiling", 1, () -> {
                Fixture f = fixture[0];
                f.refillPower();
                check(
                    -f.machine.lEUt >= 30 && -f.machine.lEUt <= 31,
                    "UXV power should retain Cupronickel's 30 EU/t standby cost, got " + -f.machine.lEUt);
                check(
                    Math.abs(
                        f.state()
                            .temperature() - 2200)
                        < 0.01
                        && f.state()
                            .liquidMetalMb() == 71424,
                    "Funded heat losses froze or overheated the high-power bath");
            })
            .succeed();
    }

    @GameTest(template = "native_fixture", timeoutTicks = 800)
    public static void castIronSolidifiesWithoutLosingItsMaterial(GameTestHelper helper) {
        BaseMetaTileEntity[] casters = new BaseMetaTileEntity[3];
        OrePrefixes[] forms = { OrePrefixes.ingot, OrePrefixes.nugget, OrePrefixes.block };
        ItemList[] molds = { ItemList.Shape_Mold_Ingot, ItemList.Shape_Mold_Nugget, ItemList.Shape_Mold_Block };
        int[] amounts = { 144, 16, 1296 };
        int[] durations = { 32, 16, 288 };
        int[] eut = { 7, 4, 7 };
        helper.afterTest(
            () -> {
                for (int i = 0; i < casters.length; i++) if (casters[i] != null) helper.getWorld()
                    .setBlockToAir(X + i * 2, Y, Z);
            });
        ClientTest.scenario(helper)
            .server("prepare native Cast Iron solidifiers", () -> {
                WorldServer world = helper.getWorld();
                FixtureWorld.loadAround(world, X, Z);
                if (world.mapStorage.loadData(ProgressionData.class, ProgressionData.NAME) == null)
                    world.mapStorage.setData(ProgressionData.NAME, new ProgressionData());
                for (ProgressionTier tier : ProgressionTier.values()) MachineProgression.certify(world, tier);
                FakePlayer placer = FakePlayerFactory.getMinecraft(world);
                for (int i = 0; i < casters.length; i++) {
                    ItemStack stack = ItemList.Machine_LV_FluidSolidifier.get(1);
                    check(
                        ((ItemBlock) stack.getItem())
                            .placeBlockAt(stack, placer, world, X + i * 2, Y, Z, 1, 0.5f, 0.5f, 0.5f, 0),
                        "Could not place the native Cast Iron solidifier");
                    casters[i] = (BaseMetaTileEntity) world.getTileEntity(X + i * 2, Y, Z);
                    MTEBasicMachine machine = (MTEBasicMachine) casters[i].getMetaTileEntity();
                    machine.setMainFacing(ForgeDirection.NORTH);
                    casters[i].setFrontFacing(ForgeDirection.SOUTH);
                    machine.setInventorySlotContents(machine.getInputSlot(), molds[i].get(1));
                    check(
                        machine.fill(ForgeDirection.UNKNOWN, Materials.CastIron.getMolten(amounts[i]), true)
                            == amounts[i],
                        "The native solidifier rejected molten Cast Iron");
                    casters[i].setStoredEU(casters[i].getEUCapacity());
                    casters[i].enableWorking();
                }
            })
            .withinTicks(600)
            .awaitServer("all three native cast products remain Cast Iron", () -> {
                for (int i = 0; i < casters.length; i++) {
                    MTEBasicMachine machine = (MTEBasicMachine) casters[i].getMetaTileEntity();
                    ItemStack output = machine.getStackInSlot(machine.getOutputSlot());
                    check(output != null, "Native Cast Iron " + forms[i] + " output is not ready");
                    ItemData data = GTOreDictUnificator.getItemData(output);
                    check(
                        data != null && data.mMaterial != null && data.mMaterial.mMaterial == Materials.CastIron,
                        "Molten Cast Iron produced " + output.getDisplayName() + " instead of Cast Iron " + forms[i]);
                    check(
                        data.mPrefix == forms[i] && output.stackSize == 1,
                        "Cast Iron solidification changed the native form or yield");
                    check(machine.getFluidAmount() == 0, "Cast Iron solidification left unexpected fluid");
                }
                for (int i = 0; i < forms.length; i++) {
                    final ItemStack mold = molds[i].get(0);
                    final FluidStack fluid = Materials.CastIron.getMolten(amounts[i]);
                    GTRecipe[] matching = RecipeMaps.fluidSolidifierRecipes.getAllRecipes()
                        .stream()
                        .filter(
                            recipe -> recipe.mInputs != null && recipe.mInputs.length == 1
                                && recipe.mInputs[0] != null
                                && recipe.mInputs[0].isItemEqual(mold)
                                && recipe.mFluidInputs != null
                                && recipe.mFluidInputs.length == 1
                                && recipe.mFluidInputs[0].isFluidEqual(fluid)
                                && recipe.mFluidInputs[0].amount == fluid.amount)
                        .toArray(GTRecipe[]::new);
                    check(
                        matching.length == 1,
                        "Cast Iron " + forms[i] + " has competing solidifier recipes: " + matching.length);
                    check(
                        matching[0].mDuration == durations[i] && matching[0].mEUt == eut[i],
                        "Cast Iron " + forms[i] + " changed its native duration or EU cost");
                }
            })
            .succeed();
    }

    @GameTest(template = "native_fixture", timeoutTicks = 2400, required = false)
    public static void neiStyleReference(GameTestHelper helper) {
        prepareClientView(helper)
            .client(
                "verify the runner comparison viewport",
                client -> check(
                    Minecraft.getMinecraft().displayWidth == 1280 && Minecraft.getMinecraft().displayHeight == 720,
                    "The reference scenario requires the runner's 1280x720 viewport"))
            .client(
                "open the native GT5 HTGR reference",
                client -> check(
                    GuiCraftingRecipe.openRecipeGui("kubatech.htgrrecipes"),
                    "Native HTGR recipe screen did not open"))
            .awaitScreen(GuiCraftingRecipe.class)
            .awaitClient("HTGR reference is visible at the comparison scale", client -> {
                GuiCraftingRecipe gui = client.screen(GuiCraftingRecipe.class);
                check(
                    "kubatech.htgrrecipes".equals(
                        gui.getHandler()
                            .getOverlayIdentifier()),
                    "The visible recipe handler is not HTGR");
                check(
                    !gui.getRecipeIndices()
                        .isEmpty(),
                    "The HTGR reference has no visible recipes");
                check(
                    Minecraft.getMinecraft().displayWidth == 1280 && Minecraft.getMinecraft().displayHeight == 720,
                    "The HTGR comparison viewport changed");
            })
            .click(blankScreenCorner())
            .capture("reference-htgr")
            .client("open the iron bath at the same scale", client -> {
                Minecraft.getMinecraft()
                    .displayGuiScreen(null);
                check(
                    GuiCraftingRecipe.openRecipeGui(DIAGRAM_HANDLER, IronProduct.STEEL),
                    "Iron bath comparison screen did not open");
            })
            .awaitScreen(GuiCraftingRecipe.class)
            .awaitClient("iron bath comparison is visible", client -> {
                verifyDiagram(IronProduct.STEEL);
                check(
                    Minecraft.getMinecraft().displayWidth == 1280 && Minecraft.getMinecraft().displayHeight == 720,
                    "Comparison viewport changed between captures");
            })
            .click(blankScreenCorner())
            .capture("reference-iron-bath")
            .succeed();
    }

    static ClientScenario prepareClientView(GameTestHelper helper) {
        int[] guiScale = new int[1];
        int[] startupWindow = new int[1];
        boolean[] hideGui = new boolean[1];
        return ClientTest.scenario(helper)
            .defaultTimeoutTicks(600)
            .server("suspend the login consent prompt during visual inspection", () -> {
                ConsentPrompt.stop();
                EntityPlayerMP viewer = player();
                startupWindow[0] = viewer.openContainer == viewer.inventoryContainer ? 0
                    : viewer.openContainer.windowId;
                helper.afterTest(
                    () -> ConsentPrompt.start(MinecraftServer.getServer(), GTNHExtraNerf.isDevelopmentEnvironment()));
            })
            .awaitClient("the startup container has reached the client", client -> {
                Minecraft mc = Minecraft.getMinecraft();
                check(mc.theWorld != null && mc.thePlayer != null, "The client has not joined the world");
                check(
                    startupWindow[0] == 0 || mc.thePlayer.openContainer.windowId == startupWindow[0],
                    "The startup container has not synchronized");
            })
            .client("prepare readable native views", client -> {
                Minecraft mc = Minecraft.getMinecraft();
                guiScale[0] = mc.gameSettings.guiScale;
                hideGui[0] = mc.gameSettings.hideGUI;
                mc.gameSettings.guiScale = 2;
                mc.gameSettings.hideGUI = false;
                mc.thePlayer.closeScreen();
            })
            .afterTest(client -> {
                Minecraft mc = Minecraft.getMinecraft();
                if (mc.thePlayer != null) mc.thePlayer.closeScreen();
                mc.displayGuiScreen(null);
                mc.gameSettings.guiScale = guiScale[0];
                mc.gameSettings.hideGUI = hideGui[0];
            })
            .awaitServer(
                "the server has closed the startup container",
                () -> check(player().openContainer == player().inventoryContainer, "A startup container remains open"))
            .awaitClient("the client is ready for visual inspection", client -> {
                Minecraft mc = Minecraft.getMinecraft();
                check(mc.theWorld != null && mc.thePlayer != null, "The client has not joined the world");
                check(
                    mc.currentScreen == null && mc.thePlayer.openContainer == mc.thePlayer.inventoryContainer,
                    "A startup screen or container remains open");
            });
    }

    @GameTest(template = "native_fixture", timeoutTicks = 6000)
    public static void neiDiagrams(GameTestHelper helper) {
        ClientScenario scenario = prepareClientView(helper);
        IronProduct[] targets = { IronProduct.IRON, IronProduct.STEEL, IronProduct.WROUGHT_IRON, IronProduct.CAST_IRON,
            IronProduct.PIG_IRON, IronProduct.OVERCARBURIZED_IRON, IronProduct.SLAG };
        String[] names = { "iron", "steel", "wrought", "cast", "pig", "overcarburized", "slag" };
        for (int index = 0; index < targets.length; index++) {
            IronProduct target = targets[index];
            scenario.client("open registered NEI diagram for " + names[index], client -> {
                Minecraft.getMinecraft()
                    .displayGuiScreen(null);
                check(
                    GuiCraftingRecipe.openRecipeGui(DIAGRAM_HANDLER, target),
                    "Registered iron bath diagram did not open for " + target);
            })
                .awaitScreen(GuiCraftingRecipe.class)
                .awaitClient("verify native diagram result", client -> verifyDiagram(target))
                .click(blankScreenCorner())
                .capture("nei-" + names[index]);
        }
        scenario.client("find diagrams through a real Steel ingot output search", client -> {
            Minecraft.getMinecraft()
                .displayGuiScreen(null);
            check(
                GuiCraftingRecipe.openRecipeGui("item", GTOreDictUnificator.get(OrePrefixes.ingot, Materials.Steel, 1)),
                "Native Steel ingot output search did not open");
        })
            .awaitScreen(GuiCraftingRecipe.class)
            .client("select registered bath diagram from output search results", client -> {
                GuiCraftingRecipe gui = client.screen(GuiCraftingRecipe.class);
                check(
                    gui.currenthandlers.stream()
                        .noneMatch(
                            handler -> handler instanceof GTNEIDefaultHandler nativeHandler
                                && nativeHandler.getRecipeMap() == IronBathFurnace.RECIPES),
                    "Steel output search still includes the duplicate material-balance page");
                ICraftingHandler diagram = gui.currenthandlers.stream()
                    .filter(handler -> handler instanceof IronBathRecipeHandler)
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Steel output search omitted the bath diagram"));
                check(
                    gui.openTargetRecipe(Recipe.RecipeId.of(diagram, 0)) >= 0,
                    "NEI could not select the registered output-search diagram");
            })
            .awaitClient("Steel output search displays the bath diagram", client -> verifyDiagram(IronProduct.STEEL))
            .click(blankScreenCorner())
            .capture("nei-steel-output-search")
            .client("select a different native fuel diagram", client -> {
                GuiCraftingRecipe gui = client.screen(GuiCraftingRecipe.class);
                check(
                    gui.getHandler()
                        .numRecipes() == 5,
                    "Steel diagram does not expose all five fuels");
                check(
                    gui.openTargetRecipe(Recipe.RecipeId.of(gui.getHandler(), 1)) == 1,
                    "NEI could not select the second fuel alternative");
            })
            .awaitClient("alternative fuel page is visible", client -> {
                verifyDiagram(IronProduct.STEEL);
                check(
                    client.screen(GuiCraftingRecipe.class)
                        .getRecipeIndices()
                        .contains(1),
                    "The selected fuel alternative is not on the visible page");
            })
            .click(blankScreenCorner())
            .capture("nei-steel-fuel-alternative")
            .succeed();
    }

    private static void verifyDiagram(IronProduct target) {
        GuiCraftingRecipe gui = (GuiCraftingRecipe) Minecraft.getMinecraft().currentScreen;
        check(gui.getHandler() instanceof IronBathRecipeHandler, "Wrong NEI recipe handler is visible");
        check(
            gui.getHandler()
                .numRecipes() == (target == IronProduct.IRON ? 1 : 5),
            "Unexpected number of fuel diagrams for " + target);
        check(
            !gui.getRecipeIndices()
                .isEmpty(),
            "NEI diagram has no visible recipe");
        for (int recipe : gui.getRecipeIndices()) {
            FluidStack result = StackInfo.getFluid(
                gui.getHandler()
                    .getResultStack(recipe).item);
            check(
                result != null && result.isFluidEqual(
                    IronBathFurnace.material(target)
                        .getMolten(1)),
                "Visible NEI diagram has the wrong molten output for " + target);
        }
        EnumSet<IronBath.Fuel> fuels = EnumSet.noneOf(IronBath.Fuel.class);
        for (int recipe = 0; recipe < gui.getHandler()
            .numRecipes(); recipe++) {
            IronBath.Fuel fuel = verifyDiagramBalance(gui.getHandler(), recipe, target);
            if (fuel != null) check(fuels.add(fuel), "Duplicate fuel alternative for " + target);
        }
        check(target == IronProduct.IRON || fuels.size() == 5, "Missing fuel alternative for " + target);
    }

    private static IronBath.Fuel verifyDiagramBalance(ICraftingHandler handler, int recipe, IronProduct target) {
        List<PositionedStack> inputs = handler.getIngredientStacks(recipe);
        check(!inputs.isEmpty(), "Diagram omitted the Iron input");
        check(
            inputs.get(0).item.isItemEqual(GTOreDictUnificator.get(OrePrefixes.ingot, Materials.Iron, 1))
                || inputs.get(0).item.isItemEqual(GTOreDictUnificator.get(OrePrefixes.dust, Materials.Iron, 1)),
            "Diagram metal input is not Iron ingots or dust");
        int iron = diagramItemCount(inputs.get(0));
        FluidStack result = StackInfo.getFluid(handler.getResultStack(recipe).item);
        check(result != null, "Diagram result is not a native fluid display");
        List<PositionedStack> other = handler.getOtherStacks(recipe);
        if (target == IronProduct.IRON) {
            check(
                iron == 1 && result.amount == 144 && other.isEmpty(),
                "Direct Iron melting is not one ingot to 144 mB");
            return null;
        }
        check(inputs.size() >= 3, "Alloy diagram omitted fuel or Calcite");
        int fuelItems = diagramItemCount(inputs.get(1));
        int calciteIcon = diagramItemCount(inputs.get(2));
        check(
            inputs.get(2) instanceof IronBathRecipeHandler.CalciteStack,
            "Calcite input does not expose its exact consumption overlay");
        BigDecimal calcite = new BigDecimal(((IronBathRecipeHandler.CalciteStack) inputs.get(2)).consumptionText());
        check(
            calcite.signum() > 0 && calcite.setScale(0, RoundingMode.CEILING)
                .intValueExact() == calciteIcon,
            "Calcite icon count does not match the displayed fractional consumption");
        IronBath.Fuel fuel = IronBathFurnace.fuel(inputs.get(1).item);
        check(fuel != null, "Diagram has an unrecognized carbon fuel");
        check(
            inputs.get(2).item.isItemEqual(GTOreDictUnificator.get(OrePrefixes.dust, Materials.Calcite, 1)),
            "Flux input is not Calcite");
        check(other.size() == 1, "Alloy diagram must expose both metal and Slag outputs");
        FluidStack coproduct = StackInfo.getFluid(other.get(0).item);
        check(coproduct != null, "Diagram coproduct is not a native fluid display");
        FluidStack metal = target == IronProduct.SLAG ? coproduct : result;
        FluidStack slag = target == IronProduct.SLAG ? result : coproduct;
        IronProduct alloy = target == IronProduct.SLAG ? IronProduct.STEEL : target;
        check(
            metal.isFluidEqual(
                IronBathFurnace.material(alloy)
                    .getMolten(1)),
            "Wrong alloy coproduct in diagram");
        check(
            slag.isFluidEqual(
                IronBathMaterials.slag()
                    .getMolten(1)),
            "Wrong Slag coproduct in diagram");
        long impurities = (long) fuelItems * fuel.impurities();
        check(
            BigDecimal.valueOf(impurities)
                .compareTo(calcite.multiply(BigDecimal.valueOf(20))) == 0,
            "Diagram leaves unused Calcite or impurities");
        check((long) iron * 144 == metal.amount, "Diagram does not conserve Iron");
        check(impurities * 32 == slag.amount, "Diagram does not conserve Slag");
        BigDecimal suppliedCarbon = BigDecimal.valueOf(fuel.carbon())
            .multiply(BigDecimal.valueOf(fuelItems));
        BigDecimal outputCarbon = BigDecimal.valueOf(alloy.carbonPerIngot())
            .multiply(BigDecimal.valueOf(iron));
        check(suppliedCarbon.compareTo(outputCarbon) == 0, "Diagram does not conserve carbon");
        check(
            BigInteger.valueOf(iron)
                .gcd(BigInteger.valueOf(fuelItems))
                .equals(BigInteger.ONE),
            "Diagram batch is not the smallest whole-Iron and whole-fuel balance");
        if (alloy == IronProduct.STEEL && fuel == IronBath.Fuel.COAL) {
            check(
                iron == 100 && fuelItems == 1
                    && calcite.compareTo(new BigDecimal("0.25")) == 0
                    && metal.amount == 14400
                    && slag.amount == 160,
                "Coal-to-Steel diagram differs from the independent exact reference batch");
        }
        if (alloy == IronProduct.STEEL && fuel == IronBath.Fuel.COKE) {
            check(
                iron == 100 && fuelItems == 1
                    && calcite.compareTo(new BigDecimal("0.05")) == 0
                    && metal.amount == 14400
                    && slag.amount == 32,
                "Coke-to-Steel diagram differs from the independent exact reference batch");
        }
        return fuel;
    }

    private static int diagramItemCount(PositionedStack input) {
        check(input instanceof FixedPositionedStack, "Diagram item lacks the native quantity overlay");
        FixedPositionedStack fixed = (FixedPositionedStack) input;
        check(fixed.renderRealStackSize && fixed.realStackSize > 0, "Diagram hides its real item count");
        for (ItemStack alternative : fixed.items) {
            check(alternative.stackSize == fixed.realStackSize, "Diagram permutation changed the batch quantity");
        }
        return fixed.realStackSize;
    }

    @GameTest(template = "native_fixture", timeoutTicks = 1000)
    public static void nativeBlueprintRequiresShellCoilsAndDedicatedPorts(GameTestHelper helper) {
        ClientTest.scenario(helper)
            .server("validate the chimney and interchangeable controller platform ports", () -> {
                Fixture fixture = new Fixture(helper);
                check(
                    IronBathStructure.WIDTH == 13 && IronBathStructure.HEIGHT == 12 && IronBathStructure.DEPTH == 7,
                    "Native furnace dimensions differ from the 7 by 12 by 13 blueprint");
                ItemStack coil = ItemList.Casing_Coil_Cupronickel.get(1);
                Block coilBlock = Block.getBlockFromItem(coil.getItem());
                int physicalCoils = 0;
                for (int dx = -1; dx <= 11; dx++) for (int dy = -1; dy <= 10; dy++) for (int dz = -2; dz <= 4; dz++) {
                    if (fixture.world.getBlock(X + dx, Y + dy, Z + dz) == coilBlock
                        && fixture.world.getBlockMetadata(X + dx, Y + dy, Z + dz) == coil.getItemDamage())
                        physicalCoils++;
                }
                check(physicalCoils == EXPECTED_COIL_COUNT, "Native construction must place fifty Cupronickel coils");
                check(
                    IronBathStructure.COIL_POSITIONS.size() == EXPECTED_COIL_COUNT,
                    "Named coil positions do not cover the fifty physical coils");
                check(
                    fixture.state()
                        .capacityMb() == 512 * 144,
                    "Cupronickel must provide 512 ingots of capacity");

                fixture.setBlock(IronBathStructure.CRITICAL_CASING, Blocks.air, 0);
                check(!fixture.forms(), "A missing chimney casing was accepted");
                fixture.setBlock(IronBathStructure.CRITICAL_CASING, IronBathFurnace.casingBlock(), 0);
                check(fixture.forms(), "Restoring the chimney casing did not restore formation");

                Offset physicalCoil = IronBathStructure.COIL_POSITIONS.get(0);
                fixture.setBlock(physicalCoil, Blocks.air, 0);
                check(!fixture.forms(), "A missing physical coil was accepted");
                ItemStack mixedCoil = ItemList.Casing_Coil_Kanthal.get(1);
                fixture.setBlock(physicalCoil, Block.getBlockFromItem(mixedCoil.getItem()), mixedCoil.getItemDamage());
                check(!fixture.forms(), "Mixed physical coil tiers were accepted");
                fixture.setBlock(physicalCoil, coilBlock, coil.getItemDamage());
                check(fixture.forms(), "Restoring the physical coil did not restore formation");

                FakePlayer placer = FakePlayerFactory.getMinecraft(fixture.world);
                for (int height = 3; height <= 7; height++) {
                    Offset port = new Offset(9, height, -1);
                    fixture.place(placer, IronBathFurnace.tap(1), port);
                    check(fixture.forms(), "The tapping hatch was rejected at coil height " + height);
                }
                fixture.place(placer, ItemList.Hatch_Energy_HV.get(1), IronBathStructure.EXTRA_ENERGY);
                check(fixture.forms(), "A third ordinary energy hatch in an eligible base position was rejected");
                fixture.setBlock(IronBathStructure.EXTRA_ENERGY, IronBathFurnace.casingBlock(), 0);
                check(fixture.forms(), "Restoring two energy hatches did not restore formation");

                Offset rearPlatform = new Offset(0, 0, 2);
                for (ItemStack misplaced : new ItemStack[] { ItemList.Hatch_Energy_HV.get(1),
                    IronBathFurnace.tap(1) }) {
                    fixture.place(placer, misplaced, rearPlatform);
                    check(!fixture.forms(), "An energy hatch or tap was accepted on the controller platform");
                    fixture.setBlock(rearPlatform, GregTechAPI.sBlockCasings2, 0);
                    check(fixture.forms(), "Restoring the platform casing did not restore formation");
                }

                fixture.place(placer, ItemList.Hatch_Input_Bus_ME.get(1), IronBathStructure.INPUT_BUS);
                check(!fixture.forms(), "A transactional network input bus was accepted on the controller platform");
                fixture.place(placer, ItemList.Hatch_Input_Bus_HV.get(1), IronBathStructure.INPUT_BUS);
                check(fixture.forms(), "Restoring the buffered feed bus did not restore formation");

                Offset[] initialPorts = { IronBathStructure.INPUT_BUS, IronBathStructure.OXYGEN_INPUT,
                    IronBathStructure.MAINTENANCE };
                Offset[] flexiblePorts = { new Offset(-1, -1, 0), rearPlatform, new Offset(1, 0, 1) };
                ItemStack[] platformHatches = { ItemList.Hatch_Input_Bus_HV.get(1), ItemList.Hatch_Input_HV.get(1),
                    ItemList.Hatch_Maintenance.get(1) };
                for (Offset port : initialPorts) fixture.setBlock(port, GregTechAPI.sBlockCasings2, 0);
                for (int shift = 0; shift < flexiblePorts.length; shift++) {
                    for (int i = 0; i < flexiblePorts.length; i++) {
                        fixture.place(placer, platformHatches[i], flexiblePorts[(i + shift) % flexiblePorts.length]);
                    }
                    check(fixture.forms(), "Interchanging input and maintenance hatches prevented formation");
                }
                for (ItemStack hatch : platformHatches) {
                    fixture.place(placer, hatch, IronBathStructure.CRITICAL_CASING);
                    check(!fixture.forms(), "A controller platform hatch was accepted in the chimney");
                }
                fixture.setBlock(IronBathStructure.CRITICAL_CASING, IronBathFurnace.casingBlock(), 0);
                check(fixture.forms(), "Restoring the chimney casing did not restore formation");

                BaseMetaTileEntity sample = fixture.place(placer, IronBathFurnace.tap(1), IronBathStructure.SAMPLE_TAP);
                sample.setFrontFacing(ForgeDirection.NORTH);
                check(fixture.forms(), "The sample tap on the narrowing prevented formation");

                ItemStack eternal = ItemList.Casing_Coil_Eternal.get(1);
                for (Offset offset : IronBathStructure.COIL_POSITIONS)
                    fixture.setBlock(offset, Block.getBlockFromItem(eternal.getItem()), eternal.getItemDamage());
                check(fixture.forms(), "The physical Eternal coil furnace did not form");
                check(
                    fixture.state()
                        .capacityMb() == 603979776,
                    "Eternal coils must provide 4194304 ingots of capacity");
                for (Offset offset : IronBathStructure.COIL_POSITIONS)
                    fixture.setBlock(offset, coilBlock, coil.getItemDamage());
                check(fixture.forms(), "Restoring the Cupronickel coils did not restore formation");
                check(
                    fixture.state()
                        .capacityMb() == 512 * 144,
                    "Restoring Cupronickel did not restore base capacity");
            })
            .succeed();
    }

    @GameTest(template = "native_fixture", timeoutTicks = 1200)
    public static void blueprintStructureViews(GameTestHelper helper) {
        Fixture[] fixture = new Fixture[1];
        boolean[] hideGui = new boolean[1];
        int[] perspective = new int[1];
        ClientScenario scenario = prepareClientView(helper).defaultTimeoutTicks(100)
            .server("construct the native blueprint for exterior inspection", () -> fixture[0] = new Fixture(helper))
            .client("prepare the exterior camera", client -> {
                Minecraft mc = Minecraft.getMinecraft();
                hideGui[0] = mc.gameSettings.hideGUI;
                perspective[0] = mc.gameSettings.thirdPersonView;
                mc.gameSettings.hideGUI = true;
                mc.gameSettings.thirdPersonView = 0;
            })
            .afterTest(client -> {
                Minecraft mc = Minecraft.getMinecraft();
                mc.gameSettings.hideGUI = hideGui[0];
                mc.gameSettings.thirdPersonView = perspective[0];
            });
        view(scenario, fixture, "blueprint-north", X + 5.5, Y + 5, Z - 16, 0, 5);
        view(scenario, fixture, "blueprint-east", X + 26, Y + 5, Z + 1.5, 90, 5);
        view(scenario, fixture, "blueprint-south", X + 5.5, Y + 5, Z + 19, 180, 5);
        view(scenario, fixture, "blueprint-elevated", X + 22, Y + 17, Z - 15, 45, 30);
        scenario.succeed();
    }

    @GameTest(template = "native_fixture", timeoutTicks = 1000)
    public static void twoLvHatchesSupplyBasePower(GameTestHelper helper) {
        Fixture[] fixture = new Fixture[1];
        ClientTest.scenario(helper)
            .server("construct with two ordinary LV energy hatches", () -> fixture[0] = new Fixture(helper, true))
            .awaitServerAccelerated("two LV hatches supply 120 EU/t of heating", 1, () -> {
                fixture[0].refillPower();
                check(fixture[0].usedEnergy > 0, "LV hatches have not supplied furnace heat");
                check(fixture[0].machine.lEUt == -120, "Two LV hatches did not supply exactly 120 EU/t");
            })
            .succeed();
    }

    @GameTest(template = "native_fixture", timeoutTicks = 3000)
    public static void eightEnergyHatchesProvidePerfectAmperageParallel(GameTestHelper helper) {
        Offset[] ports = { IronBathStructure.ENERGY_A, IronBathStructure.ENERGY_B, IronBathStructure.EXTRA_ENERGY,
            new Offset(9, -1, -2), new Offset(9, -1, 4), new Offset(7, -1, 4), new Offset(10, -1, -1),
            new Offset(9, -1, -1), new Offset(8, -1, -1) };
        double[] reference = new double[1];
        ClientScenario scenario = ClientTest.scenario(helper);
        for (int count : new int[] { 2, 4, 8 }) {
            scenario.server("measure same-voltage heating with " + count + " energy hatches", () -> {
                Fixture fixture = new Fixture(helper, true);
                FakePlayer placer = FakePlayerFactory.getMinecraft(fixture.world);
                for (int port = 2; port < count; port++)
                    fixture.energy.add(fixture.place(placer, ItemList.Hatch_Energy_LV.get(1), ports[port]));
                check(fixture.forms(), "Furnace rejected " + count + " ordinary energy hatches");
                fixture.input
                    .setInventorySlotContents(0, GTOreDictUnificator.get(OrePrefixes.ingot, Materials.Iron, 16));
                fixture.input.markDirty();
                fixture.machine.onPostTick(fixture.controller, fixture.world.getTotalWorldTime());
                double heat = measureHeating(fixture);
                check(fixture.machine.lEUt == -60L * count, "Same-tier amperage did not scale paid power linearly");
                if (count == 2) reference[0] = heat;
                else check(
                    Math.abs(heat / reference[0] - count / 2.0) < 0.03,
                    "Same-tier amperage changed heat per paid EU");
                if (count == 8) {
                    fixture.place(placer, ItemList.Hatch_Energy_LV.get(1), ports[8]);
                    check(!fixture.forms(), "A ninth energy hatch was accepted");
                }
            });
        }
        scenario.succeed();
    }

    @GameTest(template = "native_fixture", timeoutTicks = 60000)
    public static void inputPowerAcceleratesColdAndMeltingBath(GameTestHelper helper) {
        Fixture[] fixture = new Fixture[1];
        double[][] heat = new double[3][2];
        ItemList[] hatches = { ItemList.Hatch_Energy_LV, ItemList.Hatch_Energy_MV, ItemList.Hatch_Energy_HV };
        long[] expectedDraw = { 120, 480, 1920 };
        ClientScenario scenario = ClientTest.scenario(helper)
            .defaultTimeoutTicks(20000);
        for (int tier = 0; tier < hatches.length; tier++) {
            int index = tier;
            scenario.server("construct identical Cupronickel furnace at power tier " + tier, () -> {
                fixture[0] = new Fixture(helper, hatches[index]);
                fixture[0].input
                    .setInventorySlotContents(0, GTOreDictUnificator.get(OrePrefixes.ingot, Materials.Iron, 16));
                fixture[0].input.markDirty();
            })
                .awaitServerAccelerated("native furnace draws its cold heating power", 1, () -> {
                    fixture[0].refillPower();
                    check(
                        fixture[0].machine.lEUt == -expectedDraw[index],
                        "Cold heating does not draw the native EBF overclock power");
                    check(
                        fixture[0].state()
                            .pendingMetalMb() == 16 * 144,
                        "Cold input charge is not admitted");
                })
                .server(
                    "measure cold heating with the same elapsed ticks",
                    () -> { heat[index][0] = measureHeating(fixture[0]); })
                .awaitServerAccelerated("reach the charge melting phase through paid heating", 100, () -> {
                    fixture[0].refillPower();
                    check(
                        fixture[0].state()
                            .chargeGroups(0, 1)
                            .get(0)
                            .progress() >= 0.5,
                        "Charge has not entered its latent melting phase");
                })
                .server("measure melting with the same elapsed ticks", () -> {
                    check(
                        fixture[0].state()
                            .pendingMetalMb() > 0,
                        "No charge remains to melt");
                    heat[index][1] = measureHeating(fixture[0]);
                    check(
                        fixture[0].machine.lEUt == -expectedDraw[index],
                        "Melting does not draw the native EBF overclock power");
                    if (index > 0) {
                        check(
                            heat[index][0] > heat[index - 1][0] * 1.8,
                            "Higher input power did not accelerate cold heating");
                        check(
                            heat[index][1] > heat[index - 1][1] * 1.8,
                            "Higher input power did not accelerate melting");
                    }
                });
        }
        scenario.succeed();
    }

    @GameTest(template = "native_fixture", timeoutTicks = 3000)
    public static void coilsProvideNativeHeatDiscountAndPerfectOverclock(GameTestHelper helper) {
        Fixture[] fixture = new Fixture[1];
        double[] heat = new double[3];
        double[] bathHeat = new double[3];
        ItemList[] coils = { ItemList.Casing_Coil_Cupronickel, ItemList.Casing_Coil_Kanthal,
            ItemList.Casing_Coil_Nichrome };
        long[] expectedDraw = { 480, 480, 480 };
        ClientScenario scenario = ClientTest.scenario(helper);
        for (int coil = 0; coil < coils.length; coil++) {
            int index = coil;
            scenario.server("install physical coils for native heat benefit " + coil, () -> {
                fixture[0] = new Fixture(helper, ItemList.Hatch_Energy_MV);
                ItemStack source = ItemList.Casing_Coil_Cupronickel.get(1);
                ItemStack replacement = coils[index].get(1);
                Block replacementBlock = Block.getBlockFromItem(replacement.getItem());
                int replaced = 0;
                for (Offset offset : IronBathStructure.COIL_POSITIONS) {
                    check(
                        fixture[0].world.getBlock(X + offset.x(), Y + offset.y(), Z + offset.z())
                            == Block.getBlockFromItem(source.getItem())
                            && fixture[0].world.getBlockMetadata(X + offset.x(), Y + offset.y(), Z + offset.z())
                                == source.getItemDamage(),
                        "A physical blueprint coil is missing at " + offset);
                    fixture[0].setBlock(offset, replacementBlock, replacement.getItemDamage());
                    replaced++;
                }
                check(replaced == EXPECTED_COIL_COUNT, "Fixture did not replace all fifty physical coils");
                check(
                    fixture[0].machine.checkStructure(true, fixture[0].controller),
                    "Higher coil furnace did not form");
                check(
                    fixture[0].state()
                        .capacityMb() == (512 * 144 << index),
                    "Physical coil tier was not recognized");
                check(
                    fixture[0].state()
                        .maximumTemperature() == 2200 + 900 * index,
                    "Physical coils did not select their automatic bath ceiling");
                bathHeat[index] = measureHeating(fixture[0], 1);
                if (index > 0) check(
                    Math.abs(bathHeat[index] - bathHeat[0]) < bathHeat[0] * 0.02,
                    "Coil grade changed direct bath heating at the same electrical supply");
                NBTTagCompound cold = new NBTTagCompound();
                fixture[0].machine.saveNBTData(cold);
                cold.getCompoundTag("IronBath")
                    .setDouble("Heat", 0);
                fixture[0].machine.loadNBTData(cold);
                fixture[0].input
                    .setInventorySlotContents(0, GTOreDictUnificator.get(OrePrefixes.ingot, Materials.Iron, 16));
                fixture[0].input.markDirty();
            })
                .awaitServerAccelerated("native power supplies charge melting and bath recovery", 1, () -> {
                    fixture[0].refillPower();
                    check(
                        fixture[0].machine.lEUt == -expectedDraw[index],
                        "Charge melting and bath heating exceeded the shared working EU allowance");
                    check(
                        fixture[0].state()
                            .pendingMetalMb() == 16 * 144,
                        "Equal coil comparison charge was not admitted");
                })
                .server("measure heat delivered by the physical coil upgrade", () -> {
                    heat[index] = measureChargeHeating(fixture[0]);
                    check(
                        fixture[0].machine.lEUt == -expectedDraw[index],
                        "Coil comparison lost its native power draw");
                    if (index == 1) {
                        check(
                            Math.abs(heat[index] - heat[0]) < heat[0] * 0.02,
                            "Kanthal changed heating speed instead of only discounting power");
                    }
                    if (index == 2) {
                        check(heat[index] > heat[0] * 1.8, "Nichrome did not provide its first perfect overclock");
                    }
                });
        }
        scenario.succeed();
    }

    @GameTest(template = "native_fixture", timeoutTicks = 10000)
    public static void amperageParallelUsesVolumeLimitedMelting(GameTestHelper helper) {
        Fixture[] fixture = new Fixture[1];
        helper.onEachTick(
            "meter eight-hatch native remelting",
            () -> { if (fixture[0] != null) fixture[0].observePaidRemeltingTick(); });
        ClientTest.scenario(helper)
            .defaultTimeoutTicks(2500)
            .server("prepare eight UHV hatches and Eternal coils", () -> {
                Fixture f = fixture[0] = new Fixture(helper, true);
                f.preparePaidRemelting(9);
                FakePlayer placer = FakePlayerFactory.getMinecraft(f.world);
                Offset[] extra = { IronBathStructure.EXTRA_ENERGY, new Offset(9, -1, -2), new Offset(9, -1, 4),
                    new Offset(7, -1, 4), new Offset(10, -1, -1), new Offset(9, -1, -1) };
                for (Offset port : extra) {
                    BaseMetaTileEntity tile = f.place(placer, ItemList.DebugEnergyHatch.get(1), port);
                    gregtech.api.metatileentity.implementations.MTEHatchEnergyDebug hatch = (gregtech.api.metatileentity.implementations.MTEHatchEnergyDebug) tile
                        .getMetaTileEntity();
                    hatch.setVoltageTier(9);
                    hatch.setAmperage(2);
                    hatch.setRefillInterval(1);
                    tile.setStoredEU(Long.MAX_VALUE);
                    f.energy.add(tile);
                }
                check(f.forms(), "Eight UHV energy hatches did not form");
                BaseMetaTileEntity outlet = f.place(placer, IronBathFurnace.tap(9), IronBathStructure.METAL_TAP);
                outlet.setFrontFacing(ForgeDirection.NORTH);
                f.meterTap = (MTEIronBathTap) outlet.getMetaTileEntity();
                f.meterTap.setAmountPerPulse(f.meterTap.getPulseLimit());
                f.meterTap.setRedstoneMode(2);
                check(f.forms(), "High-throughput tapping hatch did not form");
                f.meterNominalChargesPerTick *= 4;
            })
            .awaitServerAccelerated(
                "measure sustained output with volume-limited feed",
                100,
                () -> { check(fixture[0].meterTicks >= 1000, "Eight-hatch measurement is incomplete"); })
            .server("verify paid heat and queued volume", () -> {
                Fixture f = fixture[0];
                f.meterRunning = false;
                f.verifyPaidRemelting(helper, 9, efficientNativeSteelBaseline(helper, 9, 13501));
                check(
                    f.state()
                        .metalMb()
                        + f.state()
                            .pendingMetalMb()
                        <= f.state()
                            .metalCapacityMb(),
                    "Queued production exceeded the tank's metal allowance");
            })
            .succeed();
    }

    @GameTest(template = "native_fixture", timeoutTicks = 80000)
    public static void paidRemeltingAcrossNativePowerAndCoils(GameTestHelper helper) {
        Fixture[] fixture = new Fixture[1];
        int[] tiers = { 1, 2, 3, 8, 9 };
        helper.onEachTick(
            "meter paid remelting and collect native outlet fluid",
            () -> { if (fixture[0] != null) fixture[0].observePaidRemeltingTick(); });
        ClientScenario scenario = ClientTest.scenario(helper)
            .defaultTimeoutTicks(25000);
        for (int tier : tiers) {
            scenario.server("prepare continuously supplied remelting at voltage tier " + tier, () -> {
                fixture[0] = new Fixture(helper, true);
                fixture[0].preparePaidRemelting(tier);
            })
                .awaitServerAccelerated(
                    "measure sustained native remelting at voltage tier " + tier,
                    100,
                    () -> {
                        check(
                            fixture[0].meterTicks >= (tier < 4 ? 20000 : 2000),
                            "Paid remelting window is incomplete");
                    })
                .server("verify paid output and thermal ledger at voltage tier " + tier, () -> {
                    Fixture f = fixture[0];
                    f.meterRunning = false;
                    double baseline = efficientNativeSteelBaseline(helper, tier, tier < 4 ? 1801 : 13501);
                    f.verifyPaidRemelting(helper, tier, baseline);
                });
        }
        scenario.succeed();
    }

    private static double efficientNativeSteelBaseline(GameTestHelper helper, int maximumTier, int coilHeat) {
        ItemStack steel = GTOreDictUnificator.get(OrePrefixes.ingot, Materials.Steel, 1);
        ItemStack castIron = GTOreDictUnificator.get(OrePrefixes.dust, Materials.CastIron, 1);
        GTRecipe nativeSteel = RecipeMaps.blastFurnaceRecipes.getAllRecipes()
            .stream()
            .filter(recipe -> recipe.mEUt == 120 && recipe.mDuration == 100 && recipe.mSpecialValue == 1000)
            .filter(
                recipe -> java.util.Arrays.stream(recipe.mInputs)
                    .anyMatch(stack -> stack != null && stack.isItemEqual(castIron)))
            .filter(
                recipe -> java.util.Arrays.stream(recipe.mOutputs)
                    .anyMatch(stack -> stack != null && stack.isItemEqual(steel) && stack.stackSize == 1))
            .findFirst()
            .orElse(null);
        // Extra Nerf can remove this baseline recipe before the final live map is inspected.
        helper.recordDiagnostic(
            "native Cast Iron to Steel 120 EU/t, 100 ticks, 1000 K: "
                + (nativeSteel == null ? "absent from final map, using pinned NHCore 2.9.99 source signature"
                    : "live map"));
        GTRecipe extraction = RecipeMaps.fluidExtractionRecipes.getAllRecipes()
            .stream()
            .filter(recipe -> recipe.mEUt == 60 && recipe.mDuration == 24)
            .filter(
                recipe -> recipe.mInputs.length == 1 && recipe.mInputs[0] != null
                    && recipe.mInputs[0].isItemEqual(steel)
                    && recipe.mInputs[0].stackSize == 1)
            .filter(
                recipe -> recipe.mFluidOutputs.length == 1 && recipe.mFluidOutputs[0] != null
                    && recipe.mFluidOutputs[0].isFluidEqual(Materials.Steel.getMolten(144))
                    && recipe.mFluidOutputs[0].amount == 144)
            .findFirst()
            .orElseThrow(() -> new AssertionError("Native Steel extraction 60 EU/t x 24 ticks is absent"));
        gregtech.api.util.OverclockCalculator extractor = new gregtech.api.util.OverclockCalculator()
            .setRecipeEUt(extraction.mEUt)
            .setDuration(extraction.mDuration)
            .setEUt(128)
            .calculate();
        double extractionEU = extractor.getConsumption() * extractor.getDuration();
        double cheapest = Double.POSITIVE_INFINITY;
        for (int tier = 1; tier <= maximumTier; tier++) {
            for (int amps : new int[] { 1, 4 }) {
                long voltage = gregtech.api.enums.GTValues.V[tier];
                gregtech.api.util.OverclockCalculator calculator = new gregtech.api.util.OverclockCalculator()
                    .setRecipeEUt(nativeSteel == null ? 120 : nativeSteel.mEUt)
                    .setDuration(nativeSteel == null ? 100 : nativeSteel.mDuration)
                    .setRecipeHeat(nativeSteel == null ? 1000 : nativeSteel.mSpecialValue)
                    .setMachineHeat(
                        coilHeat + 100 * (gregtech.api.util.GTUtility.getTier(voltage * (amps == 1 ? 1 : 2)) - 2))
                    .setEUt(voltage)
                    .setAmperage(amps)
                    .setAmperageOC(true)
                    .setHeatOC(true)
                    .setHeatDiscount(true);
                if (voltage * amps < Math.ceil(120 * calculator.calculateHeatDiscountMultiplier())) continue;
                double parallels = calculator.calculateMultiplierUnderOneTick();
                calculator.calculate();
                double cost = (double) calculator.getConsumption() * calculator.getDuration() / parallels
                    + extractionEU;
                cheapest = Math.min(cheapest, cost);
                helper.recordDiagnostic(
                    "native baseline tier=" + tier
                        + ", amps="
                        + amps
                        + ", EU/t="
                        + calculator.getConsumption()
                        + ", ticks="
                        + calculator.getDuration()
                        + ", sub-tick recipes="
                        + parallels
                        + ", molten EU/ingot="
                        + cost);
            }
        }
        check(Double.isFinite(cheapest), "No accessible native EBF baseline was calculated");
        return cheapest;
    }

    private static double measureHeating(Fixture fixture) {
        return measureHeating(fixture, 100);
    }

    private static double measureHeating(Fixture fixture, int ticks) {
        double before = fixture.state()
            .heatDemand(IronBath.MAX_TEMPERATURE);
        long tick = fixture.world.getTotalWorldTime();
        for (int elapsed = 0; elapsed < ticks; elapsed++) {
            fixture.refillPower();
            fixture.machine.onPostTick(fixture.controller, tick + elapsed);
        }
        return before - fixture.state()
            .heatDemand(IronBath.MAX_TEMPERATURE);
    }

    private static double measureChargeHeating(Fixture fixture) {
        double before = fixture.storedChargeHeat();
        long tick = fixture.world.getTotalWorldTime();
        for (int elapsed = 0; elapsed < 100; elapsed++) {
            fixture.refillPower();
            fixture.machine.onPostTick(fixture.controller, tick + elapsed);
        }
        return fixture.storedChargeHeat() - before;
    }

    @GameTest(template = "native_fixture", timeoutTicks = 1000)
    public static void measurementHatchReadsLiveBathThroughOpenComputers(GameTestHelper helper) {
        Offset position = new Offset(9, 5, -1);
        Fixture[] fixture = new Fixture[1];
        Component[] component = new Component[1];
        ClientTest.scenario(helper)
            .server("install the optional LuV measurement hatch and discover its OC driver", () -> {
                fixture[0] = new Fixture(helper);
                FakePlayer placer = FakePlayerFactory.getMinecraft(fixture[0].world);
                for (Offset candidate : new Offset[] { IronBathStructure.PREVIEW_MEASUREMENT,
                    IronBathStructure.EXTRA_ENERGY }) {
                    fixture[0].place(placer, IronBathFurnace.measurementHatch(), candidate);
                    check(fixture[0].forms(), "The measurement hatch was rejected at " + candidate);
                    fixture[0].setBlock(candidate, IronBathFurnace.casingBlock(), 0);
                }
                BaseMetaTileEntity port = fixture[0].place(placer, IronBathFurnace.measurementHatch(), position);
                port.setFrontFacing(ForgeDirection.NORTH);
                check(
                    port.getMetaTileEntity() instanceof MTEIronBathMeasurement,
                    "Measurement hatch registration is missing");
                MTEIronBathMeasurement hatch = (MTEIronBathMeasurement) port.getMetaTileEntity();
                check(
                    hatch.getTierForStructure() == 6 && hatch.getSizeInventory() == 0,
                    "Measurement hatch is not a zero-slot LuV port");
                SidedBlock driver = Driver.driverFor(
                    fixture[0].world,
                    X + position.x(),
                    Y + position.y(),
                    Z + position.z(),
                    ForgeDirection.NORTH);
                check(driver != null, "OpenComputers did not register the measurement driver");
                ManagedEnvironment environment = driver.createEnvironment(
                    fixture[0].world,
                    X + position.x(),
                    Y + position.y(),
                    Z + position.z(),
                    ForgeDirection.NORTH);
                check(
                    environment != null && environment.node() instanceof Component,
                    "OC did not create a measurement component");
                component[0] = (Component) environment.node();
                helper.afterTest(() -> component[0].remove());
                Network.joinNewNetwork(component[0]);
                check(
                    IronBathMeasurementDriver.COMPONENT_NAME.equals(component[0].name()),
                    "OC selected the wrong measurement component name");
                check(
                    component[0].methods()
                        .contains("read"),
                    "Measurement callback is missing from OC dispatch");
                check(
                    !component[0].annotation("read")
                        .direct(),
                    "World measurements must run on the server thread");
                check(measurementRead(component[0])[0] == null, "Unassociated hatch exposed a bath");
                check(
                    fixture[0].machine.checkStructure(true, fixture[0].controller),
                    "The measurement hatch was rejected in a tapping position on the chimney");
                NBTTagCompound tag = new NBTTagCompound();
                fixture[0].machine.saveNBTData(tag);
                NBTTagCompound bathTag = tag.getCompoundTag("IronBath");
                bathTag.setInteger("Metal", 384 * 144);
                bathTag.setInteger("Slag", 320);
                bathTag.setDouble("Carbon", 321.125);
                bathTag.setInteger("Impurities", 2);
                bathTag.setInteger("Calcite", 11);
                bathTag.setDouble("Heat", 0);
                IronBath seed = new IronBath(0);
                seed.load(bathTag);
                bathTag.setDouble("Heat", seed.heatDemand(IronBath.WORKING_TEMPERATURE));
                fixture[0].machine.loadNBTData(tag);
                fixture[0].input
                    .setInventorySlotContents(0, GTOreDictUnificator.get(OrePrefixes.ingot, Materials.Iron, 4));
                fixture[0].input
                    .setInventorySlotContents(1, new ItemStack(GameRegistry.findItem("Railcraft", "fuel.coke"), 1));
                fixture[0].input.markDirty();
            })
            .awaitServerAccelerated("native intake creates measurable melting work", 1, () -> {
                fixture[0].refillPower();
                check(
                    fixture[0].state()
                        .pendingMetalMb() == 4 * 144,
                    "Native charge has not entered the heating queue");
            })
            .server("read exact live values and reject stale associations", () -> {
                IronBath bath = fixture[0].state();
                Map<?, ?> values = measurementTable(component[0]);
                check(
                    ((Number) values.get("metalMb")).intValue() == bath.metalMb(),
                    "Metal measurement differs from the owner");
                check(
                    ((Number) values.get("liquidMetalMb")).intValue() == bath.liquidMetalMb(),
                    "Liquid metal measurement differs");
                check(((Number) values.get("slagMb")).intValue() == bath.slagMb(), "Slag measurement differs");
                check(
                    ((Number) values.get("liquidSlagMb")).intValue() == bath.liquidSlagMb(),
                    "Liquid slag measurement differs");
                check(
                    ((Number) values.get("carbonUnits")).doubleValue() == bath.carbon(),
                    "Fractional carbon was rounded or stale");
                check(
                    ((Number) values.get("impurityUnits")).intValue() == bath.impurities(),
                    "Impurity measurement differs");
                check(
                    ((Number) values.get("calciteUnits")).intValue() == bath.calciteCapacity(),
                    "Calcite measurement differs");
                check(
                    ((Number) values.get("temperatureK")).doubleValue() == bath.temperature(),
                    "Temperature measurement differs");
                check(
                    ((Number) values.get("capacityMb")).intValue() == bath.capacityMb(),
                    "Capacity measurement differs");
                check(
                    ((Number) values.get("pendingMetalMb")).intValue() == 4 * 144,
                    "Pending metal measurement differs");
                check(
                    ((Number) values.get("meltingItems")).intValue() == 4,
                    "The measurement included carbon in the metal heating queue");
                check(
                    Boolean.TRUE.equals(values.get("melting"))
                        && ((Object[]) values.get("chargeGroups")).length == bath.chargeGroupCount(),
                    "Melting state is missing");
                Object[] groups = (Object[]) values.get("chargeGroups");
                Map<?, ?> group = scala.collection.JavaConverters
                    .mutableMapAsJavaMapConverter((scala.collection.mutable.Map<?, ?>) groups[0])
                    .asJava();
                check(
                    ((Number) group.get("count")).intValue() == 4
                        && ((Number) group.get("progress")).doubleValue() == bath.chargeGroups(0, 1)
                            .get(0)
                            .progress(),
                    "Grouped measurement differs from the queued item count or paid progress");
                values.clear();
                check(
                    ((Number) measurementTable(component[0]).get("metalMb")).intValue() == bath.metalMb(),
                    "Mutating a returned table changed furnace measurements");
                Offset shell = IronBathStructure.CRITICAL_CASING;
                check(
                    fixture[0].world.getBlock(X + shell.x(), Y + shell.y(), Z + shell.z())
                        == IronBathFurnace.casingBlock(),
                    "The disconnection test must remove a chimney casing");
                fixture[0].setBlock(shell, Blocks.air, 0);
                // Reads use the controller's maintained formation state, so run the structure check it would run.
                check(
                    !fixture[0].machine.checkStructure(true, fixture[0].controller),
                    "Removing a chimney casing did not break formation");
                check(measurementRead(component[0])[0] == null, "Retained OC environment exposed an unformed furnace");
                fixture[0].setBlock(shell, IronBathFurnace.casingBlock(), 0);
                check(
                    fixture[0].machine.checkStructure(true, fixture[0].controller),
                    "Restoring the chimney casing did not reform the furnace");
                check(
                    measurementTable(component[0]).containsKey("metalMb"),
                    "Reformed furnace did not restore live measurements");
                fixture[0].place(
                    FakePlayerFactory.getMinecraft(fixture[0].world),
                    IronBathFurnace.measurementHatch(),
                    IronBathStructure.PREVIEW_MEASUREMENT);
                check(!fixture[0].forms(), "A second measurement hatch was accepted");
                fixture[0].setBlock(IronBathStructure.PREVIEW_MEASUREMENT, IronBathFurnace.casingBlock(), 0);
                check(fixture[0].forms(), "Removing the second measurement hatch did not restore formation");

                fixture[0].setBlock(position, IronBathFurnace.casingBlock(), 0);
                check(
                    measurementRead(component[0])[0] == null,
                    "Removed hatch retained access through its old OC environment");
                check(
                    fixture[0].machine.checkStructure(true, fixture[0].controller),
                    "Measurement hatch became mandatory");

                Offset steel = new Offset(1, 0, 0);
                fixture[0]
                    .place(FakePlayerFactory.getMinecraft(fixture[0].world), IronBathFurnace.measurementHatch(), steel);
                check(!fixture[0].forms(), "The measurement hatch was accepted on the steel platform");
                fixture[0].setBlock(steel, GregTechAPI.sBlockCasings2, 0);
                check(fixture[0].forms(), "Restoring the steel casing did not restore formation");
            })
            .succeed();
    }

    private static Map<?, ?> measurementTable(Component component) {
        Object[] response = measurementRead(component);
        check(
            response.length == 1 && response[0] instanceof scala.collection.mutable.Map,
            "OC measurement response type=" + (response.length == 0 || response[0] == null ? "nil"
                : response[0].getClass()
                    .getName())
                + ", values="
                + java.util.Arrays.deepToString(response));
        return scala.collection.JavaConverters
            .mutableMapAsJavaMapConverter((scala.collection.mutable.Map<?, ?>) response[0])
            .asJava();
    }

    private static Object[] measurementRead(Component component) {
        try {
            return component.invoke("read", null);
        } catch (Exception failure) {
            throw new AssertionError("Native OpenComputers measurement callback failed", failure);
        }
    }

    @GameTest(template = "native_fixture", timeoutTicks = 1000)
    public static void tappingBandPrioritizesSlagThenSwitchesToMetal(GameTestHelper helper) {
        Fixture[] fixture = new Fixture[1];
        MTEIronBathTap[] outlet = new MTEIronBathTap[1];
        Offset port = new Offset(9, 5, -1);
        ClientTest.scenario(helper)
            .defaultTimeoutTicks(200)
            .server("form a bath with a mixed middle tapping band", () -> {
                Fixture f = fixture[0] = new Fixture(helper);
                FakePlayer placer = FakePlayerFactory.get(f.world, new GameProfile(UUID.randomUUID(), "BathBandTap"));
                BaseMetaTileEntity tile = f.place(placer, IronBathFurnace.tap(1), port);
                outlet[0] = (MTEIronBathTap) tile.getMetaTileEntity();
                check(f.forms(), "Middle-band tap did not form with the furnace");
                NBTTagCompound controller = new NBTTagCompound();
                f.machine.saveNBTData(controller);
                NBTTagCompound bath = controller.getCompoundTag("IronBath");
                bath.setInteger("Metal", 30000);
                bath.setInteger("Slag", 1);
                bath.setDouble("Carbon", 0);
                IronBath seeded = new IronBath(0);
                seeded.load(bath);
                bath.setDouble("Heat", seeded.heatDemand(IronBath.WORKING_TEMPERATURE));
                f.machine.loadNBTData(controller);
                outlet[0].setAmountPerPulse(144);
                outlet[0].setRedstoneMode(2);
            })
            .awaitServerAccelerated("the middle band chooses its one mB of slag", 1, () -> {
                fixture[0].refillPower();
                check(
                    fluid(
                        outlet[0],
                        IronBathMaterials.slag()
                            .getMolten(1))
                        == 1,
                    "Mixed band did not prioritize its one mB of slag");
                check(
                    fixture[0].state()
                        .metalMb() == 30000,
                    "A slag discharge also consumed metal");
            })
            .server("clear the slag and request a fresh discharge", () -> {
                outlet[0].setRedstoneMode(3);
                FluidStack slag = outlet[0].drain(Integer.MAX_VALUE, true);
                check(slag != null && slag.amount == 1, "Slag discharge changed its amount");
                outlet[0].setRedstoneMode(0);
                fixture[0].setBlock(new Offset(port.x(), port.y(), port.z() - 1), Blocks.redstone_block, 0);
            })
            .awaitServerAccelerated("the next discharge chooses the remaining metal", 1, () -> {
                fixture[0].refillPower();
                check(
                    fluid(outlet[0], Materials.Iron.getMolten(1)) == 15,
                    "The emptied band tap did not switch to Iron on the next discharge");
                check(
                    fixture[0].state()
                        .metalMb() == 30000 - 15,
                    "Metal discharge did not remove exactly its requested amount");
                check(
                    fixture[0].state()
                        .slagMb() == 0,
                    "The single slag sample was not fully removed");
            })
            .succeed();
    }

    @GameTest(template = "native_fixture", timeoutTicks = 1000)
    public static void heldProbeSignalDoesNotFireAgainAfterTapReload(GameTestHelper helper) {
        Fixture[] fixture = new Fixture[1];
        MTEIronBathTap[] restoredTap = new MTEIronBathTap[1];
        int metal = 384 * 144;
        ClientScenario scenario = ClientTest.scenario(helper)
            .defaultTimeoutTicks(200)
            .server("form a hot bath and request one rising-edge sample", () -> {
                fixture[0] = new Fixture(helper);
                NBTTagCompound controllerState = new NBTTagCompound();
                fixture[0].machine.saveNBTData(controllerState);
                NBTTagCompound bathState = controllerState.getCompoundTag("IronBath");
                bathState.setInteger("Metal", metal);
                bathState.setDouble("Carbon", 384);
                IronBath seeded = new IronBath(0);
                seeded.load(bathState);
                bathState.setDouble("Heat", seeded.heatDemand(IronBath.WORKING_TEMPERATURE));
                fixture[0].machine.loadNBTData(controllerState);
                check(
                    fixture[0].state()
                        .liquidMetalMb() == metal,
                    "Reload fixture does not contain a liquid bath");
                fixture[0].metalTap.setAmountPerPulse(1);
                fixture[0].metalTap.setRedstoneMode(0);
                fixture[0].powerMetalTap(true);
            })
            .awaitServerAccelerated("observe exactly one extracted sample", 1, () -> {
                fixture[0].refillPower();
                check(
                    fluid(fixture[0].metalTap, Materials.Steel.getMolten(1)) == 1,
                    "First rising edge did not extract one mB");
                check(
                    fixture[0].state()
                        .metalMb() == metal - 1,
                    "First sample did not debit exactly one mB");
            })
            .server("reload a fresh native tap tile while redstone stays high", () -> {
                BaseMetaTileEntity previous = (BaseMetaTileEntity) fixture[0].metalTap.getBaseMetaTileEntity();
                NBTTagCompound saved = new NBTTagCompound();
                previous.writeToNBT(saved);
                previous.getMetaTileEntity()
                    .onUnload();
                fixture[0].world.removeTileEntity(previous.xCoord, previous.yCoord, previous.zCoord);
                BaseMetaTileEntity restored = new BaseMetaTileEntity();
                restored.readFromNBT(saved);
                fixture[0].world.setTileEntity(previous.xCoord, previous.yCoord, previous.zCoord, restored);
                check(restored.getMetaTileEntity() instanceof MTEIronBathTap, "Reload did not reconstruct a bath tap");
                restoredTap[0] = (MTEIronBathTap) restored.getMetaTileEntity();
                check(restoredTap[0] != fixture[0].metalTap, "Reload reused the original tap instance");
                check(
                    fluid(restoredTap[0], Materials.Steel.getMolten(1)) == 1,
                    "Reload did not retain the first sample");
                restoredTap[0].drain(1, true);
                check(
                    fixture[0].machine.checkStructure(true, fixture[0].controller),
                    "Reloaded tap did not rejoin the furnace");
            });
        scenario.serverSequence()
            .thenIdle(10);
        scenario.server("held high signal must not become another rising edge", () -> {
            check(restoredTap[0].getFluidAmount() == 0, "Reloaded HIGH signal extracted a duplicate sample");
            check(
                fixture[0].state()
                    .metalMb() == metal - 1,
                "Reloaded HIGH signal removed more metal");
            fixture[0].powerMetalTap(false);
        });
        scenario.serverSequence()
            .thenIdle(5);
        scenario.server("a fresh low-to-high transition requests the next sample", () -> {
            check(restoredTap[0].getFluidAmount() == 0, "LOW signal extracted metal");
            fixture[0].powerMetalTap(true);
        })
            .awaitServerAccelerated("observe the second sample only after the new edge", 1, () -> {
                fixture[0].refillPower();
                check(
                    fluid(restoredTap[0], Materials.Steel.getMolten(1)) == 1,
                    "Fresh rising edge did not extract the next one mB");
                check(
                    fixture[0].state()
                        .metalMb() == metal - 2,
                    "Second sample did not debit exactly one mB");
            })
            .succeed();
    }

    @GameTest(template = "native_fixture", timeoutTicks = 180000)
    public static void feedbackSwitches(GameTestHelper helper) {
        Fixture[] fixture = new Fixture[1];
        Feedback[] feedback = new Feedback[1];
        ClientScenario scenario = ClientTest.scenario(helper)
            .defaultTimeoutTicks(30000)
            .server("prepare one furnace for output-feedback switching", () -> {
                fixture[0] = new Fixture(helper);
                fixture[0].loadWorkingCharge();
                fixture[0].metalTap.setAmountPerPulse(1);
                feedback[0] = new Feedback(fixture[0]);
            })
            .withinTicks(COLD_CHARGE_TIMEOUT_TICKS)
            .awaitServerAccelerated("heat the switching furnace from cold", 100, () -> {
                fixture[0].refillPower();
                check(
                    fixture[0].state()
                        .liquidMetalMb() == WORKING_CHARGE_INGOTS * 144,
                    "Switching charge is still heating");
            });
        // Scripted control uses extracted samples, inventory feeds and periodic redstone pulses.
        // The targets cover every directed pair among Wrought, Steel, Cast and Pig Iron.
        Materials[] targets = { IronBathMaterials.wroughtIron(), Materials.Steel, IronBathMaterials.wroughtIron(),
            Materials.CastIron, IronBathMaterials.wroughtIron(), Materials.PigIron, Materials.Steel, Materials.CastIron,
            Materials.Steel, Materials.PigIron, Materials.CastIron, Materials.PigIron,
            IronBathMaterials.wroughtIron() };
        for (Materials target : targets) scenario
            .awaitServerAccelerated("feedback reaches " + target.mName, 100, () -> feedback[0].awaitProduct(target));
        scenario.server("all feedback transitions used real powered processing", () -> {
            check(feedback[0].reached == targets.length, "Missing material feedback transition");
            check(fixture[0].usedEnergy > 0, "Feedback furnace did not consume energy");
        })
            .succeed();
    }

    @GameTest(template = "native_fixture", timeoutTicks = 160000)
    public static void productionAndViews(GameTestHelper helper) {
        Fixture[] fixture = new Fixture[1];
        boolean[] hideGui = new boolean[1];
        int[] perspective = new int[1];
        int[] guiScale = new int[1];
        ClientScenario scenario = ClientTest.scenario(helper)
            .defaultTimeoutTicks(30000)
            .server("construct native furnace", () -> {
                verifyRegistrations();
                fixture[0] = new Fixture(helper);
            })
            .client("save rendering options", client -> {
                Minecraft mc = Minecraft.getMinecraft();
                hideGui[0] = mc.gameSettings.hideGUI;
                perspective[0] = mc.gameSettings.thirdPersonView;
                guiScale[0] = mc.gameSettings.guiScale;
                mc.gameSettings.hideGUI = true;
                mc.gameSettings.thirdPersonView = 0;
                mc.gameSettings.guiScale = 2;
            })
            .afterTest(client -> {
                Minecraft mc = Minecraft.getMinecraft();
                mc.gameSettings.hideGUI = hideGui[0];
                mc.gameSettings.thirdPersonView = perspective[0];
                mc.gameSettings.guiScale = guiScale[0];
            })
            .awaitServerAccelerated("warm the empty vessel below visible incandescence", 1, () -> {
                fixture[0].refillPower();
                double kelvin = fixture[0].state()
                    .temperature();
                check(kelvin >= 550 && kelvin < 800, "Waiting for a visibly filled cold temperature gauge");
            })
            .server("load cold ingots and dust into the warmed vessel", () -> fixture[0].loadWorkingCharge())
            .awaitServerAccelerated("all fitting metal enters the heating queue", 1, () -> {
                Fixture f = fixture[0];
                f.refillPower();
                IronBath observed = f.state();
                check(
                    observed.pendingMetalMb() == WORKING_CHARGE_INGOTS * 144,
                    "The full fitting charge was not reserved in the tank");
                for (int slot = 0; slot < f.input.getSizeInventory(); slot++) {
                    ItemStack stack = f.input.getStackInSlot(slot);
                    check(stack == null, "Fitting metal was left in the input bus");
                }
                int count = 0;
                for (IronBath.ChargeGroup group : observed.chargeGroups(0, observed.chargeGroupCount())) {
                    check(
                        group.stack() != null && group.stack().stackSize == 1,
                        "A melting group lost its representative source item");
                    count += group.count();
                }
                check(count == WORKING_CHARGE_INGOTS, "The melting groups lost queued items");
            })
            .withinTicks(600)
            .awaitClient("the cold controller reaches the client before opening its GUI", client -> {
                Minecraft mc = Minecraft.getMinecraft();
                check(
                    mc.theWorld != null && mc.theWorld.getTileEntity(X, Y, Z) instanceof BaseMetaTileEntity tile
                        && tile.getMetaTileEntity() instanceof MTEIronBathFurnace,
                    "The controller tile has not synchronized");
            })
            .server("open the cold controller", () -> {
                player().setPositionAndUpdate(X + 0.5, Y + 1, Z - 1.5);
                check(
                    fixture[0].machine.onRightclick(fixture[0].controller, player()),
                    "Controller rejected cold GUI opening");
            })
            .awaitScreen(GuiContainerWrapper.class)
            .click(blankScreenCorner())
            .capture("controller-cold")
            .server("close the cold controller", () -> player().closeScreen())
            .awaitClient(
                "cold controller screen closed",
                client -> check(Minecraft.getMinecraft().currentScreen == null, "Cold controller screen is still open"))
            .awaitServerAccelerated("cold charges show partial heating progress", 1, () -> {
                fixture[0].refillPower();
                double progress = fixture[0].state()
                    .chargeGroups(0, 1)
                    .get(0)
                    .progress();
                check(progress > 0.05 && progress < 0.8, "Waiting for visible partial melting progress");
            })
            .server("open controller with melting groups", () -> {
                player().setPositionAndUpdate(X + 0.5, Y + 1, Z - 1.5);
                check(
                    fixture[0].machine.onRightclick(fixture[0].controller, player()),
                    "Controller rejected melting-group GUI opening");
            })
            .awaitScreen(GuiContainerWrapper.class)
            .click(blankScreenCorner())
            .capture("controller-melting-groups")
            .server("close melting-slot controls", () -> player().closeScreen())
            .awaitClient(
                "melting-slot screen closed",
                client -> check(Minecraft.getMinecraft().currentScreen == null, "Melting-slot screen is still open"))
            .withinTicks(COLD_CHARGE_TIMEOUT_TICKS)
            .awaitServerAccelerated("melt the actual cold charge using hatch energy", 100, () -> {
                Fixture f = fixture[0];
                f.refillPower();
                IronBath observed = f.state();
                check(
                    observed.pendingMetalMb() == 0 && observed.liquidMetalMb() == WORKING_CHARGE_INGOTS * 144,
                    "Cold charge is still heating: liquid=" + observed.liquidMetalMb()
                        + ", pending="
                        + observed.pendingMetalMb()
                        + ", temperature="
                        + observed.temperature()
                        + ", usedEU="
                        + f.usedEnergy);
                check(f.usedEnergy > 0, "Furnace melted cold feed without drawing hatch energy");
                check(empty(f.input), "Ingot/dust feed was not consumed");
            })
            .server("Oxygen keeps its normal consumption rate at zero carbon", () -> {
                check(
                    fixture[0].state()
                        .carbon() == 0,
                    "The initial Iron bath already contains carbon");
                fixture[0].verifyOxygenRate();
                check(
                    fixture[0].state()
                        .carbon() == 0,
                    "Oxygen processing changed zero carbon");
            })
            .server("top up metal to reach the reserved slag outlet", () -> {
                fixture[0].input
                    .setInventorySlotContents(0, GTOreDictUnificator.get(OrePrefixes.ingot, Materials.Iron, 64));
                fixture[0].input
                    .setInventorySlotContents(1, GTOreDictUnificator.get(OrePrefixes.ingot, Materials.Iron, 48));
            })
            .withinTicks(COLD_CHARGE_TIMEOUT_TICKS)
            .awaitServerAccelerated("the topped-up bath reaches the upper outlet", 100, () -> {
                fixture[0].refillPower();
                IronBath bath = fixture[0].state();
                check(
                    bath.pendingMetalMb() == 0 && bath.liquidMetalMb() == bath.metalCapacityMb(),
                    "The 496-ingot bath has not melted up to the slag outlet");
            })
            .server("introduce carbon and flux after warming", () -> {
                Fixture f = fixture[0];
                f.input.setInventorySlotContents(0, new ItemStack(GameRegistry.findItem("Railcraft", "fuel.coke"), 12));
                f.input.setInventorySlotContents(1, GTOreDictUnificator.get(OrePrefixes.dust, Materials.Calcite, 1));
                f.input.markDirty();
            })
            .awaitServerAccelerated("twelve Coke directly carburize the working charge into Cast Iron", 1, () -> {
                fixture[0].refillPower();
                IronBath observed = fixture[0].state();
                check(empty(fixture[0].input), "Carbon and Calcite have not been consumed");
                check(observed.chargeGroupCount() == 0, "Carbon occupied a heating position");
                check(observed.carbon() > 1100, "The twelve Coke have not directly added their carbon");
                check(
                    observed.currentProduct() == IronProduct.CAST_IRON,
                    "Twelve Coke in 496 Iron ingots did not enter the Cast Iron band");
            })
            .server(
                "meter Oxygen to refine Cast Iron into Steel",
                () -> check(
                    fixture[0].oxygen()
                        .fill(ForgeDirection.UNKNOWN, Materials.Oxygen.getGas(26112), true) == 26112,
                    "Oxygen hatch rejected the 816-carbon-unit refinement dose"))
            .awaitServerAccelerated("clean the bath and form slag", 100, () -> {
                Fixture f = fixture[0];
                f.refillPower();
                IronBath observed = f.state();
                check(
                    empty(f.input) && f.oxygen()
                        .getFluidAmount() == 0
                        && observed.impurities() == 0
                        && observed.liquidSlagMb() >= 1
                        && observed.liquidMetalMb() == observed.metalMb()
                        && observed.liquidSlagMb() == observed.slagMb(),
                    "The cleaned liquid layers have not reached the upper tap");
                check(
                    observed.currentProduct() == pl.kuba6000.gtnhextranerf.machines.ironbath.IronProduct.STEEL,
                    "Metered Oxygen refinement did not produce the Steel band: " + observed.currentProduct());
            })
            .server("pulse upper outlet with a real redstone block", () -> fixture[0].powerSlagTap(true))
            .withinTicks(200)
            .awaitServerAccelerated("observe slag in the upper native output buffer", 1, () -> {
                fixture[0].refillPower();
                check(
                    fluid(
                        fixture[0].slagTap,
                        IronBathMaterials.slag()
                            .getMolten(1))
                        == 1,
                    "Upper outlet did not extract one mB of Slag");
            });
        scenario.serverSequence()
            .thenIdle(5);
        scenario.server("held signal does not repeat a rising-edge discharge", () -> {
            check(
                fluid(
                    fixture[0].slagTap,
                    IronBathMaterials.slag()
                        .getMolten(1))
                    == 1,
                "Held redstone signal repeated a pulse");
            fixture[0].powerSlagTap(false);
            fixture[0].metalTap.setAmountPerPulse(1);
            fixture[0].powerMetalTap(true);
        })
            .awaitServerAccelerated("observe real molten Steel in the bottom outlet", 1, () -> {
                fixture[0].refillPower();
                check(
                    fluid(fixture[0].metalTap, Materials.Steel.getMolten(1)) == 1,
                    "Bottom outlet did not extract one mB of Steel");
            });
        scenario.serverSequence()
            .thenIdle(5);
        scenario.server("keep the Steel pulse stable and enable continuous discharge", () -> {
            check(fluid(fixture[0].metalTap, Materials.Steel.getMolten(1)) == 1, "Held lower signal repeated a pulse");
            fixture[0].powerMetalTap(false);
            fixture[0].metalTap.drain(1, true);
            fixture[0].metalTap.setAmountPerPulse(144);
            fixture[0].metalTap.setRedstoneMode(2);
        })
            .awaitServerAccelerated("continuous mode transfers further actual product", 1, () -> {
                fixture[0].refillPower();
                check(
                    fluid(fixture[0].metalTap, Materials.Steel.getMolten(1)) == 15,
                    "Continuous outlet did not continue production");
                fixture[0].metalTap.setRedstoneMode(3);
            });
        scenario.server("set daylight for furnace views", () -> {
            helper.getWorld()
                .setWorldTime(6000);
            helper.getWorld()
                .getWorldInfo()
                .setRaining(false);
            helper.getWorld()
                .getWorldInfo()
                .setThundering(false);
        });
        view(scenario, fixture, "north", X + 5.5, Y + 5, Z - 16, 0, 5);
        view(scenario, fixture, "east", X + 26, Y + 5, Z + 1.5, 90, 5);
        view(scenario, fixture, "south", X + 5.5, Y + 5, Z + 19, 180, 5);
        view(scenario, fixture, "west", X - 15, Y + 5, Z + 1.5, -90, 5);
        view(scenario, fixture, "elevated", X + 22, Y + 17, Z - 15, 45, 30);
        scenario.client("show controller controls", client -> Minecraft.getMinecraft().gameSettings.hideGUI = false)
            .server("open the real furnace controller", () -> {
                EntityPlayerMP viewer = player();
                int slot = viewer.inventory.currentItem;
                ItemStack held = viewer.inventory.getStackInSlot(slot);
                ItemStack saved = held == null ? null : held.copy();
                helper.afterTest(() -> viewer.inventory.setInventorySlotContents(slot, saved));
                viewer.inventory.setInventorySlotContents(slot, null);
                viewer.setPositionAndUpdate(X + 0.5, Y + 1, Z - 1.5);
                fixture[0].refillPower();
                check(
                    fixture[0].machine.onRightclick(fixture[0].controller, viewer),
                    "Controller rejected GUI opening");
            })
            .awaitScreen(GuiContainerWrapper.class)
            .click(blankScreenCorner())
            .capture("controller-gui")
            .server("close furnace controls", () -> player().closeScreen())
            .awaitClient(
                "controller screen closed",
                client -> check(Minecraft.getMinecraft().currentScreen == null, "Controller screen is still open"))
            .server("open the real bottom outlet controls", () -> {
                fixture[0].refillPower();
                Offset outlet = IronBathStructure.METAL_TAP;
                player().setPositionAndUpdate(X + outlet.x() + 0.5, Y + outlet.y(), Z + outlet.z() - 2);
                check(
                    fixture[0].metalTap.onRightclick(fixture[0].metalTap.getBaseMetaTileEntity(), player()),
                    "Bottom outlet rejected GUI opening");
            })
            .awaitScreen(GuiContainerWrapper.class)
            .awaitClient("outlet controls reached the client", client -> {
                check(
                    "144".equals(((TextFieldWidget) tapWidget("bathAmount")).getText()),
                    "Outlet amount is not synchronized");
                check(
                    ((CycleButtonWidget) tapWidget("bathTrigger")).getIntValue()
                        .getIntValue() == 3,
                    "Stopped outlet mode is not synchronized");
            })
            .click(tapTarget("bathAmount"))
            .key(Keyboard.KEY_END, '\0')
            .key(Keyboard.KEY_BACK, '\0')
            .key(Keyboard.KEY_BACK, '\0')
            .key(Keyboard.KEY_BACK, '\0')
            .key(Keyboard.KEY_1, '1')
            .key(Keyboard.KEY_6, '6')
            .key(Keyboard.KEY_RETURN, '\n')
            .click(tapTarget("bathTrigger"))
            .awaitServer("outlet GUI edits reached authoritative controls", () -> {
                fixture[0].refillPower();
                check(fixture[0].metalTap.getAmountPerPulse() == 16, "GUI amount edit did not reach the server");
                check(fixture[0].metalTap.getRedstoneMode() == 0, "GUI did not cycle stopped mode to pulse mode");
            })
            .click(blankScreenCorner())
            .capture("tap-gui")
            .server("close edited outlet controls", () -> player().closeScreen())
            .awaitClient(
                "outlet screen closed",
                client -> check(Minecraft.getMinecraft().currentScreen == null, "Outlet screen is still open"))
            .server(
                "reopen persisted outlet controls",
                () -> check(
                    fixture[0].metalTap.onRightclick(fixture[0].metalTap.getBaseMetaTileEntity(), player()),
                    "Bottom outlet rejected reopening"))
            .awaitScreen(GuiContainerWrapper.class)
            .awaitClient("edited outlet controls survived reopening", client -> {
                check(
                    "16".equals(((TextFieldWidget) tapWidget("bathAmount")).getText()),
                    "Reopened outlet lost its edited amount");
                check(
                    ((CycleButtonWidget) tapWidget("bathTrigger")).getIntValue()
                        .getIntValue() == 0,
                    "Reopened outlet lost pulse mode");
            })
            .server("close outlet controls", () -> player().closeScreen())
            .server(
                "Oxygen drain obeys the per-tick budget and retains unused gas",
                () -> fixture[0].verifyOxygenRate());
        frozenViews(scenario, fixture);
        scenario.server("queue a real cold charge before controller pickup", () -> {
            fixture[0].input.setInventorySlotContents(0, GTOreDictUnificator.get(OrePrefixes.dust, Materials.Iron, 1));
            fixture[0].input.markDirty();
        })
            .awaitServerAccelerated("cold charge reached the persisted queue", 1, () -> {
                fixture[0].refillPower();
                check(
                    fixture[0].state()
                        .pendingMetalMb() > 0,
                    "Cold charge has not entered the queue");
            })
            .server("remove buffered inputs and tap during operation", () -> fixture[0].verifyPortRemoval())
            .server("pick up and replace the loaded native controller", () -> fixture[0].verifyPickupAndPlacement())
            .succeed();
    }

    private static void frozenViews(ClientScenario scenario, Fixture[] fixture) {
        NBTTagCompound[] warmState = new NBTTagCompound[1];
        int[] resident = new int[2];
        boolean[] working = new boolean[1];
        scenario
            .awaitClient(
                "outlet screen closed before the frozen render fixture",
                client -> check(Minecraft.getMinecraft().currentScreen == null, "Outlet screen is still open"))
            .server("freeze only the thermal state of the populated render fixture", () -> {
                Fixture f = fixture[0];
                IronBath before = f.state();
                check(before.meltingItems() == 0, "Frozen render fixture still contains heating charges");
                resident[0] = before.metalMb();
                resident[1] = before.slagMb();
                check(resident[0] > 0 && resident[1] > 0, "Frozen render fixture needs both resident layers");
                warmState[0] = new NBTTagCompound();
                f.machine.saveNBTData(warmState[0]);
                NBTTagCompound cold = (NBTTagCompound) warmState[0].copy();
                cold.getCompoundTag("IronBath")
                    .setDouble("Heat", 0);
                f.machine.loadNBTData(cold);
                working[0] = f.controller.isAllowedToWork();
                f.controller.disableWorking();
                for (BaseMetaTileEntity hatch : f.energy) hatch.setStoredEU(0);
                verifyFrozenContents(f, resident);
                player().setPositionAndUpdate(X + 0.5, Y + 1, Z - 1.5);
                check(f.machine.onRightclick(f.controller, player()), "Frozen controller rejected GUI opening");
            })
            .awaitScreen(GuiContainerWrapper.class)
            .click(blankScreenCorner())
            .capture("controller-frozen");
        scenario.serverSequence()
            .thenIdle(20);
        scenario
            .server(
                "frozen contents retain both resident layers over native ticks",
                () -> verifyFrozenContents(fixture[0], resident))
            .capture("controller-frozen-later")
            .server("close the frozen controller", () -> player().closeScreen())
            .awaitClient(
                "frozen controller screen closed",
                client -> check(
                    Minecraft.getMinecraft().currentScreen == null,
                    "Frozen controller screen is still open"))
            .server("restore the warm fixture for the loaded pickup checks", () -> {
                Fixture f = fixture[0];
                f.machine.loadNBTData(warmState[0]);
                if (working[0]) f.controller.enableWorking();
                else f.controller.disableWorking();
                f.refillPower();
                check(
                    f.state()
                        .metalMb() == resident[0]
                        && f.state()
                            .slagMb() == resident[1],
                    "Restoring the warm render fixture changed resident material");
            });
    }

    private static void verifyFrozenContents(Fixture fixture, int[] resident) {
        IronBath frozen = fixture.state();
        check(
            frozen.metalMb() == resident[0] && frozen.slagMb() == resident[1],
            "Freezing changed the resident metal or Slag quantity");
        check(
            frozen.liquidMetalMb() == 0 && frozen.liquidSlagMb() == 0,
            "The frozen render fixture still contains a liquid phase");
    }

    private static IWidget tapWidget(String name) {
        GuiContainerWrapper gui = (GuiContainerWrapper) Minecraft.getMinecraft().currentScreen;
        return WidgetTree.findFirstWithName(
            gui.getScreen()
                .getMainPanel(),
            name);
    }

    private static ClientTarget blankScreenCorner() {
        return ClientTarget.of("blank screen corner", window -> {
            GuiScreen gui = window.screen();
            check(gui != null, "No screen is open for the blank-corner target");
            check(
                Minecraft.getMinecraft().thePlayer.inventory.getItemStack() == null,
                "Cannot clear hover with an item on the cursor");
            return new ClickTarget(gui, new Rectangle(2, 2, 2, 2), () -> true);
        });
    }

    private static ClientTarget tapTarget(String name) {
        return ClientTarget.of(name, window -> {
            window.screen(GuiContainerWrapper.class);
            IWidget widget = tapWidget(name);
            return new ClickTarget(widget, new Rectangle(widget.getArea()), widget::isEnabled);
        });
    }

    private static void view(ClientScenario scenario, Fixture[] fixture, String name, double x, double y, double z,
        float yaw, float pitch) {
        scenario.server("position camera " + name, () -> {
            fixture[0].refillPower();
            player().setPositionAndUpdate(x, y, z);
            player().rotationYaw = yaw;
            player().rotationPitch = pitch;
        })
            .awaitClient("receive camera position " + name, client -> {
                Minecraft mc = Minecraft.getMinecraft();
                check(
                    mc.theWorld.getTileEntity(X, Y, Z) instanceof BaseMetaTileEntity,
                    "Controller has not reached client");
                check(
                    Math.abs(mc.thePlayer.posX - x) < 1 && Math.abs(mc.thePlayer.posZ - z) < 1,
                    "Camera teleport has not reached client");
            })
            .client("orient camera " + name, client -> {
                Minecraft mc = Minecraft.getMinecraft();
                mc.thePlayer.rotationYaw = mc.thePlayer.prevRotationYaw = yaw;
                mc.thePlayer.rotationPitch = mc.thePlayer.prevRotationPitch = pitch;
            });
        scenario.serverSequence()
            .thenIdle(15);
        scenario.capture(name);
    }

    private static void verifyRegistrations() {
        check(IronBathMaterials.wroughtIron() != Materials.CastIron, "Wrought Iron aliases Cast Iron");
        for (Materials material : new Materials[] { IronBathMaterials.wroughtIron(), Materials.Steel,
            Materials.CastIron, Materials.PigIron, IronBathMaterials.overcarburizedIron() }) {
            ItemStack ingot = GTOreDictUnificator.get(OrePrefixes.ingot, material, 1);
            ItemStack dust = GTOreDictUnificator.get(OrePrefixes.dust, material, 1);
            check(
                ingot != null && dust != null && material.getMolten(144) != null,
                "Missing furnace product form: " + material.mName);
            check(
                RecipeMaps.fluidSolidifierRecipes.getAllRecipes()
                    .stream()
                    .anyMatch(
                        recipe -> output(recipe, ingot) && recipe.mFluidInputs != null
                            && recipe.mFluidInputs.length == 1
                            && recipe.mFluidInputs[0].isFluidEqual(material.getMolten(144))),
                "Missing ingot casting recipe: " + material.mName);
        }
        check(
            RecipeMaps.assemblerRecipes.getAllRecipes()
                .stream()
                .anyMatch(recipe -> output(recipe, IronBathFurnace.controller())),
            "Controller construction recipe is missing");
        for (int meta = 0; meta < 2; meta++) {
            ItemStack casing = IronBathFurnace.casing(meta, 1);
            check(
                RecipeMaps.assemblerRecipes.getAllRecipes()
                    .stream()
                    .anyMatch(recipe -> output(recipe, casing)),
                "Casing construction recipe is missing");
        }
        for (int tier = 1; tier <= 14; tier++) {
            ItemStack tap = IronBathFurnace.tap(tier);
            check(
                RecipeMaps.assemblerRecipes.getAllRecipes()
                    .stream()
                    .anyMatch(recipe -> output(recipe, tap)),
                "Tap construction recipe is missing");
        }
        check(
            IronBathFurnace.RECIPES.getAllRecipes()
                .size() == 5,
            "Fuel reference balances are missing");
    }

    private static boolean output(GTRecipe recipe, ItemStack expected) {
        if (recipe.mOutputs == null) return false;
        for (ItemStack stack : recipe.mOutputs) if (stack != null && expected.isItemEqual(stack)) return true;
        return false;
    }

    private static boolean empty(IInventory input) {
        for (int slot = 0; slot < input.getSizeInventory(); slot++)
            if (input.getStackInSlot(slot) != null) return false;
        return true;
    }

    private static int fluid(MTEIronBathTap tap, FluidStack expected) {
        FluidStack actual = tap.getFluid();
        return actual != null && actual.isFluidEqual(expected) ? actual.amount : 0;
    }

    private static EntityPlayerMP player() {
        return (EntityPlayerMP) MinecraftServer.getServer()
            .getConfigurationManager().playerEntityList.get(0);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    static final class Fixture {

        private final WorldServer world;
        final BaseMetaTileEntity controller;
        final MTEIronBathFurnace machine;
        private final IInventory input;
        private final MTEIronBathTap metalTap;
        private final MTEIronBathTap slagTap;
        private final List<BaseMetaTileEntity> energy = new ArrayList<>();
        private long previousEnergy;
        private long usedEnergy;

        Fixture(GameTestHelper helper) {
            this(helper, false);
        }

        private Fixture(GameTestHelper helper, boolean lowVoltage) {
            this(helper, lowVoltage ? ItemList.Hatch_Energy_LV : ItemList.Hatch_Energy_HV);
        }

        private Fixture(GameTestHelper helper, ItemList powerHatch) {
            world = helper.getWorld();
            WorldServer overworld = DimensionManager.getWorld(0);
            check(overworld != null, "Certification overworld is unavailable");
            ProgressionData existing = (ProgressionData) overworld.mapStorage
                .loadData(ProgressionData.class, ProgressionData.NAME);
            if (existing == null) {
                existing = new ProgressionData();
                overworld.mapStorage.setData(ProgressionData.NAME, existing);
            }
            ProgressionData certification = existing;
            NBTTagCompound savedCertification = new NBTTagCompound();
            certification.writeToNBT(savedCertification);
            helper.afterTest(() -> {
                certification.readFromNBT(savedCertification);
                certification.markDirty();
            });
            for (ProgressionTier tier : new ProgressionTier[] { ProgressionTier.STEAM, ProgressionTier.LV,
                ProgressionTier.MV, ProgressionTier.HV }) MachineProgression.certify(world, tier);
            EntityPlayerMP viewer = player();
            double px = viewer.posX, py = viewer.posY, pz = viewer.posZ;
            float yaw = viewer.rotationYaw, pitch = viewer.rotationPitch;
            long worldTime = world.getWorldTime();
            String daylightCycle = world.getGameRules()
                .getGameRuleStringValue("doDaylightCycle");
            boolean raining = world.getWorldInfo()
                .isRaining();
            boolean thundering = world.getWorldInfo()
                .isThundering();
            int rainTime = world.getWorldInfo()
                .getRainTime();
            int thunderTime = world.getWorldInfo()
                .getThunderTime();
            world.setWorldTime(6000);
            world.getGameRules()
                .setOrCreateGameRule("doDaylightCycle", "false");
            world.getWorldInfo()
                .setRaining(false);
            world.getWorldInfo()
                .setThundering(false);
            world.getWorldInfo()
                .setRainTime(1000000);
            world.getWorldInfo()
                .setThunderTime(1000000);
            WorldSettings.GameType mode = viewer.theItemInWorldManager.getGameType();
            boolean flying = viewer.capabilities.isFlying;
            viewer.theItemInWorldManager.setGameType(WorldSettings.GameType.CREATIVE);
            viewer.capabilities.isFlying = true;
            viewer.sendPlayerAbilities();
            viewer.setPositionAndUpdate(X + 5.5, Y + 5, Z - 16);
            helper.afterTest(() -> {
                viewer.closeScreen();
                viewer.theItemInWorldManager.setGameType(mode);
                viewer.capabilities.isFlying = flying;
                viewer.sendPlayerAbilities();
                viewer.setPositionAndUpdate(px, py, pz);
                viewer.rotationYaw = yaw;
                viewer.rotationPitch = pitch;
                world.setWorldTime(worldTime);
                world.getGameRules()
                    .setOrCreateGameRule("doDaylightCycle", daylightCycle);
                world.getWorldInfo()
                    .setRaining(raining);
                world.getWorldInfo()
                    .setThundering(thundering);
                world.getWorldInfo()
                    .setRainTime(rainTime);
                world.getWorldInfo()
                    .setThunderTime(thunderTime);
                clear();
            });
            FixtureWorld.loadAround(world, X, Z);
            clear();
            for (int dx = IronBathStructure.MIN_X; dx <= IronBathStructure.MAX_X; dx++)
                for (int dz = IronBathStructure.MIN_Z; dz <= IronBathStructure.MAX_Z; dz++)
                    world.setBlock(X + dx, Y + IronBathStructure.MIN_Y - 1, Z + dz, Blocks.stonebrick, 0, 3);
            FakePlayer placer = FakePlayerFactory.get(world, new GameProfile(UUID.randomUUID(), "IronBathFixture"));
            controller = place(placer, IronBathFurnace.controller(), 0, 0, 0);
            controller.setFrontFacing(ForgeDirection.NORTH);
            machine = (MTEIronBathFurnace) controller.getMetaTileEntity();
            machine.construct(IronBathFurnace.controller(), false);
            input = (IInventory) place(placer, ItemList.Hatch_Input_Bus_HV.get(1), IronBathStructure.INPUT_BUS)
                .getMetaTileEntity();
            place(placer, ItemList.Hatch_Input_HV.get(1), IronBathStructure.OXYGEN_INPUT);
            place(placer, ItemList.Hatch_Maintenance.get(1), IronBathStructure.MAINTENANCE);
            energy.add(place(placer, powerHatch.get(1), IronBathStructure.ENERGY_A));
            energy.add(place(placer, powerHatch.get(1), IronBathStructure.ENERGY_B));
            BaseMetaTileEntity metal = place(placer, IronBathFurnace.tap(1), IronBathStructure.METAL_TAP);
            metal.setFrontFacing(ForgeDirection.NORTH);
            metalTap = (MTEIronBathTap) metal.getMetaTileEntity();
            metalTap.setAmountPerPulse(144);
            BaseMetaTileEntity slag = place(placer, IronBathFurnace.tap(1), IronBathStructure.SLAG_TAP);
            slag.setFrontFacing(ForgeDirection.NORTH);
            slagTap = (MTEIronBathTap) slag.getMetaTileEntity();
            slagTap.setAmountPerPulse(1);
            check(machine.checkStructure(true, controller), "Iron bath structure did not form");
            machine.fixAllIssues();
            controller.enableWorking();
            refillPower();
        }

        private boolean meterRunning;
        private long meterLastTick;
        private long meterTicks;
        private long meterEU;
        private long meterOutputMb;
        private long meterSteelMb;
        private double meterInitialHeat;
        private double meterInitialChargeHeat;
        private double meterExportedHeat;
        private double meterHeatPerEU;
        private double meterNominalEUPerCharge;
        private double meterNominalChargesPerTick;
        private MTEIronBathTap meterTap;

        private void preparePaidRemelting(int tier) {
            controller.disableWorking();
            for (ProgressionTier certificate : ProgressionTier.values()) MachineProgression.certify(world, certificate);
            FakePlayer placer = FakePlayerFactory.getMinecraft(world);
            energy.clear();
            for (Offset offset : new Offset[] { IronBathStructure.ENERGY_A, IronBathStructure.ENERGY_B }) {
                BaseMetaTileEntity tile = place(placer, ItemList.DebugEnergyHatch.get(1), offset);
                gregtech.api.metatileentity.implementations.MTEHatchEnergyDebug debug = (gregtech.api.metatileentity.implementations.MTEHatchEnergyDebug) tile
                    .getMetaTileEntity();
                debug.setVoltageTier(tier);
                debug.setAmperage(2);
                debug.setRefillInterval(1);
                tile.setStoredEU(Long.MAX_VALUE);
                energy.add(tile);
            }
            BaseMetaTileEntity maintenance = place(
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
            ItemStack coil = (tier < 4 ? ItemList.Casing_Coil_Cupronickel : ItemList.Casing_Coil_Eternal).get(1);
            for (Offset offset : IronBathStructure.COIL_POSITIONS)
                setBlock(offset, Block.getBlockFromItem(coil.getItem()), coil.getItemDamage());
            BaseMetaTileEntity outlet = place(placer, IronBathFurnace.tap(8), IronBathStructure.METAL_TAP);
            outlet.setFrontFacing(ForgeDirection.NORTH);
            meterTap = (MTEIronBathTap) outlet.getMetaTileEntity();
            meterTap.setAmountPerPulse(meterTap.getPulseLimit());
            meterTap.setRedstoneMode(2);
            check(forms(), "Metered furnace did not form with native debug hatches");
            machine.fixAllIssues();
            NBTTagCompound seed = new NBTTagCompound();
            machine.saveNBTData(seed);
            IronBath empty = new IronBath(tier < 4 ? 0 : 13);
            NBTTagCompound bathTag = new NBTTagCompound();
            empty.save(bathTag);
            bathTag.setDouble("Heat", empty.heatDemand(IronBath.WORKING_TEMPERATURE));
            seed.setTag("IronBath", bathTag);
            machine.loadNBTData(seed);
            meterInitialHeat = storedMeterHeat();
            meterInitialChargeHeat = storedChargeHeat();
            long voltage = gregtech.api.enums.GTValues.V[tier];
            gregtech.api.util.OverclockCalculator model = new gregtech.api.util.OverclockCalculator().setRecipeEUt(120)
                .setDuration(1 << 28)
                .setEUt(voltage)
                .setAmperage(4)
                .setAmperageOC(true)
                .setRecipeHeat(1700)
                .setMachineHeat((tier < 4 ? 1801 : 13501) + 100 * (gregtech.api.util.GTUtility.getTier(voltage) - 1))
                .setHeatOC(true)
                .setHeatDiscount(true)
                .calculate();
            double speed = (double) (1 << 28) / model.getDuration();
            meterNominalEUPerCharge = 24000 * model.getConsumption() / (120 * speed * empty.heatingEfficiency());
            meterHeatPerEU = IronBath.COLD_MELTING_HEAT_PER_INGOT / meterNominalEUPerCharge;
            meterNominalChargesPerTick = model.getConsumption() / meterNominalEUPerCharge;
            supplyMeterFeed();
            meterLastTick = world.getTotalWorldTime();
            meterRunning = true;
            controller.enableWorking();
        }

        private double storedMeterHeat() {
            NBTTagCompound saved = new NBTTagCompound();
            machine.saveNBTData(saved);
            NBTTagCompound bathTag = saved.getCompoundTag("IronBath");
            double result = bathTag.getDouble("Heat");
            NBTTagCompound charges = bathTag.getCompoundTag("Charges");
            for (Object key : charges.func_150296_c()) {
                NBTTagCompound charge = charges.getCompoundTag((String) key);
                result += charge.getDouble("Heat") * charge.getInteger("Count");
            }
            return result;
        }

        private double storedChargeHeat() {
            NBTTagCompound saved = new NBTTagCompound();
            machine.saveNBTData(saved);
            NBTTagCompound bathTag = saved.getCompoundTag("IronBath");
            double result = bathTag.getInteger("Metal") / 144.0 * IronBath.COLD_MELTING_HEAT_PER_INGOT;
            NBTTagCompound charges = bathTag.getCompoundTag("Charges");
            for (Object key : charges.func_150296_c()) {
                NBTTagCompound charge = charges.getCompoundTag((String) key);
                result += charge.getDouble("Heat") * charge.getInteger("Count");
            }
            return result;
        }

        private void supplyMeterFeed() {
            for (int slot = 0; slot < input.getSizeInventory(); slot++)
                input.setInventorySlotContents(slot, GTOreDictUnificator.get(OrePrefixes.ingot, Materials.Steel, 64));
            input.markDirty();
        }

        private void observePaidRemeltingTick() {
            if (!meterRunning) return;
            long now = world.getTotalWorldTime();
            if (now == meterLastTick) return;
            check(now == meterLastTick + 1, "Wattmeter missed an actual world tick");
            meterLastTick = now;
            meterTicks++;
            check(
                machine.lEUt == 0 || machine.mEfficiency == 10000,
                "Wattmeter requires perfect native maintenance efficiency");
            meterEU += Math.max(0, -machine.lEUt);
            double temperature = state().temperature();
            FluidStack output = meterTap.drain(Integer.MAX_VALUE, true);
            if (output != null) {
                meterExportedHeat += output.amount / 144.0
                    * (IronBath.COLD_MELTING_HEAT_PER_INGOT + 20 * (temperature - IronBath.MELTING_TEMPERATURE));
                meterOutputMb += output.amount;
                if (output.isFluidEqual(Materials.Steel.getMolten(1))) meterSteelMb += output.amount;
            }
            supplyMeterFeed();
        }

        private void verifyPaidRemelting(GameTestHelper helper, int tier, double baselineEU) {
            double exportedIngots = meterOutputMb / 144.0;
            check(exportedIngots > 0, "Metered furnace exported no molten metal");
            double storedDelta = storedMeterHeat() - meterInitialHeat;
            double chargeStoredDelta = storedChargeHeat() - meterInitialChargeHeat;
            double chargedHeat = chargeStoredDelta + exportedIngots * IronBath.COLD_MELTING_HEAT_PER_INGOT;
            double chargeEU = chargedHeat / meterHeatPerEU;
            double standbyHeatPerEU = IronBath.COLD_MELTING_HEAT_PER_INGOT / 24000;
            double bathHeatPerEU = standbyHeatPerEU * state().heatingEfficiency();
            double tickAllowance = meterNominalChargesPerTick * IronBath.COLD_MELTING_HEAT_PER_INGOT * 2;
            check(chargeEU <= meterEU + tickAllowance / meterHeatPerEU, "Charges received unpaid native power");
            double paidHeat = chargedHeat + Math.max(0, meterEU - chargeEU) * bathHeatPerEU;
            double retainedAndExportedHeat = storedDelta + meterExportedHeat;
            NBTTagCompound snapshot = new NBTTagCompound();
            machine.saveNBTData(snapshot);
            int coilTier = snapshot.getCompoundTag("IronBath")
                .getInteger("CoilTier");
            double maximumHeatLoss = 60.0 * (1 << coilTier) * standbyHeatPerEU * meterTicks;
            helper.recordDiagnostic(
                "Native heat ledger: paid=" + paidHeat
                    + ", retained and exported="
                    + retainedAndExportedHeat
                    + ", stored="
                    + storedDelta
                    + ", exported="
                    + meterExportedHeat
                    + ", maximum loss="
                    + maximumHeatLoss
                    + ", allowance="
                    + tickAllowance
                    + ", EU="
                    + meterEU
                    + ", ticks="
                    + meterTicks);
            check(
                retainedAndExportedHeat <= paidHeat + tickAllowance,
                "Native charges and exported fluid received unpaid heat");
            check(
                paidHeat - retainedAndExportedHeat <= maximumHeatLoss + tickAllowance,
                "Native heat losses exceeded twice the full-temperature standby budget");
            double correctedEU = meterEU - chargeStoredDelta / meterHeatPerEU
                - (storedDelta - chargeStoredDelta) / bathHeatPerEU;
            double correctedEUPerIngot = correctedEU / exportedIngots;
            check(
                correctedEUPerIngot >= baselineEU,
                "Cold metal remelting undercut the accessible native Iron-to-Steel route");
            double rate = chargedHeat / IronBath.COLD_MELTING_HEAT_PER_INGOT / meterTicks;
            double tickRateAllowance = 2 * meterNominalChargesPerTick / meterTicks;
            check(rate <= meterNominalChargesPerTick + tickRateAllowance, "Melting exceeded native heater throughput");
            check(
                rate + tickRateAllowance >= meterNominalChargesPerTick * 0.8,
                "Sustained native remelting lost more than its thermal overhead");
            helper.recordDiagnostic(
                "paid Steel remelting lower bound: tier=" + tier
                    + ", ticks="
                    + meterTicks
                    + ", paid EU="
                    + meterEU
                    + ", output mB="
                    + meterOutputMb
                    + ", Steel mB="
                    + meterSteelMb
                    + ", stored heat delta="
                    + storedDelta
                    + ", charge EU="
                    + chargeEU
                    + ", bath EU="
                    + (meterEU - chargeEU)
                    + ", exported heat="
                    + meterExportedHeat
                    + ", heat-corrected EU/ingot="
                    + correctedEUPerIngot
                    + ", native baseline EU/ingot="
                    + baselineEU
                    + ", ingots/tick="
                    + rate
                    + ", heater charge limit/tick="
                    + meterNominalChargesPerTick);
        }

        private void setBlock(Offset offset, Block block, int meta) {
            world.setBlock(X + offset.x(), Y + offset.y(), Z + offset.z(), block, meta, 3);
        }

        private void clear() {
            for (int dx = IronBathStructure.MIN_X; dx <= IronBathStructure.MAX_X; dx++)
                for (int dy = IronBathStructure.MIN_Y - 1; dy <= IronBathStructure.MAX_Y; dy++)
                    for (int dz = IronBathStructure.MIN_Z - 1; dz <= IronBathStructure.MAX_Z; dz++)
                        world.setBlockToAir(X + dx, Y + dy, Z + dz);
        }

        private boolean forms() {
            return machine.checkStructure(true, controller);
        }

        private BaseMetaTileEntity place(FakePlayer placer, ItemStack stack, Offset offset) {
            return place(placer, stack, offset.x(), offset.y(), offset.z());
        }

        private BaseMetaTileEntity place(FakePlayer placer, ItemStack stack, int dx, int dy, int dz) {
            world.setBlockToAir(X + dx, Y + dy, Z + dz);
            check(
                ((ItemBlock) stack.getItem())
                    .placeBlockAt(stack, placer, world, X + dx, Y + dy, Z + dz, 1, 0.5f, 0.5f, 0.5f, 0),
                "Could not place furnace fixture at " + dx + "," + dy + "," + dz);
            return (BaseMetaTileEntity) world.getTileEntity(X + dx, Y + dy, Z + dz);
        }

        private void refillPower() {
            machine.fixAllIssues();
            long remaining = 0;
            for (BaseMetaTileEntity tile : energy) remaining += tile.getStoredEU();
            if (previousEnergy > remaining) usedEnergy += previousEnergy - remaining;
            previousEnergy = 0;
            for (BaseMetaTileEntity tile : energy) {
                tile.setStoredEU(tile.getEUCapacity());
                check(
                    tile.getStoredEU() > 0,
                    "Fixture energy refill stored no EU: locked="
                        + EnergyHatchCertification.isLocked(tile.getMetaTileEntity())
                        + ", capacity="
                        + tile.getEUCapacity()
                        + ", Steam="
                        + MachineProgression.isCertified(world, ProgressionTier.STEAM)
                        + ", LV="
                        + MachineProgression.isCertified(world, ProgressionTier.LV)
                        + ", MV="
                        + MachineProgression.isCertified(world, ProgressionTier.MV)
                        + ", HV="
                        + MachineProgression.isCertified(world, ProgressionTier.HV));
                previousEnergy += tile.getStoredEU();
            }
        }

        private MTEHatchInput oxygen() {
            Offset offset = IronBathStructure.OXYGEN_INPUT;
            return (MTEHatchInput) ((BaseMetaTileEntity) world
                .getTileEntity(X + offset.x(), Y + offset.y(), Z + offset.z())).getMetaTileEntity();
        }

        private void loadWorkingCharge() {
            for (int slot = 0; slot < WORKING_CHARGE_INGOTS / 64; slot++) {
                OrePrefixes prefix = slot % 2 == 0 ? OrePrefixes.ingot : OrePrefixes.dust;
                input.setInventorySlotContents(slot, GTOreDictUnificator.get(prefix, Materials.Iron, 64));
            }
            input.markDirty();
        }

        private void verifyOxygenRate() {
            refillPower();
            MTEHatchInput oxygen = oxygen();
            check(
                oxygen.fill(ForgeDirection.UNKNOWN, Materials.Oxygen.getGas(1000), true) == 1000,
                "Oxygen hatch rejected the bounded-drain fixture");
            machine.onPostTick(controller, world.getTotalWorldTime());
            int consumed = 1000 - oxygen.getFluidAmount();
            check(consumed == 32, "One native controller tick consumed " + consumed + " L Oxygen instead of 32");
            check(oxygen.getFluidAmount() == 968, "Unused Oxygen disappeared from the external tank");
            oxygen.drain(ForgeDirection.UNKNOWN, Integer.MAX_VALUE, true);
        }

        private void verifyPortRemoval() {
            setBlock(IronBathStructure.INPUT_BUS, Blocks.air, 0);
            setBlock(IronBathStructure.OXYGEN_INPUT, Blocks.air, 0);
            setBlock(IronBathStructure.METAL_TAP, Blocks.air, 0);
            machine.onPostTick(controller, world.getTotalWorldTime());
        }

        private void verifyPickupAndPlacement() {
            NBTTagCompound before = new NBTTagCompound();
            machine.saveNBTData(before);
            check(
                before.getCompoundTag("IronBath")
                    .getInteger("Metal") > 0,
                "Pickup fixture no longer contains resident metal");
            check(
                before.getCompoundTag("IronBath")
                    .getDouble("Heat") > 0,
                "Pickup fixture no longer contains heat");
            List<ItemStack> drops = controller.getDrops();
            check(
                drops.size() == 1 && !drops.get(0)
                    .hasTagCompound(),
                "Native controller drop retained item NBT");
            ItemStack dropped = drops.get(0);
            world.setBlockToAir(X, Y, Z);
            FakePlayer placer = FakePlayerFactory.get(world, new GameProfile(UUID.randomUUID(), "IronBathReplace"));
            BaseMetaTileEntity replacement = place(placer, dropped, 0, 0, 0);
            check(
                replacement.getMetaTileEntity() instanceof MTEIronBathFurnace,
                "Native item placement did not restore the furnace controller");
            NBTTagCompound after = new NBTTagCompound();
            ((MTEIronBathFurnace) replacement.getMetaTileEntity()).saveNBTData(after);
            NBTTagCompound freshBath = new NBTTagCompound();
            new IronBath(0).save(freshBath);
            check(freshBath.equals(after.getCompoundTag("IronBath")), "Replaced controller retained the loaded bath");
        }

        private IronBath state() {
            NBTTagCompound persisted = new NBTTagCompound();
            machine.saveNBTData(persisted);
            IronBath observed = new IronBath(0);
            observed.load(persisted.getCompoundTag("IronBath"));
            return observed;
        }

        private void powerSlagTap(boolean powered) {
            Offset tap = IronBathStructure.SLAG_TAP;
            setBlock(new Offset(tap.x(), tap.y(), tap.z() - 1), powered ? Blocks.redstone_block : Blocks.air, 0);
        }

        private void powerMetalTap(boolean powered) {
            Offset tap = IronBathStructure.METAL_TAP;
            setBlock(new Offset(tap.x(), tap.y(), tap.z() - 1), powered ? Blocks.redstone_block : Blocks.air, 0);
        }
    }

    private static final class Feedback {

        private final Fixture fixture;
        private long nextProbe;
        private boolean awaitingProbe;
        private int sampled;
        private int reached;
        private String lastSample = "none";

        private Feedback(Fixture fixture) {
            this.fixture = fixture;
        }

        private void awaitProduct(Materials target) {
            fixture.refillPower();
            long now = fixture.world.getTotalWorldTime();
            FluidStack sample = fixture.metalTap.getFluid();
            if (sample != null && sample.amount > 0) {
                FluidStack removed = fixture.metalTap.drain(Integer.MAX_VALUE, true);
                fixture.powerMetalTap(false);
                awaitingProbe = false;
                nextProbe = now + 5;
                lastSample = removed.getFluid()
                    .getName();
                sampled += removed.amount;
                if (sampled >= 144
                    && offer(GTOreDictUnificator.get(OrePrefixes.ingot, Materials.Iron, sampled / 144))) {
                    sampled %= 144;
                }
                if (removed.isFluidEqual(target.getMolten(1))) {
                    reached++;
                    return;
                }
                int observed = rank(removed), requested = rank(target.getMolten(1));
                check(observed >= 0, "Unexpected product in bottom probe: " + removed.getLocalizedName());
                if (observed < requested) {
                    boolean addedFuel = offer(new ItemStack(GameRegistry.findItem("Railcraft", "fuel.coke"), 1));
                    offer(GTOreDictUnificator.get(OrePrefixes.dust, Materials.Calcite, 1));
                    nextProbe = now + (addedFuel ? 600 : 20);
                } else {
                    MTEHatchInput oxygen = fixture.oxygen();
                    if (oxygen.getFluidAmount() == 0) check(
                        oxygen.fill(ForgeDirection.UNKNOWN, Materials.Oxygen.getGas(256), true) == 256,
                        "Oxygen hatch rejected metered correction dose");
                }
            } else if (now >= nextProbe) {
                if (awaitingProbe) {
                    fixture.powerMetalTap(false);
                    awaitingProbe = false;
                    nextProbe = now + 5;
                } else {
                    fixture.powerMetalTap(true);
                    awaitingProbe = true;
                    nextProbe = now + 20;
                }
            }
            throw new AssertionError("Waiting for fresh " + target.mName + " output feedback, last=" + lastSample);
        }

        private boolean offer(ItemStack stack) {
            for (int slot = 0; slot < fixture.input.getSizeInventory() - 1; slot++) {
                ItemStack existing = fixture.input.getStackInSlot(slot);
                if (existing != null && existing.isItemEqual(stack)) return false;
            }
            for (int slot = 0; slot < fixture.input.getSizeInventory() - 1; slot++) {
                if (fixture.input.getStackInSlot(slot) == null) {
                    fixture.input.setInventorySlotContents(slot, stack);
                    fixture.input.markDirty();
                    return true;
                }
            }
            return false;
        }

        private static int rank(FluidStack sample) {
            Materials[] order = { Materials.Iron, IronBathMaterials.wroughtIron(), Materials.Steel, Materials.CastIron,
                Materials.PigIron, IronBathMaterials.overcarburizedIron() };
            for (int index = 0; index < order.length; index++)
                if (sample.isFluidEqual(order[index].getMolten(1))) return index;
            return -1;
        }
    }
}
