package pl.kuba6000.gtnhextranerf.integration;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fluids.FluidStack;

import cpw.mods.fml.common.registry.GameRegistry;
import goodgenerator.api.recipe.GoodGeneratorRecipeMaps;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.Mods;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicMachine;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTModHandler;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
import pl.kuba6000.gtnhextranerf.items.MagicnetizedRods;
import pl.kuba6000.gtnhextranerf.recipes.machine.RecipeMatch;

final class MagicnetizedRodIntegrationTests {

    private MagicnetizedRodIntegrationTests() {}

    static void verify(WorldServer world) {
        Item item = GameRegistry.findItem("gtnhextranerf", "magicnetized_void_metal_rod");
        check(item != null, "Magicnetized Void Metal Rod is missing");
        ItemStack magicVoid = new ItemStack(item);
        verifyHvMotor(magicVoid);
        verifyRodRecipes(magicVoid);
        verifyAssembler(world, magicVoid);
    }

    private static void verifyAssembler(WorldServer world, ItemStack magicVoid) {
        for (IMetaTileEntity prototype : GregTechAPI.METATILEENTITIES) {
            if (prototype instanceof MTEBasicMachine
                && ((MTEBasicMachine) prototype).mName.startsWith("basicmachine.assembler.tier.")) {
                check(
                    ((MTEBasicMachine) prototype).mOutputItems.length == 2,
                    "Assembler prototype has the wrong output capacity: " + ((MTEBasicMachine) prototype).mName);
            }
        }
        BaseMetaTileEntity tile = TransformerIntegrationTests.place(world, ItemList.Machine_MV_Assembler.get(1));
        try {
            MTEBasicMachine machine = (MTEBasicMachine) tile.getMetaTileEntity();
            check(machine.mOutputItems.length == 2, "Placed Assembler lost its second output");
            ItemStack pair = MagicnetizedRods.manasteel();
            pair.stackSize = 2;
            ItemStack[] inputs = { ore(OrePrefixes.stick, Materials.Void, 1), pair,
                ore(OrePrefixes.foil, Materials.SteelMagnetic, 4),
                GTModHandler.getModItem(Mods.Thaumcraft.ID, "ItemResource", 2, 14), flower("solegnolia") };
            for (int i = 0; i < inputs.length; i++)
                machine.setInventorySlotContents(machine.getInputSlot() + i, inputs[i]);
            machine.setFillableStack(Materials.Mercury.getFluid(144));
            check(machine.checkRecipe() == 2, "Native MV Assembler rejected the Void Metal recipe");
            check(same(magicVoid, machine.mOutputItems[0]), "Native Assembler lost the new rod");
            ItemStack returned = ore(OrePrefixes.stick, Materials.Manasteel, 2);
            check(same(returned, machine.mOutputItems[1]), "Native Assembler lost the returned Manasteel Rods");
            NBTTagCompound saved = new NBTTagCompound();
            machine.saveNBTData(saved);
            machine.mOutputItems[0] = null;
            machine.mOutputItems[1] = null;
            machine.loadNBTData(saved);
            check(
                same(magicVoid, machine.mOutputItems[0]) && same(returned, machine.mOutputItems[1]),
                "Assembler lost pending outputs after saving and loading");
            machine.mProgresstime = machine.mMaxProgresstime - 1;
            tile.setStoredEU(tile.getEUCapacity());
            machine.onPostTick(tile, 100);
            check(
                same(magicVoid, machine.getStackInSlot(machine.getOutputSlot())),
                "Assembler did not deliver the magicnetized rod");
            check(
                same(returned, machine.getStackInSlot(machine.getOutputSlot() + 1)),
                "Assembler did not deliver the returned Manasteel Rods");
            check(
                same(flower("solegnolia"), machine.getStackInSlot(machine.getInputSlot() + 4)),
                "Native Assembler consumed Solegnolia");
        } finally {
            world.setBlockToAir(tile.xCoord, tile.yCoord, tile.zCoord);
        }
    }

