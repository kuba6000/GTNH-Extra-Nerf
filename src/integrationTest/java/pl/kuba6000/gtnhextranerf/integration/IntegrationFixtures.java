package pl.kuba6000.gtnhextranerf.integration;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

/** Registers recipe alternatives before GregTech finishes loading. */
@Mod(
    modid = "extranerftestfixtures",
    version = "1",
    dependencies = "required-after:gtnhextranerf;required-after:horizonqa")
public final class IntegrationFixtures {

    @Mod.EventHandler
    public void prepare(FMLPreInitializationEvent event) {
        MaterialIntegrationTests.prepareAlternatives();
    }
}
