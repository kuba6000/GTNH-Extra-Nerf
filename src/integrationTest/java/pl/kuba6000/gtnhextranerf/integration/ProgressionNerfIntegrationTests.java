package pl.kuba6000.gtnhextranerf.integration;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import bartworks.API.recipe.BartWorksRecipeMaps;
import bartworks.system.material.Werkstoff;
import bartworks.system.material.WerkstoffLoader;
import goodgenerator.blocks.tileEntity.MTECoolantTower;
import goodgenerator.items.GGMaterial;
import goodgenerator.util.ItemRefer;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.interfaces.tileentity.IVoidable;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
import gtPlusPlus.core.material.MaterialMisc;
import gtPlusPlus.core.material.MaterialsElements;
import gtPlusPlus.core.material.MaterialsOres;
import gtPlusPlus.core.material.nuclear.MaterialsFluorides;
import tectech.recipe.TecTechRecipeMaps;

/** Exercises final native recipe lookup, including routes copied into GT++ maps. */
final class ProgressionNerfIntegrationTests {

    private ProgressionNerfIntegrationTests() {}

    static void verify() {
        verifyPurification(GGMaterial.naquadahine, Materials.Naquadah, Materials.Grade1PurifiedWater);
        verifyPurification(
            GGMaterial.enrichedNaquadahSulphate,
            Materials.NaquadahEnriched,
            Materials.Grade2PurifiedWater);
        verifyPurification(GGMaterial.naquadriaSulphate, Materials.Naquadria, Materials.Grade3PurifiedWater);
        verifyHullInsulation();
        verifyAmericiumBoule();
        verifyPcbPlasma();
        verifyBioMineralMix();
        verifySolderStars();
        verifyEuropiumBoule();
        verifyLatexTransition();
        verifyCoolantRecipeMap();
        verifyAmplifiedMatterMaps();
    }

    private static void verifyAmplifiedMatterMaps() {
        boolean paid = false;
        for (GTRecipe recipe : RecipeMaps.multiblockMassFabricatorRecipes.getAllRecipes()) {
            if (Arrays.stream(recipe.mFluidOutputs)
                .noneMatch(fluid -> fluid.isFluidEqual(Materials.UUMatter.getFluid(1)))) continue;
            paid = true;
            check(
                Arrays.stream(recipe.mFluidInputs)
                    .anyMatch(fluid -> fluid.isFluidEqual(Materials.UUAmplifier.getFluid(1)) && fluid.amount > 0),
                "Matter Fabrication CPU still makes UU without amplifier");
            check(
                RecipeMaps.multiblockMassFabricatorRecipes.findRecipeQuery()
                    .items(copy(recipe.mInputs))
                    .fluids()
                    .voltage(recipe.mEUt)
                    .find() == null,
                "CPU lookup permits an unpaid UU batch");
            FluidStack[] fluids = Arrays.stream(recipe.mFluidInputs)
                .map(FluidStack::copy)
                .toArray(FluidStack[]::new);
            check(recipe.isRecipeInputEqual(true, fluids, copy(recipe.mInputs)), "Amplified CPU batch was rejected");
            for (FluidStack fluid : fluids) if (fluid.isFluidEqual(Materials.UUAmplifier.getFluid(1)))
                check(fluid.amount == 0, "CPU did not consume amplifier");
        }
        check(paid, "Amplified CPU production disappeared");
        check(
            RecipeMaps.fluidExtractionRecipes.getAllRecipes()
                .stream()
                .noneMatch(
                    recipe -> Arrays.stream(recipe.mFluidOutputs)
                        .anyMatch(fluid -> fluid.isFluidEqual(Materials.UUMatter.getFluid(1)))),
            "A Fluid Extractor still produces UU-Matter");
        check(
            RecipeMaps.fluidExtractionRecipes.getAllRecipes()
                .stream()
                .anyMatch(
                    recipe -> Arrays.stream(recipe.mFluidOutputs)
                        .anyMatch(fluid -> fluid.isFluidEqual(Materials.UUAmplifier.getFluid(1)))),
            "UUA Berry extraction disappeared");
        check(
            RecipeMaps.multiblockMassFabricatorRecipes.getAllRecipes()
                .stream()
                .anyMatch(recipe -> contains(recipe.mInputs, ItemList.IC2_Scrap.get(1))),
            "Scrap amplifier production disappeared");
        for (GTRecipe recipe : RecipeMaps.massFabFakeRecipes.getAllRecipes()) {
            if (Arrays.stream(recipe.mFluidOutputs)
                .noneMatch(fluid -> fluid.isFluidEqual(Materials.UUMatter.getFluid(1)))) continue;
            check(
                Arrays.stream(recipe.mFluidInputs)
                    .anyMatch(fluid -> fluid.isFluidEqual(Materials.UUAmplifier.getFluid(1))),
                "Singleblock NEI still advertises unamplified UU production");
        }
    }

