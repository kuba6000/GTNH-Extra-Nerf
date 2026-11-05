package pl.kuba6000.gtnhextranerf.integration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.gtnewhorizons.horizonqa.api.TestPos;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.Materials;
import gregtech.api.enums.Mods;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.SubTag;
import gregtech.api.objects.ItemData;
import gregtech.api.util.GTModHandler;
import gregtech.api.util.GTOreDictUnificator;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.CrucibleRecipe;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchItem;
import thaumcraft.api.research.ResearchPage;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.lib.crafting.ThaumcraftCraftingManager;
import thaumcraft.common.lib.research.ResearchManager;
import thaumcraft.common.tiles.TileCrucible;

/** Checks alchemical transmutation through a real crucible and its research page. */
final class MetalTransmutationIntegrationTests {

    private static final String METAL_RESEARCH = "METALLURGICPERFECTION_TRANSMUTATION";

    private MetalTransmutationIntegrationTests() {}

    static void verifyNoNuggetDuplication() {
        List<String> unnerfed = new ArrayList<>();
        for (Object entry : ThaumcraftApi.getCraftingRecipes()) {
            if (!(entry instanceof CrucibleRecipe)) continue;
            CrucibleRecipe recipe = (CrucibleRecipe) entry;
            ItemStack output = recipe.getRecipeOutput();
            if (output == null || output.stackSize <= 1) continue;
            ItemData data = GTOreDictUnificator.getAssociation(output);
            if (data != null && data.mPrefix == OrePrefixes.nugget
                && data.mMaterial != null
                && data.mMaterial.mMaterial != null
                && data.mMaterial.mMaterial.contains(SubTag.METAL)
                && recipe.catalystMatches(output)) unnerfed.add(recipe.key + " / " + data + " x" + output.stackSize);
        }
        check(unnerfed.isEmpty(), "Crucible still multiplies metal nuggets: " + unnerfed);
    }

    static void verifyIndependentResearchPages() {
        verifyResearch("TRANSIRON", Materials.Iron, cost());
        verifyResearch("TRANSGOLD", Materials.Gold, cost(Aspect.GREED));
        verifyResearch("TRANSCOPPER", Materials.Copper, cost(Aspect.EXCHANGE));
        verifyResearch("TRANSTIN", Materials.Tin, cost(Aspect.CRYSTAL));
        verifyResearch("TRANSSILVER", Materials.Silver, cost(Aspect.GREED));
        verifyResearch("TRANSLEAD", Materials.Lead, cost(Aspect.ORDER));
        verifyResearch("GT_TRANSZINC", Materials.Zinc, cost(Aspect.HEAL));
        verifyResearch("GT_TRANSANTIMONY", Materials.Antimony, cost(Aspect.WATER));
        verifyResearch("GT_TRANSNICKEL", Materials.Nickel, cost(Aspect.FIRE));
        verifyResearch("GT_TRANSCOBALT", Materials.Cobalt, cost(Aspect.TOOL));
        verifyResearch("GT_TRANSBISMUTH", Materials.Bismuth, cost(Aspect.TOOL));
        verifyResearch("GT_TRANSBRONZE", Materials.Bronze, cost(Aspect.TOOL));
        verifyResearch("GT_TRANSELECTRUM", Materials.Electrum, cost(Aspect.GREED));
        verifyResearch("GT_TRANSBRASS", Materials.Brass, cost(Aspect.TOOL));
        verifyResearch("GT_TRANSINVAR", Materials.Invar, cost(Aspect.COLD));
        verifyResearch("GT_TRANSCUPRONICKEL", Materials.Cupronickel, cost(Aspect.EXCHANGE, Aspect.FIRE));
        verifyResearch("GT_TRANSBATTERYALLOY", Materials.BatteryAlloy, cost(Aspect.WATER, Aspect.ORDER));
        verifyResearch("GT_TRANSSOLDERINGALLOY", Materials.SolderingAlloy, cost(Aspect.WATER, Aspect.CRYSTAL));
        verifyResearch("GT_TRANSALUMINIUM", Materials.Aluminium, cost(Aspect.FLIGHT, Aspect.ORDER, Aspect.FIRE));
    }

    private static AspectList cost(Aspect... additional) {
        AspectList aspects = new AspectList().add(Aspect.METAL, 8);
        for (Aspect aspect : additional) aspects.add(aspect, 4);
        return aspects;
    }

