package pl.kuba6000.gtnhextranerf.audit;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Static bridge required by lifecycle/mixin entry points. All state belongs to one session. Registration-time mixins
 * can still run after load-complete (GregTech processes late OreDictionary registrations immediately), so once the
 * startup session is finished, actions run against a throwaway session and never change the finished report.
 */
public final class AuditManager {

    private static final AuditSession SESSION = new AuditSession(Arrays.asList(AuditScope.values()));

    private AuditManager() {}

    public static AuditSession session() {
        return SESSION;
    }

    public static void run(AuditScope scope, Consumer<AuditHandle> action) {
        target(SESSION).run(scope, action);
    }

    public static <T> T auditResult(AuditScope scope, Function<AuditHandle, T> action) {
        return target(SESSION).auditResult(scope, action);
    }

    static AuditSession target(AuditSession startup) {
        return startup.isFinished() ? new AuditSession(Arrays.asList(AuditScope.values())) : startup;
    }

    public static AuditReport finish() {
        return SESSION.finish();
    }
}