    private static void verifyCoolantRecipeMap() {
        MTECoolantTower tower = (MTECoolantTower) GregTechAPI.METATILEENTITIES[ItemRefer.Coolant_Tower.get(1)
            .getItemDamage()];
        RecipeMap<?> map = tower.getRecipeMap();
        check(map != null, "Coolant Tower exposes no ordinary recipe map");
        FluidStack hot = WerkstoffLoader.HotRutheniumTetroxideSollution.getFluidOrGas(9_000);
        GTRecipe recipe = map.findRecipeQuery()
            .items()
            .fluids(hot)
            .voltage(gregtech.api.enums.TierEU.RECIPE_HV)
            .find();
        check(recipe != null, "Ruthenium separation is undiscoverable in the Coolant Tower map");
        check(contains(recipe.mOutputs, Materials.Salt.getDust(1)), "Cooling lost the Salt byproduct");
        check(
            Arrays.stream(recipe.mFluidOutputs)
                .anyMatch(fluid -> fluid.isFluidEqual(Materials.Water.getFluid(1)))
                && Arrays.stream(recipe.mFluidOutputs)
                    .anyMatch(fluid -> fluid.isFluidEqual(WerkstoffLoader.RutheniumTetroxide.getFluidOrGas(1))),
            "Cooling lost one of the two fluid outputs");
        check(recipe.getRecipeCategory().recipeMap == map, "Moved recipe still belongs to its old map category");
        FluidStack paid = hot.copy();
        check(
            recipe.isRecipeInputEqual(true, new FluidStack[] { paid }, new ItemStack[] {}) && paid.amount == 0,
            "Mapped cooling does not consume the hot solution");
        for (RecipeMap<?> old : Arrays.asList(RecipeMaps.distillationTowerRecipes, RecipeMaps.distilleryRecipes)) check(
            old.getAllRecipes()
                .stream()
                .noneMatch(
                    candidate -> Arrays.stream(candidate.mFluidInputs)
                        .anyMatch(fluid -> fluid.isFluidEqual(hot))),
            "Hot Ruthenium still bypasses the Coolant Tower");
    }

