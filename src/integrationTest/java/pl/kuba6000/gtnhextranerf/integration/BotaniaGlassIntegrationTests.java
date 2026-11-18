package pl.kuba6000.gtnhextranerf.integration;

import java.util.Objects;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import bartworks.common.loaders.ItemRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.enums.VoltageIndex;
import gregtech.api.util.GlassTier;

@GameTestHolder("gtnhextranerftests")
public final class BotaniaGlassIntegrationTests {

    private BotaniaGlassIntegrationTests() {}

    @GameTest(template = "native_fixture", timeoutTicks = 100)
    public static void structuralTiers(GameTestHelper helper) {
        for (String name : new String[] { "manaGlass", "elfGlass", "bifrostPerm" }) {
            Block block = GameRegistry.findBlock("Botania", name);
            check(block != null, "Missing Botania block: " + name);
            check(Objects.equals(GlassTier.getGlassBlockTier(block, 0), VoltageIndex.HV), name + " must be HV glass");
            ItemStack glass = new ItemStack(block, 1, 0);
            check(accepts("blockGlassHV", glass), name + " missing from HV recipes");
            check(!accepts("blockGlassEV", glass), name + " accepted in EV recipes");
            check(!accepts("blockGlassIV", glass), name + " accepted in IV recipes");
        }
        check(
            Objects.equals(GlassTier.getGlassBlockTier(ItemRegistry.bw_realglas, 1), VoltageIndex.EV),
            "Titanium Reinforced Borosilicate must remain EV");
        check(
            Objects.equals(GlassTier.getGlassBlockTier(ItemRegistry.bw_realglas, 2), VoltageIndex.IV),
            "Tungstensteel Reinforced Borosilicate must remain IV");
        helper.succeed();
    }

    private static boolean accepts(String group, ItemStack glass) {
        // Forge 1.7.10 wraps the ore list without overriding ArrayList's spliterator
        for (ItemStack candidate : OreDictionary.getOres(group)) {
            if (OreDictionary.itemMatches(candidate, glass, false)) return true;
        }
        return false;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
