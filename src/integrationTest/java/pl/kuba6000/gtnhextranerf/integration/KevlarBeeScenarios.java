package pl.kuba6000.gtnhextranerf.integration;

import static gregtech.api.util.GTRecipeConstants.QFT_CATALYST;
import static gregtech.api.util.GTRecipeConstants.QFT_FOCUS_TIER;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import forestry.api.apiculture.BeeManager;
import forestry.api.apiculture.EnumBeeChromosome;
import forestry.api.apiculture.IBeeMutation;
import forestry.core.genetics.mutations.Mutation;
import forestry.core.genetics.mutations.MutationConditionRequiresResource;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTRecipe;
import gregtech.common.items.CombType;
import gregtech.loaders.misc.GTBeeDefinition;
import gregtech.loaders.misc.GTBees;
import gtPlusPlus.xmod.gregtech.api.enums.GregtechItemList;

/** Checks Kevlar mutations and recipes in the native registries. */
@GameTestHolder("gtnhextranerftests")
public final class KevlarBeeScenarios {

    private KevlarBeeScenarios() {}

    @GameTest(template = "native_fixture")
    public static void kevlarCanBreedBeforeUv(GameTestHelper helper) {
        List<IBeeMutation> mutations = BeeManager.beeRoot.getMutations(false)
            .stream()
            .filter(
                mutation -> mutation.getTemplate()[EnumBeeChromosome.SPECIES.ordinal()]
                    == GTBeeDefinition.KEVLAR.getSpecies())
            .collect(Collectors.toList());

        helper.assertTrue(mutations.size() == 1, "Expected exactly one Kevlar mutation");
        IBeeMutation mutation = mutations.get(0);
        helper.assertTrue(
            mutation.isPartner(GTBeeDefinition.OIL.getSpecies())
                && mutation.isPartner(GTBeeDefinition.NAQUADAH.getSpecies()),
            "Kevlar must breed from Oil and Naquadah");
        helper.assertTrue(mutation.getBaseChance() == 4.0f, "Expected a 4% base mutation chance");
        helper.assertTrue(
            ((Mutation) mutation).getMutationConditions()
                .stream()
                .anyMatch(
                    condition -> condition instanceof MutationConditionRequiresResource resource
                        && ItemList.ReplicatorZPM.get(1)
                            .isItemEqual(resource.getBlockRequired())),
            "Kevlar must still require a ZPM Replicator");
        helper.succeed();
    }

    /** Checks the input threshold and consumption through GregTech's recipe matcher. */
    @GameTest(template = "native_fixture")
    public static void qftPlasticsConsumeKevlarCombs(GameTestHelper helper) {
        List<GTRecipe> plastics = RecipeMaps.quantumForceTransformerRecipes.getAllRecipes()
            .stream()
            .filter(
                recipe -> Arrays.stream(recipe.mFluidOutputs)
                    .anyMatch(
                        output -> Materials.RadoxPolymer.getMolten(1)
                            .isFluidEqual(output)))
            .collect(Collectors.toList());
        helper.assertTrue(plastics.size() == 1, "Expected one Radox Polymer recipe in the QFT");
        GTRecipe recipe = plastics.get(0);
        helper.assertTrue(recipe.getMetadataOrDefault(QFT_FOCUS_TIER, 0) == 4, "Expected focus tier 4");
        ItemStack catalyst = recipe.getMetadata(QFT_CATALYST);
        helper.assertTrue(
            catalyst != null && catalyst.isItemEqual(GregtechItemList.UltimatePlasticCatalyst.get(1)),
            "Expected the Ultimate Plastic Catalyst");

        ItemStack combs = GTBees.combs.getStackForType(CombType.KEVLAR, 63);
        ItemStack carbon = Materials.Carbon.getDust(64);
        ItemStack osmium = Materials.Osmium.getDust(24);
        FluidStack[] gases = { Materials.Hydrogen.getGas(16_000), Materials.Nitrogen.getGas(16_000) };
        helper.assertTrue(!recipe.isRecipeInputEqual(false, gases, carbon, osmium, combs), "63 combs must not suffice");
        combs.stackSize = 64;
        helper.assertTrue(recipe.isRecipeInputEqual(true, gases, carbon, osmium, combs), "64 combs must suffice");
        helper.assertTrue(combs.stackSize == 0, "A batch must consume all 64 combs");
        helper
            .assertTrue(carbon.stackSize == 0 && osmium.stackSize == 0, "A batch must still consume carbon and osmium");
        helper.succeed();
    }
}