    private static void verifyLatexTransition() {
        OrePrefixes[] widths = { OrePrefixes.cableGt01, OrePrefixes.cableGt02, OrePrefixes.cableGt04,
            OrePrefixes.cableGt08, OrePrefixes.cableGt12, OrePrefixes.cableGt16 };
        for (OrePrefixes width : widths) {
            ItemStack hv = GTOreDictUnificator.get(width, Materials.Gold, 1);
            check(
                RecipeMaps.assemblerRecipes.getAllRecipes()
                    .stream()
                    .anyMatch(recipe -> contains(recipe.mOutputs, hv)),
                "HV cable assembly was removed based on its width");
            for (Materials metal : new Materials[] { Materials.Aluminium, Materials.Tungsten }) {
                ItemStack cable = GTOreDictUnificator.get(width, metal, 1);
                check(
                    RecipeMaps.assemblerRecipes.getAllRecipes()
                        .stream()
                        .noneMatch(recipe -> contains(recipe.mOutputs, cable)),
                    "A higher-rated cable can still be assembled without LATEX: " + width + metal);
                check(
                    RecipeMaps.cableRecipes.getAllRecipes()
                        .stream()
                        .anyMatch(recipe -> contains(recipe.mOutputs, cable)),
                    "LATEX lost the higher-rated cable route: " + width + metal);
            }
        }
        ItemStack wire = GTOreDictUnificator.get(OrePrefixes.wireGt01, Materials.Aluminium, 1);
        check(
            RecipeMaps.wiremillRecipes.getAllRecipes()
                .stream()
                .anyMatch(recipe -> contains(recipe.mOutputs, wire)),
            "The insulation gate also removed bare wire production");
        check(
            RecipeMaps.assemblerRecipes.getAllRecipes()
                .stream()
                .anyMatch(
                    recipe -> contains(
                        recipe.mInputs,
                        GTOreDictUnificator.get(OrePrefixes.cableGt08, Materials.Draconium, 1))),
            "The insulation gate also removed a machine recipe consuming cables");
    }

    private static void verifyEuropiumBoule() {
        ItemStack boule = ItemList.Circuit_Silicon_Ingot4.get(1);
        ItemStack dust = MaterialsFluorides.NEPTUNIUM_HEXAFLUORIDE.getDust(1);
        boolean found = false;
        for (GTRecipe recipe : RecipeMaps.blastFurnaceRecipes.getAllRecipes()) {
            if (!contains(recipe.mOutputs, boule)) continue;
            found = true;
            verifyConsumedIngredient(RecipeMaps.blastFurnaceRecipes, recipe, dust);
        }
        check(found, "Europium boule production disappeared");
        check(
            RecipeMaps.cutterRecipes.getAllRecipes()
                .stream()
                .anyMatch(recipe -> contains(recipe.mInputs, boule)),
            "Europium boule can no longer be cut into wafers");
    }

    private static void verifySolderStars() {
        GTRecipe firstStar = RecipeMaps.autoclaveRecipes.getAllRecipes()
            .stream()
            .filter(
                recipe -> contains(recipe.mOutputs, ItemList.NuclearStar.get(1))
                    && contains(recipe.mInputs, ItemList.Gravistar.get(1)))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Missing initial Nuclear Star production"));
        // Multi Autoclave inherits native multi-hatch power handling and allows one higher recipe tier.
        ProcessingLogic autoclave = new ProcessingLogic().setRecipeMapSupplier(() -> RecipeMaps.autoclaveRecipes)
            .setMachine(
                (IVoidable) GregTechAPI.METATILEENTITIES[ItemList.Machine_Multi_Autoclave.get(1)
                    .getItemDamage()])
            .setAvailableVoltage(GTValues.V[9])
            .setAvailableAmperage(4)
            .setAmperageOC(true)
            .setInputItems(copy(firstStar.mInputs))
            .setInputFluids(
                Arrays.stream(firstStar.mFluidInputs)
                    .map(FluidStack::copy)
                    .toArray(FluidStack[]::new));
        check(
            autoclave.process()
                .wasSuccessful(),
            "Initial Nuclear Star cannot be made with multi-amp UHV power");
        check(
            contains(autoclave.getOutputItems(), ItemList.NuclearStar.get(1)),
            "Multi-amp UHV processing did not produce a Nuclear Star");
        FluidStack solder = MaterialMisc.MUTATED_LIVING_SOLDER.getFluidStack(1);
        boolean found = false;
        for (GTRecipe recipe : RecipeMaps.chemicalPlantRecipes.getAllRecipes()) {
            if (Arrays.stream(recipe.mFluidOutputs)
                .noneMatch(fluid -> fluid.isFluidEqual(solder))) continue;
            found = true;
            check(!contains(recipe.mInputs, ItemList.Gravistar.get(1)), "Gravistar still bypasses the solder upgrade");
            verifyConsumedIngredient(RecipeMaps.chemicalPlantRecipes, recipe, ItemList.NuclearStar.get(1));
        }
        check(found, "Chemical Plant solder production disappeared");
        check(
            RecipeMaps.quantumForceTransformerRecipes.getAllRecipes()
                .stream()
                .anyMatch(
                    recipe -> Arrays.stream(recipe.mFluidOutputs)
                        .anyMatch(fluid -> fluid.isFluidEqual(solder))
                        && !contains(recipe.mInputs, ItemList.NuclearStar.get(1))),
            "Independent QFT solder production was also gated");
    }

