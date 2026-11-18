package pl.kuba6000.gtnhextranerf.mixins.late;

import java.util.Objects;

import net.minecraft.block.Block;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.enums.Mods;
import gregtech.api.enums.VoltageIndex;
import gregtech.api.util.GlassTier;
import pl.kuba6000.gtnhextranerf.audit.AuditCounts;
import pl.kuba6000.gtnhextranerf.audit.AuditManager;
import pl.kuba6000.gtnhextranerf.audit.AuditScope;

@Mixin(value = GlassTier.RegisterGlassTiers.class, remap = false)
public class MixinBotaniaGlassTiers {

    @WrapOperation(
        method = "registerGlassAsTiered",
        at = @At(
            value = "INVOKE",
            target = "Lgregtech/api/util/GlassTier;addCustomGlass(Ljava/lang/String;Ljava/lang/String;III)V"),
        require = 3)
    private static void lowerBotaniaGlassTier(String modId, String name, int meta, int tier, int variant,
        Operation<Void> original) {
        int hvVariant = Mods.Botania.ID.equals(modId) && meta == 0 ? switch (name) {
            case "manaGlass" -> tier == VoltageIndex.EV ? 8 : -1;
            case "elfGlass" -> tier == VoltageIndex.IV ? 9 : -1;
            case "bifrostPerm" -> tier == VoltageIndex.IV ? 10 : -1;
            default -> -1;
        } : -1;
        if (hvVariant < 0) {
            original.call(modId, name, meta, tier, variant);
            return;
        }

        // GT reserves HV slots 0 through 7 for borosilicate, vanilla and Ender IO glass
        AuditManager.run(AuditScope.LOWER_BOTANIA_GLASS_TIERS, audit -> {
            original.call(modId, name, meta, VoltageIndex.HV, hvVariant);
            Block glass = GameRegistry.findBlock(modId, name);
            if (glass != null && Objects.equals(GlassTier.getGlassBlockTier(glass, meta), VoltageIndex.HV)) {
                audit.record(new AuditCounts(1, 0, 0));
            }
        });
    }
}
