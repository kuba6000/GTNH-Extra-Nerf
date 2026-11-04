package pl.kuba6000.gtnhextranerf.mixins.late;

import java.util.Collection;
import java.util.List;

import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import codechicken.nei.recipe.GuiRecipe;
import eu.usrv.enhancedlootbags.integration.nei.LootBagRecipeHandler;
import pl.kuba6000.gtnhextranerf.client.nei.LootAllowancePreview;

@Mixin(value = LootBagRecipeHandler.class, remap = false)
public abstract class MixinLootBagPreview {

    @WrapOperation(
        method = "handleItemTooltip",
        at = @At(value = "INVOKE", target = "Ljava/util/List;addAll(Ljava/util/Collection;)Z"),
        require = 1)
    private boolean gtnhextranerf$showCurrentAllowance(List<String> current, Collection<String> cached,
        Operation<Boolean> original, GuiRecipe<?> gui, ItemStack stack, List<String> tooltip, int recipe) {
        if (LootAllowancePreview.isController(stack)) return original.call(current, LootAllowancePreview.tooltip());
        boolean changed = original.call(current, cached);
        changed |= current.add(I18n.format("gtnhextranerf.loot.preview.base_chance"));
        changed |= current.add(I18n.format("gtnhextranerf.loot.preview.native_limit"));
        return changed;
    }
}