    private static void verifyBioMineralMix() {
        ItemStack dust = MaterialsOres.DEEP_EARTH_REACTOR_FUEL_DEPOSIT.getSmallDust(1);
        boolean found = false;
        for (GTRecipe recipe : BartWorksRecipeMaps.bacterialVatRecipes.getAllRecipes()) {
            if (Arrays.stream(recipe.mFluidOutputs)
                .noneMatch(fluid -> fluid.isFluidEqual(Materials.BioMediumRaw.getFluid(1)))) continue;
            found = true;
            check(recipe.mSpecialItems instanceof ItemStack, "Bio Vat lost its culture requirement");
            verifyConsumedIngredient(BartWorksRecipeMaps.bacterialVatRecipes, recipe, dust);
        }
        check(found, "Raw Bio Catalyst Medium production disappeared");
    }

    private static void verifyPcbPlasma() {
        for (ItemList board : new ItemList[] { ItemList.Circuit_Board_Wetware_Extreme,
            ItemList.Circuit_Board_Bio_Ultra }) {
            boolean found = false;
            for (GTRecipe recipe : RecipeMaps.pcbFactoryRecipes.getAllRecipes()) {
                if (!contains(recipe.mOutputs, board.get(1))) continue;
                found = true;
                check(
                    Arrays.stream(recipe.mFluidInputs)
                        .anyMatch(fluid -> fluid.isFluidEqual(Materials.Copper.getPlasma(1))),
                    "PCB Factory high-end board lacks Copper plasma");
                check(
                    Arrays.stream(recipe.mFluidInputs)
                        .noneMatch(fluid -> fluid.isFluidEqual(Materials.SulfuricAcid.getFluid(1))),
                    "PCB Factory still uses sulfuric acid for wet/bio boards");
                FluidStack[] fluids = Arrays.stream(recipe.mFluidInputs)
                    .map(FluidStack::copy)
                    .toArray(FluidStack[]::new);
                for (int i = 0; i < fluids.length; i++) if (fluids[i].isFluidEqual(Materials.Copper.getPlasma(1)))
                    fluids[i] = Materials.SulfuricAcid.getFluid(fluids[i].amount);
                check(
                    RecipeMaps.pcbFactoryRecipes.findRecipeQuery()
                        .items(copy(recipe.mInputs))
                        .fluids(fluids)
                        .voltage(recipe.mEUt)
                        .find() == null,
                    "Old acid still bypasses the PCB plasma gate");
                check(
                    RecipeMaps.pcbFactoryRecipes.findRecipeQuery()
                        .items(copy(recipe.mInputs))
                        .fluids(recipe.mFluidInputs)
                        .voltage(recipe.mEUt)
                        .find() == recipe,
                    "Plasma recipe is absent from native PCB lookup");
            }
            check(found, "PCB Factory lost the gated board recipes");
            for (RecipeMap<?> map : Arrays
                .asList(RecipeMaps.chemicalReactorRecipes, RecipeMaps.multiblockChemicalReactorRecipes))
                check(
                    map.getAllRecipes()
                        .stream()
                        .anyMatch(
                            recipe -> contains(recipe.mOutputs, board.get(1)) && Arrays.stream(recipe.mFluidInputs)
                                .noneMatch(fluid -> fluid.isFluidEqual(Materials.Copper.getPlasma(1)))),
                    "The plasma gate removed a chemical board alternative");
        }
        for (ItemList base : new ItemList[] { ItemList.Circuit_Board_Wetware, ItemList.Circuit_Board_Bio }) check(
            RecipeMaps.circuitAssemblerRecipes.getAllRecipes()
                .stream()
                .anyMatch(
                    recipe -> contains(recipe.mOutputs, base.get(1)) && Arrays.stream(recipe.mFluidInputs)
                        .noneMatch(fluid -> fluid.isFluidEqual(Materials.Copper.getPlasma(1)))),
            "The plasma gate removed base board production outside PCB Factory");
        check(
            RecipeMaps.pcbFactoryRecipes.getAllRecipes()
                .stream()
                .anyMatch(
                    recipe -> !contains(recipe.mOutputs, ItemList.Circuit_Board_Wetware_Extreme.get(1))
                        && !contains(recipe.mOutputs, ItemList.Circuit_Board_Bio_Ultra.get(1))
                        && Arrays.stream(recipe.mFluidInputs)
                            .anyMatch(fluid -> fluid.isFluidEqual(Materials.SulfuricAcid.getFluid(1)))),
            "The PCB plasma gate also changed unrelated boards");
    }

