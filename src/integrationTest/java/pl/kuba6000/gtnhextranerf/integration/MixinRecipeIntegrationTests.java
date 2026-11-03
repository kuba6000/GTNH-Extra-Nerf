package pl.kuba6000.gtnhextranerf.integration;

import java.util.Collections;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.Materials;
import gregtech.api.enums.MetaTileEntityIDs;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTUtility;
import gtPlusPlus.core.material.MaterialsAlloy;
import gtPlusPlus.core.material.MaterialsElements;
import gtPlusPlus.xmod.gregtech.api.metatileentity.implementations.base.MTEHatchCustomFluidBase;
import pl.kuba6000.gtnhextranerf.GTNHExtraNerf;
import pl.kuba6000.gtnhextranerf.audit.AuditDefinition;
import pl.kuba6000.gtnhextranerf.audit.AuditExpectation;
import pl.kuba6000.gtnhextranerf.audit.AuditSession;
import pl.kuba6000.gtnhextranerf.mixins.support.ProcessingWireRecipeTransformer;
import pl.kuba6000.gtnhextranerf.mixins.support.RingRecipeTransformer;
import pl.kuba6000.gtnhextranerf.recipes.machine.RecipeMatch;

/** Queries final registered recipes after the scratch server has started. */
final class MixinRecipeIntegrationTests {

    private MixinRecipeIntegrationTests() {}

    static void verifyRecipes() {
        MTEHatchCustomFluidBase steamHatch = (MTEHatchCustomFluidBase) GregTechAPI.METATILEENTITIES[MetaTileEntityIDs.Hatch_Input_Steam.ID];
        check(steamHatch.getCapacity() == 640000, "Steam Input Hatch capacity must be 640000 L");
        check(
            ((MTEHatchCustomFluidBase) steamHatch.newMetaEntity(null)).getCapacity() == 640000,
            "new Steam Input Hatches must retain the registered capacity");
        CasingFinishingIntegrationTests.verify();
        MaterialIntegrationTests.verify();
        GTNHExtraNerf.LOG.info("Extra Nerf material integration tests PASSED");
        verifyRings();
        verifyDirectCables(Materials.Rubber, 144);
        verifyDirectCables(Materials.StyreneButadieneRubber, 108);
        verifyDirectCables(Materials.RubberSilicone, 72);
        verifyDielectricCables(OrePrefixes.dust, 4, 144);
        verifyDielectricCables(OrePrefixes.dustSmall, 1, 36);
        verifyUnrelatedRecipes();
        LvRecipeIntegrationTests.verify();
        HeatingCoilIntegrationTests.verify();
        IndustrialRecipeIntegrationTests.verify();
        AlloyBlastSmelterIntegrationTests.verify();
        Ae2RecipeIntegrationTests.verify();
        WirelessPowerIntegrationTests.verify();
        verifyStaballoyCentrifuges();
        FusionRecipeIntegrationTests.verify();
        ProgressionNerfIntegrationTests.verify();
        GTNHExtraNerf.LOG.info("Extra Nerf mixin integration tests PASSED");
    }

    private static void verifyStaballoyCentrifuges() {
        RecipeMatch input = RecipeMatch.builder()
            .itemInputs(MaterialsAlloy.STABALLOY.getDust(10))
            .build();
        for (RecipeMap<?> map : new RecipeMap<?>[] { RecipeMaps.centrifugeRecipes,
            RecipeMaps.centrifugeNonCellRecipes }) {
            java.util.List<GTRecipe> found = new java.util.ArrayList<>();
            for (GTRecipe recipe : map.getAllRecipes()) if (input.test(recipe)) found.add(recipe);
            check(found.size() == 1, "expected one Staballoy recipe in " + map.unlocalizedName);
            GTRecipe recipe = found.get(0);
            check(
                GTUtility.getTier(recipe.mEUt) == GTUtility.getTier(TierEU.RECIPE_IV),
                "Staballoy centrifuging must require IV in " + map.unlocalizedName + "; got " + recipe.mEUt);
            check(
                RecipeMatch.builder()
                    .itemOutputs(
                        MaterialsElements.getInstance().URANIUM238.getDust(9),
                        MaterialsElements.getInstance().TITANIUM.getDust(1))
                    .fluidInputs()
                    .fluidOutputs()
                    .outputChances(10000, 10000)
                    .duration((MaterialsAlloy.STABALLOY.voltageMultiplier / 10) * GTRecipeBuilder.SECONDS)
                    .build()
                    .test(recipe),
                "Staballoy non-voltage recipe data changed in " + map.unlocalizedName);
        }
    }

    private static void verifyRings() {
        ItemStack ring = item(OrePrefixes.ring, Materials.Copper, 1);
        ItemStack stick = item(OrePrefixes.stick, Materials.Copper, 1);
        Object[] recipe = { "h ", "fX", 'X', stick };
        check(RingRecipeTransformer.replaceStick(ring, recipe), "ring rod was not replaced");
        sameStack(item(OrePrefixes.stickLong, Materials.Copper, 1), (ItemStack) recipe[3]);
        check(
            "h ".equals(recipe[0]) && "fX".equals(recipe[1])
                && Character.valueOf('X')
                    .equals(recipe[2]),
            "ring pattern changed");
        check(!RingRecipeTransformer.replaceStick(ring, recipe), "ring transformation was not idempotent");

        Object[] foreign = { "h ", "fX", 'X', stick };
        check(
            !RingRecipeTransformer.replaceStick(item(OrePrefixes.plate, Materials.Copper, 1), foreign),
            "non-ring output was modified");
        check(foreign[3] == stick, "foreign ingredient changed");
        Object[] wrongBinding = { "h ", "fX", 'Y', stick };
        check(!RingRecipeTransformer.replaceStick(ring, wrongBinding), "wrong symbol was accepted");
        check(wrongBinding[3] == stick, "wrong-symbol recipe changed");
    }

