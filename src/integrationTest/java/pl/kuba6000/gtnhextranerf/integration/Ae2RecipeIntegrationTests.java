package pl.kuba6000.gtnhextranerf.integration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.Mods;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.objects.ItemData;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.AssemblyLineUtils;
import gregtech.api.util.GTModHandler;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTScannerResult;
import gregtech.loaders.postload.ScannerHandlerLoader;
import pl.kuba6000.gtnhextranerf.mixins.early.MixinCraftingManagerAccessor;
import pl.kuba6000.gtnhextranerf.mixins.early.MixinRecipeAccessor;
import pl.kuba6000.gtnhextranerf.recipes.machine.RecipeChange;
import pl.kuba6000.gtnhextranerf.recipes.machine.RecipeMatch;

final class Ae2RecipeIntegrationTests {

    private Ae2RecipeIntegrationTests() {}

    static void verify() {
        verifyFluidSlotReplacement();
        List<String> failures = new ArrayList<>();
        int tpvCrafting = 0;
        int sunariumPatterns = 0;
        for (Object recipe : ((MixinCraftingManagerAccessor) CraftingManager.getInstance())
            .gtnhextranerf$getRecipes()) {
            ItemStack output = ((MixinRecipeAccessor) recipe).gtnhextranerf$getRecipeOutput();
            if (!ae2(output)) continue;
            if (name(output).equals("tile.BlockController")) failures.add("controller crafting survives");
            Iterable<?> inputs = recipe instanceof ShapedOreRecipe
                ? Arrays.asList(((ShapedOreRecipe) recipe).getInput())
                : recipe instanceof ShapelessOreRecipe ? ((ShapelessOreRecipe) recipe).getInput() : new ArrayList<>();
            if (hasMaterial(inputs, Materials.Titanium)) failures.add("titanium crafting: " + output);
            if (hasMaterial(inputs, Materials.TPV)) tpvCrafting++;
            if (name(output).equals("item.ItemMultiMaterial") && output.getItemDamage() == 52) {
                if (hasMaterial(inputs, Materials.Glowstone)) failures.add("glowstone Blank Pattern survives");
                if (hasMaterial(inputs, Materials.Sunnarium)) sunariumPatterns++;
            }
        }
        int tpvMachine = 0;
        for (RecipeMap<?> map : RecipeMap.ALL_RECIPE_MAPS.values()) {
            for (GTRecipe recipe : map.getAllRecipes()) {
                if (recipe.mOutputs == null || !Arrays.stream(recipe.mOutputs)
                    .anyMatch(Ae2RecipeIntegrationTests::ae2)) continue;
                if (hasMaterial(Arrays.asList(recipe.mInputs), Materials.Titanium))
                    failures.add("titanium machine: " + map.unlocalizedName);
                if (hasMaterial(Arrays.asList(recipe.mInputs), Materials.TPV)) tpvMachine++;
                for (net.minecraftforge.fluids.FluidStack input : recipe.mFluidInputs) {
                    if (input.isFluidEqual(Materials.Titanium.getMolten(1)))
                        failures.add("molten Titanium: " + map.unlocalizedName);
                }

                if (map == RecipeMaps.assemblerRecipes && Arrays.stream(recipe.mOutputs)
                    .anyMatch(output -> ae2(output) && name(output).equals("tile.BlockController")))
                    failures.add("controller assembler survives");
            }
        }
        if (tpvCrafting == 0 || tpvMachine == 0) failures.add("TPV recipe family missing");
        if (sunariumPatterns == 0) failures.add("Sunarium Blank Pattern missing");
        int controllers = 0;
        for (GTRecipe.RecipeAssemblyLine recipe : GTRecipe.RecipeAssemblyLine.sAssemblylineRecipes) {
            if (!ae2(recipe.mOutput)) continue;
            if (name(recipe.mOutput).equals("tile.BlockController")) {
                controllers++;
                if (!hasMaterial(Arrays.asList(recipe.mInputs), Materials.TPV))
                    failures.add("Controller Assembly Line lacks TPV");
            }
            if (hasMaterial(Arrays.asList(recipe.mInputs), Materials.Titanium))
                failures.add("Titanium in real Assembly Line recipe");
        }
        if (controllers != 1) failures.add("Expected one Controller Assembly Line recipe, found " + controllers);
        ItemStack cell = GTModHandler.getModItem(Mods.AppliedEnergistics2.ID, "item.ItemBasicStorageCell.64k", 1, 0);
        GTScannerResult scan = ScannerHandlerLoader
            .doAssemblyLineResearch(null, cell, ItemList.Tool_DataStick.get(1), null);
        if (!(scan instanceof GTScannerResult.ALScannerResult)
            || !ae2(((GTScannerResult.ALScannerResult) scan).alRecipe.mOutput)
            || !name(((GTScannerResult.ALScannerResult) scan).alRecipe.mOutput).equals("tile.BlockController")
            || scan.inputConsume != 1
            || !ItemList.Tool_DataStick.isStackEqual(scan.output, false, true)
            || !AssemblyLineUtils.findALRecipeFromDataStick(scan.output)
                .contains(((GTScannerResult.ALScannerResult) scan).alRecipe))
            failures.add("64k cell does not produce Controller research on a Data Stick");
        RecipeMatch originalQuartz = RecipeMatch.builder()
            .itemInputs(
                GTOreDictUnificator.get(OrePrefixes.dust, Materials.CertusQuartz, 1),
                GTOreDictUnificator.get(OrePrefixes.dust, Materials.Redstone, 1))
            .itemOutputs(GTOreDictUnificator.get(OrePrefixes.dust, Materials.CertusQuartzCharged, 1))
            .build();
        for (RecipeMap<?> map : Arrays
            .asList(RecipeMaps.chemicalReactorRecipes, RecipeMaps.multiblockChemicalReactorRecipes)) {
            if (map.getAllRecipes()
                .stream()
                .noneMatch(originalQuartz)) failures.add("redstone Certus route missing: " + map.unlocalizedName);
        }
        if (!failures.isEmpty()) throw new AssertionError(failures.toString());
    }