    private static void verifyAmericiumBoule() {
        ItemStack boule = ItemList.Circuit_Silicon_Ingot5.get(1);
        ItemStack protactinium = MaterialsElements.getInstance().PROTACTINIUM.getDust(1);
        boolean found = false;
        for (GTRecipe recipe : RecipeMaps.blastFurnaceRecipes.getAllRecipes()) {
            if (!contains(recipe.mOutputs, boule)) continue;
            found = true;
            check(
                recipe.mFluidInputs.length == 1
                    && recipe.mFluidInputs[0].isFluidEqual(WerkstoffLoader.Oganesson.getFluidOrGas(1)),
                "Americium boule can still be baked without Oganesson");
            verifyConsumedIngredient(RecipeMaps.blastFurnaceRecipes, recipe, protactinium);
            FluidStack wrongGas = Materials.Nitrogen.getGas(recipe.mFluidInputs[0].amount);
            check(
                RecipeMaps.blastFurnaceRecipes.findRecipeQuery()
                    .items(copy(recipe.mInputs))
                    .fluids(wrongGas)
                    .voltage(recipe.mEUt)
                    .find() == null,
                "Another gas still bypasses the Americium boule gate");
        }
        check(found, "The Oganesson Americium boule route disappeared");
        List<GTRecipe> moltenBouleRecipes = TecTechRecipeMaps.godforgeMoltenRecipes.getAllRecipes()
            .stream()
            .filter(recipe -> contains(recipe.mOutputs, boule))
            .collect(Collectors.toList());
        check(moltenBouleRecipes.size() == 1, "Melting Core must have exactly one Americium boule recipe");
        GTRecipe moltenBoule = moltenBouleRecipes.get(0);
        ItemStack mineralMix = MaterialsOres.DEEP_EARTH_REACTOR_FUEL_DEPOSIT.getDust(1);
        check(!contains(moltenBoule.mInputs, protactinium), "Melting Core still requires Protactinium");
        check(
            Arrays.stream(moltenBoule.mInputs)
                .filter(input -> input != null && input.isItemEqual(mineralMix))
                .mapToInt(input -> input.stackSize)
                .sum() == 1,
            "Melting Core must require one Radioactive Mineral Mix dust");
        check(
            moltenBoule.mFluidInputs.length == 1
                && moltenBoule.mFluidInputs[0].isFluidEqual(WerkstoffLoader.Oganesson.getFluidOrGas(1)),
            "Melting Core must still require Oganesson");
        verifyConsumedIngredient(TecTechRecipeMaps.godforgeMoltenRecipes, moltenBoule, mineralMix);
        check(
            RecipeMaps.cutterRecipes.getAllRecipes()
                .stream()
                .anyMatch(recipe -> contains(recipe.mInputs, boule)),
            "Americium boule can no longer be cut into wafers");
    }

