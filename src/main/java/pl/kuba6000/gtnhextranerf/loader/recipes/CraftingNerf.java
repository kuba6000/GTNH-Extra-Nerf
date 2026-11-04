package pl.kuba6000.gtnhextranerf.loader.recipes;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import com.dreammaster.scripts.IngredientFactory;

import bartworks.system.material.WerkstoffLoader;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.Mods;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.ToolDictNames;
import gregtech.api.util.GTOreDictUnificator;
import gtPlusPlus.core.material.MaterialsAlloy;
import pl.kuba6000.gtnhextranerf.audit.AuditScope;
import pl.kuba6000.gtnhextranerf.items.MagicnetizedManasteelRod;
import pl.kuba6000.gtnhextranerf.machines.RawMachineCasings;
import pl.kuba6000.gtnhextranerf.recipes.RecipeContext;
import pl.kuba6000.gtnhextranerf.recipes.RecipeItems;
import pl.kuba6000.gtnhextranerf.recipes.RecipeModule;
import pl.kuba6000.gtnhextranerf.recipes.RecipeRegistry;
import pl.kuba6000.gtnhextranerf.recipes.RecipeValidationException;
import pl.kuba6000.gtnhextranerf.recipes.crafting.CraftingDefinition;
import pl.kuba6000.gtnhextranerf.recipes.crafting.CraftingIngredient;
import pl.kuba6000.gtnhextranerf.recipes.crafting.CraftingRecipeBuilder;
import pl.kuba6000.gtnhextranerf.recipes.crafting.CraftingRecipeType;

public final class CraftingNerf implements RecipeModule {

    @Override
    public void register(RecipeRegistry registry) {
        registry.register(AuditScope.MODIFY_MV_MOTOR_HAND_CRAFTING, CraftingNerf::mvMotor);
        registry.register(AuditScope.ADD_LV_FLORAL_FERTILIZER, CraftingNerf::floralFertilizer);
        registry.register(AuditScope.REDUCE_BRONZE_DUST_HAND_CRAFTING_OUTPUT, CraftingNerf::bronzeDust);
        registry.register(AuditScope.MODIFY_LARGE_CHEMICAL_REACTOR_HAND_CRAFTING, CraftingNerf::largeChemicalReactor);
        registry.register(AuditScope.MODIFY_LV_CIRCUIT_ASSEMBLER_HAND_CRAFTING, CraftingNerf::lvCircuitAssembler);
        registry.register(AuditScope.MODIFY_FLINT_AND_STEEL_HAND_CRAFTING, CraftingNerf::flintAndSteel);
        registry.register(AuditScope.MODIFY_TOOL_STATION_SLAB_HAND_CRAFTING, CraftingNerf::toolStationSlab);
        registry.register(AuditScope.REPLACE_MACHINE_CASING_HAND_CRAFTING, CraftingNerf::machineCasings);
        registry.register(AuditScope.REMOVE_GOOD_CIRCUIT_HAND_CRAFTING, CraftingNerf::removeGoodCircuitHandCrafting);
        registry.register(AuditScope.MODIFY_DIAMOND_SAWBLADE_HAND_CRAFTING, CraftingNerf::diamondSawblade);
        registry
            .register(AuditScope.REMOVE_MV_ENERGY_HATCH_HAND_CRAFTING, CraftingNerf::removeMvEnergyHatchHandCrafting);
    }

    private static void floralFertilizer(RecipeContext recipes) {
        recipes.crafting()
            .addOne(
                CraftingRecipeBuilder.shaped()
                    .pattern("FAF", "FPF", "FAF")
                    .ingredient('P', RecipeItems.requireModItem(Mods.NewHorizonsCoreMod, "TwilightCrystal", 1, 0))
                    .ingredient('A', RecipeItems.requireModItem(Mods.Thaumcraft, "ItemResource", 1, 0))
                    .ingredient('F', RecipeItems.requireModItem(Mods.CropsNH, "fertilizer", 1, 0))
                    .output(RecipeItems.requireModItem(Mods.Botania, "fertilizer", 6, 0))
                    .build());
    }

    private static void mvMotor(RecipeContext recipes) {
        CraftingDefinition original = CraftingRecipeBuilder.shaped()
            .pattern("CWR", "WIW", "RWC")
            .ingredient('C', "cableGt01AnyCopper")
            .ingredient('W', "wireGt02Cupronickel")
            .ingredient('R', "stickAluminium")
            .ingredient('I', "stickSteelMagnetic")
            .output(ItemList.Electric_Motor_MV.get(1))
            .matchType(CraftingRecipeType.GT_SHAPED)
            .removable(false)
            .build();
        recipes.crafting()
            .replaceOne(
                original,
                original.toBuilder()
                    .ingredient('I', MagicnetizedManasteelRod.stack())
                    .build());
    }

