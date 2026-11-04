package pl.kuba6000.gtnhextranerf.mixins.late;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import pl.kuba6000.gtnhextranerf.items.MagicnetizedRods;
import pl.kuba6000.gtnhextranerf.recipes.crafting.ArcaneWorkbenchSync;
import thaumcraft.common.container.SlotCraftingArcaneWorkbench;

@Mixin(value = SlotCraftingArcaneWorkbench.class, remap = false)
public abstract class MixinArcaneCraftingRemainders {

    @Shadow
    @Final
    private IInventory craftMatrix;

    @Unique
    private boolean gtnhextranerf$magicnetizing;

    @WrapMethod(method = "func_82870_a")
    private void gtnhextranerf$craft(EntityPlayer player, ItemStack result, Operation<Void> original) {
        boolean previous = gtnhextranerf$magicnetizing;
        gtnhextranerf$magicnetizing = MagicnetizedRods.isCrafting(craftMatrix, player, result);
        try {
            original.call(player, result);
            if (gtnhextranerf$magicnetizing && player instanceof EntityPlayerMP) {
                ArcaneWorkbenchSync.request((EntityPlayerMP) player, player.openContainer);
            }
        } finally {
            gtnhextranerf$magicnetizing = previous;
        }
    }

    @WrapOperation(
        method = "func_82870_a",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/item/Item;hasContainerItem(Lnet/minecraft/item/ItemStack;)Z"),
        require = 1)
    private boolean gtnhextranerf$hasRemainder(Item item, ItemStack input, Operation<Boolean> original) {
        return (gtnhextranerf$magicnetizing && MagicnetizedRods.craftingRemainder(input) != null)
            || original.call(item, input);
    }

    @WrapOperation(
        method = "func_82870_a",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/item/Item;getContainerItem(Lnet/minecraft/item/ItemStack;)Lnet/minecraft/item/ItemStack;"),
        require = 1)
    private ItemStack gtnhextranerf$remainder(Item item, ItemStack input, Operation<ItemStack> original) {
        if (gtnhextranerf$magicnetizing) {
            ItemStack remainder = MagicnetizedRods.craftingRemainder(input);
            if (remainder != null) return remainder;
        }
        return original.call(item, input);
    }

    @WrapOperation(
        method = "func_82870_a",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/player/InventoryPlayer;func_70441_a(Lnet/minecraft/item/ItemStack;)Z"),
        require = 1)
    private boolean gtnhextranerf$keepFlower(InventoryPlayer inventory, ItemStack remainder,
        Operation<Boolean> original) {
        if (gtnhextranerf$magicnetizing) {
            ItemStack flower = MagicnetizedRods.craftingRemainder(remainder);
            ItemStack current = craftMatrix.getStackInSlot(7);
            if (flower != null && flower.isItemEqual(remainder)
                && ItemStack.areItemStackTagsEqual(flower, remainder)
                && (current == null
                    || current.isItemEqual(remainder) && ItemStack.areItemStackTagsEqual(current, remainder)
                        && current.stackSize < current.getMaxStackSize())) {
                ItemStack restored = remainder.copy();
                if (current != null) restored.stackSize += current.stackSize;
                craftMatrix.setInventorySlotContents(7, restored);
                return true;
            }
        }
        return original.call(inventory, remainder);
    }
}