    private static void verifyHvMotor(ItemStack magic) {
        ItemStack motor = ItemList.Electric_Motor_HV.get(1);
        ItemStack cable = ore(OrePrefixes.cableGt02, Materials.Silver, 1);
        ItemStack wire = ore(OrePrefixes.wireGt04, Materials.Electrum, 1);
        ItemStack steel = ore(OrePrefixes.stick, Materials.StainlessSteel, 1);
        ItemStack magnetic = ore(OrePrefixes.stick, Materials.SteelMagnetic, 1);
        check(
            same(motor, craft(cable, wire, steel, wire, magic, wire, steel, wire, cable)),
            "Magicnetized HV motor crafting is missing");
        check(
            !same(motor, craft(cable, wire, steel, wire, magnetic, wire, steel, wire, cable)),
            "Old HV motor crafting survived");
        RecipeMatch updated = RecipeMatch.builder()
            .itemInputs(
                magic,
                ore(OrePrefixes.stick, Materials.StainlessSteel, 2),
                ore(OrePrefixes.wireGt04, Materials.Electrum, 4),
                ore(OrePrefixes.cableGt02, Materials.Silver, 2))
            .itemOutputs(motor)
            .fluidInputs()
            .fluidOutputs()
            .duration(20)
            .eut(30)
            .build();
        check(
            RecipeMaps.assemblerRecipes.getAllRecipes()
                .stream()
                .filter(updated)
                .count() == 1,
            "Expected one Magicnetized HV motor assembler recipe");
        for (RecipeMap<?> map : RecipeMap.ALL_RECIPE_MAPS.values()) {
            if (map == GoodGeneratorRecipeMaps.componentAssemblyLineRecipes) continue;
            for (GTRecipe recipe : map.getAllRecipes()) {
                if (!produces(recipe, motor)) continue;
                check(contains(recipe.mInputs, magic), "HV motor bypass in " + map.unlocalizedName);
            }
        }
        RecipeMatch coal = RecipeMatch.builder()
            .itemInputs(
                ore(OrePrefixes.stickLong, Materials.SteelMagnetic, 24),
                ore(OrePrefixes.stickLong, Materials.StainlessSteel, 48),
                ore(OrePrefixes.wireGt16, Materials.Electrum, 48),
                ore(OrePrefixes.cableGt16, Materials.Silver, 12))
            .itemOutputs(ItemList.Electric_Motor_HV.get(64))
            .build();
        check(
            GoodGeneratorRecipeMaps.componentAssemblyLineRecipes.getAllRecipes()
                .stream()
                .filter(coal)
                .count() == 1,
            "Original HV motor COAL recipe is missing");
    }

    private static void verifyRodRecipes(ItemStack magicVoid) {
        ItemStack flower = flower("solegnolia");
        ItemStack salis = GTModHandler.getModItem(Mods.Thaumcraft.ID, "ItemResource", 2, 14);
        ItemStack manasteel = ore(OrePrefixes.stick, Materials.Manasteel, 1);
        ItemStack magicMana = MagicnetizedRods.manasteel();
        ItemStack[] manaInputs = { manasteel.copy(), ore(OrePrefixes.stick, Materials.SteelMagnetic, 2), salis.copy(),
            flower.copy() };
        GTRecipe mana = assembler(magicMana);
        check(
            RecipeMatch.builder()
                .itemInputs(manaInputs[0], manaInputs[1], manaInputs[2], zero(flower))
                .itemOutputs(magicMana, ore(OrePrefixes.stick, Materials.Steel, 2))
                .fluidInputs(Materials.Mercury.getFluid(144))
                .fluidOutputs()
                .duration(400)
                .eut(480)
                .build()
                .test(mana),
            "Wrong automated Manasteel recipe or missing Steel Rod return");
        verifyConsumption(mana, manaInputs);
        ItemStack pair = magicMana.copy();
        pair.stackSize = 2;
        ItemStack[] voidInputs = { ore(OrePrefixes.stick, Materials.Void, 1), pair,
            ore(OrePrefixes.foil, Materials.SteelMagnetic, 4), salis.copy(), flower.copy() };
        ItemStack returned = manasteel.copy();
        returned.stackSize = 2;
        GTRecipe voidRecipe = assembler(magicVoid);
        check(
            RecipeMatch.builder()
                .itemInputs(voidInputs[0], pair, voidInputs[2], salis, zero(flower))
                .itemOutputs(magicVoid, returned)
                .fluidInputs(Materials.Mercury.getFluid(144))
                .fluidOutputs()
                .duration(600)
                .eut(120)
                .build()
                .test(voidRecipe),
            "Wrong Void Metal recipe or Manasteel return");
        verifyConsumption(voidRecipe, voidInputs);
    }