    static void verifyUniversalWirelessTerminal(World world) {
        ItemStack universal = terminalItem(Mods.AE2FluidCraft, "wireless_ultra_terminal", 0);
        List<ItemStack> inputs = new ArrayList<>(
            Arrays.asList(
                terminalItem(Mods.AppliedEnergistics2, "item.ToolWirelessTerminal", 0),
                terminalItem(Mods.AE2FluidCraft, "wireless_fluid_pattern_terminal", 0),
                terminalItem(Mods.AE2FluidCraft, "wireless_interface_terminal", 0),
                terminalItem(Mods.AE2FluidCraft, "wireless_level_terminal", 0)));
        if (Mods.AE2WCT.isModLoaded()) {
            inputs.add(terminalItem(Mods.AE2WCT, "wirelessCraftingTerminal", 0));
        }
        ItemStack oldResult = craft(world, inputs);
        if (oldResult != null && universal.isItemEqual(oldResult)) {
            throw new AssertionError("Universal Wireless Terminal crafts without a Processing Pattern Terminal");
        }

        inputs.add(0, terminalItem(Mods.AppliedEnergistics2, "item.ItemMultiPart", 500));
        ItemStack result = craft(world, inputs);
        if (!ItemStack.areItemStacksEqual(universal, result)) {
            throw new AssertionError("Expected one Universal Wireless Terminal with the Processing Pattern Terminal");
        }
        verifyWirelessCard(world, result, "quantum_bridge_card", "infinityBoosterCard");
        verifyWirelessCard(world, result, "energy_card", "InfinityEnergyCard");
    }

    private static void verifyWirelessCard(World world, ItemStack terminal, String cardName, String tag) {
        ItemStack upgraded = craft(world, Arrays.asList(terminal, terminalItem(Mods.AE2FluidCraft, cardName, 0)));
        if (upgraded == null || !terminal.isItemEqual(upgraded)
            || upgraded.getTagCompound() == null
            || !upgraded.getTagCompound()
                .getBoolean(tag)) {
            throw new AssertionError("Universal Wireless Terminal could not install " + cardName);
        }
    }

    private static ItemStack terminalItem(Mods mod, String name, int metadata) {
        ItemStack stack = GTModHandler.getModItem(mod.ID, name, 1, metadata);
        if (stack == null) throw new AssertionError("Expected registered item " + mod.ID + ":" + name);
        return stack;
    }

    private static ItemStack craft(World world, List<ItemStack> inputs) {
        InventoryCrafting grid = new InventoryCrafting(new Container() {

            @Override
            public boolean canInteractWith(EntityPlayer player) {
                return false;
            }
        }, 3, 3);
        for (int i = 0; i < inputs.size(); i++) {
            grid.setInventorySlotContents(
                i,
                inputs.get(i)
                    .copy());
        }
        return CraftingManager.getInstance()
            .findMatchingRecipe(grid, world);
    }

    private static void verifyFluidSlotReplacement() {
        FluidStack titanium = Materials.Titanium.getMolten(36);
        FluidStack tpv = Materials.TPV.getMolten(36);
        FluidStack water = Materials.Water.getFluid(1000);
        GTRecipe recipe = GTRecipeBuilder.builder()
            .fluidInputs(titanium, water)
            .itemOutputs(GTOreDictUnificator.get(OrePrefixes.plate, Materials.TPV, 1))
            .duration(100)
            .eut(480)
            .build()
            .get();
        recipe.mFluidInputChances = new int[] { 5000, 7500 };
        recipe.mAltFluidInputs = new FluidStack[][] { { titanium.copy() }, { water.copy() } };
        RecipeChange.builder()
            .replaceFluidInput(titanium, tpv)
            .build()
            .accept(recipe);
        if (!recipe.mFluidInputs[0].isFluidStackIdentical(tpv) || !recipe.mFluidInputs[1].isFluidStackIdentical(water)
            || recipe.mFluidInputChances[0] != 5000
            || recipe.mFluidInputChances[1] != 7500
            || recipe.mAltFluidInputs[0] != null
            || !recipe.mAltFluidInputs[1][0].isFluidStackIdentical(water)
            || recipe.mDuration != 100
            || recipe.mEUt != 480)
            throw new AssertionError(
                "fluid replacement changed quantities, chances, other alternatives or processing data");
    }

    private static boolean hasMaterial(Iterable<?> inputs, Materials material) {
        for (Object input : inputs) {
            if (input instanceof Iterable<?> && hasMaterial((Iterable<?>) input, material)) return true;
            if (input instanceof ItemStack) {
                ItemData data = GTOreDictUnificator.getAssociation((ItemStack) input);
                if (data != null && data.mMaterial != null && data.mMaterial.mMaterial == material) return true;
            }
        }
        return false;
    }

    private static boolean ae2(ItemStack stack) {
        return stack != null && stack.getItem() != null
            && GameRegistry.findUniqueIdentifierFor(stack.getItem()) != null
            && Mods.AppliedEnergistics2.ID.equals(GameRegistry.findUniqueIdentifierFor(stack.getItem()).modId);
    }

    private static String name(ItemStack stack) {
        return GameRegistry.findUniqueIdentifierFor(stack.getItem()).name;
    }
}
