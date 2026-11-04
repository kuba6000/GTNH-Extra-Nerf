package pl.kuba6000.gtnhextranerf.integration;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;
import com.gtnewhorizons.horizonqa.api.client.ClientScenario;
import com.gtnewhorizons.horizonqa.api.client.ClientTest;
import com.gtnewhorizons.horizonqa.client.LwjglInput;

import appeng.api.AEApi;
import codechicken.lib.gui.GuiDraw;
import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.GuiUsageRecipe;
import codechicken.nei.recipe.IUsageHandler;
import codechicken.nei.recipe.Recipe;
import eu.usrv.enhancedlootbags.EnhancedLootBags;
import pl.kuba6000.gtnhextranerf.loot.runtime.ServerLootRuntime;

/** Exercises the actual open NEI tooltip across a real server-side loot claim. */
@GameTestHolder(value = "gtnhextranerfloottests", clientOnly = true)
public final class LootPreviewScenarios {

    private LootPreviewScenarios() {}

    @GameTest(template = "gtnhextranerftests:native_fixture", timeoutTicks = 3600)
    public static void controllerPreviewTracksClaim(GameTestHelper helper) {
        ClientScenario scenario = ClientTest.scenario(helper)
            .defaultTimeoutTicks(400)
            .resizeWindow(1280, 960)
            .client("open the actual lootbag usage screen", client -> openBag(37, 0))
            .awaitScreen(GuiUsageRecipe.class)
            .awaitClient("unspent world allowance is visible", client -> {
                List<String> lines = controllerTooltip();
                check(contains(lines, "1 / 1"), "missing available global allowance: " + lines);
            })
            .awaitClient("ordinary percentages are identified as single draws", client -> {
                List<String> lines = ordinaryLootTooltip();
                check(contains(lines, "%"), "missing native weight percentage: " + lines);
                check(contains(lines, "draw"), "base per-draw percentage is not explained: " + lines);
                check(
                    lines.stream()
                        .anyMatch(
                            line -> line.contains("limit") && (line.contains("stale") || line.contains("out of date")
                                || line.contains("outdated"))),
                    "missing warning that native player limits may be stale: " + lines);
            })
            .server("make the server allowance temporarily unavailable", ServerLootRuntime::stop)
            .awaitClient("unavailable allowance is not guessed", client -> {
                List<String> lines = controllerTooltip();
                check(contains(lines, "unknown"), "missing unknown server state: " + lines);
                check(
                    !contains(lines, "1 / 1") && !contains(lines, "0 / 1") && !contains(lines, "0%"),
                    "unknown server state was presented as a known allowance: " + lines);
            })
            .server("reopen the world ledger", () -> ServerLootRuntime.start(MinecraftServer.getServer()))
            .awaitClient("world allowance is resynchronized", client -> {
                List<String> lines = controllerTooltip();
                check(contains(lines, "1 / 1"), "missing restored global allowance: " + lines);
            })
            .server(
                "claim the controller through a native lootbag and reopen the ledger",
                () -> { LootOpeningIntegrationTests.verify(MinecraftServer.getServer()); })
            .awaitClient("already open NEI reflects the spent allowance", client -> {
                List<String> lines = controllerTooltip();
                check(contains(lines, "0 / 1"), "missing spent global allowance: " + lines);
                check(contains(lines, "0%"), "missing zero effective chance: " + lines);
                check(!contains(lines, "0 / 10"), "native player limit is presented as global: " + lines);
            });
        for (int group : new int[] { 37, 38 }) for (int fortune = 0; fortune <= 3; fortune++) {
            final int bagGroup = group, bagFortune = fortune;
            scenario
                .client(
                    "open lootbag " + bagGroup + " with Fortune " + bagFortune,
                    client -> openBag(bagGroup, bagFortune))
                .awaitClient("spent allowance remains visible", client -> {
                    List<String> lines = controllerTooltip();
                    check(
                        contains(lines, "0 / 1") && contains(lines, "0%"),
                        "spent allowance not visible for " + bagGroup + "/" + bagFortune + ": " + lines);
                    check(!contains(lines, "0 / 10"), "misleading native limit: " + lines);
                });
        }
        scenario.capture("loot-controller-spent")
            .succeed();
    }

    private static void openBag(int group, int fortune) {
        Minecraft mc = Minecraft.getMinecraft();
        mc.displayGuiScreen(new GuiInventory(mc.thePlayer));
        ItemStack bag = new ItemStack(EnhancedLootBags.LootGroupHandler.getLootBagItem(), 1, group);
        if (fortune > 0) bag.addEnchantment(Enchantment.fortune, fortune);
        check(GuiUsageRecipe.openRecipeGui("item", bag), "lootbag usage screen did not open");
        GuiUsageRecipe gui = (GuiUsageRecipe) mc.currentScreen;
        IUsageHandler loot = gui.currenthandlers.stream()
            .filter(handler -> "enhancedlootbags".equals(handler.getHandlerId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing lootbag category"));
        gui.openTargetRecipe(Recipe.RecipeId.of(loot, 0));
    }

    private static List<String> controllerTooltip() {
        ItemStack controller = AEApi.instance()
            .definitions()
            .blocks()
            .controller()
            .maybeStack(1)
            .get();
        return lootTooltip(controller, true);
    }

    private static List<String> ordinaryLootTooltip() {
        return lootTooltip(null, false);
    }

    private static List<String> lootTooltip(ItemStack wanted, boolean controller) {
        Minecraft mc = Minecraft.getMinecraft();
        check(mc.currentScreen instanceof GuiUsageRecipe, "NEI usage screen closed");
        GuiUsageRecipe gui = (GuiUsageRecipe) mc.currentScreen;
        check(
            "enhancedlootbags".equals(
                gui.getHandler()
                    .getHandlerId()),
            "wrong NEI category");
        check(
            !gui.getRecipeIndices()
                .isEmpty(),
            "no visible lootbag recipe");
        int recipe = gui.getRecipeIndices()
            .get(0);
        PositionedStack match = null;
        for (PositionedStack stack : gui.getHandler()
            .getOtherStacks(recipe)) {
            if (stack.item != null && (controller ? stack.item.isItemEqual(wanted)
                : !stack.item.isItemEqual(
                    AEApi.instance()
                        .definitions()
                        .blocks()
                        .controller()
                        .maybeStack(1)
                        .get()))) {
                match = stack;
                break;
            }
        }
        check(match != null, "wanted reward is missing from the lootbag preview");
        Point mouse = GuiDraw.getMousePosition();
        Point relative = gui.getRecipeMousePosition(recipe);
        int x = mouse.x - relative.x + match.relx + 8;
        int y = mouse.y - relative.y + match.rely + 8;
        LwjglInput.move(gui, x, y);
        Point hovered = GuiDraw.getMousePosition();
        return gui.handleItemTooltip(gui, match.item, hovered.x, hovered.y, new ArrayList<>());
    }

    private static boolean contains(List<String> lines, String expected) {
        return lines.stream()
            .anyMatch(line -> line.contains(expected));
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
