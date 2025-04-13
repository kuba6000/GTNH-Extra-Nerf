package com.kuba6000.gtnhextranerf.mixin;

import static com.kuba6000.gtnhextranerf.mixin.TargetedMod.VANILLA;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import cpw.mods.fml.relauncher.FMLLaunchHandler;

public enum Mixin {

    ;

    public final String mixinClass;
    public final List<TargetedMod> targetedMods;
    private final Side side;
    private final Phase phase;

    Mixin(String mixinClass, TargetedMod... targetedMods) {
        this.mixinClass = mixinClass;
        this.targetedMods = Arrays.asList(targetedMods);
        this.side = Side.BOTH;
        this.phase = this.targetedMods.size() == 1 && this.targetedMods.contains(VANILLA) ? Phase.EARLY : Phase.LATE;
    }

    public static List<String> getEarlyMixins(Set<String> loadedCoreMods) {
        final List<String> mixins = new ArrayList<>();
        for (Mixin mixin : Mixin.values()) {
            if (mixin.phase == Phase.EARLY) {
                mixins.add(mixin.mixinClass);
            }
        }
        return mixins;
    }

    public static List<String> getLateMixins(Set<String> loadedMods) {
        // NOTE: Any targetmod here needs a modid, not a coremod id
        final List<String> mixins = new ArrayList<>();
        for (Mixin mixin : Mixin.values()) {
            if (mixin.phase == Mixin.Phase.LATE) {
                if (mixin.shouldLoad(loadedMods)) {
                    mixins.add(mixin.mixinClass);
                }
            }
        }
        return mixins;
    }

    private boolean shouldLoad(Set<String> loadedMods) {
        return shouldLoadSide() && allModsLoaded(loadedMods);
    }

    private boolean shouldLoadSide() {
        return side == Side.BOTH || (side == Side.SERVER && FMLLaunchHandler.side()
            .isServer())
            || (side == Side.CLIENT && FMLLaunchHandler.side()
                .isClient());
    }

    private boolean allModsLoaded(Set<String> loadedMods) {
        if (targetedMods.isEmpty()) return false;
        for (TargetedMod target : targetedMods) {
            if (target == VANILLA) continue;
            if (target.modId != null && !loadedMods.isEmpty() && !loadedMods.contains(target.modId)) {
                return false;
            }
        }
        return true;
    }

    private enum Side {
        BOTH,
        CLIENT,
        SERVER
    }

    private enum Phase {
        EARLY,
        LATE,
    }
}
