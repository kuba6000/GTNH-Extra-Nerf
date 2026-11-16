package pl.kuba6000.gtnhextranerf.integration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;

import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.oredict.OreDictionary;

import com.dreammaster.item.NHItemList;
import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;
import com.kuba6000.mobsinfo.api.VillagerTrade;

import forestry.api.recipes.ICentrifugeRecipe;
import forestry.api.recipes.RecipeManagers;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.objects.ItemData;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;
import gregtech.common.items.CombType;
import gregtech.loaders.misc.GTBees;
import mods.railcraft.common.carts.EnumCart;
import mods.railcraft.common.worldgen.VillagerTradeHandler;
import pl.kuba6000.gtnhextranerf.machines.IronBathFurnace;
import pl.kuba6000.gtnhextranerf.materials.IronBathMaterials;

/** Verifies alloy gates through the final native recipe registries. */
@GameTestHolder("gtnhextranerftests")
public final class IronBathAlloyScenarios {

    private IronBathAlloyScenarios() {}

    @GameTest(template = "native_fixture")
    public static void industrialSteelRequiresLatePower(GameTestHelper helper) {
        helper.startSequence()
            .thenExecute(AlloyBlastSmelterIntegrationTests::verifySteel)
            .thenSucceed();
    }

    @GameTest(template = "native_fixture")
    public static void locomotiveTradeRequiresSuppliedSteel(GameTestHelper helper) {
        helper.startSequence()
            .thenExecute(() -> {
                ItemStack locomotive = EnumCart.LOCO_STEAM_SOLID.getCartItem();
                MerchantRecipe offer = new MerchantRecipe(new ItemStack(Items.emerald, 36), locomotive.copy());
                ItemStack plates = GTOreDictUnificator.get(OrePrefixes.plate, Materials.Steel, 17);
                check(
                    ItemStack.areItemStacksEqual(plates, offer.getSecondItemToBuy()),
                    "Locomotive trade supplies free steel");
                MerchantRecipeList offers = new MerchantRecipeList();
                offers.add(offer);
                check(
                    offers.canRecipeBeUsed(new ItemStack(Items.emerald, 36), null, 0) == null,
                    "Emeralds alone can buy the locomotive");
                check(
                    offers.canRecipeBeUsed(
                        new ItemStack(Items.emerald, 36),
                        GTOreDictUnificator.get(OrePrefixes.plate, Materials.Steel, 16),
                        0) == null,
                    "The locomotive trade accepts too little steel");
                check(
                    offers.canRecipeBeUsed(new ItemStack(Items.emerald, 36), plates.copy(), 0) == offer,
                    "The locomotive trade rejects its advertised payment");
                check(
                    offers.canRecipeBeUsed(
                        new ItemStack(Items.emerald, 36),
                        GTOreDictUnificator.get(OrePrefixes.plate, Materials.Copper, 17),
                        0) == null,
                    "Copper plates can pay for the locomotive's steel");
                NBTTagCompound saved = offer.writeToTags();
                saved.removeTag("buyB");
                saved.setInteger("uses", 3);
                saved.setInteger("maxUses", 11);
                MerchantRecipe loaded = new MerchantRecipe(saved);
                check(
                    ItemStack.areItemStacksEqual(plates, loaded.getSecondItemToBuy()),
                    "A saved locomotive offer omits the steel payment");
                NBTTagCompound reloaded = loaded.writeToTags();
                check(
                    reloaded.getInteger("uses") == 3 && reloaded.getInteger("maxUses") == 11,
                    "Updating the locomotive trade reset its stock");
                check(
                    loaded.getItemToBuy().stackSize == 36
                        && ItemStack.areItemStacksEqual(locomotive, loaded.getItemToSell()),
                    "Updating the locomotive trade changed its emerald price or output");
                MerchantRecipe ordinary = new MerchantRecipe(
                    new ItemStack(Items.emerald, 8),
                    new ItemStack(Items.minecart));
                check(!ordinary.hasSecondItemToBuy(), "An unrelated minecart trade acquired a steel payment");
            })
            .thenSucceed();
    }

