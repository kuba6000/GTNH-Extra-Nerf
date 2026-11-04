package pl.kuba6000.gtnhextranerf.loader.recipes;

import static bartworks.API.recipe.BartWorksRecipeMaps.bacterialVatRecipes;
import static gregtech.api.recipe.RecipeMaps.assemblerRecipes;
import static gregtech.api.recipe.RecipeMaps.blastFurnaceRecipes;
import static gregtech.api.recipe.RecipeMaps.cableRecipes;
import static gregtech.api.recipe.RecipeMaps.chemicalPlantRecipes;
import static gregtech.api.recipe.RecipeMaps.chemicalReactorRecipes;
import static gregtech.api.recipe.RecipeMaps.distillationTowerRecipes;
import static gregtech.api.recipe.RecipeMaps.distilleryRecipes;
import static gregtech.api.recipe.RecipeMaps.electrolyzerNonCellRecipes;
import static gregtech.api.recipe.RecipeMaps.electrolyzerRecipes;
import static gregtech.api.recipe.RecipeMaps.fluidExtractionRecipes;
import static gregtech.api.recipe.RecipeMaps.massFabFakeRecipes;
import static gregtech.api.recipe.RecipeMaps.multiblockChemicalReactorRecipes;
import static gregtech.api.recipe.RecipeMaps.multiblockMassFabricatorRecipes;
import static gregtech.api.recipe.RecipeMaps.pcbFactoryRecipes;
import static gregtech.api.recipe.RecipeMaps.purificationOzonationRecipes;
import static gregtech.api.util.GTRecipeBuilder.SECONDS;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import bartworks.system.material.Werkstoff;
import bartworks.system.material.WerkstoffLoader;
import cpw.mods.fml.common.registry.GameRegistry;
import goodgenerator.items.GGMaterial;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTECable;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.metadata.PurificationPlantBaseChanceKey;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipeConstants;
import gregtech.api.util.GTUtility;
import gregtech.common.tileentities.machines.basic.MTEMassfabricator;
import gregtech.common.tileentities.machines.multi.purification.MTEPurificationPlant;
import gtPlusPlus.core.material.MaterialMisc;
import gtPlusPlus.core.material.MaterialsElements;
import gtPlusPlus.core.material.MaterialsOres;
import gtPlusPlus.core.material.nuclear.MaterialsFluorides;
import gtPlusPlus.xmod.gregtech.api.enums.GregtechItemList;
import pl.kuba6000.gtnhextranerf.GTNHExtraNerf;
import pl.kuba6000.gtnhextranerf.audit.AuditScope;
import pl.kuba6000.gtnhextranerf.machines.ProgressionMachineNerf;
import pl.kuba6000.gtnhextranerf.machines.RawMachineCasings;
import pl.kuba6000.gtnhextranerf.recipes.RecipeContext;
import pl.kuba6000.gtnhextranerf.recipes.RecipeModule;
import pl.kuba6000.gtnhextranerf.recipes.RecipeRegistry;
import pl.kuba6000.gtnhextranerf.recipes.RecipeValidationException;
import pl.kuba6000.gtnhextranerf.recipes.crafting.CraftingDefinition;
import pl.kuba6000.gtnhextranerf.recipes.crafting.CraftingRecipeBuilder;
import pl.kuba6000.gtnhextranerf.recipes.crafting.CraftingRecipeType;
import pl.kuba6000.gtnhextranerf.recipes.machine.RecipeChange;
import pl.kuba6000.gtnhextranerf.recipes.machine.RecipeMatch;

/** Late progression edits share the existing recipe transactions and audit policy. */
public final class ProgressionRecipeNerf implements RecipeModule {

