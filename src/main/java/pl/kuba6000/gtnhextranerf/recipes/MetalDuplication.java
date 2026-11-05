package pl.kuba6000.gtnhextranerf.recipes;

import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.SubTag;
import gregtech.api.objects.ItemData;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTUtility;
import pl.kuba6000.gtnhextranerf.audit.AuditManager;
import pl.kuba6000.gtnhextranerf.audit.AuditScope;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.CrucibleRecipe;

/** Recognizes a crucible recipe that multiplies a nugget of the same GT material. */
public final class MetalDuplication {

    private static final AtomicInteger MODIFIED = new AtomicInteger();

    private MetalDuplication() {}

    public static ItemStack replacement(String research, ItemStack output, Object catalyst, AspectList aspects) {
        if (!GTUtility.isStackValid(output) || output.stackSize <= 1 || catalyst == null || aspects == null)
            return null;
        Materials material = material(output);
        if (material == null || !material.contains(SubTag.METAL)) return null;
        ItemStack nugget = GTOreDictUnificator.get(OrePrefixes.nugget, material, 1);
        CrucibleRecipe original = new CrucibleRecipe(research, output, catalyst, aspects);
        if (!original.catalystMatches(output) && (nugget == null || !original.catalystMatches(nugget))
            && OreDictionary.getOres(
                OrePrefixes.nugget.get(material)
                    .toString())
                .stream()
                .noneMatch(original::catalystMatches))
            return null;
        ItemStack dust = GTOreDictUnificator.get(OrePrefixes.dustTiny, material, 2);
        if (!GTUtility.isStackValid(dust))
            throw new RecipeValidationException("Missing tiny dust for metal duplication: " + material);
        return dust;
    }

    private static Materials material(ItemStack output) {
        ItemData data = GTOreDictUnificator.getAssociation(output);
        if (data != null && data.mPrefix == OrePrefixes.nugget && data.mMaterial != null)
            return data.mMaterial.mMaterial;
        for (int oreId : OreDictionary.getOreIDs(output)) {
            String name = OreDictionary.getOreName(oreId);
            if (!name.startsWith("nugget")) continue;
            Materials material = Materials.get(name.substring("nugget".length()));
            if (material != Materials._NULL) return material;
        }
        return null;
    }

    public static AspectList scaledAspects(AspectList original) {
        AspectList scaled = new AspectList();
        for (Aspect aspect : original.getAspects())
            scaled.add(aspect, Math.multiplyExact(original.getAmount(aspect), 4));
        return scaled;
    }

    public static void registered() {
        MODIFIED.incrementAndGet();
    }

    public static void verify() {
        AuditManager.run(
            AuditScope.MODIFY_OTHER_METAL_TRANSMUTATIONS,
            audit -> { for (int count = MODIFIED.get(); count > 0; count--) audit.recipeModified(); });
    }
}
