package pl.kuba6000.gtnhextranerf.integration;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.oredict.OreDictionary;

import com.dreammaster.scripts.IngredientFactory;

import bartworks.system.material.WerkstoffLoader;
import cpw.mods.fml.common.registry.GameRegistry;
import goodgenerator.api.recipe.GoodGeneratorRecipeMaps;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.Mods;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.objects.OreDictItemStack;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTModHandler;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipeBuilder;
import gtPlusPlus.core.material.MaterialsAlloy;
import pl.kuba6000.gtnhextranerf.items.MagicnetizedRods;
import pl.kuba6000.gtnhextranerf.recipes.machine.RecipeMatch;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.crafting.ShapedArcaneRecipe;
import thaumcraft.common.tiles.TileMagicWorkbench;

/** Final registered recipes, queried through Minecraft and GT's public APIs. */
final class LvRecipeIntegrationTests {

    private LvRecipeIntegrationTests() {}

    static void verify() {
        verifyLvCircuitVariants();
        verifyOreDictionaryMatching();
        verifyMotors();
        verifyMvMotor();
        verifyFloralFertilizer();
        verifyCasing(Materials.CastIron, ItemList.Casing_ULV);
        verifyCasing(Materials.Steel, ItemList.Casing_LV);
        verifyCasing(Materials.Aluminium, ItemList.Casing_MV);
        verifyCasing(Materials.StainlessSteel, ItemList.Casing_HV);
        verifyCasing(Materials.Titanium, ItemList.Casing_EV);
        verifyCasing(Materials.TungstenSteel, ItemList.Casing_IV);
        verifyCasing(WerkstoffLoader.RhodiumPlatedPalladium.getGTMaterial(), ItemList.Casing_LuV);
        verifyCasing(Materials.Iridium, ItemList.Casing_ZPM);
        verifyCasing(Materials.Osmium, ItemList.Casing_UV);
        verifyCasing(Materials.Neutronium, ItemList.Casing_MAX);
        verifyToolStation();
        verifyHandCraftedComponents();
    }

    private static void verifyFloralFertilizer() {
        ItemStack portal = GTModHandler.getModItem(Mods.NewHorizonsCoreMod.ID, "TwilightCrystal", 1, 0);
        ItemStack alumentum = GTModHandler.getModItem(Mods.Thaumcraft.ID, "ItemResource", 1, 0);
        ItemStack fertilizer = GTModHandler.getModItem(Mods.CropsNH.ID, "fertilizer", 1, 0);
        ItemStack floral = GTModHandler.getModItem(Mods.Botania.ID, "fertilizer", 6, 0);
        check(
            portal != null && alumentum != null && fertilizer != null && floral != null,
            "Missing floral fertilizer fixture items");
        ItemStack[] inputs = { fertilizer, alumentum, fertilizer, fertilizer, portal, fertilizer, fertilizer, alumentum,
            fertilizer };
        check(same(floral, craft(inputs)), "LV floral fertilizer crafting is missing or has the wrong output count");
        ItemStack[] misplaced = inputs.clone();
        misplaced[0] = alumentum;
        misplaced[1] = fertilizer;
        check(!same(floral, craft(misplaced)), "Floral fertilizer accepted the wrong crafting pattern");
        for (int slot = 0; slot < inputs.length; slot++) {
            ItemStack[] incomplete = inputs.clone();
            incomplete[slot] = null;
            check(!same(floral, craft(incomplete)), "Floral fertilizer accepted a missing ingredient in slot " + slot);
        }
        for (ItemStack input : new ItemStack[] { portal, alumentum, fertilizer }) {
            check(
                !input.getItem()
                    .hasContainerItem(input),
                "Floral fertilizer ingredient would be returned: " + input);
        }
    }