    private static void verifyConsumption(GTRecipe recipe, ItemStack[] inputs) {
        FluidStack[] fluids = { Materials.Mercury.getFluid(144) };
        ItemStack flower = inputs[inputs.length - 1];
        inputs[inputs.length - 1] = null;
        check(!recipe.isRecipeInputEqual(false, fluids, inputs), "Recipe accepts a missing Solegnolia");
        inputs[inputs.length - 1] = flower("jadedAmaranthus");
        check(!recipe.isRecipeInputEqual(false, fluids, inputs), "Recipe accepts a different Botania flower");
        inputs[inputs.length - 1] = flower;
        check(recipe.isRecipeInputEqual(true, fluids, inputs), "Valid magicnetizing inputs were rejected");
        for (int i = 0; i < inputs.length - 1; i++) {
            check(inputs[i] == null || inputs[i].stackSize == 0, "Magicnetizing reagent was not consumed");
        }
        check(flower.stackSize == 1, "Solegnolia catalyst was consumed");
        check(fluids[0] == null || fluids[0].amount == 0, "Mercury was not consumed");
    }

    private static GTRecipe assembler(ItemStack output) {
        GTRecipe result = null;
        for (GTRecipe recipe : RecipeMaps.assemblerRecipes.getAllRecipes()) {
            if (!produces(recipe, output)) continue;
            check(result == null, "Duplicate assembler recipe for " + output);
            result = recipe;
        }
        check(result != null, "Missing assembler recipe for " + output);
        return result;
    }

    private static boolean produces(GTRecipe recipe, ItemStack output) {
        return contains(recipe.mOutputs, output);
    }

    private static boolean contains(ItemStack[] stacks, ItemStack item) {
        for (ItemStack stack : stacks) if (stack != null && stack.isItemEqual(item)) return true;
        return false;
    }

    private static ItemStack zero(ItemStack item) {
        ItemStack catalyst = item.copy();
        catalyst.stackSize = 0;
        return catalyst;
    }

    private static ItemStack flower(String type) {
        ItemStack flower = new ItemStack(GameRegistry.findItem("Botania", "specialFlower"));
        NBTTagCompound tags = new NBTTagCompound();
        tags.setString("type", type);
        flower.setTagCompound(tags);
        return flower;
    }

    private static ItemStack ore(OrePrefixes prefix, Materials material, int amount) {
        return GTOreDictUnificator.get(prefix, material, amount);
    }

    private static ItemStack craft(ItemStack... inputs) {
        InventoryCrafting grid = new InventoryCrafting(new Container() {

            @Override
            public boolean canInteractWith(EntityPlayer player) {
                return false;
            }
        }, 3, 3);
        for (int i = 0; i < inputs.length; i++) grid.setInventorySlotContents(i, inputs[i].copy());
        return CraftingManager.getInstance()
            .findMatchingRecipe(grid, null);
    }

    private static boolean same(ItemStack expected, ItemStack actual) {
        return ItemStack.areItemStacksEqual(expected, actual);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