    @Override
    public void register(RecipeRegistry registry) {
        registry.register(AuditScope.ADD_NAQUADAH_PURIFICATION_WATER, ProgressionRecipeNerf::naquadahWater);
        registry.register(AuditScope.MODIFY_HIGHEST_OZONATION_RECIPE, ProgressionRecipeNerf::ozonation);
        registry.register(AuditScope.REMOVE_FLOCCULATION_WASTE_RECYCLING, ProgressionRecipeNerf::flocculationWaste);
        registry.register(AuditScope.MODIFY_HIGH_TIER_HULL_INSULATION, ProgressionRecipeNerf::hullInsulation);
        registry.register(AuditScope.MODIFY_AMERICIUM_BOULE, ProgressionRecipeNerf::americiumBoule);
        registry.register(AuditScope.MODIFY_PCB_FACTORY_WET_BIO_PLASMA, ProgressionRecipeNerf::pcbPlasma);
        registry.register(AuditScope.ADD_MINERAL_MIX_TO_RAW_BIO_MEDIUM, ProgressionRecipeNerf::bioMineralMix);
        registry.register(AuditScope.MODIFY_MUTATED_SOLDER_STAR, ProgressionRecipeNerf::solderStar);
        registry.register(AuditScope.ADD_NEPTUNIUM_FLUORIDE_TO_EUROPIUM_BOULE, ProgressionRecipeNerf::europiumBoule);
        registry.register(AuditScope.REMOVE_EV_CABLE_ASSEMBLER_RECIPES, ProgressionRecipeNerf::latexTransition);
        registry.register(AuditScope.MOVE_RUTHENIUM_TO_COOLANT_TOWER, ProgressionRecipeNerf::rutheniumCooling);
        registry.register(AuditScope.REMOVE_UNAMPLIFIED_UU_RECIPES, ProgressionRecipeNerf::amplifiedMatter);
        registry.register(AuditScope.REDIRECT_HIGH_TIER_RAW_CASINGS, ProgressionRecipeNerf::highRawCasings);
    }

    private static void highRawCasings(RecipeContext recipes) {
        ItemList[] casings = { ItemList.Casing_UEV, ItemList.Casing_UIV, ItemList.Casing_UMV, ItemList.Casing_UXV };
        Materials[] materials = { Materials.Infinity, Materials.TranscendentMetal, Materials.SpaceTime,
            Materials.MHDCSM };
        for (int tier = 0; tier < casings.length; tier++) {
            boolean uxv = tier == 3;
            ItemStack finished = casings[tier].get(1), raw = RawMachineCasings.productionOutput(finished);
            ItemStack plate = item(OrePrefixes.plate, materials[tier], uxv ? 4 : 8);
            ItemStack magmatter = uxv ? item(OrePrefixes.plate, Materials.MagMatter, 4) : null;
            ItemStack[] inputs = uxv ? new ItemStack[] { plate, magmatter, GTUtility.getIntegratedCircuit(8) }
                : new ItemStack[] { plate, GTUtility.getIntegratedCircuit(8) };
            OrePrefixes upgradedPlate = uxv ? OrePrefixes.plateDense : OrePrefixes.plateDouble;
            RecipeChange.Builder change = RecipeChange.builder()
                .replaceItemInput(plate, item(upgradedPlate, materials[tier], uxv ? 1 : 8))
                .replaceItemOutput(finished, raw);
            if (uxv) change.replaceItemInput(magmatter, item(OrePrefixes.plateDouble, Materials.MagMatter, 4));
            recipes.machine(assemblerRecipes)
                .modifyOne(
                    RecipeMatch.builder()
                        .itemInputs(inputs)
                        .itemOutputs(finished)
                        .fluidInputs()
                        .fluidOutputs()
                        .duration(50)
                        .eut(TierEU.RECIPE_LV / 2)
                        .build(),
                    change.build());
            CraftingRecipeBuilder crafting = CraftingRecipeBuilder.shaped()
                .pattern(uxv ? new String[] { "PSP", "SWS", "PSP" } : new String[] { "PPP", "PWP", "PPP" })
                .ingredient(
                    'P',
                    OrePrefixes.plate.get(materials[tier])
                        .toString())
                .ingredient('W', "craftingToolWrench")
                .output(finished)
                .matchType(CraftingRecipeType.GT_SHAPED)
                .removable(false);
            if (uxv) crafting.ingredient(
                'S',
                OrePrefixes.plate.get(Materials.MagMatter)
                    .toString());
            CraftingDefinition original = crafting.build();
            CraftingRecipeBuilder replacement = original.toBuilder()
                .ingredient(
                    'P',
                    upgradedPlate.get(materials[tier])
                        .toString())
                .output(raw);
            if (uxv) replacement.pattern("SPS", " W ", "S S")
                .ingredient(
                    'S',
                    OrePrefixes.plateDouble.get(Materials.MagMatter)
                        .toString());
            recipes.crafting()
                .expectOutputCount(finished, 1);
            recipes.crafting()
                .replaceOne(original, replacement.build());
            recipes.crafting()
                .expectOutputCount(finished, 0);
        }
    }

