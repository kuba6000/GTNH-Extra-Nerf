package pl.kuba6000.gtnhextranerf.integration;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;
import com.gtnewhorizons.horizonqa.api.client.ClientScenario;
import com.gtnewhorizons.horizonqa.api.client.ClientTest;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.StackInfo;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicMachine;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTModHandler;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
import gregtech.nei.GTNEIDefaultHandler;

/** Tests the actual LV cutter and the recipe handler that supplies its NEI page. */
@GameTestHolder(value = "gtnhextranerftests", clientOnly = true)
public final class CutterCoolantScenarios {

    private CutterCoolantScenarios() {}

    @GameTest(template = "native_fixture", timeoutTicks = 1200)
    public static void machine(GameTestHelper helper) {
        NativeScenarios.scenario(
            helper,
            "cutter coolant",
            true,
            () -> verifyMachine(helper.getWorld(), helper.absolute("workbench")));
    }

    @GameTest(template = "native_fixture", timeoutTicks = 1200)
    public static void nei(GameTestHelper helper) {
        ClientScenario scenario = ClientTest.scenario(helper)
            .defaultTimeoutTicks(400)
            .client("check the cutter's NEI alternatives", client -> verifyNei());
        // Let first-world rendering finish before HQA's short client teardown budget starts.
        scenario.serverSequence()
            .thenIdle(200);
        scenario.client("wait for the client thread", client -> {})
            .succeed();
    }

    @GameTest(template = "native_fixture", timeoutTicks = 1200)
    public static void lubricantOnLv(GameTestHelper helper) {
        NativeScenarios.scenario(
            helper,
            "LV lubricant production",
            true,
            () -> verifyLubricant(helper.getWorld(), helper.absolute("workbench")));
    }

    private static void verifyLubricant(WorldServer world, TestPos pos) {
        BaseMetaTileEntity tile = TransformerIntegrationTests
            .placeAt(world, ItemList.Machine_LV_Distillery.get(1), pos.x(), pos.y(), pos.z());
        MTEBasicMachine machine = (MTEBasicMachine) tile.getMetaTileEntity();
        try {
            machine
                .setInventorySlotContents(machine.getCircuitSlot(), ItemList.Circuit_Integrated.getWithDamage(1, 24));
            check(machine.fill(Materials.OilHeavy.getFluid(24), true) == 24, "LV distillery rejected heavy oil");
            for (int i = 0; i < 200 && machine.getDrainableStack() == null; i++) {
                tile.setStoredEU(tile.getEUCapacity());
                tile.updateEntityProfiled();
            }
            FluidStack result = machine.getDrainableStack();
            check(
                result != null && result.isFluidEqual(Materials.Lubricant.getFluid(1)) && result.amount > 0,
                "LV distillery did not produce lubricant");
        } finally {
            world.setBlockToAir(pos.x(), pos.y(), pos.z());
        }
    }

    private static void verifyMachine(WorldServer world, TestPos pos) {
        BaseMetaTileEntity tile = TransformerIntegrationTests
            .placeAt(world, ItemList.Machine_LV_Cutter.get(1), pos.x(), pos.y(), pos.z());
        MTEBasicMachine machine = (MTEBasicMachine) tile.getMetaTileEntity();
        ItemStack livingrock = item(OrePrefixes.block, Materials.Livingrock);
        ItemStack plate = item(OrePrefixes.plate, Materials.Livingrock);
        try {
            machine.setInventorySlotContents(machine.getInputSlot(), livingrock.copy());
            machine.setInventorySlotContents(machine.getCircuitSlot(), ItemList.Circuit_Integrated.getWithDamage(1, 3));
            machine.setFillableStack(Materials.Water.getFluid(1_000));
            for (int i = 0; i < 200; i++) {
                tile.setStoredEU(tile.getEUCapacity());
                tile.updateEntityProfiled();
            }
            check(
                tile.getMaxProgress() == 0 && machine.getStackInSlot(machine.getInputSlot()) != null,
                "The LV cutter accepted water for Livingrock");
            fillLubricant(machine);
            for (int i = 0; i < 2_500 && machine.getStackInSlot(machine.getOutputSlot()) == null; i++) {
                tile.setStoredEU(tile.getEUCapacity());
                tile.updateEntityProfiled();
            }
            ItemStack result = machine.getStackInSlot(machine.getOutputSlot());
            check(result != null && result.isItemEqual(plate), "The LV cutter rejected Lubricant for Livingrock");

            verifyGeneratedRecipe(
                machine,
                tile,
                CutterCoolantFixture.AUTOMATIC_VARIANTS_CIRCUIT,
                new ItemStack(Items.feather));
            verifyGeneratedRecipe(
                machine,
                tile,
                CutterCoolantFixture.CLASSIC_VARIANTS_CIRCUIT,
                new ItemStack(Items.egg));

            verifyWaterOnlyRejected(machine, tile);
            verifyGeneratedRecipe(
                machine,
                tile,
                CutterCoolantFixture.SEPARATE_VARIANTS_CIRCUIT,
                new ItemStack(Items.arrow));
            verifySiliconWafer(machine, tile);
            verifyNoWaterVariants();
            check(
                RecipeMaps.cutterRecipes.containsInput(Materials.UUMatter.getFluid(1)),
                "A special UU-Matter cutter recipe disappeared");
            check(
                RecipeMaps.cutterRecipes.containsInput(Materials.Grade1PurifiedWater.getFluid(1)),
                "A special purified-water cutter recipe disappeared");
        } finally {
            world.setBlockToAir(pos.x(), pos.y(), pos.z());
        }
    }

