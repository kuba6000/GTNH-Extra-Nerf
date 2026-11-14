package pl.kuba6000.gtnhextranerf.mixins.late;

import net.minecraft.block.Block;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import forestry.api.apiculture.EnumBeeChromosome;
import forestry.core.genetics.mutations.Mutation;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.MetaTileEntityIDs;
import gregtech.loaders.misc.GTBeeDefinition;
import pl.kuba6000.gtnhextranerf.audit.AuditCounts;
import pl.kuba6000.gtnhextranerf.audit.AuditManager;
import pl.kuba6000.gtnhextranerf.audit.AuditScope;

/** Changes the Kevlar resource before Forestry creates the mutation condition and description. */
@Mixin(value = Mutation.class, remap = false)
public abstract class MixinKevlarBeeMutation {

    @ModifyVariable(
        method = "requireResource(Lnet/minecraft/block/Block;I)Lforestry/core/genetics/mutations/Mutation;",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0,
        require = 1)
    private int gtnhextranerf$useZpmReplicator(int metadata, Block block, int originalMetadata) {
        if (block != GregTechAPI.sBlockMachines || metadata != MetaTileEntityIDs.MATTER_REPLICATOR_UHV.ID
            || ((Mutation) (Object) this).getTemplate()[EnumBeeChromosome.SPECIES.ordinal()]
                != GTBeeDefinition.KEVLAR.getSpecies())
            return metadata;
        AuditManager
            .run(AuditScope.MODIFY_KEVLAR_BEE_MUTATION_RESOURCE, audit -> audit.record(new AuditCounts(1, 0, 0)));
        return MetaTileEntityIDs.MATTER_REPLICATOR_ZPM.ID;
    }
}