    private static void verifyLvCircuitVariants() {
        String lvCircuits = OrePrefixes.circuit.get(Materials.LV)
            .toString();
        List<ItemStack> variants = OreDictionary.getOres(lvCircuits);
        check(variants.size() > 1, "multiple LV circuit variants are required for this regression test");
        ItemStack station = IngredientFactory.getModItem(Mods.TinkerConstruct.ID, "ToolStationBlock", 1, 0);
        ItemStack slab = IngredientFactory.getModItem(Mods.TinkerConstruct.ID, "CraftingSlab", 1, 1);
        ItemStack saw = ore("craftingToolSaw");
        ItemStack arm = ItemList.Robot_Arm_LV.get(1);
        ItemStack emitter = ItemList.Emitter_LV.get(1);
        ItemStack conveyor = ItemList.Conveyor_Module_LV.get(1);
        ItemStack hull = ItemList.Hull_LV.get(1);
        ItemStack cable = item(OrePrefixes.cableGt01, Materials.Tin, 1);
        ItemStack assembler = ItemList.Machine_LV_CircuitAssembler.get(1);
        ItemStack first = variants.get(0)
            .copy();
        first.stackSize = 1;
        List<String> rejected = new ArrayList<>();
        for (ItemStack variant : variants) {
            ItemStack circuit = variant.copy();
            circuit.stackSize = 1;
            if (!same(slab, craft(saw, station, circuit))) rejected.add("Slab: " + circuit);
            if (!same(assembler, craft(arm, circuit, emitter, conveyor, hull, conveyor, cable, first, cable)))
                rejected.add("Assembler upper slot: " + circuit);
            if (!same(assembler, craft(arm, first, emitter, conveyor, hull, conveyor, cable, circuit, cable)))
                rejected.add("Assembler lower slot: " + circuit);
        }
        check(rejected.isEmpty(), "registered LV circuit variants were rejected: " + rejected);
        ItemStack wrongTier = item(OrePrefixes.circuit, Materials.MV, 1);
        check(!same(slab, craft(saw, station, wrongTier)), "Slab accepted an MV circuit as LV");
        check(
            !same(assembler, craft(arm, wrongTier, emitter, conveyor, hull, conveyor, cable, first, cable)),
            "Assembler accepted an MV circuit as LV");
    }

    private static void verifyOreDictionaryMatching() {
        ItemStack ordinary = item(OrePrefixes.stick, Materials.IronMagnetic, 1);
        GTRecipe.GTRecipe_WithAlt recipe = GTRecipeBuilder.builder()
            .itemInputs(ordinary, new OreDictItemStack("stickAnyIron", 2), new OreDictItemStack("wireGt01AnyCopper", 4))
            .itemOutputs(ItemList.Electric_Motor_LV.get(1))
            .duration(20)
            .eut(30)
            .buildWithAlt()
            .get();
        RecipeMatch.Builder builder = RecipeMatch.builder()
            .itemInputs(ordinary)
            .oreDictInput("stickAnyIron", 2);
        RecipeMatch single = builder.build();
        RecipeMatch multiple = builder.oreDictInput("wireGt01AnyCopper", 4)
            .build();
        check(multiple.test(recipe), "two OreDict slots did not match");
        check(!single.test(recipe), "building a second match changed the first");
        recipe.mInputs[1].stackSize++;
        check(!multiple.test(recipe), "wrong OreDict amount was accepted");
        recipe.mInputs[1].stackSize--;
        recipe.mOreDictIds[2] = recipe.mOreDictIds[1];
        check(!multiple.test(recipe), "duplicate OreDict slot was accepted");
    }

    private static void verifyMotors() {
        ItemStack motor = ItemList.Electric_Motor_LV.get(1);
        RecipeMatch iron = RecipeMatch.builder()
            .itemInputs(
                item(OrePrefixes.stick, Materials.IronMagnetic, 1),
                item(OrePrefixes.cableGt01, Materials.Tin, 2))
            .oreDictInput("stickAnyIron", 2)
            .oreDictInput("wireGt01AnyCopper", 4)
            .itemOutputs(motor)
            .build();
        check(
            RecipeMaps.assemblerRecipes.getAllRecipes()
                .stream()
                .noneMatch(iron),
            "iron assembler motor survived");
        check(
            RecipeMaps.assemblerRecipes.getAllRecipes()
                .stream()
                .anyMatch(
                    RecipeMatch.builder()
                        .itemOutputs(motor)
                        .build()),
            "all assembler motor routes were removed");
        ItemStack copper = item(OrePrefixes.wireGt01, Materials.Copper, 1);
        ItemStack cable = item(OrePrefixes.cableGt01, Materials.Tin, 1);
        ItemStack rod = item(OrePrefixes.stick, Materials.Iron, 1);
        check(
            !same(
                motor,
                craft(
                    cable,
                    copper,
                    rod,
                    copper,
                    item(OrePrefixes.stick, Materials.IronMagnetic, 1),
                    copper,
                    rod,
                    copper,
                    cable)),
            "iron crafting motor survived");
        ItemStack steel = item(OrePrefixes.stick, Materials.Steel, 1);
        check(
            same(
                motor,
                craft(
                    cable,
                    copper,
                    steel,
                    copper,
                    item(OrePrefixes.stick, Materials.SteelMagnetic, 1),
                    copper,
                    steel,
                    copper,
                    cable)),
            "steel crafting motor is missing");
    }