    private static void amplifiedMatter(RecipeContext recipes) {
        Predicate<GTRecipe> matter = recipe -> Arrays.stream(recipe.mFluidOutputs)
            .anyMatch(output -> output.isFluidEqual(Materials.UUMatter.getFluid(1)));
        Predicate<GTRecipe> noAmplifier = recipe -> Arrays.stream(recipe.mFluidInputs)
            .noneMatch(input -> input.isFluidEqual(Materials.UUAmplifier.getFluid(1)) && input.amount > 0);
        recipes.machine(multiblockMassFabricatorRecipes)
            .removeExactly(matter.and(noAmplifier), 2);
        recipes.machine(multiblockMassFabricatorRecipes)
            .removeExactly(matter.and(noAmplifier), 0);
        recipes.machine(massFabFakeRecipes)
            .removeExactly(matter.and(noAmplifier), MTEMassfabricator.sRequiresUUA ? 0 : 1);
        Item leaf = GameRegistry.findItem("cropsnh", "materialLeaf");
        if (leaf == null) throw new RecipeValidationException("Missing CropsNH material leaf item");
        for (int meta : new int[] { 17, 23 }) recipes.machine(fluidExtractionRecipes)
            .removeExactly(
                RecipeMatch.builder()
                    .itemInputs(new ItemStack(leaf, 1, meta))
                    .itemOutputs()
                    .fluidInputs()
                    .fluidOutputs(Materials.UUMatter.getFluid(4))
                    .duration(128)
                    .eut(4)
                    .build(),
                1);
        recipes.machine(fluidExtractionRecipes)
            .removeExactly(matter, 0);
    }

    private static void rutheniumCooling(RecipeContext recipes) {
        FluidStack hot = WerkstoffLoader.HotRutheniumTetroxideSollution.getFluidOrGas(9_000);
        FluidStack ruthenium = WerkstoffLoader.RutheniumTetroxide.getFluidOrGas(7_200);
        if (hot == null || ruthenium == null) throw new RecipeValidationException("Missing Ruthenium cooling fluids");
        RecipeMatch route = RecipeMatch.builder()
            .itemInputs()
            .itemOutputs(Materials.Salt.getDust(6))
            .fluidInputs(hot)
            .fluidOutputs(Materials.Water.getFluid(1_800), ruthenium)
            .duration(75 * SECONDS)
            .eut(TierEU.RECIPE_HV)
            .build();
        GTRecipe original = recipes.machine(distillationTowerRecipes)
            .requireExactlyOne(route);
        recipes.machine(ProgressionMachineNerf.COOLANT_RECIPES)
            .addCopyOne(original, route);
        recipes.machine(distillationTowerRecipes)
            .removeExactly(recipe -> recipe == original, 1);
        Predicate<GTRecipe> bypass = recipe -> Arrays.stream(recipe.mFluidInputs)
            .anyMatch(input -> input.isFluidEqual(hot));
        recipes.machine(distillationTowerRecipes)
            .removeExactly(bypass, 0);
        recipes.machine(distilleryRecipes)
            .removeExactly(bypass, 0);
    }

