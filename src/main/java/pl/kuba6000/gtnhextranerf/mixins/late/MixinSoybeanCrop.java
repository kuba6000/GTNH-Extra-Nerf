package pl.kuba6000.gtnhextranerf.mixins.late;

import net.minecraft.item.Item;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import pl.kuba6000.gtnhextranerf.items.BlightedSoybean;

@Pseudo
@Mixin(targets = "com.pam.harvestcraft.BlockPamCrop", remap = false)
public abstract class MixinSoybeanCrop {

    @ModifyReturnValue(method = { "func_149865_P", "func_149866_i" }, at = @At("RETURN"), require = 2)
    private Item gtnhextranerf$blightSoybean(Item original) {
        return BlightedSoybean.replaceCropDrop(original);
    }
}
