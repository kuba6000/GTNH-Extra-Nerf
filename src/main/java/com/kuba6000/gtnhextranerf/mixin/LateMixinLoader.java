package com.kuba6000.gtnhextranerf.mixin;

import java.util.List;
import java.util.Set;

import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import com.gtnewhorizon.gtnhmixins.LateMixin;

@LateMixin
public class LateMixinLoader implements ILateMixinLoader {

    @Override
    public String getMixinConfig() {
        return "mixins.gtnhextranerf.late.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedMods) {
        return Mixin.getLateMixins(loadedMods);
    }

}