    @GameTest(template = "native_fixture")
    public static void locomotiveTradePreviewMatchesPayment(GameTestHelper helper) {
        helper.startSequence()
            .thenExecute(() -> {
                ArrayList<VillagerTrade> trades = new ArrayList<>();
                new VillagerTradeHandler().provideTrades(new EntityVillager(helper.getWorld()), 0, trades);
                ItemStack locomotive = EnumCart.LOCO_STEAM_SOLID.getCartItem();
                VillagerTrade trade = trades.stream()
                    .filter(entry -> entry.getOutput().stack.isItemEqual(locomotive))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Railcraft omitted the locomotive offer"));
                check(
                    trade.hasSecondInput() && ItemStack.areItemStacksEqual(
                        GTOreDictUnificator.get(OrePrefixes.plate, Materials.Steel, 17),
                        trade.getSecondInput().stack),
                    "Mobs Info omits the locomotive's steel payment");
                check(
                    trade.getFirstInput().stack.getItem() == Items.emerald
                        && trade.getFirstInput().possibleSizes.contains(32)
                        && trade.getFirstInput().possibleSizes.contains(40)
                        && Math.abs(trade.getChance() - 0.1) < 1e-6,
                    "Updating the trade preview changed its price range or chance");
                MerchantRecipeList live = new MerchantRecipeList();
                VillagerTradeHandler handler = new VillagerTradeHandler();
                MerchantRecipe offer = null;
                for (int seed = 0; seed < 100 && offer == null; seed++) {
                    handler.manipulateTradesForVillager(new EntityVillager(helper.getWorld()), live, new Random(seed));
                    for (Object entry : live) {
                        MerchantRecipe candidate = (MerchantRecipe) entry;
                        if (candidate.getItemToSell()
                            .isItemEqual(locomotive)) offer = candidate;
                    }
                }
                check(offer != null, "Railcraft did not generate a locomotive trade");
                check(
                    ItemStack.areItemStacksEqual(trade.getSecondInput().stack, offer.getSecondItemToBuy()),
                    "The generated locomotive offer differs from Mobs Info");
            })
            .thenSucceed();
    }

    @GameTest(template = "native_fixture")
    public static void chainmailRecoveryRequiresMeltingItsDust(GameTestHelper helper) {
        helper.startSequence()
            .thenExecute(() -> {
                for (var armor : new net.minecraft.item.Item[] { Items.chainmail_helmet, Items.chainmail_chestplate,
                    Items.chainmail_leggings, Items.chainmail_boots }) {
                    ItemStack stack = new ItemStack(armor);
                    GTRecipe grinding = RecipeMaps.maceratorRecipes.findRecipeQuery()
                        .items(stack)
                        .find();
                    check(
                        grinding != null && Arrays.stream(grinding.mOutputs)
                            .anyMatch(output -> {
                                ItemData data = GTOreDictUnificator.getItemData(output);
                                return data != null && data.mMaterial.mMaterial == Materials.Steel;
                            }),
                        "Chainmail lost its steel dust recovery");
                    for (RecipeMap<?> map : new RecipeMap<?>[] { RecipeMaps.arcFurnaceRecipes,
                        RecipeMaps.fluidExtractionRecipes, RecipeMaps.alloySmelterRecipes }) {
                        check(
                            map.getAllRecipes()
                                .stream()
                                .noneMatch(
                                    recipe -> Arrays.stream(recipe.mInputs)
                                        .anyMatch(input -> input != null && input.getItem() == armor)),
                            "Chainmail still has a thermal recovery recipe in " + map.unlocalizedName);
                    }
                    check(
                        FurnaceRecipes.smelting()
                            .getSmeltingResult(stack) == null,
                        "Chainmail still smelts in an ordinary furnace");
                }
            })
            .thenSucceed();
    }

