package pl.kuba6000.gtnhextranerf.mixins.late;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Pseudo
@Mixin(targets = "witchinggadgets.common.WGResearch", remap = false)
public abstract class MixinMetalResearchText {

    @ModifyConstant(
        method = "registerResearch",
        constant = @Constant(stringValue = "witchinggadgets_research_page.METALLURGICPERFECTION_TRANSMUTATION.1"),
        require = 1)
    private static String gtnhextranerf$metalDescription(String original) {
        return "gtnhextranerf.research.metal_transmutation";
    }
}