    private static void verifyConsumedIngredient(RecipeMap<?> map, GTRecipe recipe, ItemStack ingredient) {
        check(contains(recipe.mInputs, ingredient), "Missing consumed ingredient in " + map.unlocalizedName);
        ItemStack[] unpaid = Arrays.stream(copy(recipe.mInputs))
            .filter(item -> !item.isItemEqual(ingredient))
            .toArray(ItemStack[]::new);
        check(
            map.findRecipeQuery()
                .items(unpaid)
                .fluids(recipe.mFluidInputs)
                .specialSlot(recipe.mSpecialItems instanceof ItemStack ? (ItemStack) recipe.mSpecialItems : null)
                .voltage(recipe.mEUt)
                .find() == null,
            "Recipe lookup permits a missing ingredient in " + map.unlocalizedName);
        check(
            !recipe.isRecipeInputEqual(
                true,
                Arrays.stream(recipe.mFluidInputs)
                    .map(FluidStack::copy)
                    .toArray(FluidStack[]::new),
                unpaid),
            "Native processing permits a missing ingredient in " + map.unlocalizedName);
        ItemStack[] paid = copy(recipe.mInputs);
        FluidStack[] fluids = Arrays.stream(recipe.mFluidInputs)
            .map(FluidStack::copy)
            .toArray(FluidStack[]::new);
        check(
            map.findRecipeQuery()
                .items(paid)
                .fluids(fluids)
                .specialSlot(recipe.mSpecialItems instanceof ItemStack ? (ItemStack) recipe.mSpecialItems : null)
                .voltage(recipe.mEUt)
                .find() == recipe,
            "Paid recipe is absent from native lookup in " + map.unlocalizedName);
        check(recipe.isRecipeInputEqual(true, fluids, paid), "Paid recipe was rejected in " + map.unlocalizedName);
        for (ItemStack item : paid) if (item.isItemEqual(ingredient))
            check(item.stackSize == 0, "Additional ingredient was not consumed in " + map.unlocalizedName);
    }

    private static void verifyHullInsulation() {
        verifyHullFluid(ItemList.Hull_UV, Materials.Kevlar, Materials.Polybenzimidazole);
        verifyHullFluid(ItemList.Hull_MAX, Materials.Kevlar, Materials.Polybenzimidazole);
        for (ItemList hull : new ItemList[] { ItemList.Hull_UEV, ItemList.Hull_UIV, ItemList.Hull_UMV,
            ItemList.Hull_UXV }) {
            verifyHullFluid(hull, Materials.RadoxPolymer, Materials.Polybenzimidazole);
            verifyHullFluid(hull, Materials.RadoxPolymer, Materials.Kevlar);
        }
        verifyHullFluid(ItemList.Hull_ZPM, Materials.Polybenzimidazole, Materials.Kevlar);
        verifyHullCrafting(
            ItemList.Hull_UV,
            ItemList.Casing_UV,
            Materials.Osmium,
            OrePrefixes.cableGt04,
            Materials.NaquadahAlloy,
            OrePrefixes.plate,
            Materials.Kevlar,
            Materials.Polybenzimidazole);
        verifyHullCrafting(
            ItemList.Hull_MAX,
            ItemList.Casing_MAX,
            Materials.Neutronium,
            OrePrefixes.wireGt04,
            Materials.SuperconductorUV,
            OrePrefixes.plate,
            Materials.Kevlar,
            Materials.Polybenzimidazole);
        verifyHullCrafting(
            ItemList.Hull_UEV,
            ItemList.Casing_UEV,
            Materials.Infinity,
            OrePrefixes.cableGt08,
            Materials.Draconium,
            OrePrefixes.plateDouble,
            Materials.RadoxPolymer,
            Materials.Polybenzimidazole);
        verifyHullCrafting(
            ItemList.Hull_UIV,
            ItemList.Casing_UIV,
            Materials.TranscendentMetal,
            OrePrefixes.cableGt08,
            Materials.NetherStar,
            OrePrefixes.plateDouble,
            Materials.RadoxPolymer,
            Materials.Polybenzimidazole);
        verifyHullCrafting(
            ItemList.Hull_UMV,
            ItemList.Casing_UMV,
            Materials.SpaceTime,
            OrePrefixes.cableGt12,
            Materials.Quantium,
            OrePrefixes.plateDouble,
            Materials.RadoxPolymer,
            Materials.Polybenzimidazole);
        verifyHullCrafting(
            ItemList.Hull_UXV,
            ItemList.Casing_UXV,
            Materials.MHDCSM,
            OrePrefixes.cableGt16,
            Materials.BlackPlutonium,
            OrePrefixes.plateDense,
            Materials.RadoxPolymer,
            Materials.Kevlar);
    }