    private static void verifyDirectCables(Materials rubber, int fluidPerFoil) {
        ItemStack wire = item(OrePrefixes.wireGt01, Materials.Aluminium, 2);
        ItemStack circuit = GTUtility.getIntegratedCircuit(24);
        GTRecipeBuilder recipe = cableRecipe(2, rubber.getMolten(2L * fluidPerFoil)).itemInputs(wire, circuit);
        transform(recipe, 1);
        ItemStack[] inputs = recipe.getItemInputsBasic();
        check(inputs.length == 3, "direct cable ingredient count");
        sameStack(wire, inputs[0]);
        sameStack(item(OrePrefixes.foil, Materials.PolyphenyleneSulfide, 2), inputs[1]);
        sameStack(circuit, inputs[2]);
        unchangedRecipeData(recipe, 2, rubber.getMolten(2L * fluidPerFoil));
        transform(recipe, 0);
        check(recipe.getItemInputsBasic().length == 3, "PPS was added twice");
    }

    private static void verifyDielectricCables(OrePrefixes dust, int cableCount, int fluidAmount) {
        for (Materials dielectric : new Materials[] { Materials.PolyvinylChloride, Materials.Polydimethylsiloxane }) {
            for (Materials rubber : new Materials[] { Materials.StyreneButadieneRubber, Materials.RubberSilicone }) {
                ItemStack wire = item(OrePrefixes.wireGt01, Materials.Aluminium, cableCount);
                ItemStack powder = item(dust, dielectric, 1);
                GTRecipeBuilder recipe = cableRecipe(cableCount, rubber.getMolten(fluidAmount))
                    .itemInputs(wire, powder);
                transform(recipe, 1);
                ItemStack[] inputs = recipe.getItemInputsBasic();
                check(inputs.length == 3, "dielectric cable ingredient count");
                sameStack(wire, inputs[0]);
                sameStack(powder, inputs[1]);
                sameStack(item(OrePrefixes.foil, Materials.PolyphenyleneSulfide, cableCount), inputs[2]);
                unchangedRecipeData(recipe, cableCount, rubber.getMolten(fluidAmount));
            }
        }
    }

    private static void verifyUnrelatedRecipes() {
        ItemStack wire = item(OrePrefixes.wireGt01, Materials.Aluminium, 1);
        ItemStack circuit = GTUtility.getIntegratedCircuit(24);
        GTRecipeBuilder wrongFluidAmount = cableRecipe(1, Materials.Rubber.getMolten(145)).itemInputs(wire, circuit);
        transform(wrongFluidAmount, 0);
        check(wrongFluidAmount.getItemInputsBasic().length == 2, "non-divisible fluid amount accepted");
        sameStack(wire, wrongFluidAmount.getItemInputsBasic()[0]);
        sameStack(circuit, wrongFluidAmount.getItemInputsBasic()[1]);

        GTRecipeBuilder wireOutput = cableRecipe(1, Materials.Rubber.getMolten(144)).itemInputs(wire, circuit)
            .itemOutputs(wire);
        transform(wireOutput, 0);
        sameStack(wire, wireOutput.getItemOutputs()[0]);
        check(wireOutput.getItemInputsBasic().length == 2, "wire output was modified");
    }

    private static GTRecipeBuilder cableRecipe(int amount, FluidStack fluid) {
        return GTRecipeBuilder.builder()
            .itemOutputs(item(OrePrefixes.cableGt01, Materials.Aluminium, amount))
            .fluidInputs(fluid)
            .duration(80)
            .eut(16);
    }

    private static void transform(GTRecipeBuilder recipe, final int expectedChanges) {
        AuditDefinition definition = new AuditDefinition() {

            public String id() {
                return "integration";
            }

            public AuditExpectation expectation() {
                return AuditExpectation.exact()
                    .modified(expectedChanges)
                    .build();
            }
        };
        AuditSession session = new AuditSession(Collections.singletonList(definition));
        session.run(
            definition,
            handle -> check(
                ProcessingWireRecipeTransformer
                    .transform(recipe, RecipeMaps.assemblerRecipes, OrePrefixes.wireGt01, Materials.Aluminium, handle)
                    == recipe,
                "builder identity changed"));
        check(
            session.finish()
                .accepted(),
            "unexpected wire transformation count");
    }

    private static void unchangedRecipeData(GTRecipeBuilder recipe, int amount, FluidStack fluid) {
        sameStack(item(OrePrefixes.cableGt01, Materials.Aluminium, amount), recipe.getItemOutputs()[0]);
        check(recipe.getDuration() == 80 && recipe.getEUt() == 16, "recipe processing data changed");
        check(
            recipe.getFluidInputs().length == 1 && recipe.getFluidInputs()[0].isFluidStackIdentical(fluid),
            "recipe fluid changed");
    }

    private static ItemStack item(OrePrefixes prefix, Materials material, int amount) {
        ItemStack stack = GTOreDictUnificator.get(prefix, material, amount);
        check(stack != null, "missing integration fixture: " + prefix + material);
        return stack;
    }

    private static void sameStack(ItemStack expected, ItemStack actual) {
        check(ItemStack.areItemStacksEqual(expected, actual), "ingredient/output mismatch");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