    @GameTest(template = "native_fixture")
    public static void bothMixerMapsConsumeBathProducts(GameTestHelper helper) {
        helper.startSequence()
            .thenExecute(() -> {
                for (RecipeMap<?> map : new RecipeMap<?>[] { RecipeMaps.mixerRecipes,
                    RecipeMaps.mixerNonCellRecipes }) {
                    verifyMixer(
                        map,
                        Materials.StainlessSteel,
                        9,
                        IronBathMaterials.wroughtIron(),
                        6,
                        2,
                        dust(Materials.Nickel, 1),
                        dust(Materials.Manganese, 1),
                        dust(Materials.Chrome, 1));
                    verifyMixer(
                        map,
                        Materials.StainlessSteel,
                        9,
                        IronBathMaterials.wroughtIron(),
                        4,
                        1,
                        dust(Materials.Invar, 3),
                        dust(Materials.Manganese, 1),
                        dust(Materials.Chrome, 1));
                    verifyMixer(
                        map,
                        Materials.ElectricalSteel,
                        3,
                        Materials.CastIron,
                        1,
                        2,
                        dust(Materials.Steel, 1),
                        dust(Materials.Coal, 1),
                        dust(Materials.Silicon, 1));
                    verifyMixer(
                        map,
                        Materials.DarkSteel,
                        3,
                        Materials.PigIron,
                        1,
                        2,
                        dust(Materials.ElectricalSteel, 1),
                        dust(Materials.Coal, 1),
                        dust(Materials.Obsidian, 1));
                    verifyMixer(
                        map,
                        Materials.DarkSteel,
                        9,
                        Materials.PigIron,
                        3,
                        14,
                        dust(Materials.Steel, 1),
                        dust(Materials.Coal, 4),
                        dust(Materials.Silicon, 1),
                        dust(Materials.Obsidian, 3));
                    requireGatedMixerOutputs(map, Materials.StainlessSteel, IronBathMaterials.wroughtIron());
                    requireGatedMixerOutputs(map, Materials.ElectricalSteel, Materials.CastIron);
                    requireGatedMixerOutputs(map, Materials.DarkSteel, Materials.PigIron);
                }
            })
            .thenSucceed();
    }

    @GameTest(template = "native_fixture")
    public static void handMixturesConsumeBathProducts(GameTestHelper helper) {
        helper.startSequence()
            .thenExecute(() -> {
                verifyCrafting(
                    Materials.ElectricalSteel,
                    Materials.CastIron,
                    dust(Materials.Steel, 1),
                    dust(Materials.Coal, 1),
                    dust(Materials.Silicon, 1));
                verifyCrafting(
                    Materials.DarkSteel,
                    Materials.PigIron,
                    dust(Materials.ElectricalSteel, 1),
                    dust(Materials.Coal, 1),
                    dust(Materials.Obsidian, 1));
            })
            .thenSucceed();
    }

    @GameTest(template = "native_fixture")
    public static void castingAndOreMatchingKeepBathMetalsSeparate(GameTestHelper helper) {
        helper.startSequence()
            .thenExecute(() -> {
                Materials[] metals = { IronBathMaterials.wroughtIron(), Materials.CastIron, Materials.PigIron };
                check(
                    Materials.WroughtIron == metals[0],
                    "Native Wrought Iron does not resolve to the registered material");
                for (int index = 0; index < metals.length; index++) {
                    Materials material = metals[index];
                    ItemStack ingot = item(OrePrefixes.ingot, material, 1);
                    ItemStack powder = dust(material, 1);
                    check(
                        !ingot.isItemEqual(item(OrePrefixes.ingot, Materials.Iron, 1)),
                        material + " aliases Iron ingots");
                    check(!powder.isItemEqual(dust(Materials.Iron, 1)), material + " aliases Iron Dust");
                    for (int other = index + 1; other < metals.length; other++) check(
                        !ingot.isItemEqual(item(OrePrefixes.ingot, metals[other], 1)),
                        "Bath products share an ingot identity");
                    for (int ore : OreDictionary.getOreIDs(ingot)) check(
                        !"ingotAnyIron".equals(OreDictionary.getOreName(ore)),
                        material + " still matches generic Iron");
                    FluidStack molten = material.getMolten(144);
                    check(
                        molten != null && !molten.isFluidEqual(Materials.Iron.getMolten(144)),
                        material + " aliases Molten Iron");
                    GTRecipe casting = RecipeMaps.fluidSolidifierRecipes.findRecipeQuery()
                        .items(ItemList.Shape_Mold_Ingot.get(1))
                        .fluids(molten.copy())
                        .voltage(128)
                        .find();
                    check(produces(casting, ingot, 1), material + " does not cast into its own ingot");
                    ItemStack smelted = FurnaceRecipes.smelting()
                        .getSmeltingResult(powder);
                    check(
                        smelted == null || smelted.isItemEqual(ingot),
                        material + " furnace smelting loses its identity");
                }
            })
            .thenSucceed();
    }