    private static void verifyHullFluid(ItemList hull, Materials required, Materials obsolete) {
        GTRecipe found = null;
        for (GTRecipe recipe : RecipeMaps.assemblerRecipes.getAllRecipes()) {
            if (!contains(recipe.mOutputs, hull.get(1))) continue;
            check(found == null, "Ambiguous hull assembly: " + hull);
            found = recipe;
        }
        check(found != null, "Missing hull assembly: " + hull);
        check(
            found.mFluidInputs.length == 1 && found.mFluidInputs[0].isFluidEqual(required.getMolten(1)),
            "Hull does not require its tier's insulation: " + hull);
        FluidStack old = obsolete.getMolten(found.mFluidInputs[0].amount);
        check(
            RecipeMaps.assemblerRecipes.findRecipeQuery()
                .items(copy(found.mInputs))
                .fluids(old)
                .voltage(found.mEUt)
                .find() == null,
            "Obsolete insulation still assembles a hull: " + hull);
        FluidStack paid = found.mFluidInputs[0].copy();
        check(
            found.isRecipeInputEqual(true, new FluidStack[] { paid }, copy(found.mInputs)) && paid.amount == 0,
            "Hull assembly did not consume its insulation: " + hull);
        // Native hull recycling deliberately excludes the extra assembly insulation.
        for (gregtech.api.objects.MaterialStack component : GTOreDictUnificator.getItemData(hull.get(1))
            .getAllMaterialStacks()) {
            check(
                component.mMaterial != Materials.Kevlar && component.mMaterial != Materials.RadoxPolymer
                    && component.mMaterial != Materials.Polybenzimidazole,
                "Hull recycling can duplicate assembly insulation: " + hull);
        }
    }

    private static void verifyHullCrafting(ItemList hull, ItemList casing, Materials metal, OrePrefixes cablePrefix,
        Materials cableMaterial, OrePrefixes platePrefix, Materials required, Materials obsolete) {
        ItemStack polymer = GTOreDictUnificator.get(platePrefix, required, 1);
        ItemStack old = GTOreDictUnificator.get(platePrefix, obsolete, 1);
        ItemStack top = GTOreDictUnificator.get(OrePrefixes.plate, metal, 1);
        ItemStack cable = GTOreDictUnificator.get(cablePrefix, cableMaterial, 1);
        check(
            contains(
                new ItemStack[] { LvRecipeIntegrationTests
                    .craft(polymer, top, polymer, cable, casing.get(1), cable, null, null, null) },
                hull.get(1)),
            "New hull insulation is rejected by hand crafting: " + hull);
        check(
            !contains(
                new ItemStack[] {
                    LvRecipeIntegrationTests.craft(old, top, old, cable, casing.get(1), cable, null, null, null) },
                hull.get(1)),
            "Hand crafting bypasses new hull insulation: " + hull);
    }