    private static void verifyGeneratedRecipe(MTEBasicMachine machine, BaseMetaTileEntity tile, int circuit,
        ItemStack expected) {
        machine.setInventorySlotContents(machine.getOutputSlot(), null);
        machine.setInventorySlotContents(machine.getInputSlot(), new ItemStack(Items.nether_star));
        machine.setInventorySlotContents(machine.getCircuitSlot(), CutterCoolantFixture.circuit(circuit));
        machine.setFillableStack(Materials.Water.getFluid(1_000));
        for (int i = 0; i < 200; i++) {
            tile.setStoredEU(tile.getEUCapacity());
            tile.updateEntityProfiled();
        }
        check(
            tile.getMaxProgress() == 0 && machine.getStackInSlot(machine.getInputSlot()) != null,
            "The LV cutter accepted water for a generated coolant family");
        fillLubricant(machine);
        for (int i = 0; i < 300 && machine.getStackInSlot(machine.getOutputSlot()) == null; i++) {
            tile.setStoredEU(tile.getEUCapacity());
            tile.updateEntityProfiled();
        }
        ItemStack result = machine.getStackInSlot(machine.getOutputSlot());
        check(result != null && result.isItemEqual(expected), "The LV cutter rejected a generated lubricant variant");
    }

    private static void verifySiliconWafer(MTEBasicMachine machine, BaseMetaTileEntity tile) {
        machine.setInventorySlotContents(machine.getOutputSlot(), null);
        machine.setInventorySlotContents(machine.getInputSlot(), ItemList.Circuit_Silicon_Ingot.get(1));
        machine.setInventorySlotContents(machine.getCircuitSlot(), null);
        machine.setFillableStack(Materials.Water.getFluid(1_000));
        for (int i = 0; i < 200; i++) {
            tile.setStoredEU(tile.getEUCapacity());
            tile.updateEntityProfiled();
        }
        check(
            tile.getMaxProgress() == 0 && machine.getStackInSlot(machine.getInputSlot()) != null,
            "The LV cutter accepted water for the previously nerfed silicon wafer recipe");
        fillLubricant(machine);
        for (int i = 0; i < 1_500 && machine.getStackInSlot(machine.getOutputSlot()) == null; i++) {
            tile.setStoredEU(tile.getEUCapacity());
            tile.updateEntityProfiled();
        }
        ItemStack result = machine.getStackInSlot(machine.getOutputSlot());
        check(
            result != null && result.isItemEqual(ItemList.Circuit_Silicon_Wafer.get(1)),
            "The LV cutter rejected lubricant for silicon wafers");
    }

    private static void verifyWaterOnlyRejected(MTEBasicMachine machine, BaseMetaTileEntity tile) {
        machine.setInventorySlotContents(machine.getOutputSlot(), null);
        machine.setInventorySlotContents(machine.getInputSlot(), new ItemStack(Items.nether_star));
        machine.setInventorySlotContents(
            machine.getCircuitSlot(),
            CutterCoolantFixture.circuit(CutterCoolantFixture.WATER_ONLY_CIRCUIT));
        machine.setFillableStack(Materials.Water.getFluid(1_000));
        for (int i = 0; i < 200; i++) {
            tile.setStoredEU(tile.getEUCapacity());
            tile.updateEntityProfiled();
        }
        check(
            tile.getMaxProgress() == 0 && machine.getStackInSlot(machine.getInputSlot()) != null,
            "The LV cutter accepted a separately registered water-only recipe");
    }

    private static void verifyNoWaterVariants() {
        Fluid water = FluidRegistry.WATER;
        Fluid distilled = GTModHandler.getDistilledWater(1)
            .getFluid();
        for (GTRecipe recipe : RecipeMaps.cutterRecipes.getAllRecipes()) {
            for (FluidStack fluid : recipe.mFluidInputs) check(
                fluid == null || fluid.getFluid() != water && fluid.getFluid() != distilled,
                "A water-cooled cutter recipe remains");
            if (recipe.mAltFluidInputs != null) for (FluidStack[] alternatives : recipe.mAltFluidInputs)
                if (alternatives != null) for (FluidStack fluid : alternatives) check(
                    fluid == null || fluid.getFluid() != water && fluid.getFluid() != distilled,
                    "A water coolant alternative remains");
        }
    }

    private static void fillLubricant(MTEBasicMachine machine) {
        machine.setFillableStack(null);
        check(
            machine.fill(Materials.Lubricant.getFluid(1_000), true) == 1_000,
            "The LV cutter rejected Lubricant in its input tank");
    }