    private static void verifyMvMotor() {
        ItemStack motor = ItemList.Electric_Motor_MV.get(1);
        ItemStack magic = MagicnetizedRods.manasteel();
        ItemStack magnetic = item(OrePrefixes.stick, Materials.SteelMagnetic, 1);
        ItemStack aluminium = item(OrePrefixes.stick, Materials.Aluminium, 1);
        ItemStack wire = item(OrePrefixes.wireGt02, Materials.Cupronickel, 1);
        ItemStack cable = item(OrePrefixes.cableGt01, Materials.Copper, 1);
        check(
            same(motor, craft(cable, wire, aluminium, wire, magic, wire, aluminium, wire, cable)),
            "magicnetized MV motor crafting is missing");
        check(
            !same(motor, craft(cable, wire, aluminium, wire, magnetic, wire, aluminium, wire, cable)),
            "old MV motor crafting survived");
        RecipeMatch assembler = RecipeMatch.builder()
            .itemInputs(
                magic,
                item(OrePrefixes.stick, Materials.Aluminium, 2),
                item(OrePrefixes.wireGt02, Materials.Cupronickel, 4))
            .oreDictInput("cableGt01AnyCopper", 2)
            .itemOutputs(motor)
            .build();
        check(
            RecipeMaps.assemblerRecipes.getAllRecipes()
                .stream()
                .anyMatch(assembler),
            "magicnetized MV motor assembler recipe is missing");
        RecipeMatch oldAssembler = RecipeMatch.builder()
            .itemInputs(
                magnetic,
                item(OrePrefixes.stick, Materials.Aluminium, 2),
                item(OrePrefixes.wireGt02, Materials.Cupronickel, 4))
            .oreDictInput("cableGt01AnyCopper", 2)
            .itemOutputs(motor)
            .build();
        check(
            RecipeMaps.assemblerRecipes.getAllRecipes()
                .stream()
                .noneMatch(oldAssembler),
            "old MV motor assembler recipe survived");
        for (Materials copper : new Materials[] { Materials.Copper, Materials.AnnealedCopper }) {
            RecipeMatch coal = RecipeMatch.builder()
                .itemInputs(
                    item(OrePrefixes.stickLong, Materials.SteelMagnetic, 24),
                    item(OrePrefixes.stickLong, Materials.Aluminium, 48),
                    item(OrePrefixes.wireGt16, Materials.Cupronickel, 24),
                    item(OrePrefixes.cableGt16, copper, 6))
                .itemOutputs(ItemList.Electric_Motor_MV.get(64))
                .build();
            check(
                GoodGeneratorRecipeMaps.componentAssemblyLineRecipes.getAllRecipes()
                    .stream()
                    .anyMatch(coal),
                "original MV motor COAL recipe is missing");
        }
        boolean arcane = false;
        for (Object candidate : ThaumcraftApi.getCraftingRecipes()) {
            if (!(candidate instanceof ShapedArcaneRecipe)) continue;
            ShapedArcaneRecipe recipe = (ShapedArcaneRecipe) candidate;
            if (same(magic, recipe.getRecipeOutput()) && recipe.getAspects()
                .getAmount(Aspect.AIR) == 10
                && recipe.getAspects()
                    .getAmount(Aspect.ORDER) == 10) {
                TileMagicWorkbench grid = new TileMagicWorkbench();
                ItemStack flower = new ItemStack(GameRegistry.findItem("Botania", "specialFlower"));
                NBTTagCompound tags = new NBTTagCompound();
                tags.setString("type", "solegnolia");
                tags.setString("test-extra-data", "preserved");
                flower.setTagCompound(tags);
                grid.stackList[3] = magnetic.copy();
                grid.stackList[4] = item(OrePrefixes.stick, Materials.Manasteel, 1);
                grid.stackList[5] = magnetic.copy();
                grid.stackList[7] = flower;
                check(recipe.matches(grid, null, null), "magicnetized rod ingredients do not match");
                check(
                    same(item(OrePrefixes.stick, Materials.Steel, 1), MagicnetizedRods.craftingRemainder(magnetic)),
                    "magnetic steel should return steel");
                check(same(flower, MagicnetizedRods.craftingRemainder(flower)), "Solegnolia data was lost");
                grid.stackList[7] = null;
                check(!recipe.matches(grid, null, null), "magicnetized rod recipe accepts a missing flower");
                arcane = true;
            }
        }
        check(arcane, "magicnetized Manasteel Rod arcane recipe is missing");
    }