    private static void verifyPurification(Werkstoff intermediate, Materials metal, Materials water) {
        verifyWaterGate(
            RecipeMaps.blastFurnaceRecipes,
            intermediate,
            GTOreDictUnificator.get(OrePrefixes.ingotHot, metal, 1),
            water);
        List<RecipeMap<?>> alternatives = intermediate == GGMaterial.naquadahine
            ? Arrays.asList(RecipeMaps.chemicalReactorRecipes, RecipeMaps.multiblockChemicalReactorRecipes)
            : Arrays.asList(RecipeMaps.electrolyzerRecipes, RecipeMaps.electrolyzerNonCellRecipes);
        for (RecipeMap<?> map : alternatives) verifyWaterGate(map, intermediate, metal.getDust(1), water);

        for (GTRecipe recipe : RecipeMaps.blastFurnaceRecipes.getAllRecipes()) {
            if (!contains(recipe.mInputs, metal.getDust(1))) continue;
            for (FluidStack input : recipe.mFluidInputs) {
                check(
                    !input.isFluidEqual(Materials.Grade1PurifiedWater.getFluid(1))
                        && !input.isFluidEqual(Materials.Grade2PurifiedWater.getFluid(1))
                        && !input.isFluidEqual(Materials.Grade3PurifiedWater.getFluid(1)),
                    "Pure metal dust smelting was charged purification water again");
            }
        }
    }

    private static void verifyWaterGate(RecipeMap<?> map, Werkstoff intermediate, ItemStack output, Materials water) {
        GTRecipe found = null;
        for (GTRecipe recipe : map.getAllRecipes()) {
            if (!contains(recipe.mInputs, intermediate.get(OrePrefixes.dust, 1)) || !contains(recipe.mOutputs, output))
                continue;
            check(found == null, "Ambiguous purification route in " + map.unlocalizedName);
            found = recipe;
        }
        check(found != null, "Missing purification route in " + map.unlocalizedName);
        check(
            found.mFluidInputs.length == 1 && found.mFluidInputs[0].isFluidEqual(water.getFluid(1))
                && found.mFluidInputs[0].amount > 0,
            "Intermediate conversion must consume the matching water grade in " + map.unlocalizedName);
        check(
            map.findRecipeQuery()
                .items(copy(found.mInputs))
                .fluids()
                .voltage(found.mEUt)
                .find() == null,
            "Intermediate conversion is still available without water in " + map.unlocalizedName);
        check(
            map.findRecipeQuery()
                .items(copy(found.mInputs))
                .fluids(found.mFluidInputs)
                .voltage(found.mEUt)
                .find() == found,
            "Water-gated conversion is missing from the machine lookup in " + map.unlocalizedName);
        FluidStack insufficient = found.mFluidInputs[0].copy();
        insufficient.amount--;
        check(
            map.findRecipeQuery()
                .items(copy(found.mInputs))
                .fluids(insufficient)
                .voltage(found.mEUt)
                .find() == null,
            "Intermediate conversion accepts insufficient water in " + map.unlocalizedName);
        check(
            !found.isRecipeInputEqual(true, new FluidStack[] {}, copy(found.mInputs)),
            "Native input consumption accepted a waterless batch");
        FluidStack paid = found.mFluidInputs[0].copy();
        check(
            found.isRecipeInputEqual(true, new FluidStack[] { paid }, copy(found.mInputs)) && paid.amount == 0,
            "Native processing did not consume the supplied purification water");
    }

    private static ItemStack[] copy(ItemStack[] items) {
        return Arrays.stream(items)
            .map(ItemStack::copy)
            .toArray(ItemStack[]::new);
    }

    private static boolean contains(ItemStack[] items, ItemStack target) {
        for (ItemStack item : items) if (item != null && item.isItemEqual(target)) return true;
        return false;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