    private static void verifyNei() {
        ItemStack livingrock = item(OrePrefixes.block, Materials.Livingrock);
        ItemStack plate = item(OrePrefixes.plate, Materials.Livingrock);
        GTNEIDefaultHandler handler = new GTNEIDefaultHandler(RecipeMaps.cutterFakeRecipes.getDefaultRecipeCategory());
        Set<Fluid> seen = visibleFluids(handler, livingrock, null, plate);
        check(!seen.contains(FluidRegistry.WATER), "NEI shows water for Livingrock");
        check(
            !seen.contains(
                GTModHandler.getDistilledWater(1)
                    .getFluid()),
            "NEI shows distilled water for Livingrock");
        check(
            seen.contains(
                Materials.Lubricant.getFluid(1)
                    .getFluid()),
            "NEI hides Lubricant for Livingrock");
        Set<Fluid> automatic = fixtureFluids(
            handler,
            CutterCoolantFixture.AUTOMATIC_VARIANTS_CIRCUIT,
            new ItemStack(Items.feather));
        check(
            !automatic.contains(FluidRegistry.WATER) && !automatic.contains(
                GTModHandler.getDistilledWater(1)
                    .getFluid()),
            "NEI shows water for an automatically generated recipe");
        check(
            automatic.contains(
                Materials.Lubricant.getFluid(1)
                    .getFluid())
                && automatic.contains(
                    Materials.DimensionallyShiftedSuperfluid.getFluid(1)
                        .getFluid()),
            "NEI does not show both automatically generated cutter fluids");

        Set<Fluid> classic = fixtureFluids(
            handler,
            CutterCoolantFixture.CLASSIC_VARIANTS_CIRCUIT,
            new ItemStack(Items.egg));
        check(
            !classic.contains(FluidRegistry.WATER) && !classic.contains(
                GTModHandler.getDistilledWater(1)
                    .getFluid()),
            "NEI shows water for a classic coolant family");
        check(
            classic.contains(
                Materials.Lubricant.getFluid(1)
                    .getFluid())
                && classic.contains(
                    Materials.DimensionallyShiftedSuperfluid.getFluid(1)
                        .getFluid()),
            "NEI hides a lubricant or superfluid variant from a classic coolant family");

        Set<Fluid> silicon = visibleFluids(
            handler,
            ItemList.Circuit_Silicon_Ingot.get(1),
            null,
            ItemList.Circuit_Silicon_Wafer.get(1));
        check(!silicon.contains(FluidRegistry.WATER), "NEI shows the removed silicon-wafer water variant");
        check(
            !silicon.contains(
                GTModHandler.getDistilledWater(1)
                    .getFluid()),
            "NEI shows the removed silicon-wafer distilled-water variant");
        check(
            silicon.contains(
                Materials.Lubricant.getFluid(1)
                    .getFluid()),
            "NEI hides silicon-wafer lubricant");

        Set<Fluid> waterOnly = fixtureFluids(
            handler,
            CutterCoolantFixture.WATER_ONLY_CIRCUIT,
            new ItemStack(Items.book));
        check(waterOnly.isEmpty(), "NEI shows the separately registered water-only recipe");
        Set<Fluid> separate = fixtureFluids(
            handler,
            CutterCoolantFixture.SEPARATE_VARIANTS_CIRCUIT,
            new ItemStack(Items.arrow));
        check(!separate.contains(FluidRegistry.WATER), "NEI shows a separately registered water variant");
        check(
            separate.contains(
                Materials.Lubricant.getFluid(1)
                    .getFluid()),
            "NEI hides the separately registered lubricant variant");
    }

    private static Set<Fluid> fixtureFluids(GTNEIDefaultHandler handler, int circuit, ItemStack output) {
        return visibleFluids(handler, new ItemStack(Items.nether_star), CutterCoolantFixture.circuit(circuit), output);
    }

    private static Set<Fluid> visibleFluids(GTNEIDefaultHandler handler, ItemStack item, ItemStack circuit,
        ItemStack output) {
        Set<Fluid> seen = new HashSet<>();
        for (GTNEIDefaultHandler.CachedDefaultRecipe recipe : handler.getCache()) {
            if (recipe.mInputs.stream()
                .noneMatch(stack -> stack.contains(item))
                || circuit != null && recipe.mInputs.stream()
                    .noneMatch(stack -> stack.contains(circuit))
                || recipe.mOutputs.stream()
                    .noneMatch(stack -> stack.contains(output)))
                continue;
            for (PositionedStack input : recipe.mInputs) {
                if (input.items == null) continue;
                for (ItemStack alternative : input.items) {
                    FluidStack fluid = StackInfo.getFluid(alternative);
                    if (fluid != null) seen.add(fluid.getFluid());
                }
            }
        }
        return seen;
    }

    private static ItemStack item(OrePrefixes prefix, Materials material) {
        ItemStack stack = GTOreDictUnificator.get(prefix, material, 1);
        if (stack == null) throw new AssertionError("Missing material form " + prefix + material);
        return stack;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
