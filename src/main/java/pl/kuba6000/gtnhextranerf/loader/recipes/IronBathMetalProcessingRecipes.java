package pl.kuba6000.gtnhextranerf.loader.recipes;

import java.util.Arrays;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import pl.kuba6000.gtnhextranerf.audit.AuditScope;
import pl.kuba6000.gtnhextranerf.recipes.RecipeContext;
import pl.kuba6000.gtnhextranerf.recipes.RecipeModule;
import pl.kuba6000.gtnhextranerf.recipes.RecipeRegistry;
import pl.kuba6000.gtnhextranerf.recipes.machine.RecipeMatch;

/** Preserves iron composition and routes chainmail recovery through dust. */
public final class IronBathMetalProcessingRecipes implements RecipeModule {

    private static final OrePrefixes[] MAGNETIC_FORMS = { OrePrefixes.dustTiny, OrePrefixes.dustSmall, OrePrefixes.dust,
        OrePrefixes.nugget, OrePrefixes.ingot, OrePrefixes.plate, OrePrefixes.plateDouble, OrePrefixes.plateDense,
        OrePrefixes.stick, OrePrefixes.round, OrePrefixes.bolt, OrePrefixes.screw, OrePrefixes.ring, OrePrefixes.foil,
        OrePrefixes.toolHeadHammer, OrePrefixes.toolHeadFile, OrePrefixes.toolHeadSaw, OrePrefixes.toolHeadDrill,
        OrePrefixes.toolHeadChainsaw, OrePrefixes.toolHeadWrench, OrePrefixes.toolHeadBuzzSaw, OrePrefixes.turbineBlade,
        OrePrefixes.itemCasing, OrePrefixes.wireFine, OrePrefixes.gearGtSmall, OrePrefixes.rotor, OrePrefixes.stickLong,
        OrePrefixes.springSmall, OrePrefixes.spring, OrePrefixes.gearGt, OrePrefixes.plateSuperdense, OrePrefixes.block,
        OrePrefixes.frameGt, OrePrefixes.sheetmetal };

    @Override
    public void register(RecipeRegistry registry) {
        registry
            .register(AuditScope.REMOVE_IRON_BATH_IRON_WASHING, IronBathMetalProcessingRecipes::preserveComposition);
        registry.register(AuditScope.RESTRICT_CHAINMAIL_STEEL_RECOVERY, IronBathMetalProcessingRecipes::chainmailDust);
    }

    private static void chainmailDust(RecipeContext recipes) {
        for (var map : new RecipeMap<?>[] { RecipeMaps.arcFurnaceRecipes, RecipeMaps.fluidExtractionRecipes }) {
            recipes.machine(map)
                .removeExactly(
                    recipe -> Arrays.stream(recipe.mInputs)
                        .anyMatch(
                            input -> input != null && (input.getItem() == Items.chainmail_helmet
                                || input.getItem() == Items.chainmail_chestplate
                                || input.getItem() == Items.chainmail_leggings
                                || input.getItem() == Items.chainmail_boots)),
                    4);
        }
    }

    private static void preserveComposition(RecipeContext recipes) {
        for (OrePrefixes form : MAGNETIC_FORMS) {
            int duration = (int) Math.max(16, form.getMaterialAmount() * 128 / GTValues.M);
            recipes.machine(RecipeMaps.polarizerRecipes)
                .removeExactly(
                    RecipeMatch.builder()
                        .itemInputs(GTOreDictUnificator.get(form, Materials.CastIron, 1))
                        .itemOutputs(GTOreDictUnificator.get(form, Materials.IronMagnetic, 1))
                        .fluidInputs()
                        .fluidOutputs()
                        .duration(duration)
                        .eut(15)
                        .build(),
                    1);
        }
        recipes.machine(RecipeMaps.alloySmelterRecipes)
            .removeExactly(
                RecipeMatch.builder()
                    .itemInputs(
                        GTOreDictUnificator.get(OrePrefixes.ingot, Materials.CastIron, 31),
                        ItemList.Shape_Mold_Anvil.get(0))
                    .itemOutputs(new ItemStack(Blocks.anvil))
                    .fluidInputs()
                    .fluidOutputs()
                    .duration(512)
                    .eut(60)
                    .build(),
                1);
        recipes.machine(RecipeMaps.extruderRecipes)
            .removeExactly(
                RecipeMatch.builder()
                    .itemInputs(
                        GTOreDictUnificator.get(OrePrefixes.ingot, Materials.CastIron, 1),
                        ItemList.Shape_Extruder_Cell.get(0))
                    .itemOutputs(ItemList.IC2_Fuel_Rod_Empty.get(1))
                    .fluidInputs()
                    .fluidOutputs()
                    .duration(128)
                    .eut(30)
                    .build(),
                1);
    }
}
