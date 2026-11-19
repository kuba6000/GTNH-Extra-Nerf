package pl.kuba6000.gtnhextranerf.mixins.late;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import forestry.api.apiculture.IAlleleBeeSpeciesCustom;
import gregtech.api.enums.Materials;
import gregtech.common.bees.GTAlleleBeeSpecies;
import gregtech.loaders.misc.GTBeeDefinition;
import pl.kuba6000.gtnhextranerf.audit.AuditCounts;
import pl.kuba6000.gtnhextranerf.audit.AuditManager;
import pl.kuba6000.gtnhextranerf.audit.AuditScope;

/** Removes the Kevlar nugget specialty during species registration. */
@Mixin(value = GTAlleleBeeSpecies.class, remap = false)
public abstract class MixinKevlarBeeProducts {

    @Inject(method = "addSpecialty", at = @At("HEAD"), cancellable = true, require = 1, allow = 1)
    private void gtnhextranerf$removeKevlarNugget(ItemStack specialty, Float chance,
        CallbackInfoReturnable<IAlleleBeeSpeciesCustom> callback) {
        if ((Object) this != GTBeeDefinition.KEVLAR.getSpecies() || specialty == null
            || !Materials.Kevlar.getNuggets(1)
                .isItemEqual(specialty))
            return;
        AuditManager.run(AuditScope.REMOVE_KEVLAR_BEE_NUGGET, audit -> audit.record(new AuditCounts(0, 0, 1)));
        callback.setReturnValue((GTAlleleBeeSpecies) (Object) this);
    }
}
