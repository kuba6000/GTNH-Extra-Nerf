package pl.kuba6000.gtnhextranerf.integration;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import pl.kuba6000.gtnhextranerf.GTNHExtraNerf;

/** Exercises the reduced deobfuscated development environment. */
@GameTestHolder(value = "gtnhextranerfdevtests", clientOnly = true)
public final class BrainrotterDevelopmentTests {

    @GameTest(template = "native_fixture", timeoutTicks = 1200)
    public static void reducedRuntime(GameTestHelper helper) {
        NativeScenarios.scenario(helper, "reduced runtime Brainrotter", true, () -> {
            if (!GTNHExtraNerf.isDevelopmentEnvironment()) throw new AssertionError("Expected a deobfuscated runtime");
            BrainrotterIntegrationTests.verifyDevelopment(helper.getWorld());
        });
    }
}