    private static void latexTransition(RecipeContext recipes) {
        Predicate<GTRecipe> production = recipe -> Arrays.stream(recipe.mOutputs)
            .anyMatch(ProgressionRecipeNerf::lateCable);
        List<GTRecipe> targets = assemblerRecipes.getAllRecipes()
            .stream()
            .filter(production)
            .collect(Collectors.toList());
        if (targets.isEmpty()) throw new RecipeValidationException("No EV+ insulated cable assembly recipes found");
        for (GTRecipe original : targets) {
            // Existing cables and their downstream uses are outside this insulation-production change.
            if (Arrays.stream(original.mInputs)
                .anyMatch(input -> {
                    MTECable cable = cablePrototype(input);
                    return cable != null && cable.mInsulated;
                }))
                throw new RecipeValidationException("Unexpected cable-to-cable Assembler conversion; review its scope");
            for (ItemStack output : original.mOutputs) {
                if (!lateCable(output)) continue;
                if (cableRecipes.getAllRecipes()
                    .stream()
                    .noneMatch(
                        recipe -> Arrays.stream(recipe.mOutputs)
                            .anyMatch(item -> item.isItemEqual(output))))
                    throw new RecipeValidationException("No LATEX production route for " + output);
            }
        }
        recipes.machine(assemblerRecipes)
            .removeExactly(production, targets.size());
        recipes.machine(assemblerRecipes)
            .removeExactly(production, 0);
        GTNHExtraNerf.LOG.info(
            "LATEX transition: removed {} EV+ cable Assembler variants; native cable-coater routes retained",
            targets.size());
    }

    private static boolean lateCable(ItemStack stack) {
        MTECable cable = cablePrototype(stack);
        return cable != null && cable.mInsulated && cable.mVoltage >= GTValues.V[4];
    }

    private static MTECable cablePrototype(ItemStack stack) {
        if (stack == null || stack.getItem() != Item.getItemFromBlock(GregTechAPI.sBlockMachines)) return null;
        int id = stack.getItemDamage();
        if (id < 0 || id >= GregTechAPI.METATILEENTITIES.length) return null;
        IMetaTileEntity prototype = GregTechAPI.METATILEENTITIES[id];
        return prototype instanceof MTECable ? (MTECable) prototype : null;
    }

    private static void europiumBoule(RecipeContext recipes) {
        ItemStack boule = ItemList.Circuit_Silicon_Ingot4.get(1);
        ItemStack dust = MaterialsFluorides.NEPTUNIUM_HEXAFLUORIDE.getDust(2);
        if (dust == null) throw new RecipeValidationException("Missing Neptunium Hexafluoride dust");
        List<GTRecipe> targets = blastFurnaceRecipes.getAllRecipes()
            .stream()
            .filter(
                recipe -> Arrays.stream(recipe.mOutputs)
                    .anyMatch(output -> output.isItemEqual(boule)))
            .collect(Collectors.toList());
        if (targets.size() != 8)
            throw new RecipeValidationException("EBF: expected 8 Europium boule variants, observed " + targets.size());
        for (GTRecipe original : targets) recipes.machine(blastFurnaceRecipes)
            .modifyKnownOne(
                original,
                RecipeChange.builder()
                    .addItemInput(dust)
                    .build());
    }

    private static void solderStar(RecipeContext recipes) {
        FluidStack cryotheum = FluidRegistry.getFluidStack("cryotheum", 4_000);
        FluidStack solder = MaterialMisc.MUTATED_LIVING_SOLDER.getFluidStack(40_320);
        if (cryotheum == null || solder == null)
            throw new RecipeValidationException("Missing Cryotheum or Mutated Living Solder");
        Predicate<GTRecipe> original = RecipeMatch.builder()
            .itemInputs(
                GregtechItemList.InfiniteMutationCatalyst.get(0),
                ItemList.Circuit_Chip_Biocell.get(64),
                ItemList.Gravistar.get(8),
                Materials.InfinityCatalyst.getDust(2))
            .itemOutputs()
            .fluidInputs(Materials.Tin.getPlasma(18_000), Materials.Bismuth.getPlasma(18_000), cryotheum)
            .fluidOutputs(solder)
            .duration(800 * SECONDS)
            .eut(3_842_160)
            .build()
            .and(recipe -> recipe.getMetadataOrDefault(GTRecipeConstants.CHEMPLANT_CASING_TIER, 0) == 7);
        recipes.machine(chemicalPlantRecipes)
            .modifyOne(
                original,
                RecipeChange.builder()
                    .replaceItemInput(ItemList.Gravistar.get(8), ItemList.NuclearStar.get(8))
                    .build());
    }

