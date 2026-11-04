package pl.kuba6000.gtnhextranerf.mixins;

import javax.annotation.Nonnull;

import com.gtnewhorizon.gtnhmixins.builders.ITargetMod;
import com.gtnewhorizon.gtnhmixins.builders.TargetModBuilder;

public enum TargetedMod implements ITargetMod {

    GT5U("gregtech"), // Also matches GT6.
    GT6("gregtech.asm.GT_ASM", "gregapi"), // Can be used to exclude GT6 from the GT5U target.
    DREAMCRAFT("com.dreammaster.coremod.DreamCoreMod", "dreamcraft"),
    GOODGENERATOR("goodgenerator"),
    APPLIED_ENERGISTICS_2("appliedenergistics2"),
    EXTRA_UTILITIES("ExtraUtilities"),
    HARVESTCRAFT("harvestcraft"),
    ENHANCED_LOOT_BAGS("enhancedlootbags"),
    NOT_ENOUGH_ITEMS("NotEnoughItems"),
    BETTER_QUESTING("betterquesting"),
    BQ_STANDARD("bq_standard"),
    CHISEL("chisel"),
    THAUMCRAFT("Thaumcraft");

    private final TargetModBuilder builder;

    TargetedMod(TargetModBuilder builder) {
        this.builder = builder;
    }

    TargetedMod(String modId) {
        this(null, modId, null);
    }

    TargetedMod(String coreModClass, String modId) {
        this(coreModClass, modId, null);
    }

    TargetedMod(String coreModClass, String modId, String targetClass) {
        this.builder = new TargetModBuilder().setCoreModClass(coreModClass)
            .setModId(modId)
            .setTargetClass(targetClass);
    }

    @Nonnull
    @Override
    public TargetModBuilder getBuilder() {
        return builder;
    }
}
