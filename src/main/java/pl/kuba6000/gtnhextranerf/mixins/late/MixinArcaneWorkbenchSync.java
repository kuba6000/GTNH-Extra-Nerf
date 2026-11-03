package pl.kuba6000.gtnhextranerf.mixins.late;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import pl.kuba6000.gtnhextranerf.items.MagicnetizedManasteelRod;
import thaumcraft.common.container.ContainerArcaneWorkbench;
import thaumcraft.common.tiles.TileArcaneWorkbench;

@Mixin(value = ContainerArcaneWorkbench.class, remap = false)
public abstract class MixinArcaneWorkbenchSync {

    @Shadow
    private TileArcaneWorkbench tileEntity;

    @Unique
    private int gtnhextranerf$clickDepth;

    @WrapMethod(method = "func_75144_a")
    private ItemStack gtnhextranerf$syncShiftCraft(int slot, int button, int mode, EntityPlayer player,
        Operation<ItemStack> original) {
        boolean sync = gtnhextranerf$clickDepth == 0 && slot == 0
            && mode == 1
            && player instanceof EntityPlayerMP
            && MagicnetizedManasteelRod.isCrafting(tileEntity, player, tileEntity.getStackInSlot(9));
        gtnhextranerf$clickDepth++;
        ItemStack result;
        try {
            result = original.call(slot, button, mode, player);
        } finally {
            gtnhextranerf$clickDepth--;
        }
        if (sync && result != null && player.openContainer == (Object) this) {
            ((EntityPlayerMP) player).sendContainerToPlayer(player.openContainer);
        }
        return result;
    }
}