    private static void bronzeDust(RecipeContext recipes) {
        ItemStack copper = ore(
            AuditScope.REDUCE_BRONZE_DUST_HAND_CRAFTING_OUTPUT,
            OrePrefixes.dust.get(Materials.AnyCopper)
                .toString());
        ItemStack tin = ore(
            AuditScope.REDUCE_BRONZE_DUST_HAND_CRAFTING_OUTPUT,
            OrePrefixes.dust.get(Materials.Tin)
                .toString());
        recipes.crafting()
            .outputAmount(
                CraftingRecipeBuilder.shapeless()
                    .inputs(copper, copper, copper, tin)
                    .output(GTOreDictUnificator.get(OrePrefixes.dust, Materials.Bronze, 3))
                    .matchType(CraftingRecipeType.GT_SHAPELESS)
                    .build(),
                1);
    }

    private static void flintAndSteel(RecipeContext recipes) {
        CraftingDefinition original = CraftingRecipeBuilder.universalShaped()
            .pattern(" G ", " F ", " S ")
            .ingredient('G', "gearGtSmallSteel")
            .ingredient('F', new ItemStack(Items.flint))
            .ingredient('S', "springSmallSteel")
            .output(new ItemStack(Items.flint_and_steel))
            .build();
        recipes.crafting()
            .replaceOne(
                original,
                original.toBuilder()
                    .ingredient('G', "gearSteel")
                    .ingredient('S', "springSteel")
                    .build());
    }

    private static void toolStationSlab(RecipeContext recipes) {
        AuditScope scope = AuditScope.MODIFY_TOOL_STATION_SLAB_HAND_CRAFTING;
        ItemStack slab = requiredItem(
            scope,
            IngredientFactory.getModItem(Mods.TinkerConstruct.ID, "CraftingSlab", 1, 1));
        ItemStack station = requiredItem(
            scope,
            IngredientFactory.getModItem(Mods.TinkerConstruct.ID, "ToolStationBlock", 1, 0));
        CraftingIngredient saw = CraftingIngredient.ore(ToolDictNames.craftingToolSaw.name());
        CraftingIngredient stationInput = CraftingIngredient.item(station);
        CraftingDefinition original = CraftingRecipeBuilder.universalShapeless()
            .ingredients(saw, stationInput)
            .output(slab)
            .matchType(CraftingRecipeType.UNIVERSAL_SHAPELESS)
            .build();
        recipes.crafting()
            .replaceOne(
                original,
                original.toBuilder()
                    .ingredients(
                        saw,
                        stationInput,
                        CraftingIngredient.ore(
                            OrePrefixes.circuit.get(Materials.LV)
                                .toString()))
                    .build());
    }

    private static void machineCasings(RecipeContext recipes) {
        machineCasing(recipes, Materials.CastIron, ItemList.Casing_ULV);
        machineCasing(recipes, Materials.Steel, ItemList.Casing_LV);
        machineCasing(recipes, Materials.Aluminium, ItemList.Casing_MV);
        machineCasing(recipes, Materials.StainlessSteel, ItemList.Casing_HV);
        machineCasing(recipes, Materials.Titanium, ItemList.Casing_EV);
        machineCasing(recipes, Materials.TungstenSteel, ItemList.Casing_IV);
        machineCasing(recipes, WerkstoffLoader.RhodiumPlatedPalladium.getGTMaterial(), ItemList.Casing_LuV);
        machineCasing(recipes, Materials.Iridium, ItemList.Casing_ZPM);
        machineCasing(recipes, Materials.Osmium, ItemList.Casing_UV);
        machineCasing(recipes, Materials.Neutronium, ItemList.Casing_MAX);
    }

    private static void machineCasing(RecipeContext recipes, Materials material, ItemList casing) {
        CraftingDefinition original = CraftingRecipeBuilder.shaped()
            .pattern("PPP", "PWP", "PPP")
            .ingredient('P', GTOreDictUnificator.get(OrePrefixes.plate, material, 1))
            .ingredient('W', ToolDictNames.craftingToolWrench.name())
            .output(casing.get(1))
            .matchType(CraftingRecipeType.GT_SHAPED)
            .removable(false)
            .build();
        recipes.crafting()
            .replaceOne(
                original,
                original.toBuilder()
                    .ingredient('P', GTOreDictUnificator.get(OrePrefixes.plateDouble, material, 1))
                    .output(RawMachineCasings.productionOutput(casing.get(1)))
                    .build());
    }

