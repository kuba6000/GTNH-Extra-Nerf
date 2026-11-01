package pl.kuba6000.gtnhextranerf.progression.exam;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

import net.minecraft.network.PacketBuffer;

import com.cleanroommc.modularui.factory.GuiManager;
import com.cleanroommc.modularui.factory.SimpleGuiFactory;

import io.netty.buffer.Unpooled;
import pl.kuba6000.gtnhextranerf.GTNHExtraNerf;
import pl.kuba6000.gtnhextranerf.audit.AuditManager;
import pl.kuba6000.gtnhextranerf.audit.AuditScope;
import pl.kuba6000.gtnhextranerf.progression.ProgressionTier;

/** Common registration. Clients display the server projection instead of using their local bank. */
public final class CertificationExams {

    public static final String FACTORY_ID = "gtnhextranerf.lv_exam";
    private static final Map<ProgressionTier, ExamDefinition> DEFINITIONS = new EnumMap<>(ProgressionTier.class);
    private static final Map<ProgressionTier, SimpleGuiFactory> FACTORIES = new EnumMap<>(ProgressionTier.class);

    private CertificationExams() {}

    public static void register(File configDirectory) {
        if (!FACTORIES.isEmpty()) throw new IllegalStateException("Certification exams already registered");
        for (ProgressionTier tier : ProgressionTier.values()) register(configDirectory, tier);
    }

    private static void register(File configDirectory, ProgressionTier tier) {
        if (!tier.hasExam()) {
            FACTORIES.put(tier, new SimpleGuiFactory(factoryId(tier), new ExamGui(tier, null)));
            return;
        }
        ExamDefinition definition = BundledExams.of(tier);
        // Exams used to be copied to the configuration directory; they are part of the mod now.
        Path obsolete = configDirectory.toPath()
            .resolve(GTNHExtraNerf.MODID)
            .resolve(tier.id() + "-exam.json");
        if (Files.exists(obsolete)) GTNHExtraNerf.LOG.info(
            "Ignoring {}: the {} exam is part of the mod and no longer read from the configuration",
            obsolete,
            tier.id());
        DEFINITIONS.put(tier, definition);
        FACTORIES.put(tier, new SimpleGuiFactory(factoryId(tier), new ExamGui(definition)));
    }

    public static ExamDefinition definition() {
        return definition(ProgressionTier.LV);
    }

    public static SimpleGuiFactory factory() {
        return factory(ProgressionTier.LV);
    }

    public static ExamDefinition definition(ProgressionTier tier) {
        return Objects.requireNonNull(DEFINITIONS.get(tier), "Exam has not been registered: " + tier);
    }

    public static SimpleGuiFactory factory(ProgressionTier tier) {
        return Objects.requireNonNull(FACTORIES.get(tier), "Exam has not been registered: " + tier);
    }

    private static String factoryId(ProgressionTier tier) {
        return GTNHExtraNerf.MODID + "." + tier.id() + "_exam";
    }

    public static void verify() {
        AuditManager.run(AuditScope.VERIFY_CERTIFICATION_EXAM, audit -> {
            for (ProgressionTier tier : ProgressionTier.values()) {
                if (GuiManager.getFactory(factoryId(tier)) != factory(tier))
                    throw new IllegalStateException(tier.id() + " exam GUI factory is missing");
                if (!tier.hasExam()) continue;
                PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
                try {
                    ExamProtocol.writePaper(buffer, definition(tier).paper());
                    ExamProtocol.readPaper(buffer);
                } catch (IOException failure) {
                    throw new IllegalStateException(tier.id() + " exam cannot be synchronized", failure);
                } finally {
                    buffer.release();
                }
            }
            audit.success();
        });
    }
}