    private static void bioMineralMix(RecipeContext recipes) {
        FluidStack medium = Materials.BioMediumRaw.getFluid(1);
        ItemStack dust = MaterialsOres.DEEP_EARTH_REACTOR_FUEL_DEPOSIT.getDust(1);
        if (medium == null || dust == null)
            throw new RecipeValidationException("Missing Raw Bio Medium or Mineral Mix dust");
        List<GTRecipe> targets = bacterialVatRecipes.getAllRecipes()
            .stream()
            .filter(
                recipe -> Arrays.stream(recipe.mFluidOutputs)
                    .anyMatch(output -> output.isFluidEqual(medium)))
            .collect(Collectors.toList());
        if (targets.size() != 3) throw new RecipeValidationException(
            "Bio Vat: expected 3 Raw Bio Medium variants, observed " + targets.size());
        for (GTRecipe original : targets) recipes.machine(bacterialVatRecipes)
            .modifyKnownOne(
                original,
                RecipeChange.builder()
                    .addItemInput(dust)
                    .build());
    }

    private static void pcbPlasma(RecipeContext recipes) {
        pcbBoardPlasma(recipes, ItemList.Circuit_Board_Wetware_Extreme.get(1), 12);
        pcbBoardPlasma(recipes, ItemList.Circuit_Board_Bio_Ultra.get(1), 9);
    }

    private static void pcbBoardPlasma(RecipeContext recipes, ItemStack board, int expected) {
        List<GTRecipe> targets = pcbFactoryRecipes.getAllRecipes()
            .stream()
            .filter(
                recipe -> Arrays.stream(recipe.mOutputs)
                    .anyMatch(output -> output.isItemEqual(board)))
            .collect(Collectors.toList());
        if (targets.size() != expected) throw new RecipeValidationException(
            "PCB Factory: expected " + expected + " board variants for " + board + ", observed " + targets.size());
        for (GTRecipe original : targets) {
            FluidStack acid = Arrays.stream(original.mFluidInputs)
                .filter(input -> input.isFluidEqual(Materials.SulfuricAcid.getFluid(1)))
                .findFirst()
                .orElseThrow(() -> new RecipeValidationException("PCB Factory board lost its acid input"));
            FluidStack plasma = Materials.Copper.getPlasma(acid.amount);
            if (plasma == null) throw new RecipeValidationException("Missing Copper plasma");
            recipes.machine(pcbFactoryRecipes)
                .modifyKnownOne(
                    original,
                    RecipeChange.builder()
                        .replaceFluidInput(acid, plasma)
                        .build());
        }
    }

    private static void americiumBoule(RecipeContext recipes) {
        Predicate<GTRecipe> scope = RecipeMatch.builder()
            .itemOutputs(ItemList.Circuit_Silicon_Ingot5.get(1))
            .build();
        RecipeMatch oganesson = RecipeMatch.builder()
            .itemInputs(
                item(OrePrefixes.block, Materials.SiliconSG, 64),
                ItemList.GalliumArsenideCrystal.get(4),
                item(OrePrefixes.ingot, Materials.Americium, 4),
                GTUtility.getIntegratedCircuit(3))
            .itemOutputs(ItemList.Circuit_Silicon_Ingot5.get(1))
            .fluidInputs(WerkstoffLoader.Oganesson.getFluidOrGas(1_600))
            .fluidOutputs()
            .duration(315 * SECONDS)
            .eut(TierEU.RECIPE_LuV)
            .specialValue(9000)
            .build();
        recipes.machine(blastFurnaceRecipes)
            .retainExactly(scope, oganesson, 1, 7);
        ItemStack dust = MaterialsElements.getInstance().PROTACTINIUM.getDust(2);
        if (dust == null) throw new RecipeValidationException("Missing Protactinium dust");
        recipes.machine(blastFurnaceRecipes)
            .modifyOne(
                oganesson,
                RecipeChange.builder()
                    .addItemInput(dust)
                    .build());
    }