    @GameTest(template = "native_fixture")
    public static void initialBathPartsUsePreBathMaterials(GameTestHelper helper) {
        helper.startSequence()
            .thenExecute(() -> {
                for (ItemStack output : new ItemStack[] { IronBathFurnace.controller(), IronBathFurnace.casing(0, 1),
                    IronBathFurnace.tap(1) }) {
                    int count = 0;
                    for (GTRecipe recipe : RecipeMaps.assemblerRecipes.getAllRecipes()) {
                        if (!produces(recipe, output, 1)) continue;
                        count++;
                        check(
                            recipe.mEUt <= 30,
                            "Initial bath part requires an assembler above LV: " + output.getDisplayName());
                        for (ItemStack input : recipe.mInputs) {
                            ItemData data = GTOreDictUnificator.getItemData(input);
                            if (data == null || data.mMaterial == null) continue;
                            Materials material = data.mMaterial.mMaterial;
                            check(
                                material != IronBathMaterials.wroughtIron() && material != Materials.CastIron
                                    && material != Materials.PigIron,
                                "Initial bath part depends on a bath product: " + output.getDisplayName());
                        }
                    }
                    check(
                        count == 1,
                        "Initial bath part has missing or ambiguous assembler production: " + output.getDisplayName());
                }
                GTRecipe casing = RecipeMaps.assemblerRecipes.findRecipeQuery()
                    .items(
                        ItemList.Casing_Firebricks.get(1),
                        GTOreDictUnificator.get(OrePrefixes.foil, Materials.Steel, 4))
                    .voltage(30)
                    .find();
                check(
                    casing != null && produces(casing, IronBathFurnace.casing(0, 1), 1),
                    "Basic casing requires one Firebricks Casing and four Steel Foils at LV");
                GTRecipe foils = RecipeMaps.benderRecipes.findRecipeQuery()
                    .items(
                        GTOreDictUnificator.get(OrePrefixes.plate, Materials.Steel, 1),
                        GTUtility.getIntegratedCircuit(1))
                    .voltage(32)
                    .find();
                check(
                    foils != null && produces(foils, GTOreDictUnificator.get(OrePrefixes.foil, Materials.Steel, 4), 4),
                    "One Steel Plate must yield four Steel Foils at LV");
            })
            .thenSucceed();
    }

    @GameTest(template = "native_fixture")
    public static void reinforcedCasingsUseWroughtPlatesAndMica(GameTestHelper helper) {
        helper.startSequence()
            .thenExecute(() -> {
                ItemStack plates = GTOreDictUnificator.get(OrePrefixes.plate, IronBathMaterials.wroughtIron(), 2);
                check(plates != null, "Wrought Iron plates are missing");
                GTRecipe upgrade = RecipeMaps.assemblerRecipes.findRecipeQuery()
                    .items(IronBathFurnace.casing(0, 1), plates, NHItemList.MicaInsulatorFoil.get(4))
                    .voltage(30)
                    .find();
                check(
                    upgrade != null && produces(upgrade, IronBathFurnace.casing(1, 1), 1),
                    "Reinforced casing requires a basic casing, two Wrought Iron plates and four mica foils at LV");
                GTRecipe bending = RecipeMaps.benderRecipes.findRecipeQuery()
                    .items(
                        GTOreDictUnificator.get(OrePrefixes.ingot, IronBathMaterials.wroughtIron(), 1),
                        GTUtility.getIntegratedCircuit(1))
                    .voltage(32)
                    .find();
                check(bending != null && produces(bending, plates, 1), "Wrought Iron plates cannot be bent at LV");
            })
            .thenSucceed();
    }

