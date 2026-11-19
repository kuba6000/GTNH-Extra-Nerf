package pl.kuba6000.gtnhextranerf.mixins.late;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import gregtech.loaders.misc.GTBeeDefinition;
import pl.kuba6000.gtnhextranerf.audit.AuditCounts;
import pl.kuba6000.gtnhextranerf.audit.AuditManager;
import pl.kuba6000.gtnhextranerf.audit.AuditScope;

/** Replaces Kevlar's late-game parent before GregTech adds the mutation to Forestry's registry. */
@Mixin(value = GTBeeDefinition.class, remap = false)
public abstract class MixinKevlarBeeParents {

    @ModifyVariable(
        method = "registerMutation(Lgregtech/loaders/misc/GTBeeDefinition;Lgregtech/loaders/misc/GTBeeDefinition;I)Lforestry/api/apiculture/IBeeMutationCustom;",
        at = @At("HEAD"),
        argsOnly = true,
        index = 2,
        require = 1,
        allow = 1)
    private GTBeeDefinition gtnhextranerf$useNaquadahParent(GTBeeDefinition parent) {
        if ((GTBeeDefinition) (Object) this != GTBeeDefinition.KEVLAR || parent != GTBeeDefinition.INFINITYCATALYST)
            return parent;
        AuditManager.run(AuditScope.MODIFY_KEVLAR_BEE_MUTATION_PARENT, audit -> audit.record(new AuditCounts(1, 0, 0)));
        return GTBeeDefinition.NAQUADAH;
    }
}