    private static void hullInsulation(RecipeContext recipes) {
        hullFluid(
            recipes,
            ItemList.Hull_UV,
            ItemList.Casing_UV,
            OrePrefixes.cableGt04,
            Materials.NaquadahAlloy,
            Materials.Polybenzimidazole,
            Materials.Kevlar,
            288);
        hullFluid(
            recipes,
            ItemList.Hull_MAX,
            ItemList.Casing_MAX,
            OrePrefixes.wireGt04,
            Materials.SuperconductorUV,
            Materials.Polybenzimidazole,
            Materials.Kevlar,
            288);
        hullFluid(
            recipes,
            ItemList.Hull_UEV,
            ItemList.Casing_UEV,
            OrePrefixes.cableGt08,
            Materials.Draconium,
            Materials.Polybenzimidazole,
            Materials.RadoxPolymer,
            576);
        hullFluid(
            recipes,
            ItemList.Hull_UIV,
            ItemList.Casing_UIV,
            OrePrefixes.cableGt08,
            Materials.NetherStar,
            Materials.Polybenzimidazole,
            Materials.RadoxPolymer,
            576);
        hullFluid(
            recipes,
            ItemList.Hull_UMV,
            ItemList.Casing_UMV,
            OrePrefixes.cableGt12,
            Materials.Quantium,
            Materials.Polybenzimidazole,
            Materials.RadoxPolymer,
            576);
        hullFluid(
            recipes,
            ItemList.Hull_UXV,
            ItemList.Casing_UXV,
            OrePrefixes.cableGt16,
            Materials.BlackPlutonium,
            Materials.Kevlar,
            Materials.RadoxPolymer,
            576);
        hullCrafting(
            recipes,
            ItemList.Hull_UV,
            ItemList.Casing_UV,
            Materials.Osmium,
            OrePrefixes.cableGt04,
            Materials.NaquadahAlloy,
            OrePrefixes.plate,
            Materials.Polybenzimidazole,
            Materials.Kevlar);
        hullCrafting(
            recipes,
            ItemList.Hull_MAX,
            ItemList.Casing_MAX,
            Materials.Neutronium,
            OrePrefixes.wireGt04,
            Materials.SuperconductorUV,
            OrePrefixes.plate,
            Materials.Polybenzimidazole,
            Materials.Kevlar);
        hullCrafting(
            recipes,
            ItemList.Hull_UEV,
            ItemList.Casing_UEV,
            Materials.Infinity,
            OrePrefixes.cableGt08,
            Materials.Draconium,
            OrePrefixes.plateDouble,
            Materials.Polybenzimidazole,
            Materials.RadoxPolymer);
        hullCrafting(
            recipes,
            ItemList.Hull_UIV,
            ItemList.Casing_UIV,
            Materials.TranscendentMetal,
            OrePrefixes.cableGt08,
            Materials.NetherStar,
            OrePrefixes.plateDouble,
            Materials.Polybenzimidazole,
            Materials.RadoxPolymer);
        hullCrafting(
            recipes,
            ItemList.Hull_UMV,
            ItemList.Casing_UMV,
            Materials.SpaceTime,
            OrePrefixes.cableGt12,
            Materials.Quantium,
            OrePrefixes.plateDouble,
            Materials.Polybenzimidazole,
            Materials.RadoxPolymer);
        hullCrafting(
            recipes,
            ItemList.Hull_UXV,
            ItemList.Casing_UXV,
            Materials.MHDCSM,
            OrePrefixes.cableGt16,
            Materials.BlackPlutonium,
            OrePrefixes.plateDense,
            Materials.Kevlar,
            Materials.RadoxPolymer);
        // Native recycling includes casing and cables only, excluding extra hull insulation to prevent duplication.
    }

    private static void hullFluid(RecipeContext recipes, ItemList hull, ItemList casing, OrePrefixes cable,
        Materials conductor, Materials original, Materials replacement, int amount) {
        FluidStack insulation = replacement.getMolten(amount);
        if (insulation == null) throw new RecipeValidationException("Missing molten hull insulation: " + replacement);
        recipes.machine(assemblerRecipes)
            .modifyOne(
                RecipeMatch.builder()
                    .itemInputs(item(cable, conductor, 2), casing.get(1))
                    .itemOutputs(hull.get(1))
                    .fluidInputs(original.getMolten(amount))
                    .fluidOutputs()
                    .duration(50)
                    .eut(TierEU.RECIPE_LV / 2)
                    .build(),
                RecipeChange.builder()
                    .fluidInputs(insulation)
                    .build());
    }