    @GameTest(template = "native_fixture")
    public static void alloyRecoveryRetainsNativeOutputs(GameTestHelper helper) {
        helper.startSequence()
            .thenExecute(() -> {
                CombType[] combs = { CombType.ELECTRICALSTEEL, CombType.DARKSTEEL, CombType.STAINLESSSTEEL };
                Materials[] alloys = { Materials.ElectricalSteel, Materials.DarkSteel, Materials.StainlessSteel };
                int[] duration = { 128, 256, 384 };
                int[] power = { 5, 120, 480 };
                int[] chances = { 10000, 10000, 5000 };
                ItemStack wax = ItemList.FR_RefractoryWax.get(1);
                for (int index = 0; index < combs.length; index++) {
                    ItemStack comb = GTBees.combs.getStackForType(combs[index]);
                    for (RecipeMap<?> map : new RecipeMap<?>[] { RecipeMaps.centrifugeRecipes,
                        RecipeMaps.centrifugeNonCellRecipes }) {
                        GTRecipe recipe = map.findRecipeQuery()
                            .items(comb.copy())
                            .voltage(512)
                            .find();
                        check(produces(recipe, wax, 1), "Comb processing lost its wax in " + map.unlocalizedName);
                        check(
                            produces(recipe, dust(alloys[index], 1), 1),
                            "Comb processing lost its alloy in " + map.unlocalizedName);
                        check(
                            recipe.mDuration == duration[index] && recipe.mEUt == power[index],
                            "Comb processing changed its native operating cost in " + map.unlocalizedName);
                        for (int slot = 0; slot < recipe.mOutputs.length; slot++) {
                            if (recipe.mOutputs[slot] != null && recipe.mOutputs[slot].isItemEqual(wax))
                                check(recipe.getOutputChance(slot) == 5000, "Comb wax chance changed");
                            if (recipe.mOutputs[slot] != null
                                && recipe.mOutputs[slot].isItemEqual(dust(alloys[index], 1)))
                                check(recipe.getOutputChance(slot) == chances[index], "Comb alloy chance changed");
                        }
                    }
                }
                int forestryElectrical = 0;
                for (ICentrifugeRecipe recipe : RecipeManagers.centrifugeManager.recipes()) {
                    if (!recipe.getInput()
                        .isItemEqual(GTBees.combs.getStackForType(CombType.ELECTRICALSTEEL))) continue;
                    forestryElectrical++;
                    boolean hasWax = false;
                    boolean hasAlloy = false;
                    for (Map.Entry<ItemStack, Float> product : recipe.getAllProducts()
                        .entrySet()) {
                        if (product.getKey()
                            .isItemEqual(wax)) {
                            hasWax = product.getKey().stackSize == 1 && Math.abs(product.getValue() - 0.5f) < 0.000001f;
                        }
                        if (product.getKey()
                            .isItemEqual(dust(Materials.ElectricalSteel, 1)))
                            hasAlloy = product.getKey().stackSize == 1
                                && Math.abs(product.getValue() - 1.0f) < 0.000001f;
                    }
                    check(
                        hasWax && hasAlloy && recipe.getProcessingTime() == 40,
                        "Forestry Electrical Steel comb lost its native outputs or processing time");
                }
                check(forestryElectrical == 1, "Forestry Electrical Steel comb processing is missing or ambiguous");

                for (RecipeMap<?> map : new RecipeMap<?>[] { RecipeMaps.electrolyzerRecipes,
                    RecipeMaps.electrolyzerNonCellRecipes }) {
                    GTRecipe dark = map.findRecipeQuery()
                        .items(dust(Materials.DarkSteel, 3))
                        .voltage(128)
                        .find();
                    check(
                        produces(dark, dust(Materials.ElectricalSteel, 1), 1)
                            && produces(dark, dust(Materials.Coal, 1), 1)
                            && produces(dark, dust(Materials.Obsidian, 1), 1),
                        "Dark Steel decomposition lost native products in " + map.unlocalizedName);
                    check(dark.mDuration == 78 && dark.mEUt == 90, "Dark Steel decomposition cost changed");
                    GTRecipe end = map.findRecipeQuery()
                        .items(dust(Materials.EndSteel, 3))
                        .voltage(128)
                        .find();
                    check(
                        produces(end, dust(Materials.DarkSteel, 1), 1) && produces(end, dust(Materials.Tungsten, 1), 1)
                            && produces(end, dust(Materials.Endstone, 1), 1),
                        "End Steel decomposition lost native products in " + map.unlocalizedName);
                    check(end.mDuration == 258 && end.mEUt == 90, "End Steel decomposition cost changed");
                }
            })
            .thenSucceed();
    }

