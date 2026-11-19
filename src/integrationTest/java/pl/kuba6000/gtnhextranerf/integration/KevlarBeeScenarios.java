package pl.kuba6000.gtnhextranerf.integration;

import java.util.List;
import java.util.stream.Collectors;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import forestry.api.apiculture.BeeManager;
import forestry.api.apiculture.EnumBeeChromosome;
import forestry.api.apiculture.IBeeMutation;
import forestry.core.genetics.mutations.Mutation;
import forestry.core.genetics.mutations.MutationConditionRequiresResource;
import gregtech.api.enums.ItemList;
import gregtech.loaders.misc.GTBeeDefinition;

/** Checks the Kevlar mutation in Forestry's registry. */
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
}
