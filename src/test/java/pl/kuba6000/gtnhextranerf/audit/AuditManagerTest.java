package pl.kuba6000.gtnhextranerf.audit;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;

import org.junit.jupiter.api.Test;

class AuditManagerTest {

    @Test
    void actionsAfterFinishStillRunWithoutChangingTheFinishedReport() {
        AuditScope scope = AuditScope.EXPAND_ASSEMBLER_OUTPUTS;
        AuditSession startup = new AuditSession(Collections.singletonList(scope));
        assertSame(startup, AuditManager.target(startup));
        startup.run(scope, AuditHandle::recipeModified);
        AuditReport report = startup.finish();

        // A late OreDictionary registration reaching a registration-time mixin.
        assertEquals(
            42,
            (int) AuditManager.target(startup)
                .auditResult(scope, handle -> {
                    handle.recipeModified();
                    return 42;
                }));
        assertSame(report, startup.finish());
        assertEquals(
            1,
            report.result(scope.id())
                .counts()
                .modified());
        assertEquals(
            1,
            report.result(scope.id())
                .successfulRuns());
    }
}