    private static void verifyMixer(RecipeMap<?> map, Materials output, int yield, Materials required, int amount,
        int circuit, ItemStack... remaining) {
        ItemStack[] paid = Arrays.copyOf(remaining, remaining.length + 2);
        paid[remaining.length] = dust(required, amount);
        paid[remaining.length + 1] = GTUtility.getIntegratedCircuit(circuit);
        GTRecipe recipe = map.findRecipeQuery()
            .items(copy(paid))
            .voltage(128)
            .find();
        check(
            produces(recipe, dust(output, 1), yield),
            "Paid " + output + " route missing from " + map.unlocalizedName);
        ItemStack[] unpaid = Arrays.copyOf(remaining, remaining.length + 1);
        unpaid[remaining.length] = GTUtility.getIntegratedCircuit(circuit);
        check(
            !produces(
                map.findRecipeQuery()
                    .items(copy(unpaid))
                    .voltage(128)
                    .find(),
                dust(output, 1),
                yield),
            "Missing " + required + " bypasses " + output + " in " + map.unlocalizedName);
        check(!recipe.isRecipeInputEqual(false, null, copy(unpaid)), "Native processing accepts unpaid " + output);
        check(recipe.isRecipeInputEqual(true, null, paid), "Native processing rejects paid " + output);
        check(paid[remaining.length].stackSize == 0, "Native processing retained " + required);
        if (required == IronBathMaterials.wroughtIron()) {
            ItemStack[] ordinaryIron = Arrays.copyOf(remaining, remaining.length + 2);
            ordinaryIron[remaining.length] = dust(Materials.Iron, amount);
            ordinaryIron[remaining.length + 1] = GTUtility.getIntegratedCircuit(circuit);
            check(
                !produces(
                    map.findRecipeQuery()
                        .items(ordinaryIron)
                        .voltage(128)
                        .find(),
                    dust(output, 1),
                    yield),
                "Ordinary Iron still bypasses Wrought Iron in " + map.unlocalizedName);
        }
    }

    private static void verifyCrafting(Materials output, Materials required, ItemStack... remaining) {
        ItemStack[] paid = Arrays.copyOf(remaining, remaining.length + 1);
        paid[remaining.length] = dust(required, 1);
        ItemStack result = craft(paid);
        check(
            result != null && result.isItemEqual(dust(output, 1)) && result.stackSize == 2,
            "Paid hand mixture lost its native yield for " + output);
        ItemStack unpaid = craft(remaining);
        check(unpaid == null || !unpaid.isItemEqual(dust(output, 1)), "Hand mixture bypasses " + required);
    }

    private static void requireGatedMixerOutputs(RecipeMap<?> map, Materials output, Materials required) {
        int checked = 0;
        for (GTRecipe recipe : map.getAllRecipes()) {
            if (Arrays.stream(recipe.mOutputs)
                .noneMatch(item -> item != null && item.isItemEqual(dust(output, 1)))) continue;
            if (containsMaterial(recipe.mInputs, output)) continue;
            checked++;
            check(
                containsMaterial(recipe.mInputs, required),
                "Another " + map.unlocalizedName + " recipe bypasses " + required + " for " + output);
        }
        check(checked > 0, "No raw " + output + " production was checked in " + map.unlocalizedName);
    }

    private static boolean containsMaterial(ItemStack[] items, Materials material) {
        for (ItemStack item : items) {
            ItemData data = GTOreDictUnificator.getItemData(item);
            if (data != null && data.mMaterial != null && data.mMaterial.mMaterial == material) return true;
        }
        return false;
    }

    private static ItemStack craft(ItemStack... inputs) {
        InventoryCrafting grid = new InventoryCrafting(new Container() {

            @Override
            public boolean canInteractWith(EntityPlayer player) {
                return false;
            }
        }, 3, 3);
        for (int slot = 0; slot < inputs.length; slot++) grid.setInventorySlotContents(slot, inputs[slot].copy());
        return CraftingManager.getInstance()
            .findMatchingRecipe(grid, null);
    }

    private static boolean produces(GTRecipe recipe, ItemStack output, int amount) {
        if (recipe == null) return false;
        for (ItemStack product : recipe.mOutputs)
            if (product != null && product.isItemEqual(output) && product.stackSize == amount) return true;
        return false;
    }

    private static ItemStack dust(Materials material, int amount) {
        return item(OrePrefixes.dust, material, amount);
    }

    private static ItemStack item(OrePrefixes prefix, Materials material, int amount) {
        ItemStack stack = GTOreDictUnificator.get(prefix, material, amount);
        check(stack != null && stack.getItem() != null, "Missing native fixture " + prefix + material);
        return stack;
    }

    private static ItemStack[] copy(ItemStack[] inputs) {
        return Arrays.stream(inputs)
            .map(ItemStack::copy)
            .toArray(ItemStack[]::new);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