    private static void verifyHandCraftedComponents() {
        ItemStack sawblade = ItemList.Component_Sawblade_Diamond.get(1);
        ItemStack dust = item(OrePrefixes.dustSmall, Materials.Diamond, 1);
        ItemStack brassGear = item(OrePrefixes.gear, Materials.CobaltBrass, 1);
        ItemStack bloodSteelGear = MaterialsAlloy.BLOODSTEEL.getGear(1);
        check(
            !same(sawblade, craft(null, dust, null, dust, brassGear, dust, null, dust, null)),
            "cobalt brass sawblade survived");
        check(
            same(sawblade, craft(null, dust, null, dust, bloodSteelGear, dust, null, dust, null)),
            "blood steel sawblade is missing");

        ItemStack lubricant = ore(
            OrePrefixes.cell.get(Materials.Lubricant)
                .toString());
        check(
            !same(
                ItemList.Hatch_Energy_MV.get(1),
                craft(
                    ItemList.Circuit_Chip_ULPIC.get(1),
                    ItemList.MV_Coil.get(1),
                    lubricant,
                    item(OrePrefixes.cableGt01, Materials.Copper, 1),
                    ItemList.Hull_MV.get(1),
                    ItemList.Electric_Pump_MV.get(1),
                    ItemList.Circuit_Chip_ULPIC.get(1),
                    ItemList.MV_Coil.get(1),
                    lubricant)),
            "MV energy hatch hand crafting survived");
        // The same layout one tier lower is not nerfed, which proves the grid above is the real GT layout.
        ItemStack basicCircuit = ore(
            OrePrefixes.circuit.get(Materials.LV)
                .toString());
        check(
            same(
                ItemList.Hatch_Energy_LV.get(1),
                craft(
                    item(OrePrefixes.cableGt01, Materials.Tin, 1),
                    ItemList.LV_Coil.get(1),
                    lubricant,
                    basicCircuit,
                    ItemList.Hull_LV.get(1),
                    ItemList.Electric_Pump_LV.get(1),
                    item(OrePrefixes.cableGt01, Materials.Tin, 1),
                    ItemList.LV_Coil.get(1),
                    lubricant)),
            "LV energy hatch hand crafting is missing");
    }

    private static void verifyCasing(Materials material, ItemList casing) {
        ItemStack plate = item(OrePrefixes.plate, material, 1);
        ItemStack doublePlate = item(OrePrefixes.plateDouble, material, 1);
        ItemStack wrench = ore("craftingToolWrench");
        check(
            !same(
                casing.get(1),
                CasingFinishingIntegrationTests
                    .finish(craft(plate, plate, plate, plate, wrench, plate, plate, plate, plate))),
            "single-plate casing survived: " + casing);
        check(
            same(
                casing.get(1),
                CasingFinishingIntegrationTests.finish(
                    craft(
                        doublePlate,
                        doublePlate,
                        doublePlate,
                        doublePlate,
                        wrench,
                        doublePlate,
                        doublePlate,
                        doublePlate,
                        doublePlate))),
            "double-plate casing missing: " + casing);
    }

    private static void verifyToolStation() {
        ItemStack station = IngredientFactory.getModItem(Mods.TinkerConstruct.ID, "ToolStationBlock", 1, 0);
        ItemStack slab = IngredientFactory.getModItem(Mods.TinkerConstruct.ID, "CraftingSlab", 1, 1);
        ItemStack saw = ore("craftingToolSaw");
        check(!same(slab, craft(saw, station)), "tool station slab still needs no circuit");
        check(
            same(slab, craft(saw, station, GTModHandler.getIC2Item("electronicCircuit", 1))),
            "tool station slab with circuit is missing");
        ItemStack stick = ore("stickWood");
        ItemStack pattern = IngredientFactory.getModItem(Mods.TinkerConstruct.ID, "blankPattern", 1, 0);
        ItemStack craftingStation = IngredientFactory.getModItem(Mods.TinkerConstruct.ID, "CraftingStation", 1, 0);
        for (ItemStack table : new ItemStack[] { craftingStation, new ItemStack(Blocks.crafting_table) }) {
            check(
                same(
                    station,
                    craft(stick, pattern, stick, stick, table, stick, null, ore("craftingToolSoftMallet"), null)),
                "ordinary tool station still requires the added circuit");
        }
    }

    static ItemStack craft(ItemStack... inputs) {
        InventoryCrafting grid = new InventoryCrafting(new Container() {

            @Override
            public boolean canInteractWith(EntityPlayer player) {
                return false;
            }
        }, 3, 3);
        for (int i = 0; i < inputs.length; i++)
            grid.setInventorySlotContents(i, inputs[i] == null ? null : inputs[i].copy());
        return CraftingManager.getInstance()
            .findMatchingRecipe(grid, null);
    }

    private static ItemStack item(OrePrefixes prefix, Materials material, int amount) {
        ItemStack result = GTOreDictUnificator.get(prefix, material, amount);
        check(result != null, "missing fixture: " + prefix + material);
        return result;
    }

    private static ItemStack ore(String name) {
        ItemStack result = GTOreDictUnificator.getFirstOre(name, 1);
        check(result != null, "missing fixture: " + name);
        return result;
    }

    private static boolean same(ItemStack first, ItemStack second) {
        return ItemStack.areItemStacksEqual(first, second);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