    private static void verifyResearch(String key, Materials material, AspectList cost) {
        ResearchItem research = ResearchCategories.getResearch(key);
        check(research != null, "Metal research is missing: " + key);
        ItemStack nugget = item(OrePrefixes.nugget, material);
        ItemStack dust = item(OrePrefixes.dustTiny, material);
        CrucibleRecipe found = null;
        for (ResearchPage page : research.getPages()) {
            if (!(page.recipe instanceof CrucibleRecipe)) continue;
            CrucibleRecipe recipe = (CrucibleRecipe) page.recipe;
            if (!key.equals(recipe.key) || !recipe.catalystMatches(nugget)) continue;
            check(found == null, "Multiple transmutations under " + key);
            found = recipe;
            check(
                page.recipeOutput != null && page.recipeOutput.isItemEqual(dust) && page.recipeOutput.stackSize == 2,
                "Wrong Thaumonomicon result for " + key);
        }
        check(found != null, "Missing Thaumonomicon recipe for " + key);
        check(
            found.getRecipeOutput()
                .isItemEqual(dust) && found.getRecipeOutput().stackSize == 2,
            "Wrong crucible output for " + key);
        check(found.aspects.size() == cost.size(), "Wrong number of aspects for " + key);
        for (Aspect aspect : cost.getAspects())
            check(found.aspects.getAmount(aspect) == cost.getAmount(aspect), "Wrong aspect cost for " + key);
        check(found.matches(cost, nugget), "Crucible rejects the displayed recipe for " + key);
        check(!found.catalystMatches(item(OrePrefixes.ingot, material)), "An ingot replaces the nugget for " + key);
    }

    static void verifyEmerald() {
        EntityPlayerMP player = (EntityPlayerMP) MinecraftServer.getServer()
            .getConfigurationManager().playerEntityList.get(0);
        String name = player.getCommandSenderName();
        ResearchManager.completeResearchUnsaved(name, "TRANSEMERALD");
        ItemStack emerald = GTModHandler.getModItem(Mods.ForbiddenMagic.ID, "FMResource", 1, 0);
        check(emerald != null, "Missing emerald fragment");
        AspectList aspects = new AspectList().add(Aspect.GREED, 4)
            .add(Aspect.CRYSTAL, 4);
        CrucibleRecipe recipe = ThaumcraftCraftingManager.findMatchingCrucibleRecipe(name, aspects, emerald);
        check(recipe != null && "TRANSEMERALD".equals(recipe.key), "Emerald transmutation is missing");
        check(
            recipe.getRecipeOutput()
                .isItemEqual(emerald) && recipe.getRecipeOutput().stackSize == 4,
            "Metal nerf changed emerald transmutation");
    }

    static void verifyAluminium(WorldServer world, TestPos pos) {
        EntityPlayerMP player = (EntityPlayerMP) MinecraftServer.getServer()
            .getConfigurationManager().playerEntityList.get(0);
        String name = player.getCommandSenderName();
        String researchKey = "GT_TRANSALUMINIUM";
        ResearchManager.completeResearchUnsaved(name, researchKey);
        ItemStack nugget = item(OrePrefixes.nugget, Materials.Aluminium);
        ItemStack dust = item(OrePrefixes.dustTiny, Materials.Aluminium);
        AspectList aspects = new AspectList().add(Aspect.METAL, 8)
            .add(Aspect.FLIGHT, 4)
            .add(Aspect.ORDER, 4)
            .add(Aspect.FIRE, 4);
        CrucibleRecipe recipe = ThaumcraftCraftingManager.findMatchingCrucibleRecipe(name, aspects, nugget);
        check(recipe != null && researchKey.equals(recipe.key), "Aluminium research no longer accepts its nugget");
        ItemStack tinkersNugget = GTModHandler.getModItem(Mods.TinkerConstruct.ID, "materials", 1, 22);
        check(
            tinkersNugget != null && recipe.catalystMatches(tinkersNugget),
            "Aluminium research no longer accepts the Tinkers nugget");
        check(
            recipe.getRecipeOutput()
                .isItemEqual(dust) && recipe.getRecipeOutput().stackSize == 2,
            "Aluminium transmutation does not produce two tiny dusts: " + recipe.getRecipeOutput());
        for (Aspect aspect : aspects.getAspects()) check(
            recipe.aspects.getAmount(aspect) == aspects.getAmount(aspect),
            "Aluminium transmutation has the wrong aspect cost for " + aspect.getTag());
        ResearchItem research = ResearchCategories.getResearch(researchKey);
        boolean displayed = false;
        for (ResearchPage page : research.getPages()) {
            if (page.recipe == recipe && page.recipeOutput != null && page.recipeOutput.isItemEqual(dust)) {
                displayed = true;
                break;
            }
        }
        check(displayed, "Thaumonomicon does not show the usable aluminium recipe");

        check(
            world.setBlock(pos.x(), pos.y(), pos.z(), ConfigBlocks.blockMetalDevice, 0, 3),
            "Could not place a crucible");
        try {
            TileCrucible crucible = (TileCrucible) world.getTileEntity(pos.x(), pos.y(), pos.z());
            crucible.tank.fill(new FluidStack(FluidRegistry.WATER, 1_000), true);
            for (Aspect aspect : aspects.getAspects()) crucible.getAspects()
                .add(aspect, aspects.getAmount(aspect));
            Set<Object> previous = new HashSet<>(world.loadedEntityList);
            EntityItem input = new EntityItem(world, pos.x() + 0.5, pos.y() + 0.5, pos.z() + 0.5, nugget.copy());
            input.getEntityData()
                .setString("thrower", name);
            crucible.attemptSmelt(input);
            boolean produced = world.loadedEntityList.stream()
                .filter(entity -> entity instanceof EntityItem && !previous.contains(entity))
                .map(entity -> ((EntityItem) entity).getEntityItem())
                .anyMatch(stack -> stack.isItemEqual(dust) && stack.stackSize == 2);
            check(produced, "The real crucible did not produce tiny aluminium dust");
        } finally {
            world.setBlockToAir(pos.x(), pos.y(), pos.z());
        }
    }