    private static void lvCircuitAssembler(RecipeContext recipes) {
        CraftingDefinition original = CraftingRecipeBuilder.shaped()
            .pattern("ACE", "VMV", "WCW")
            .ingredient('A', ItemList.Robot_Arm_LV.get(1))
            .ingredient(
                'C',
                ore(
                    AuditScope.MODIFY_LV_CIRCUIT_ASSEMBLER_HAND_CRAFTING,
                    OrePrefixes.circuit.get(Materials.MV)
                        .toString()))
            .ingredient('E', ItemList.Emitter_LV.get(1))
            .ingredient('V', ItemList.Conveyor_Module_LV.get(1))
            .ingredient('M', ItemList.Hull_LV.get(1))
            .ingredient(
                'W',
                ore(
                    AuditScope.MODIFY_LV_CIRCUIT_ASSEMBLER_HAND_CRAFTING,
                    OrePrefixes.cableGt01.get(Materials.Tin)
                        .toString()))
            .output(ItemList.Machine_LV_CircuitAssembler.get(1))
            .matchType(CraftingRecipeType.GT_SHAPED)
            .removable(false)
            .build();
        recipes.crafting()
            .replaceOne(
                original,
                original.toBuilder()
                    .ingredient(
                        'C',
                        OrePrefixes.circuit.get(Materials.LV)
                            .toString())
                    .build());
    }

    private static void removeGoodCircuitHandCrafting(RecipeContext recipes) {
        AuditScope scope = AuditScope.REMOVE_GOOD_CIRCUIT_HAND_CRAFTING;
        recipes.crafting()
            .removeOne(
                CraftingRecipeBuilder.shaped()
                    .pattern("WSW", "CBC", "DCD")
                    .ingredient(
                        'W',
                        OrePrefixes.wireGt01.get(Materials.Copper)
                            .toString())
                    .ingredient(
                        'S',
                        OrePrefixes.itemCasing.get(Materials.Steel)
                            .toString())
                    .ingredient(
                        'C',
                        requiredItem(
                            scope,
                            IngredientFactory.getModItem(Mods.IndustrialCraft2.ID, "itemPartCircuit", 1, 0)))
                    .ingredient('B', ItemList.Circuit_Board_Phenolic_Good.get(1))
                    .ingredient('D', ItemList.Circuit_Parts_Diode.get(1))
                    .output(ItemList.Circuit_Good.get(1))
                    .build());
    }

    private static void diamondSawblade(RecipeContext recipes) {
        CraftingDefinition original = CraftingRecipeBuilder.shaped()
            .pattern(" D ", "DGD", " D ")
            .ingredient(
                'D',
                OrePrefixes.dustSmall.get(Materials.Diamond)
                    .toString())
            .ingredient(
                'G',
                OrePrefixes.gear.get(Materials.CobaltBrass)
                    .toString())
            .output(ItemList.Component_Sawblade_Diamond.get(1))
            .matchType(CraftingRecipeType.GT_SHAPED)
            .build();
        recipes.crafting()
            .replaceOne(
                original,
                original.toBuilder()
                    .ingredient('G', MaterialsAlloy.BLOODSTEEL.getGear(1))
                    .build());
    }

    private static void removeMvEnergyHatchHandCrafting(RecipeContext recipes) {
        recipes.crafting()
            .removeOne(
                CraftingRecipeBuilder.shaped()
                    .pattern("ICL", "WHP", "ICL")
                    .ingredient('I', ItemList.Circuit_Chip_ULPIC.get(1))
                    .ingredient('C', ItemList.MV_Coil.get(1))
                    .ingredient(
                        'L',
                        OrePrefixes.cell.get(Materials.Lubricant)
                            .toString())
                    .ingredient(
                        'W',
                        OrePrefixes.cableGt01.get(Materials.Copper)
                            .toString())
                    .ingredient('H', ItemList.Hull_MV.get(1))
                    .ingredient('P', ItemList.Electric_Pump_MV.get(1))
                    .output(ItemList.Hatch_Energy_MV.get(1))
                    .matchType(CraftingRecipeType.GT_SHAPED)
                    .build());
    }

    private static void largeChemicalReactor(RecipeContext recipes) {
        CraftingDefinition original = CraftingRecipeBuilder.shaped()
            .pattern("CRC", "PMP", "CBC")
            .ingredient(
                'C',
                OrePrefixes.circuit.get(Materials.HV)
                    .toString())
            .ingredient(
                'R',
                OrePrefixes.rotor.get(Materials.StainlessSteel)
                    .toString())
            .ingredient(
                'P',
                OrePrefixes.pipeLarge.get(Materials.Polytetrafluoroethylene)
                    .toString())
            .ingredient('M', ItemList.Electric_Motor_HV.get(1))
            .ingredient('B', ItemList.Hull_HV.get(1))
            .output(ItemList.Machine_Multi_LargeChemicalReactor.get(1))
            .removable(false)
            .build();
        recipes.crafting()
            .replaceOne(
                original,
                original.toBuilder()
                    .ingredient(
                        'C',
                        OrePrefixes.circuit.get(Materials.EV)
                            .toString())
                    .build());
    }

    private static ItemStack ore(AuditScope scope, String name) {
        return requiredItem(scope, GTOreDictUnificator.getFirstOre(name, 1));
    }

    private static ItemStack requiredItem(AuditScope scope, ItemStack item) {
        if (item == null)
            throw new RecipeValidationException("Recipe patch " + scope + " could not resolve a required item");
        return item;
    }
}