    private static void hullCrafting(RecipeContext recipes, ItemList hull, ItemList casing, Materials metal,
        OrePrefixes cable, Materials conductor, OrePrefixes plate, Materials oldPolymer, Materials newPolymer) {
        CraftingDefinition original = CraftingRecipeBuilder.shaped()
            .pattern("PHP", "CMC")
            .ingredient(
                'P',
                plate.get(oldPolymer)
                    .toString())
            .ingredient(
                'H',
                OrePrefixes.plate.get(metal)
                    .toString())
            .ingredient(
                'C',
                cable.get(conductor)
                    .toString())
            .ingredient('M', casing.get(1))
            .output(hull.get(1))
            .matchType(CraftingRecipeType.GT_SHAPED)
            .removable(false)
            .build();
        recipes.crafting()
            .expectOutputCount(hull.get(1), 1);
        item(plate, newPolymer, 1);
        recipes.crafting()
            .replaceOne(
                original,
                original.toBuilder()
                    .ingredient(
                        'P',
                        plate.get(newPolymer)
                            .toString())
                    .build());
    }

    private static void naquadahWater(RecipeContext recipes) {
        addWater(
            recipes,
            blastFurnaceRecipes,
            RecipeMatch.builder()
                .itemInputs(GGMaterial.naquadahine.get(OrePrefixes.dust, 3), Materials.Carbon.getDust(1))
                .itemOutputs(item(OrePrefixes.ingotHot, Materials.Naquadah, 1))
                .fluidInputs()
                .fluidOutputs(Materials.CarbonDioxide.getGas(1_000))
                .duration(2 * SECONDS)
                .eut(TierEU.RECIPE_IV)
                .specialValue(5000)
                .build(),
            Materials.Grade1PurifiedWater);
        addWater(
            recipes,
            blastFurnaceRecipes,
            RecipeMatch.builder()
                .itemInputs(GGMaterial.enrichedNaquadahSulphate.get(OrePrefixes.dust, 11), Materials.Zinc.getDust(2))
                .itemOutputs(
                    item(OrePrefixes.ingotHot, Materials.NaquadahEnriched, 1),
                    WerkstoffLoader.ZincSulfate.get(OrePrefixes.dust, 12))
                .fluidInputs()
                .fluidOutputs()
                .duration(5 * SECONDS)
                .eut(TierEU.RECIPE_IV)
                .specialValue(7500)
                .build(),
            Materials.Grade2PurifiedWater);
        addWater(
            recipes,
            blastFurnaceRecipes,
            RecipeMatch.builder()
                .itemInputs(GGMaterial.naquadriaSulphate.get(OrePrefixes.dust, 11), Materials.Magnesium.getDust(2))
                .itemOutputs(
                    item(OrePrefixes.ingotHot, Materials.Naquadria, 1),
                    GGMaterial.magnesiumSulphate.get(OrePrefixes.dust, 12))
                .fluidInputs()
                .fluidOutputs()
                .duration(5 * SECONDS)
                .eut(TierEU.RECIPE_ZPM)
                .specialValue(9100)
                .build(),
            Materials.Grade3PurifiedWater);

        RecipeMatch reduction = RecipeMatch.builder()
            .itemInputs(GGMaterial.naquadahine.get(OrePrefixes.dust, 3), Materials.Sodium.getDust(4))
            .itemOutputs(Materials.Naquadah.getDust(1), Materials.SodiumOxide.getDust(6))
            .fluidInputs()
            .fluidOutputs()
            .duration(5 * SECONDS)
            .eut(TierEU.RECIPE_EV)
            .build();
        addWater(recipes, chemicalReactorRecipes, reduction, Materials.Grade1PurifiedWater);
        addWater(recipes, multiblockChemicalReactorRecipes, reduction, Materials.Grade1PurifiedWater);
        sulphateElectrolysis(
            recipes,
            GGMaterial.enrichedNaquadahSulphate,
            Materials.NaquadahEnriched,
            Materials.Grade2PurifiedWater);
        sulphateElectrolysis(recipes, GGMaterial.naquadriaSulphate, Materials.Naquadria, Materials.Grade3PurifiedWater);
    }