    static void verifyAllGeneratedMetals() {
        ResearchItem research = ResearchCategories.getResearch(METAL_RESEARCH);
        check(research != null, "The metal transmutation research is missing");
        int checked = 0;
        for (Materials material : GregTechAPI.sGeneratedMaterials) {
            if (material == null || !material.contains(SubTag.TRANSMUTABLE_NUGGETS)) continue;
            ItemStack nugget = GTOreDictUnificator.get(OrePrefixes.nugget, material, 1);
            if (nugget == null) continue;
            ItemStack expected = item(OrePrefixes.dustTiny, material);
            CrucibleRecipe match = null;
            for (ResearchPage page : research.getPages()) {
                if (!(page.recipe instanceof CrucibleRecipe)) continue;
                CrucibleRecipe recipe = (CrucibleRecipe) page.recipe;
                if (!METAL_RESEARCH.equals(recipe.key) || !recipe.catalystMatches(nugget)) continue;
                check(match == null, "More than one research recipe matches " + material);
                match = recipe;
                check(
                    page.recipeOutput != null && page.recipeOutput.isItemEqual(expected)
                        && page.recipeOutput.stackSize == 2,
                    "Wrong research output for " + material);
            }
            check(match != null, "Missing transmutation for " + material);
            check(
                match.getRecipeOutput()
                    .isItemEqual(expected) && match.getRecipeOutput().stackSize == 2,
                "Wrong transmutation output for " + material);
            for (Aspect aspect : match.aspects.getAspects()) check(
                match.aspects.getAmount(aspect) > 0 && match.aspects.getAmount(aspect) % 4 == 0,
                "Unscaled transmutation aspect for " + material);
            checked++;
        }
        check(checked > 0, "No GT metal transmutations were checked");
    }

    static void verifySteelDuplication(WorldServer world, TestPos pos) {
        EntityPlayerMP player = (EntityPlayerMP) MinecraftServer.getServer()
            .getConfigurationManager().playerEntityList.get(0);
        String name = player.getCommandSenderName();
        ResearchManager.completeResearchUnsaved(name, METAL_RESEARCH);

        ItemStack ingot = item(OrePrefixes.ingot, Materials.Steel);
        ItemStack nugget = item(OrePrefixes.nugget, Materials.Steel);
        ItemStack dust = item(OrePrefixes.dustTiny, Materials.Steel);
        AspectList aspects = new AspectList().add(Aspect.METAL, 8)
            .add(Aspect.ORDER, 4);
        CrucibleRecipe recipe = ThaumcraftCraftingManager.findMatchingCrucibleRecipe(name, aspects, nugget);
        check(
            recipe != null && METAL_RESEARCH.equals(recipe.key),
            "Steel nugget is not accepted by the metal research");
        check(
            recipe.getRecipeOutput()
                .isItemEqual(dust) && recipe.getRecipeOutput().stackSize == 2,
            "Steel transmutation does not produce two tiny dusts");
        check(
            recipe.aspects.getAmount(Aspect.METAL) == 8 && recipe.aspects.getAmount(Aspect.ORDER) == 4,
            "Steel transmutation does not require four times its original aspects");
        check(!recipe.catalystMatches(ingot), "A steel ingot incorrectly matches the nugget recipe");
        check(
            !recipe.matches(
                new AspectList().add(Aspect.METAL, 7)
                    .add(Aspect.ORDER, 4),
                nugget),
            "Steel transmutation accepted insufficient Metallum");

        ResearchItem research = ResearchCategories.getResearch(METAL_RESEARCH);
        boolean displayed = false;
        for (ResearchPage page : research.getPages()) {
            if (page.recipe == recipe && page.recipeOutput != null && page.recipeOutput.isItemEqual(dust)) {
                displayed = true;
                break;
            }
        }
        check(displayed, "Thaumonomicon does not show the usable steel recipe");

        check(
            world.setBlock(pos.x(), pos.y(), pos.z(), ConfigBlocks.blockMetalDevice, 0, 3),
            "Could not place a crucible");
        try {
            TileCrucible crucible = (TileCrucible) world.getTileEntity(pos.x(), pos.y(), pos.z());
            crucible.tank.fill(new FluidStack(FluidRegistry.WATER, 1_000), true);
            crucible.getAspects()
                .add(Aspect.METAL, 8)
                .add(Aspect.ORDER, 4);
            Set<Object> previous = new HashSet<>(world.loadedEntityList);
            EntityItem input = new EntityItem(world, pos.x() + 0.5, pos.y() + 0.5, pos.z() + 0.5, nugget.copy());
            input.getEntityData()
                .setString("thrower", name);
            crucible.attemptSmelt(input);
            boolean produced = world.loadedEntityList.stream()
                .filter(entity -> entity instanceof EntityItem && !previous.contains(entity))
                .map(entity -> ((EntityItem) entity).getEntityItem())
                .anyMatch(stack -> stack.isItemEqual(dust) && stack.stackSize == 2);
            check(produced, "The real crucible did not produce tiny steel dust");
            check(
                crucible.getAspects()
                    .getAmount(Aspect.METAL) == 0
                    && crucible.getAspects()
                        .getAmount(Aspect.ORDER) == 0,
                "The crucible did not consume the required aspects");
        } finally {
            world.setBlockToAir(pos.x(), pos.y(), pos.z());
        }
    }

