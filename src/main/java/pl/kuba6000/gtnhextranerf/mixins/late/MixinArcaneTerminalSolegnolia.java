package pl.kuba6000.gtnhextranerf.mixins.late;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;

import appeng.tile.inventory.AppEngInternalInventory;
import pl.kuba6000.gtnhextranerf.items.MagicnetizedRods;
import thaumicenergistics.common.container.ContainerPartArcaneCraftingTerminal;
import thaumicenergistics.common.integration.tc.ArcaneRecipeHelper;

/** Keeps the Solegnolia catalyst in the Arcane Crafting Terminal. */
@Mixin(value = ContainerPartArcaneCraftingTerminal.class, remap = false)
public abstract class MixinArcaneTerminalSolegnolia {

    private static final int CRAFTING_GRID_SIZE = 9;

    /** Reports the catalyst slot as empty to the terminal's consumption loop. */
    @WrapOperation(
        method = "consumeIngredients",
        slice = @Slice(
            from = @At(value = "INVOKE", target = "Lappeng/tile/inventory/AppEngInternalInventory;func_70302_i_()I")),
        at = @At(
            value = "INVOKE",
            target = "Lappeng/tile/inventory/AppEngInternalInventory;func_70301_a(I)Lnet/minecraft/item/ItemStack;"),
        require = 1,
        allow = 1)
    private ItemStack gtnhextranerf$skipCatalyst(AppEngInternalInventory grid, int slot, Operation<ItemStack> original,
        @Local(argsOnly = true) EntityPlayer player, @Local(ordinal = 0) ItemStack result,
        @Share("magicnetizing") LocalBooleanRef magicnetizing) {
        if (slot == 0) {
            // Match the recipe before the terminal consumes any ingredients
            magicnetizing.set(
                MagicnetizedRods.isCrafting(
                    ArcaneRecipeHelper.INSTANCE.createBridgeInventory(grid, 0, CRAFTING_GRID_SIZE),
                    player,
                    result));
        }
        ItemStack input = original.call(grid, slot);
        return magicnetizing.get() && MagicnetizedRods.isSolegnolia(input) ? null : input;
    }
}
