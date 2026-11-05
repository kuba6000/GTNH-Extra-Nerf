package pl.kuba6000.gtnhextranerf.integration;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;
import com.gtnewhorizons.horizonqa.api.client.ClientScenario;
import com.gtnewhorizons.horizonqa.api.client.ClientTest;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.GuiCraftingRecipe;
import codechicken.nei.recipe.ICraftingHandler;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.util.GTOreDictUnificator;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.lib.research.ResearchManager;

/** Queries the registered NEI alchemy handler with the player's completed research. */
@GameTestHolder(value = "gtnhextranerftests", clientOnly = true)
public final class MetalTransmutationNeiScenarios {

    private MetalTransmutationNeiScenarios() {}

    @GameTest(template = "native_fixture", timeoutTicks = 1200)
    public static void alchemyRecipes(GameTestHelper helper) {
        ClientScenario scenario = ClientTest.scenario(helper)
            .defaultTimeoutTicks(400)
            .server("complete the two researches", () -> {
                EntityPlayerMP player = (EntityPlayerMP) MinecraftServer.getServer()
                    .getConfigurationManager().playerEntityList.get(0);
                String name = player.getCommandSenderName();
                ResearchManager.completeResearchUnsaved(name, "METALLURGICPERFECTION_TRANSMUTATION");
                ResearchManager.completeResearchUnsaved(name, "GT_IRON_TO_STEEL");
            })
            .awaitClient("NEI displays the current alchemy recipes", client -> verifyNei());
        // Let first-world rendering finish before HQA's short client teardown budget starts.
        scenario.serverSequence()
            .thenIdle(200);
        scenario.client("wait for the client thread", client -> {})
            .succeed();
    }

    private static void verifyNei() {
        String name = Minecraft.getMinecraft().thePlayer.getCommandSenderName();
        ResearchManager.completeResearchUnsaved(name, "METALLURGICPERFECTION_TRANSMUTATION");
        ResearchManager.completeResearchUnsaved(name, "GT_IRON_TO_STEEL");
        ICraftingHandler alchemy = null;
        for (ICraftingHandler handler : GuiCraftingRecipe.craftinghandlers) {
            if ("thaumcraft.alchemy".equals(handler.getOverlayIdentifier())) {
                alchemy = handler;
                break;
            }
        }
        check(alchemy != null, "NEI's alchemy handler has not loaded");
        ItemStack dust = item(OrePrefixes.dustTiny, Materials.Steel);
        ICraftingHandler results = alchemy.getRecipeHandler("item", dust);
        check(
            hasRecipe(results, item(OrePrefixes.nugget, Materials.Steel), dust, 2, 8, 4),
            "NEI does not show the updated steel nugget transmutation");
        check(
            hasRecipe(results, item(OrePrefixes.ingot, Materials.Iron), dust, 1, 0, 4),
            "NEI does not show the updated iron-to-steel transmutation");
    }

    private static boolean hasRecipe(ICraftingHandler handler, ItemStack catalyst, ItemStack output, int amount,
        int metal, int order) {
        for (int index = 0; index < handler.numRecipes(); index++) {
            PositionedStack result = handler.getResultStack(index);
            if (result == null || result.item == null
                || !result.item.isItemEqual(output)
                || result.item.stackSize != amount) continue;
            List<PositionedStack> inputs = handler.getIngredientStacks(index);
            boolean hasCatalyst = false;
            int shownMetal = 0, shownOrder = 0;
            for (PositionedStack input : inputs) {
                if (input.contains(catalyst)) hasCatalyst = true;
                ItemStack stack = input.item;
                if (stack == null || !stack.hasTagCompound()) continue;
                String aspect = stack.getTagCompound()
                    .getString("Aspect");
                if (Aspect.METAL.getTag()
                    .equals(aspect)) shownMetal = stack.stackSize;
                if (Aspect.ORDER.getTag()
                    .equals(aspect)) shownOrder = stack.stackSize;
            }
            if (hasCatalyst && shownMetal == metal && shownOrder == order) return true;
        }
        return false;
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