    static void verifyIronToSteel(WorldServer world, TestPos pos) {
        EntityPlayerMP player = (EntityPlayerMP) MinecraftServer.getServer()
            .getConfigurationManager().playerEntityList.get(0);
        String name = player.getCommandSenderName();
        String researchKey = "GT_IRON_TO_STEEL";
        ResearchManager.completeResearchUnsaved(name, researchKey);
        ItemStack ingot = item(OrePrefixes.ingot, Materials.Iron);
        ItemStack nugget = item(OrePrefixes.nugget, Materials.Iron);
        ItemStack dust = item(OrePrefixes.dustTiny, Materials.Steel);
        AspectList aspects = new AspectList().add(Aspect.ORDER, 4);
        CrucibleRecipe recipe = ThaumcraftCraftingManager.findMatchingCrucibleRecipe(name, aspects, ingot);
        check(recipe != null && researchKey.equals(recipe.key), "Iron ingot is not accepted for steel transmutation");
        check(
            recipe.getRecipeOutput()
                .isItemEqual(dust) && recipe.getRecipeOutput().stackSize == 1,
            "Iron transmutation does not produce one tiny steel dust");
        check(recipe.aspects.getAmount(Aspect.ORDER) == 4, "Iron transmutation does not require four Ordo");
        check(!recipe.catalystMatches(nugget), "Iron nugget still matches the steel transmutation recipe");
        ResearchItem research = ResearchCategories.getResearch(researchKey);
        boolean displayed = false;
        for (ResearchPage page : research.getPages()) {
            if (page.recipe == recipe && page.recipeOutput != null && page.recipeOutput.isItemEqual(dust)) {
                displayed = true;
                break;
            }
        }
        check(displayed, "Thaumonomicon does not show the usable iron-to-steel recipe");

        check(
            world.setBlock(pos.x(), pos.y(), pos.z(), ConfigBlocks.blockMetalDevice, 0, 3),
            "Could not place a crucible");
        try {
            TileCrucible crucible = (TileCrucible) world.getTileEntity(pos.x(), pos.y(), pos.z());
            crucible.tank.fill(new FluidStack(FluidRegistry.WATER, 1_000), true);
            crucible.getAspects()
                .add(Aspect.ORDER, 4);
            Set<Object> previous = new HashSet<>(world.loadedEntityList);
            EntityItem input = new EntityItem(world, pos.x() + 0.5, pos.y() + 0.5, pos.z() + 0.5, ingot.copy());
            input.getEntityData()
                .setString("thrower", name);
            crucible.attemptSmelt(input);
            boolean produced = world.loadedEntityList.stream()
                .filter(entity -> entity instanceof EntityItem && !previous.contains(entity))
                .map(entity -> ((EntityItem) entity).getEntityItem())
                .anyMatch(stack -> stack.isItemEqual(dust) && stack.stackSize == 1);
            check(produced, "The real crucible did not produce tiny steel dust from iron");
            check(
                crucible.getAspects()
                    .getAmount(Aspect.ORDER) == 0,
                "The crucible did not consume the required Ordo");
        } finally {
            world.setBlockToAir(pos.x(), pos.y(), pos.z());
        }
    }

    private static ItemStack item(OrePrefixes prefix, Materials material) {
        ItemStack stack = GTOreDictUnificator.get(prefix, material, 1);
        if (stack == null) throw new AssertionError("Missing material form " + prefix + material);
        return stack;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