    private static void sulphateElectrolysis(RecipeContext recipes, Werkstoff intermediate, Materials metal,
        Materials water) {
        // BartWorks generates these from material composition; GT++ copies them before our late edits.
        RecipeMatch decomposition = RecipeMatch.builder()
            .itemInputs(intermediate.get(OrePrefixes.dust, 11))
            .itemOutputs(metal.getDust(1), Materials.Sulfur.getDust(2))
            .fluidInputs()
            .fluidOutputs(Materials.Oxygen.getGas(8_000))
            .build();
        addWater(recipes, electrolyzerRecipes, decomposition, water);
        addWater(recipes, electrolyzerNonCellRecipes, decomposition, water);
    }

    private static void addWater(RecipeContext recipes, RecipeMap<?> map, RecipeMatch original, Materials water) {
        FluidStack input = water.getFluid(1_000);
        if (input == null) throw new RecipeValidationException("Missing purified water: " + water);
        recipes.machine(map)
            .modifyOne(
                original,
                RecipeChange.builder()
                    .fluidInputs(input)
                    .build());
    }

    private static ItemStack item(OrePrefixes prefix, Materials material, int amount) {
        ItemStack stack = GTOreDictUnificator.get(prefix, material, amount);
        if (stack == null) throw new RecipeValidationException("Missing material form: " + prefix + material);
        return stack;
    }

    private static void ozonation(RecipeContext recipes) {
        recipes.machine(purificationOzonationRecipes)
            .requireExactlyOne(ozonationVariant(128_000, 20.0f));
        recipes.machine(purificationOzonationRecipes)
            .requireExactlyOne(ozonationVariant(256_000, 40.0f));
        recipes.machine(purificationOzonationRecipes)
            .requireExactlyOne(ozonationVariant(512_000, 60.0f));
        recipes.machine(purificationOzonationRecipes)
            .modifyOne(
                ozonationVariant(1_024_000, 80.0f),
                RecipeChange.builder()
                    .fluidInputAmount(Materials.Ozone.getGas(1_024_000), ProgressionMachineNerf.OZONE_LIMIT)
                    .build());
    }

    private static Predicate<GTRecipe> ozonationVariant(int ozone, float chance) {
        return RecipeMatch.builder()
            .itemInputs()
            .itemOutputs(Materials.Manganese.getDust(1), Materials.Iron.getDust(1), Materials.Sulfur.getDust(1))
            .fluidInputs(Materials.Grade1PurifiedWater.getFluid(1_000), Materials.Ozone.getGas(ozone))
            .fluidOutputs(Materials.Grade2PurifiedWater.getFluid(900))
            .outputChances(500, 500, 500)
            .duration(MTEPurificationPlant.CYCLE_TIME_TICKS)
            .eut(TierEU.RECIPE_LuV)
            .build()
            .and(recipe -> recipe.getMetadataOrDefault(PurificationPlantBaseChanceKey.INSTANCE, 0.0f) == chance);
    }

    private static void flocculationWaste(RecipeContext recipes) {
        recipes.machine(distillationTowerRecipes)
            .removeExactly(
                RecipeMatch.builder()
                    .itemInputs()
                    .itemOutputs(Materials.Aluminium.getDust(20))
                    .fluidInputs(Materials.FlocculationWasteLiquid.getFluid(10_000))
                    .fluidOutputs(Materials.Oxygen.getGas(30_000), Materials.HydrochloricAcid.getFluid(30_000))
                    .duration(1 * SECONDS)
                    .eut(TierEU.RECIPE_EV)
                    .build(),
                1);
        Predicate<GTRecipe> wasteInput = recipe -> {
            for (FluidStack input : recipe.mFluidInputs)
                if (input.isFluidEqual(Materials.FlocculationWasteLiquid.getFluid(1))) return true;
            return false;
        };
        // Check the staged result and reject a newly added distillery shortcut as well.
        recipes.machine(distillationTowerRecipes)
            .removeExactly(wasteInput, 0);
        recipes.machine(distilleryRecipes)
            .removeExactly(wasteInput, 0);
    }
}
